package com.knigdelioglu.puanla.data.repository

import com.knigdelioglu.puanla.data.local.*
import com.knigdelioglu.puanla.data.local.AssessmentEntity
import com.knigdelioglu.puanla.data.local.AuditLogEntity
import com.knigdelioglu.puanla.data.local.ClassroomEntity
import com.knigdelioglu.puanla.data.local.CriterionEntity
import com.knigdelioglu.puanla.data.local.CriterionScoreEntity
import com.knigdelioglu.puanla.data.local.GroupTaskEntity
import com.knigdelioglu.puanla.data.local.PuanlaDatabase
import com.knigdelioglu.puanla.data.local.RubricEntity
import com.knigdelioglu.puanla.data.local.RubricCatalog
import com.knigdelioglu.puanla.data.local.RubricCatalogParser
import com.knigdelioglu.puanla.data.local.RubricSeeder
import com.knigdelioglu.puanla.data.local.toCriterionEntities
import com.knigdelioglu.puanla.data.local.toEntity
import com.knigdelioglu.puanla.data.local.StudentEntity
import com.knigdelioglu.puanla.domain.export.CsvCriterion
import com.knigdelioglu.puanla.domain.export.CsvStudent
import com.knigdelioglu.puanla.domain.export.CsvExportFormatter
import com.knigdelioglu.puanla.domain.export.PdfReportSnapshot
import com.knigdelioglu.puanla.domain.CriterionScore
import com.knigdelioglu.puanla.domain.summarizeScores
import androidx.room.withTransaction
import com.knigdelioglu.puanla.data.local.BackupSnapshot
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import com.knigdelioglu.puanla.domain.student.StudentNameRules
import com.knigdelioglu.puanla.domain.group.GroupChecklistRules
import com.knigdelioglu.puanla.domain.group.GroupPartitioner

class PuanlaRepository(
    private val db: PuanlaDatabase,
    private val rubricCatalogSource: suspend () -> String
) {

    private val classroomDao = db.classroomDao()
    private val studentDao = db.studentDao()
    private val rubricDao = db.rubricDao()
    private val assessmentDao = db.assessmentDao()
    private val runDao = db.assessmentRunDao()
    private val groupTaskDao = db.groupTaskDao()
    private val auditLogDao = db.auditLogDao()

    private val rubricCatalogMutex = Mutex()
    @Volatile private var rubricCatalog: RubricCatalog? = null

    private suspend fun loadRubricCatalog(): RubricCatalog = rubricCatalogMutex.withLock {
        rubricCatalog ?: withContext(Dispatchers.IO) {
            RubricCatalogParser.parse(rubricCatalogSource())
        }.also { rubricCatalog = it }
    }

    suspend fun initialize() {
        val catalog = loadRubricCatalog()
        db.withTransaction {
            catalog.rubrics.forEach { definition ->
                if (rubricDao.getRubricById(definition.id) == null) {
                    rubricDao.insertRubricIfAbsent(definition.toEntity())
                    rubricDao.insertCriteriaFromApprovedSource(definition.toCriterionEntities())
                }
            }
            // Freeze migrated v2 attempts before any future template edits.
            runDao.runsMissingCriterionSnapshots().forEach { run ->
                val savedCriteria = rubricDao.getCriteriaForRubric(run.rubricId)
                if (savedCriteria.isNotEmpty()) {
                    runDao.update(run.copy(criteriaSnapshotJson = Json.encodeToString(savedCriteria)))
                }
            }
        }
    }

    // Classrooms
    fun getAllClassroomsFlow(): Flow<List<ClassroomEntity>> = classroomDao.getAllClassroomsFlow()

    suspend fun getAllClassrooms(): List<ClassroomEntity> = classroomDao.getAllClassrooms()

    suspend fun getClassroomById(id: String): ClassroomEntity? = classroomDao.getClassroomById(id)

    suspend fun createClassroom(grade: Int, section: String, academicYear: String = "2026-2027"): ClassroomEntity {
        val upperSection = section.trim().uppercase()
        val id = "class_${grade}_${upperSection}"
        val entity = ClassroomEntity(
            id = id,
            grade = grade,
            section = upperSection,
            name = "$grade-$upperSection",
            academicYear = academicYear
        )
        classroomDao.insertClassroom(entity)
        auditLogDao.insertLog(
            AuditLogEntity(
                id = UUID.randomUUID().toString(),
                action = "CREATE_CLASSROOM",
                details = "Sınıf oluşturuldu: ${entity.name}"
            )
        )
        return entity
    }

    suspend fun deleteClassroom(classroom: ClassroomEntity) = db.withTransaction {
        require(
            studentDao.countForClassroom(classroom.id) == 0 &&
            db.groupAssessmentDao().countSessionsForClassroom(classroom.id) == 0 &&
                groupTaskDao.getGroupTasksFlow(classroom.id).first().isEmpty() &&
                runDao.countAssignmentsForClassroom(classroom.id) == 0
        ) {
            "Bu sınıfta öğrenci, değerlendirme veya grup kaydı var. Veri kaybını önlemek için sınıf silinemez."
        }
        classroomDao.deleteClassroom(classroom)
        auditLogDao.insertLog(
            AuditLogEntity(
                id = UUID.randomUUID().toString(),
                action = "DELETE_CLASSROOM",
                details = "Sınıf silindi: ${classroom.name}"
            )
        )
    }

    suspend fun updateClassroomDetails(
        classroom: ClassroomEntity,
        section: String,
        academicYear: String
    ): ClassroomEntity = db.withTransaction {
        val current = requireNotNull(classroomDao.getClassroomById(classroom.id)) { "Sınıf bulunamadı." }
        val normalizedSection = section.trim().uppercase()
        require(normalizedSection.isNotBlank()) { "Şube adı boş olamaz." }
        require(academicYear.isNotBlank()) { "Öğretim yılı boş olamaz." }
        require(classroomDao.getAllClassrooms().none {
            it.id != current.id && it.grade == current.grade && it.section.equals(normalizedSection, ignoreCase = true)
        }) { "${current.grade}-$normalizedSection sınıfı zaten mevcut." }
        val updated = current.copy(
            section = normalizedSection,
            name = "${current.grade}-$normalizedSection",
            academicYear = academicYear.trim()
        )
        classroomDao.updateClassroom(updated)
        auditLogDao.insertLog(AuditLogEntity(
            id = UUID.randomUUID().toString(),
            action = "UPDATE_CLASSROOM",
            details = "Sınıf bilgileri güncellendi: ${current.name} → ${updated.name}"
        ))
        updated
    }

    // Students
    fun getStudentsForClassroomFlow(classroomId: String): Flow<List<StudentEntity>> =
        studentDao.getStudentsForClassroomFlow(classroomId)

    suspend fun getStudentsForClassroom(classroomId: String): List<StudentEntity> =
        studentDao.getStudentsForClassroom(classroomId)

    suspend fun addStudent(
        classroomId: String,
        studentNumber: String,
        firstName: String,
        lastName: String,
        gender: String? = null,
        boardingStatus: String? = null
    ): Result<StudentEntity> {
        return try {
            val trimmedNum = studentNumber.trim()
            require(trimmedNum.isNotEmpty() && trimmedNum.all(Char::isDigit)) {
                "Öğrenci numarası yalnızca rakamlardan oluşmalı."
            }
            val student = StudentEntity(
                id = UUID.randomUUID().toString(),
                classroomId = classroomId,
                studentNumber = trimmedNum,
                firstName = StudentNameRules.firstName(firstName),
                lastName = StudentNameRules.lastName(lastName),
                gender = gender?.trim(),
                boardingStatus = boardingStatus?.trim()
            )
            // Both the duplicate check and insert must be part of one transaction;
            // otherwise two near-simultaneous submissions can both pass the check.
            db.withTransaction {
                require(classroomDao.getClassroomById(classroomId) != null) { "Sınıf bulunamadı." }
                require(studentDao.getStudentByNumber(classroomId, trimmedNum) == null) {
                    "Bu okul numarasına ($trimmedNum) sahip öğrenci zaten mevcut."
                }
                studentDao.insertStudent(student)
                auditLogDao.insertLog(AuditLogEntity(
                    id = UUID.randomUUID().toString(), action = "STUDENT_ADDED",
                    details = "${student.studentNumber} - ${student.firstName} ${student.lastName} ($classroomId)"
                ))
            }
            Result.success(student)
        } catch (cancel: kotlinx.coroutines.CancellationException) {
            throw cancel
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addStudents(students: List<StudentEntity>) = db.withTransaction {
        require(students.isNotEmpty()) { "Aktarılacak onaylı öğrenci yok." }
        require(students.all {
            it.studentNumber.isNotBlank() && it.studentNumber.all(Char::isDigit) &&
                it.firstName.isNotBlank() && it.lastName.isNotBlank()
        }) { "Eksik veya geçersiz öğrenci numarası/ad-soyad var." }
        students.forEach {
            StudentNameRules.firstName(it.firstName)
            StudentNameRules.lastName(it.lastName)
        }
        require(students.map { "${it.classroomId}/${it.studentNumber}" }.distinct().size == students.size) {
            "Aktarım listesindeki okul numaraları tekrar ediyor."
        }
        for (student in students) {
            require(classroomDao.getClassroomById(student.classroomId) != null) { "Sınıf bulunamadı." }
            require(studentDao.getStudentByNumber(student.classroomId, student.studentNumber) == null) {
                "Bu okul numarası sınıfta zaten kayıtlı: ${student.studentNumber}"
            }
        }
        studentDao.insertStudents(students)
        auditLogDao.insertLog(
            AuditLogEntity(id = UUID.randomUUID().toString(), action = "BULK_IMPORT_STUDENTS",
                details = "${students.size} onaylanmış öğrenci aktarıldı.")
        )
    }

    suspend fun updateStudent(student: StudentEntity) = db.withTransaction {
        val stored = requireNotNull(studentDao.getStudentById(student.id)) { "Öğrenci bulunamadı." }
        require(stored.classroomId == student.classroomId && stored.studentNumber == student.studentNumber) {
            "Sınıf veya okul numarası isim düzeltmesiyle değiştirilemez."
        }
        val updated = stored.copy(
            firstName = StudentNameRules.firstName(student.firstName),
            lastName = StudentNameRules.lastName(student.lastName)
        )
        if (updated != stored) {
            studentDao.updateStudent(updated)
            auditLogDao.insertLog(AuditLogEntity(
                id = UUID.randomUUID().toString(), action = "STUDENT_UPDATED",
                details = "Öğrenci no ${stored.studentNumber} (${stored.classroomId})"
            ))
        }
    }

    /** Only one name field changes; overlapping inline edits cannot lose updates. */
    suspend fun updateStudentName(studentId: String, firstName: String? = null, lastName: String? = null) = db.withTransaction {
        require((firstName == null) != (lastName == null)) { "Yalnız bir isim alanı güncellenebilir." }
        val stored = requireNotNull(studentDao.getStudentById(studentId)) { "Öğrenci bulunamadı." }
        val updated = stored.copy(
            firstName = firstName?.let(StudentNameRules::firstName) ?: stored.firstName,
            lastName = lastName?.let(StudentNameRules::lastName) ?: stored.lastName
        )
        if (updated != stored) {
            studentDao.updateStudent(updated)
            auditLogDao.insertLog(AuditLogEntity(
                id = UUID.randomUUID().toString(), action = "STUDENT_UPDATED",
                details = "Öğrenci no ${stored.studentNumber} (${stored.classroomId})"
            ))
        }
    }

    suspend fun deleteStudent(student: StudentEntity) = db.withTransaction {
        require(db.groupAssessmentDao().countResultsForStudent(student.id) == 0) {
            "Bu öğrencinin grup değerlendirme notu var. Önce not geçmişini güvenli biçimde arşivleyin."
        }
        studentDao.deleteStudent(student)
        auditLogDao.insertLog(AuditLogEntity(
            id = UUID.randomUUID().toString(), action = "STUDENT_DELETED",
            details = "${student.studentNumber} - ${student.firstName} ${student.lastName} (${student.classroomId})"
        ))
    }

    // Rubrics
    fun getAllRubricsFlow(): Flow<List<RubricEntity>> = rubricDao.getAllRubricsFlow().map { all ->
        // Do not delete prior user data; hide the eight fabricated legacy rubrics from grading.
        all.filterNot { it.id in RubricSeeder.unverifiedLegacyIds }
    }

    suspend fun getCriteriaForRubric(rubricId: String): List<CriterionEntity> =
        rubricDao.getCriteriaForRubric(rubricId)

    suspend fun getGroupPreparationChecklist(rubricId: String): List<String> =
        loadRubricCatalog().rubrics.firstOrNull { it.id == rubricId }
            ?.groupPreparationChecklist?.items.orEmpty()

    fun getCriteriaForRubricFlow(rubricId: String): Flow<List<CriterionEntity>> =
        rubricDao.getCriteriaForRubricFlow(rubricId)

    // Distinct evaluation runs reuse the source rubric but never share marks.
    fun observeRunsForClassroom(classroomId: String, rubricId: String): Flow<List<AssessmentRunEntity>> =
        runDao.observeForClassroom(classroomId, rubricId)

    fun observeRuns(): Flow<List<AssessmentRunEntity>> = runDao.observeAll()

    fun observeRunProgress(runId: String): Flow<List<RunClassroomProgress>> = runDao.observeProgress(runId)

    suspend fun getRun(runId: String): AssessmentRunEntity? = runDao.get(runId)

    suspend fun getRunClassrooms(runId: String): List<RunClassroomEntity> = runDao.getAssignments(runId)

    suspend fun getCriteriaForRun(run: AssessmentRunEntity): List<CriterionEntity> {
        return if (run.criteriaSnapshotJson.isBlank()) {
            rubricDao.getCriteriaForRubric(run.rubricId)
        } else {
            Json.decodeFromString<List<CriterionEntity>>(run.criteriaSnapshotJson)
        }
    }

    suspend fun createAssessmentRun(
        rubricId: String, title: String, classroomIds: List<String>
    ): AssessmentRunEntity = db.withTransaction {
        require(title.trim().isNotEmpty()) { "Değerlendirme adı boş olamaz." }
        val ids = classroomIds.distinct()
        require(ids.isNotEmpty()) { "En az bir sınıf seçin." }
        val rubric = requireNotNull(rubricDao.getRubricById(rubricId)) { "Rubrik bulunamadı." }
        require(rubric.id !in RubricSeeder.unverifiedLegacyIds) { "Doğrulanmamış rubrik kullanılamaz." }
        require(ids.all { id -> classroomDao.getClassroomById(id)?.grade == rubric.grade }) {
            "Sınıflar rubrik düzeyine uygun değil."
        }
        val criteria = rubricDao.getCriteriaForRubric(rubricId)
        require(criteria.isNotEmpty()) { "Rubrikte ölçüt bulunamadı." }
        val run = AssessmentRunEntity(
            id = UUID.randomUUID().toString(),
            rubricId = rubricId,
            title = title.trim(),
            criteriaSnapshotJson = Json.encodeToString(criteria)
        )
        runDao.insert(run)
        runDao.assign(ids.map { RunClassroomEntity(UUID.randomUUID().toString(), run.id, it) })
        run
    }

    suspend fun addClassroomsToRun(runId: String, classroomIds: List<String>) = db.withTransaction {
        val run = requireNotNull(runDao.get(runId)) { "Değerlendirme bulunamadı." }
        require(run.status == "ACTIVE") { "Arşivlenmiş değerlendirme düzenlenemez." }
        val rubric = requireNotNull(rubricDao.getRubricById(run.rubricId))
        val ids = classroomIds.distinct()
        require(ids.all { id -> classroomDao.getClassroomById(id)?.grade == rubric.grade }) {
            "Sınıf rubrik düzeyine uygun değil."
        }
        runDao.assign(ids.map { RunClassroomEntity(UUID.randomUUID().toString(), runId, it) })
    }

    suspend fun archiveRun(runId: String, archived: Boolean) = db.withTransaction {
        val run = requireNotNull(runDao.get(runId))
        runDao.update(run.copy(status = if (archived) "ARCHIVED" else "ACTIVE"))
    }

    private suspend fun ensureLegacyRun(classroomId: String, rubricId: String): String {
        val id = "legacy_${classroomId}_${rubricId}"
        val historicalCriteria = rubricDao.getCriteriaForRubric(rubricId)
        runDao.insertIfAbsent(AssessmentRunEntity(
            id, rubricId, "Önceki değerlendirme",
            criteriaSnapshotJson = Json.encodeToString(historicalCriteria),
            status = "ACTIVE", createdAt = 0
        ))
        runDao.assign(listOf(RunClassroomEntity("assign_$id", id, classroomId)))
        return id
    }

    fun getRunAssessmentsFlow(classroomId: String, runId: String): Flow<List<AssessmentEntity>> =
        assessmentDao.observeForRun(classroomId, runId)

    suspend fun getRunAssessmentForStudent(studentId: String, runId: String): AssessmentEntity? =
        assessmentDao.getStudentInRun(studentId, runId)

    suspend fun getRunCriterionScoresForStudent(studentId: String, runId: String): Map<String, Int?> {
        val assessment = assessmentDao.getStudentInRun(studentId, runId) ?: return emptyMap()
        return assessmentDao.getScoresForAssessment(assessment.id).associate { it.criterionId to it.points }
    }

    // Assessments & Scoring
    fun getAssessmentsFlow(classroomId: String, rubricId: String): Flow<List<AssessmentEntity>> =
        assessmentDao.getAssessmentsFlow(classroomId, rubricId)

    suspend fun getAssessmentForStudent(studentId: String, rubricId: String): AssessmentEntity? =
        assessmentDao.getAssessmentForStudent(studentId, rubricId)

    suspend fun getCriterionScoresForStudent(studentId: String, rubricId: String): Map<String, Int?> {
        val assessment = assessmentDao.getAssessmentForStudent(studentId, rubricId) ?: return emptyMap()
        return assessmentDao.getScoresForAssessment(assessment.id).associate { it.criterionId to it.points }
    }

    fun getAssessmentForStudentFlow(studentId: String, rubricId: String): Flow<AssessmentEntity?> =
        assessmentDao.getAssessmentForStudentFlow(studentId, rubricId)

    fun getScoresForAssessmentFlow(assessmentId: String): Flow<List<CriterionScoreEntity>> =
        assessmentDao.getScoresForAssessmentFlow(assessmentId)

    suspend fun getScoresForAssessment(assessmentId: String): List<CriterionScoreEntity> =
        assessmentDao.getScoresForAssessment(assessmentId)

    /**
     * A single atomic write: validate the exact classroom/student/rubric/criterion,
     * record the score, then update its summary without REPLACE deleting child rows.
     * Returns the score that existed before this change for identity-safe undo.
     */
    suspend fun saveCriterionScore(
        classroomId: String,
        studentId: String,
        rubricId: String,
        criterionId: String,
        scorePoints: Int?,
        evidenceNote: String? = null,
        runId: String? = null
    ): CriterionScoreEntity? = db.withTransaction {
        val classroom = requireNotNull(classroomDao.getClassroomById(classroomId)) { "Sınıf bulunamadı." }
        val student = requireNotNull(studentDao.getStudentById(studentId)) { "Öğrenci bulunamadı." }
        require(student.classroomId == classroom.id) { "Öğrenci farklı sınıfa ait." }
        require(rubricId !in RubricSeeder.unverifiedLegacyIds) { "Bu rubriğin kaynağı doğrulanmamış; puanlama engellendi." }
        val rubric = requireNotNull(rubricDao.getRubricById(rubricId)) { "Rubrik bulunamadı." }
        require(rubric.grade == classroom.grade) { "Rubrik sınıf düzeyine uygun değil." }
        val criterion = requireNotNull(rubricDao.getCriterionById(criterionId)) { "Ölçüt bulunamadı." }
        require(criterion.rubricId == rubric.id) { "Ölçüt başka rubriğe ait." }
        val actualRunId = runId ?: ensureLegacyRun(classroomId, rubricId)
        val run = requireNotNull(runDao.get(actualRunId)) { "Değerlendirme oturumu bulunamadı." }
        require(run.rubricId == rubricId && runDao.containsClassroom(actualRunId, classroomId)) {
            "Bu sınıf değerlendirmeye bağlı değil."
        }
        require(run.status == "ACTIVE") { "Arşivlenmiş değerlendirme düzenlenemez." }
        val frozenCriteria = getCriteriaForRun(run)
        val frozenCriterion = requireNotNull(frozenCriteria.firstOrNull { it.id == criterionId }) {
            "Ölçüt bu değerlendirmeye ait değil."
        }
        require(scorePoints == null || scorePoints in 0..frozenCriterion.maxPoints) {
            "Puan geçerli ölçüt aralığında değil."
        }
        val existing = assessmentDao.getStudentInRun(studentId, actualRunId)
        val assessment = existing ?: AssessmentEntity(
            id = "assess_${studentId}_${actualRunId}",
            classroomId = classroomId,
            studentId = studentId,
            rubricId = rubricId,
            runId = actualRunId
        )
        require(assessment.classroomId == classroomId) { "Değerlendirme başka sınıfa ait." }
        if (existing == null) assessmentDao.insertOrUpdateAssessment(assessment)
        val scoreId = "score_${assessment.id}_${criterionId}"
        val previous = assessmentDao.getScoresForAssessment(assessment.id).firstOrNull { it.criterionId == criterionId }
        assessmentDao.insertOrUpdateScore(
            CriterionScoreEntity(
                id = previous?.id ?: scoreId,
                assessmentId = assessment.id,
                criterionId = criterionId,
                points = scorePoints,
                evidenceNote = evidenceNote,
                scoredAt = System.currentTimeMillis()
            )
        )
        val criteria = frozenCriteria
        val scoreMap = assessmentDao.getScoresForAssessment(assessment.id).associate { it.criterionId to it.points }
        val summary = summarizeScores(criteria.map { CriterionScore(it.maxPoints, scoreMap[it.id]) })
        assessmentDao.insertOrUpdateAssessment(
            assessment.copy(
                isCompleted = summary.completed,
                definitiveTotal = summary.definitiveTotal,
                scoredCount = summary.scoredCount,
                lastModifiedAt = System.currentTimeMillis()
            )
        )
        if (previous?.points != scorePoints) {
            auditLogDao.insertLog(AuditLogEntity(
                id = UUID.randomUUID().toString(), action = "CRITERION_SCORE_CHANGED",
                details = "${classroom.name}, no ${student.studentNumber}, ${frozenCriterion.title}: " +
                    "${previous?.points?.toString() ?: "Boş"} → ${scorePoints?.toString() ?: "Boş"}"
            ))
        }
        previous
    }

    /**
     * Clear only the requested assessment; a student may have several attempts
     * with the same rubric. Archived attempts are immutable.
     */
    suspend fun clearAssessmentScores(assessmentId: String, studentId: String, rubricId: String) =
        db.withTransaction {
            val assessment = requireNotNull(assessmentDao.getAssessmentById(assessmentId)) {
                "Değerlendirme bulunamadı."
            }
            require(assessment.studentId == studentId && assessment.rubricId == rubricId) {
                "Değerlendirme kimliği uyuşmuyor."
            }
            val run = requireNotNull(runDao.get(assessment.runId)) { "Değerlendirme oturumu yok." }
            require(run.status == "ACTIVE" && run.rubricId == rubricId) {
                "Arşivlenmiş değerlendirme değiştirilemez."
            }
            require(runDao.containsClassroom(run.id, assessment.classroomId)) {
                "Oturum ve sınıf ilişkisi geçersiz."
            }
            assessmentDao.clearScoresForAssessment(assessment.id)
            assessmentDao.insertOrUpdateAssessment(
                assessment.copy(
                    isCompleted = false, definitiveTotal = null, scoredCount = 0,
                    lastModifiedAt = System.currentTimeMillis()
                )
            )
        }

    // Group Tasks
    fun getGroupTasksFlow(classroomId: String): Flow<List<GroupTaskEntity>> =
        groupTaskDao.getGroupTasksFlow(classroomId)

    suspend fun saveGroupTask(task: GroupTaskEntity) = db.withTransaction {
        val members = JSONArray(task.memberStudentIdsJson).let { json ->
            (0 until json.length()).map { json.getString(it) }
        }
        require(members.isNotEmpty() && members.distinct().size == members.size) {
            "Grup üyeleri geçersiz."
        }
        require(members.all { studentDao.getStudentById(it)?.classroomId == task.classroomId }) {
            "Bütün üyeler aynı sınıfa ait olmalı."
        }
        val otherGroups = groupTaskDao.getGroupTasksFlow(task.classroomId).first()
            .filterNot { it.id == task.id }
        val conflict = otherGroups.firstNotNullOfOrNull { other ->
            val json = JSONArray(other.memberStudentIdsJson)
            val overlapping = (0 until json.length()).map { json.getString(it) }
                .firstOrNull { it in members }
            overlapping?.let { it to other.title }
        }
        require(conflict == null) {
            val (studentId, groupTitle) = requireNotNull(conflict)
            val student = studentDao.getStudentById(studentId)
            "${student?.firstName} ${student?.lastName} zaten '${groupTitle}' grubunda."
        }
        groupTaskDao.insertOrUpdateGroupTask(task)
    }

    /**
     * Creates all groups in one transaction from students not already assigned
     * to any classroom group. A stale UI snapshot cannot duplicate membership.
     */
    suspend fun createRandomGroups(classroomId: String, averageSize: Int): List<GroupTaskEntity> =
        db.withTransaction {
            require(averageSize > 0) { "Ortalama grup büyüklüğü pozitif olmalı." }
            requireNotNull(classroomDao.getClassroomById(classroomId)) { "Sınıf bulunamadı." }
            val existing = groupTaskDao.getGroupTasksFlow(classroomId).first()
            val assigned = existing.flatMap { group ->
                val json = JSONArray(group.memberStudentIdsJson)
                (0 until json.length()).map { json.getString(it) }
            }.toSet()
            val available = studentDao.getStudentsForClassroom(classroomId)
                .map { it.id }.filterNot { it in assigned }
            require(available.isNotEmpty()) { "Gruplandırılmamış öğrenci kalmadı." }
            val partitions = GroupPartitioner.distribute(available, averageSize)
            val names = existing.map { it.title }.toMutableSet()
            var number = 1
            partitions.map { memberIds ->
                var title: String
                do { title = "Grup ${number++}" } while (title in names)
                names.add(title)
                GroupTaskEntity(
                    id = UUID.randomUUID().toString(),
                    classroomId = classroomId,
                    rubricId = "",
                    title = title,
                    memberStudentIdsJson = JSONArray(memberIds).toString(),
                    checklistJson = "[]"
                ).also { groupTaskDao.insertOrUpdateGroupTask(it) }
            }
        }

    suspend fun deleteGroupTask(task: GroupTaskEntity) = db.withTransaction {
        require(db.groupAssessmentDao().countAssessmentsForGroup(task.id) == 0) {
            "Bu grubun değerlendirme geçmişi var. Not kaybını önlemek için silinemez."
        }
        groupTaskDao.deleteGroupTask(task)
    }

    /** Atomic read-modify-write of the most recent checklist. */
    suspend fun toggleGroupChecklistItem(taskId: String, index: Int) = db.withTransaction {
        val current = requireNotNull(groupTaskDao.getGroupTaskById(taskId)) { "Grup bulunamadı." }
        groupTaskDao.insertOrUpdateGroupTask(
            current.copy(checklistJson = GroupChecklistRules.toggle(current.checklistJson, index))
        )
    }

    // Audit logs
    fun getRecentLogsFlow(limit: Int = 30): Flow<List<AuditLogEntity>> =
        auditLogDao.getRecentLogsFlow(limit)

    fun getAssessmentActivityFlow(sinceInclusive: Long): Flow<List<AuditLogEntity>> =
        auditLogDao.getAssessmentActivityFlow(sinceInclusive)

    /**
     * Reads one consistent database snapshot and delegates the actual serialization
     * to the same tested writer used in CSV unit tests.
     */
    suspend fun generateCsvExport(classroomId: String, rubricId: String, runId: String? = null): String =
        db.withTransaction {
            val snapshot = buildRunReport(classroomId, rubricId, runId)
            CsvExportFormatter.generate(snapshot.criteria, snapshot.students)
        }

    suspend fun generatePdfReportSnapshot(
        classroomId: String, rubricId: String, runId: String? = null
    ): PdfReportSnapshot = db.withTransaction { buildRunReport(classroomId, rubricId, runId) }

    private suspend fun buildRunReport(
        classroomId: String, rubricId: String, runId: String?
    ): PdfReportSnapshot {
        val classroom = requireNotNull(classroomDao.getClassroomById(classroomId)) { "Sınıf bulunamadı." }
        val rubric = requireNotNull(rubricDao.getRubricById(rubricId)) { "Rubrik bulunamadı." }
        require(rubric.id !in RubricSeeder.unverifiedLegacyIds && rubric.grade == classroom.grade) {
            "Rubrik bu sınıfa uygun değil."
        }
        // A missing run ID is retained for older callers, but can never
        // silently mix repeated attempts of the same rubric.
        val actualRunId = runId ?: runDao.getForClassroom(classroomId, rubricId).let { matches ->
            require(matches.size <= 1) {
                "Bu rubrikle birden fazla değerlendirme var. Çıktı için oturum seçin."
            }
            matches.singleOrNull()?.id
        }
        val run = actualRunId?.let { id ->
            requireNotNull(runDao.get(id)) { "Değerlendirme bulunamadı." }.also {
                require(it.rubricId == rubricId && runDao.containsClassroom(it.id, classroomId)) {
                    "Seçilen değerlendirme bu sınıfa ait değil."
                }
            }
        }
        val criteria = run?.let { getCriteriaForRun(it) } ?: rubricDao.getCriteriaForRubric(rubricId)
        val assessments = if (run != null) {
            assessmentDao.getForRun(classroomId, run.id)
        } else emptyList<AssessmentEntity>()
        val assessmentsByStudent = assessments.associateBy { it.studentId }
        val rows = studentDao.getStudentsForClassroom(classroomId).map { student ->
            val assessment = assessmentsByStudent[student.id]
            val scores = assessment?.let { recorded ->
                assessmentDao.getScoresForAssessment(recorded.id).associate { it.criterionId to it.points }
            }.orEmpty()
            CsvStudent(
                number = student.studentNumber, firstName = student.firstName, lastName = student.lastName,
                scores = scores, definitiveTotal = assessment?.definitiveTotal,
                isCompleted = assessment?.isCompleted == true, scoredCount = assessment?.scoredCount ?: 0
            )
        }
        return PdfReportSnapshot(
            classroomName = classroom.name,
            academicYear = classroom.academicYear,
            rubricTitle = run?.let { "${it.title} · ${rubric.title}" } ?: rubric.title,
            criteria = criteria.map { CsvCriterion(it.id, it.title, it.maxPoints) },
            students = rows
        )
    }

    /**
     * Group reports are intentionally separate from individual assessment runs.
     * A member inherits every criterion score from the authoritative group record;
     * no individual student's own score is overwritten.
     */
    suspend fun generateGroupPdfReportSnapshot(sessionId: String): PdfReportSnapshot =
        db.withTransaction { buildGroupReport(sessionId) }

    suspend fun generateGroupCsvExport(sessionId: String): String = db.withTransaction {
        buildGroupReport(sessionId).let { CsvExportFormatter.generate(it.criteria, it.students) }
    }

    private suspend fun buildGroupReport(sessionId: String): PdfReportSnapshot {
        val dao = db.groupAssessmentDao()
        val session = requireNotNull(dao.getSession(sessionId)) { "Grup oturumu bulunamadı." }
        val classroom = requireNotNull(classroomDao.getClassroomById(session.classroomId)) {
            "Sınıf bulunamadı."
        }
        val rubric = requireNotNull(rubricDao.getRubricById(session.rubricId)) {
            "Rubrik bulunamadı."
        }
        require(rubric.grade == classroom.grade && rubric.id !in RubricSeeder.unverifiedLegacyIds) {
            "Rubrik bu sınıfa uygun değil."
        }
        val criteria = rubricDao.getCriteriaForRubric(rubric.id)
        val memberScores = mutableMapOf<String, Triple<GroupMemberResultEntity, String, Map<String, Int?>>>()
        for (evaluation in dao.getAssessments(session.id)) {
            val scores = dao.getScores(evaluation.id).associate { it.criterionId to it.points }
            for (member in dao.getMemberResults(evaluation.id)) {
                require(member.studentId !in memberScores) {
                    "Öğrenci aynı grup oturumunda birden fazla kez değerlendirilmiş."
                }
                memberScores[member.studentId] = Triple(member, evaluation.groupTitleSnapshot, scores)
            }
        }
        val rows = studentDao.getStudentsForClassroom(classroom.id).map { student ->
            val result = memberScores[student.id]
            CsvStudent(
                number = student.studentNumber,
                firstName = student.firstName,
                lastName = student.lastName,
                scores = result?.third.orEmpty(),
                definitiveTotal = result?.first?.definitiveTotal,
                isCompleted = result?.first?.isCompleted == true,
                scoredCount = result?.third?.values?.count { it != null } ?: 0,
                groupTitle = result?.second
            )
        }
        return PdfReportSnapshot(
            classroomName = classroom.name,
            academicYear = classroom.academicYear,
            rubricTitle = "Grup · ${session.title} · ${rubric.title}",
            criteria = criteria.map { CsvCriterion(it.id, it.title, it.maxPoints) },
            students = rows
        )
    }

    /**
     * Explicit repair for legacy or edited classroom group rosters. Replaces
     * membership projections within THIS session only; graded criteria and
     * other sessions stay unchanged. The caller must confirm this operation.
     */
    suspend fun syncGroupSessionMembership(sessionId: String): Int = db.withTransaction {
        val dao = db.groupAssessmentDao()
        val session = requireNotNull(dao.getSession(sessionId)) { "Oturum bulunamadı." }
        val assessments = dao.getAssessments(sessionId)
        val currentMembers = assessments.map { assessment ->
            val group = requireNotNull(groupTaskDao.getGroupTaskById(assessment.groupId)) {
                "'${assessment.groupTitleSnapshot}' grubu artık bulunmuyor."
            }
            require(group.classroomId == session.classroomId) { "Grup başka sınıfa ait." }
            val array = JSONArray(group.memberStudentIdsJson)
            val members = (0 until array.length()).map { array.getString(it) }
            require(members.isNotEmpty() && members.distinct().size == members.size) {
                "'${group.title}' üye listesi geçersiz."
            }
            require(members.all { studentDao.getStudentById(it)?.classroomId == session.classroomId }) {
                "'${group.title}' üyeleri sınıfa ait değil."
            }
            assessment to members
        }
        val allIds = currentMembers.flatMap { it.second }
        require(allIds.distinct().size == allIds.size) {
            "Güncel gruplarda bir öğrenci birden fazla puanlanmış grupta. Önce grup üyelerini düzeltin."
        }
        var updated = 0
        for ((assessment, memberIds) in currentMembers) {
            val array = JSONArray(assessment.memberStudentIdsJson)
            val oldIds = (0 until array.length()).map { array.getString(it) }
            if (oldIds != memberIds) {
                dao.deleteMemberResults(assessment.id)
                dao.upsertMemberResults(memberIds.map { studentId ->
                    GroupMemberResultEntity(
                        id = assessment.id + "_" + studentId,
                        groupAssessmentId = assessment.id, studentId = studentId,
                        definitiveTotal = assessment.definitiveTotal,
                        isCompleted = assessment.isCompleted
                    )
                })
                dao.upsertAssessment(
                    assessment.copy(memberStudentIdsJson = JSONArray(memberIds).toString(),
                        lastModifiedAt = System.currentTimeMillis())
                )
                updated++
            }
        }
        if (updated > 0) auditLogDao.insertLog(AuditLogEntity(
            id = UUID.randomUUID().toString(), action = "GROUP_MEMBERSHIP_SYNCED",
            details = "Oturum ${session.id}: ${updated} grup üye listesi güncellendi."
        ))
        updated
    }

    /** Sessions are independent of editable classroom groups, and scoped to a rubric. */
    fun getStudentGroupGradesFlow(studentId: String, rubricId: String): Flow<List<StudentGroupGrade>> =
        db.groupAssessmentDao().getStudentGroupGradesFlow(studentId, rubricId)

    fun getGroupSessionsFlow(classroomId: String, rubricId: String): Flow<List<GroupAssessmentSessionEntity>> =
        db.groupAssessmentDao().getSessionsFlow(classroomId, rubricId)

    fun getGroupAssessmentsFlow(sessionId: String): Flow<List<GroupAssessmentEntity>> =
        db.groupAssessmentDao().getAssessmentsFlow(sessionId)

    suspend fun getGroupScores(assessmentId: String): List<GroupCriterionScoreEntity> =
        db.groupAssessmentDao().getScores(assessmentId)

    suspend fun getGroupAssessment(sessionId: String, groupId: String): GroupAssessmentEntity? =
        db.groupAssessmentDao().getAssessment(sessionId, groupId)

    suspend fun getGroupMemberResults(assessmentId: String): List<GroupMemberResultEntity> =
        db.groupAssessmentDao().getMemberResults(assessmentId)

    suspend fun createGroupSession(classroomId: String, rubricId: String, title: String): GroupAssessmentSessionEntity =
        db.withTransaction {
            val classroom = requireNotNull(classroomDao.getClassroomById(classroomId)) { "Sınıf yok." }
            val rubric = requireNotNull(rubricDao.getRubricById(rubricId)) { "Rubrik yok." }
            require(rubric.grade == classroom.grade && rubric.id !in RubricSeeder.unverifiedLegacyIds)
            require(title.isNotBlank())
            GroupAssessmentSessionEntity(
                id = UUID.randomUUID().toString(), classroomId = classroomId,
                rubricId = rubricId, title = title.trim()
            ).also { db.groupAssessmentDao().insertSession(it) }
        }

    /**
     * Atomic source + member projections. All rubric criteria must be scored before a
     * member receives a definitive grade. 0 and unscored (null) are distinct.
     */
    suspend fun scoreGroup(
        sessionId: String, groupId: String, criterionId: String, points: Int?
    ): GroupAssessmentEntity = db.withTransaction {
        val dao = db.groupAssessmentDao()
        val session = requireNotNull(dao.getSession(sessionId)) { "Oturum yok." }
        val group = requireNotNull(groupTaskDao.getGroupTaskById(groupId)) { "Grup yok." }
        require(group.classroomId == session.classroomId) { "Grup sınıfı farklı." }
        val rubric = requireNotNull(rubricDao.getRubricById(session.rubricId))
        require(rubric.id !in RubricSeeder.unverifiedLegacyIds)
        val criterion = requireNotNull(rubricDao.getCriterionById(criterionId))
        require(criterion.rubricId == session.rubricId && (points == null || points in 0..criterion.maxPoints)) {
            "Geçersiz ölçüt puanı."
        }
        var evaluation = dao.getAssessment(sessionId, groupId)
        if (evaluation == null) {
            val memberArray = JSONArray(group.memberStudentIdsJson)
            val memberIds = (0 until memberArray.length()).map { memberArray.getString(it) }
            require(memberIds.isNotEmpty() && memberIds.distinct().size == memberIds.size) { "Üyeler geçersiz." }
            for (id in memberIds) {
                require(studentDao.getStudentById(id)?.classroomId == session.classroomId) {
                    "Grup üyeleri sınıfa ait değil."
                }
            }
            val overlap = dao.getAssessments(sessionId).firstNotNullOfOrNull { previous ->
                val array = JSONArray(previous.memberStudentIdsJson)
                val previousIds = (0 until array.length()).map { array.getString(it) }.toSet()
                memberIds.firstOrNull { it in previousIds }?.let { it to previous.groupTitleSnapshot }
            }
            require(overlap == null) {
                val (studentId, groupName) = requireNotNull(overlap)
                val student = studentDao.getStudentById(studentId)
                "${student?.firstName ?: "Öğrenci"} ${student?.lastName.orEmpty()} " +
                    "bu oturumda '${groupName}' grubunun kayıtlı üyelerinde. " +
                    "Güncel gruplar farklıysa 'Üyelikleri Eşitle' seçeneğini kullanın."
            }
            evaluation = GroupAssessmentEntity(
                id = UUID.randomUUID().toString(), sessionId = sessionId, groupId = groupId,
                groupTitleSnapshot = group.title,
                memberStudentIdsJson = JSONArray(memberIds).toString()
            )
            dao.upsertAssessment(evaluation)
            dao.upsertMemberResults(memberIds.map { studentId ->
                GroupMemberResultEntity(
                    id = evaluation.id + "_" + studentId,
                    groupAssessmentId = evaluation.id, studentId = studentId
                )
            })
        }
        val current = requireNotNull(evaluation)
        val previous = dao.getScores(current.id).firstOrNull { it.criterionId == criterionId }
        dao.upsertScore(GroupCriterionScoreEntity(
            id = previous?.id ?: current.id + "_" + criterionId,
            groupAssessmentId = current.id, criterionId = criterionId, points = points,
            evidenceNote = previous?.evidenceNote
        ))
        val criteria = rubricDao.getCriteriaForRubric(session.rubricId)
        val scores = dao.getScores(current.id).associate { it.criterionId to it.points }
        val summary = summarizeScores(criteria.map { CriterionScore(it.maxPoints, scores[it.id]) })
        val updated = current.copy(
            isCompleted = summary.completed, definitiveTotal = summary.definitiveTotal,
            scoredCount = summary.scoredCount, lastModifiedAt = System.currentTimeMillis()
        )
        dao.upsertAssessment(updated)
        dao.upsertMemberResults(dao.getMemberResults(current.id).map {
            it.copy(isCompleted = updated.isCompleted, definitiveTotal = updated.definitiveTotal)
        })
        auditLogDao.insertLog(AuditLogEntity(
            id = UUID.randomUUID().toString(), action = "GROUP_CRITERION_SCORE_CHANGED",
            details = "Oturum " + session.id + ", grup " + group.id + ", ölçüt " + criterionId +
                ": " + (previous?.points?.toString() ?: "Boş") + " → " + (points?.toString() ?: "Boş")
        ))
        updated
    }

    /**
     * Persist criterion evidence independently of points and member grades.
     * An unscored group may hold a note; it must remain incomplete until
     * every rubric criterion has a non-null score. The first write takes a
     * validated group membership snapshot through scoreGroup(..., null).
     */
    suspend fun saveGroupEvidenceNote(
        sessionId: String, groupId: String, criterionId: String, note: String
    ): Unit = db.withTransaction {
        val dao = db.groupAssessmentDao()
        val session = requireNotNull(dao.getSession(sessionId)) { "Oturum yok." }
        val group = requireNotNull(groupTaskDao.getGroupTaskById(groupId)) { "Grup yok." }
        require(group.classroomId == session.classroomId) { "Grup sınıfı farklı." }
        val rubric = requireNotNull(rubricDao.getRubricById(session.rubricId)) { "Rubrik yok." }
        require(rubric.id !in RubricSeeder.unverifiedLegacyIds)
        val criterion = requireNotNull(rubricDao.getCriterionById(criterionId)) { "Ölçüt yok." }
        require(criterion.rubricId == rubric.id) { "Ölçüt seçili rubriğe ait değil." }
        val assessment = dao.getAssessment(sessionId, groupId)
            ?: scoreGroup(sessionId, groupId, criterionId, null)
        val row = dao.getScores(assessment.id).firstOrNull { it.criterionId == criterionId }
        if (row == null) {
            // A new criterion note does not count as a completed score.
            scoreGroup(sessionId, groupId, criterionId, null)
        }
        val current = requireNotNull(
            dao.getScores(assessment.id).firstOrNull { it.criterionId == criterionId }
        )
        if (current.evidenceNote != note) {
            dao.upsertScore(current.copy(evidenceNote = note, scoredAt = System.currentTimeMillis()))
        }
    }

    /** Conditional group-only undo; validation and restoration share one Room transaction. */
    suspend fun undoGroupScoreIfCurrent(
        sessionId: String,
        groupId: String,
        criterionId: String,
        expectedCurrentPoints: Int?,
        restorePoints: Int?,
        removeAssessmentIfEmpty: Boolean = false
    ): Boolean = db.withTransaction {
        val dao = db.groupAssessmentDao()
        val assessment = dao.getAssessment(sessionId, groupId) ?: return@withTransaction false
        val current = dao.getScores(assessment.id).firstOrNull { it.criterionId == criterionId }
        if (current?.points != expectedCurrentPoints) return@withTransaction false
        val otherScores = dao.getScores(assessment.id).filterNot { it.criterionId == criterionId }
        if (removeAssessmentIfEmpty && restorePoints == null &&
            otherScores.none { it.points != null || !it.evidenceNote.isNullOrBlank() } &&
            current?.evidenceNote.isNullOrBlank()
        ) {
            dao.deleteScores(assessment.id)
            dao.deleteMemberResults(assessment.id)
            dao.deleteAssessment(assessment.id)
            return@withTransaction true
        }
        scoreGroup(sessionId, groupId, criterionId, restorePoints)
        true
    }

    // Full JSON backup/restore: every table, prevalidation and a single Room transaction.
    // Never treat a partially restored roster as a successful full backup.
    private val backupJson = Json { encodeDefaults = true; explicitNulls = true; ignoreUnknownKeys = false }

    suspend fun generateFullBackupJson(): String = db.withTransaction {
        val dao = db.backupDao()
        backupJson.encodeToString(BackupSnapshot(
            version = 3,
            classrooms = dao.classrooms(),
            students = dao.students(),
            rubrics = dao.rubrics(),
            criteria = dao.criteria(),
            levels = dao.levels(),
            assessments = dao.assessments(),
            scores = dao.scores(),
            groups = dao.groups(),
            auditLogs = dao.auditLogs(),
            groupSessions = dao.groupSessions(),
            groupAssessments = dao.groupAssessments(),
            groupScores = dao.groupScores(),
            groupMemberResults = dao.groupMemberResults(),
            runs = dao.runs(),
            runClassrooms = dao.runClassrooms()
        ).also { it.validate() })
    }

    /** Decode and validate before pausing live writes; keep legacy conversion shared with restore. */
    fun validateBackupJson(jsonString: String): Result<Unit> = try {
        decodeValidatedBackup(jsonString)
        Result.success(Unit)
    } catch (cancel: kotlinx.coroutines.CancellationException) {
        throw cancel
    } catch (e: Exception) {
        Result.failure(e)
    }

    private fun decodeValidatedBackup(jsonString: String): BackupSnapshot {
        val imported = backupJson.decodeFromString<BackupSnapshot>(jsonString)
        imported.validate()
        // v1/v2 backups have no runs; retain every mark and assign each original
        // classroom/rubric pair to its own historical run.
        val snapshot = if (imported.version < 3) {
            val grouped = imported.assessments.distinctBy { it.classroomId to it.rubricId }
            val legacyRuns = grouped.map { assessment ->
                AssessmentRunEntity(
                    id = "legacy_${assessment.classroomId}_${assessment.rubricId}",
                    rubricId = assessment.rubricId,
                    title = "Önceki değerlendirme",
                    criteriaSnapshotJson = imported.criteria.filter { it.rubricId == assessment.rubricId }
                        .takeIf { it.isNotEmpty() }?.let { Json.encodeToString(it) } ?: "",
                    createdAt = 0
                )
            }
            imported.copy(
                version = 3,
                runs = legacyRuns,
                runClassrooms = grouped.map { assessment ->
                    val id = "legacy_${assessment.classroomId}_${assessment.rubricId}"
                    RunClassroomEntity("assign_$id", id, assessment.classroomId)
                },
                assessments = imported.assessments.map { assessment ->
                    assessment.copy(runId = "legacy_${assessment.classroomId}_${assessment.rubricId}")
                }
            )
        } else imported
        snapshot.validate()
        return snapshot
    }

    suspend fun restoreBackupFromJson(jsonString: String): Result<Int> = try {
        // Parsing, legacy conversion, and relationship validation happen before the transaction.
        val snapshot = decodeValidatedBackup(jsonString)
        db.withTransaction {
            val dao = db.backupDao()
            dao.clearScores()
            dao.clearAssessments()
            dao.clearRunClassrooms()
            dao.clearRuns()
            dao.clearGroupMemberResults()
            dao.clearGroupScores()
            dao.clearGroupAssessments()
            dao.clearGroupSessions()
            dao.clearGroups()
            dao.clearLevels()
            dao.clearCriteria()
            dao.clearStudents()
            dao.clearRubrics()
            dao.clearClassrooms()
            dao.clearLogs()
            dao.putClassrooms(snapshot.classrooms)
            dao.putRubrics(snapshot.rubrics)
            dao.putStudents(snapshot.students)
            dao.putCriteria(snapshot.criteria)
            dao.putLevels(snapshot.levels)
            dao.putRuns(snapshot.runs)
            dao.putRunClassrooms(snapshot.runClassrooms)
            dao.putAssessments(snapshot.assessments)
            dao.putScores(snapshot.scores)
            dao.putGroups(snapshot.groups)
            dao.putGroupSessions(snapshot.groupSessions)
            dao.putGroupAssessments(snapshot.groupAssessments)
            dao.putGroupScores(snapshot.groupScores)
            dao.putGroupMemberResults(snapshot.groupMemberResults)
            dao.putLogs(snapshot.auditLogs)
        }
        Result.success(snapshot.students.size)
    } catch (cancel: kotlinx.coroutines.CancellationException) {
        throw cancel
    } catch (e: Exception) {
        Result.failure(e)
    }

}
