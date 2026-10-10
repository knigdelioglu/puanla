package com.knigdelioglu.puanla.data.local

import kotlinx.serialization.Serializable
import com.knigdelioglu.puanla.domain.CriterionScore
import com.knigdelioglu.puanla.domain.summarizeScores

/**
 * Full offline backup. All nine tables, not only class rosters.
 * Import is strictly validated BEFORE database mutation.
 */
@Serializable
data class BackupSnapshot(
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val classrooms: List<ClassroomEntity>,
    val students: List<StudentEntity>,
    val rubrics: List<RubricEntity>,
    val criteria: List<CriterionEntity>,
    val levels: List<CriterionLevelEntity>,
    val assessments: List<AssessmentEntity>,
    val scores: List<CriterionScoreEntity>,
    val groups: List<GroupTaskEntity>,
    val auditLogs: List<AuditLogEntity>
) {
    fun validate() {
        require(version == 1) { "Desteklenmeyen yedek sürümü: $version" }
        fun <T> unique(list: List<T>, key: (T) -> String) {
            require(list.map(key).distinct().size == list.size) { "Yedekte yinelenen kayıt kimliği var." }
        }
        unique(classrooms) { it.id }
        unique(students) { it.id }
        unique(rubrics) { it.id }
        unique(criteria) { it.id }
        unique(levels) { it.id }
        unique(assessments) { it.id }
        unique(scores) { it.id }
        unique(groups) { it.id }
        unique(auditLogs) { it.id }
        require(classrooms.all { it.grade in 9..12 && it.section.isNotBlank() && it.academicYear.isNotBlank() })
        unique(classrooms) { "${it.grade}/${it.section}" } // matches current Room uniqueness
        val classById = classrooms.associateBy { it.id }
        val studentById = students.associateBy { it.id }
        val rubricById = rubrics.associateBy { it.id }
        val criterionById = criteria.associateBy { it.id }
        val assessmentById = assessments.associateBy { it.id }
        require(students.all {
            classById.containsKey(it.classroomId) && it.studentNumber.isNotBlank() &&
                it.firstName.isNotBlank() && it.lastName.isNotBlank()
        }) { "Öğrenci bilgileri geçersiz." }
        unique(students) { "${it.classroomId}/${it.studentNumber}" }
        require(rubrics.all { it.grade in 9..12 })
        require(criteria.all { rubricById.containsKey(it.rubricId) && it.maxPoints >= 0 })
        require(levels.all { (criterionById[it.criterionId]?.maxPoints ?: -1) >= it.points && it.points >= 0 })
        unique(levels) { "${it.criterionId}/${it.orderIndex}" }
        unique(criteria) { "${it.rubricId}/${it.orderIndex}" }
        require(assessments.all {
            val clazz = classById[it.classroomId]
            val student = studentById[it.studentId]
            val rubric = rubricById[it.rubricId]
            clazz != null && student?.classroomId == clazz.id && rubric?.grade == clazz.grade
        }) { "Değerlendirme sınıf, öğrenci veya rubrik ilişkisi geçersiz." }
        unique(assessments) { "${it.studentId}/${it.rubricId}" }
        require(scores.all {
            val assessment = assessmentById[it.assessmentId]
            val criterion = criterionById[it.criterionId]
            assessment != null && criterion?.rubricId == assessment.rubricId &&
                (it.points == null || it.points in 0..criterion.maxPoints)
        }) { "Ölçüt puanı veya ilişkisi geçersiz." }
        unique(scores) { "${it.assessmentId}/${it.criterionId}" }
        for (assessment in assessments) {
            val rubricCriteria = criteria.filter { it.rubricId == assessment.rubricId }
            val scoreMap = scores.filter { it.assessmentId == assessment.id }.associate { it.criterionId to it.points }
            val summary = summarizeScores(rubricCriteria.map { CriterionScore(it.maxPoints, scoreMap[it.id]) })
            require(assessment.isCompleted == summary.completed &&
                assessment.definitiveTotal == summary.definitiveTotal &&
                assessment.scoredCount == summary.scoredCount
            ) { "Yedekte değerlendirme toplamı veya tamamlanma durumu tutarsız." }
        }
        require(groups.all {
            classById.containsKey(it.classroomId) &&
                rubricById[it.rubricId]?.grade == classById[it.classroomId]?.grade
        }) { "Grup ilişki bilgileri geçersiz." }
    }
}
