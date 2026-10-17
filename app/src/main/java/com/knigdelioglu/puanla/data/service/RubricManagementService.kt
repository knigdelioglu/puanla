package com.knigdelioglu.puanla.data.service

import androidx.room.withTransaction
import com.knigdelioglu.puanla.data.local.AuditLogEntity
import com.knigdelioglu.puanla.data.local.CriterionEntity
import com.knigdelioglu.puanla.data.local.CriterionLevelEntity
import com.knigdelioglu.puanla.data.local.PuanlaDatabase
import com.knigdelioglu.puanla.data.local.RubricEntity
import com.knigdelioglu.puanla.data.local.RubricSummary
import com.knigdelioglu.puanla.domain.rubric.RubricCriterionDraft
import com.knigdelioglu.puanla.domain.rubric.RubricDraft
import com.knigdelioglu.puanla.domain.rubric.RubricDraftRules
import com.knigdelioglu.puanla.domain.rubric.RubricLevelDraft
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.UUID

data class RubricDetails(
    val rubric: RubricEntity,
    val criteria: List<CriterionEntity>,
    val levels: List<CriterionLevelEntity>,
    val usageCount: Int
)

/** Owns rubric CRUD, validation and the history-preservation boundary. */
class RubricManagementService(private val db: PuanlaDatabase) {
    private val rubricDao = db.rubricDao()

    fun observeSummaries(): Flow<List<RubricSummary>> = rubricDao.getRubricSummariesFlow()

    fun observeDetails(rubricId: String): Flow<RubricDetails?> = combine(
        rubricDao.observeRubric(rubricId),
        rubricDao.getCriteriaForRubricFlow(rubricId),
        rubricDao.getLevelsForRubricFlow(rubricId),
        rubricDao.observeRubricUsage(rubricId)
    ) { rubric, criteria, levels, usageCount ->
        rubric?.let { RubricDetails(it, criteria, levels, usageCount) }
    }

    suspend fun getDetails(rubricId: String): RubricDetails {
        val rubric = requireNotNull(rubricDao.getRubricById(rubricId)) { "Rubrik bulunamadı." }
        return RubricDetails(
            rubric = rubric,
            criteria = rubricDao.getCriteriaForRubric(rubricId),
            levels = rubricDao.getLevelsForRubric(rubricId),
            usageCount = rubricDao.countRubricUsage(rubricId)
        )
    }

    suspend fun save(draft: RubricDraft, existingRubricId: String? = null): RubricEntity = db.withTransaction {
        RubricDraftRules.validate(draft)
        val existing = existingRubricId?.let {
            requireNotNull(rubricDao.getRubricById(it)) { "Düzenlenecek rubrik bulunamadı." }
        }
        val previousCriteria = existing?.let { rubricDao.getCriteriaForRubric(it.id).associateBy(CriterionEntity::id) }
            .orEmpty()
        require(draft.criteria.mapNotNull(RubricCriterionDraft::id).all { it in previousCriteria }) {
            "Ölçüt kimliği başka bir rubriğe ait."
        }
        if (existing != null) {
            require(rubricDao.countRubricUsage(existing.id) == 0) {
                "Bu rubrik değerlendirmelerde kullanılmış. Geçmiş puanları korumak için çoğaltıp yeni sürümü düzenleyin."
            }
        } else {
            require(draft.criteria.none { it.id != null }) { "Yeni rubrik ölçütleri yeni kimliklerle oluşturulmalıdır." }
        }

        val rubricId = existing?.id ?: UUID.randomUUID().toString()
        val rubric = RubricEntity(
            id = rubricId,
            title = draft.title.trim(),
            description = draft.description.trim(),
            grade = draft.grade,
            targetTask = draft.subject.trim(),
            isBuiltIn = false,
            createdAt = existing?.createdAt ?: System.currentTimeMillis()
        )
        rubricDao.insertRubric(rubric)
        // Editing is allowed only before use, so rebuilding criteria cannot detach a score.
        rubricDao.deleteCriteriaForRubric(rubricId)
        val criteria = draft.criteria.mapIndexed { index, item ->
            val criterionId = item.id ?: UUID.randomUUID().toString()
            CriterionEntity(
                id = criterionId,
                rubricId = rubricId,
                title = item.title.trim(),
                description = item.category.trim(),
                maxPoints = item.maxPoints,
                orderIndex = index
            )
        }
        rubricDao.insertCriteria(criteria)
        rubricDao.insertLevels(draft.criteria.zip(criteria).flatMap { (item, criterion) ->
            item.levels.mapIndexed { index, level ->
                CriterionLevelEntity(
                    id = UUID.randomUUID().toString(),
                    criterionId = criterion.id,
                    points = level.points,
                    description = level.description.trim(),
                    orderIndex = index
                )
            }
        })
        db.auditLogDao().insertLog(
            AuditLogEntity(
                id = UUID.randomUUID().toString(),
                action = if (existing == null) "RUBRIC_CREATED" else "RUBRIC_UPDATED",
                details = rubric.title
            )
        )
        rubric
    }

    suspend fun duplicate(rubricId: String): RubricEntity {
        return db.withTransaction {
            val source = getDetails(rubricId)
            val duplicate = source.rubric.copy(
                id = UUID.randomUUID().toString(),
                title = "${source.rubric.title} (Kopya)",
                isBuiltIn = false,
                createdAt = System.currentTimeMillis()
            )
            rubricDao.insertRubric(duplicate)
            val criterionIds = source.criteria.associate { it.id to UUID.randomUUID().toString() }
            rubricDao.insertCriteria(source.criteria.map { criterion ->
                criterion.copy(id = requireNotNull(criterionIds[criterion.id]), rubricId = duplicate.id)
            })
            val copiedLevels = source.levels.map { level ->
                level.copy(
                    id = UUID.randomUUID().toString(),
                    criterionId = requireNotNull(criterionIds[level.criterionId])
                )
            }
            if (copiedLevels.isNotEmpty()) rubricDao.insertLevels(copiedLevels)
            db.auditLogDao().insertLog(
                AuditLogEntity(
                    id = UUID.randomUUID().toString(),
                    action = "RUBRIC_DUPLICATED",
                    details = "${source.rubric.title} → ${duplicate.title}"
                )
            )
            duplicate
        }
    }

    suspend fun delete(rubricId: String) = db.withTransaction {
        val rubric = requireNotNull(rubricDao.getRubricById(rubricId)) { "Rubrik bulunamadı." }
        require(!rubric.isBuiltIn) { "Kaynak rubrik silinemez. Kaynak rubriği korumak için kopyasını silebilirsiniz." }
        require(rubricDao.countRubricUsage(rubricId) == 0) {
            "Bu rubriğe bağlı değerlendirme geçmişi var; veri kaybını önlemek için silinemez."
        }
        rubricDao.deleteRubricById(rubricId)
        db.auditLogDao().insertLog(
            AuditLogEntity(
                id = UUID.randomUUID().toString(),
                action = "RUBRIC_DELETED",
                details = rubric.title
            )
        )
    }

}
