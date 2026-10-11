package com.knigdelioglu.puanla.data.repository

import com.knigdelioglu.puanla.data.local.AssessmentEntity
import com.knigdelioglu.puanla.data.local.AuditLogEntity
import com.knigdelioglu.puanla.data.local.ClassroomEntity
import com.knigdelioglu.puanla.data.local.CriterionEntity
import com.knigdelioglu.puanla.data.local.CriterionScoreEntity
import com.knigdelioglu.puanla.data.local.GroupTaskEntity
import com.knigdelioglu.puanla.data.local.PuanlaDatabase
import com.knigdelioglu.puanla.data.local.RubricEntity
import com.knigdelioglu.puanla.data.local.RubricSeeder
import com.knigdelioglu.puanla.data.local.StudentEntity
import com.knigdelioglu.puanla.domain.export.CsvCriterion
import com.knigdelioglu.puanla.domain.export.CsvStudent
import com.knigdelioglu.puanla.domain.export.CsvExportFormatter
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
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import com.knigdelioglu.puanla.domain.student.StudentNameRules
import com.knigdelioglu.puanla.domain.group.GroupChecklistRules

class PuanlaRepository(private val db: PuanlaDatabase) {

    private val classroomDao = db.classroomDao()
    private val studentDao = db.studentDao()
    private val rubricDao = db.rubricDao()
    private val assessmentDao = db.assessmentDao()
    private val groupTaskDao = db.groupTaskDao()
    private val auditLogDao = db.auditLogDao()

    suspend fun initialize() {
        // No approved rubric source is in the repository. Never seed invented assessment criteria.
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

    suspend fun deleteClassroom(classroom: ClassroomEntity) {
        classroomDao.deleteClassroom(classroom)
        auditLogDao.insertLog(
            AuditLogEntity(
                id = UUID.randomUUID().toString(),
                action = "DELETE_CLASSROOM",
                details = "Sınıf silindi: ${classroom.name}"
            )
        )
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

    fun getCriteriaForRubricFlow(rubricId: String): Flow<List<CriterionEntity>> =
        rubricDao.getCriteriaForRubricFlow(rubricId)

    // Assessments & Scoring
    fun getAssessmentsFlow(classroomId: String, rubricId: String): Flow<List<AssessmentEntity>> =
        assessmentDao.getAssessmentsFlow(classroomId, rubricId)

    suspend fun getAssessmentForStudent(studentId: String, rubricId: String): AssessmentEntity? =
        assessmentDao.getAssessmentForStudent(studentId, rubricId)

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
        evidenceNote: String? = null
    ): CriterionScoreEntity? = db.withTransaction {
        val classroom = requireNotNull(classroomDao.getClassroomById(classroomId)) { "Sınıf bulunamadı." }
        val student = requireNotNull(studentDao.getStudentById(studentId)) { "Öğrenci bulunamadı." }
        require(student.classroomId == classroom.id) { "Öğrenci farklı sınıfa ait." }
        require(rubricId !in RubricSeeder.unverifiedLegacyIds) { "Bu rubriğin kaynağı doğrulanmamış; puanlama engellendi." }
        val rubric = requireNotNull(rubricDao.getRubricById(rubricId)) { "Rubrik bulunamadı." }
        require(rubric.grade == classroom.grade) { "Rubrik sınıf düzeyine uygun değil." }
        val criterion = requireNotNull(rubricDao.getCriterionById(criterionId)) { "Ölçüt bulunamadı." }
        require(criterion.rubricId == rubric.id) { "Ölçüt başka rubriğe ait." }
        require(scorePoints == null || scorePoints in 0..criterion.maxPoints) { "Puan geçerli ölçüt aralığı dışında." }

        val existing = assessmentDao.getAssessmentForStudent(studentId, rubricId)
        val assessment = existing ?: AssessmentEntity(
            id = "assess_${studentId}_${rubricId}",
            classroomId = classroomId,
            studentId = studentId,
            rubricId = rubricId
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
        val criteria = rubricDao.getCriteriaForRubric(rubricId)
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
                details = "${classroom.name}, no ${student.studentNumber}, ${criterion.title}: " +
                    "${previous?.points?.toString() ?: "Boş"} → ${scorePoints?.toString() ?: "Boş"}"
            ))
        }
        previous
    }

    suspend fun clearAssessmentScores(assessmentId: String, studentId: String, rubricId: String) {
        db.withTransaction {
            val assessment = assessmentDao.getAssessmentForStudent(studentId, rubricId)
            require(assessment?.id == assessmentId) { "Değerlendirme kimliği uyuşmuyor." }
            assessmentDao.clearScoresForAssessment(assessmentId)
            assessmentDao.insertOrUpdateAssessment(
                assessment.copy(isCompleted = false, definitiveTotal = null, scoredCount = 0,
                    lastModifiedAt = System.currentTimeMillis())
            )
        }
    }

    // Group Tasks
    fun getGroupTasksFlow(classroomId: String, rubricId: String): Flow<List<GroupTaskEntity>> =
        groupTaskDao.getGroupTasksFlow(classroomId, rubricId)

    suspend fun saveGroupTask(task: GroupTaskEntity) {
        groupTaskDao.insertOrUpdateGroupTask(task)
    }

    suspend fun deleteGroupTask(task: GroupTaskEntity) {
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

    /**
     * Reads one consistent database snapshot and delegates the actual serialization
     * to the same tested writer used in CSV unit tests.
     */
    suspend fun generateCsvExport(classroomId: String, rubricId: String): String = db.withTransaction {
        val classroom = requireNotNull(classroomDao.getClassroomById(classroomId)) { "Sınıf bulunamadı." }
        val rubric = requireNotNull(rubricDao.getRubricById(rubricId)) { "Rubrik bulunamadı." }
        require(rubric.id !in RubricSeeder.unverifiedLegacyIds) { "Kaynağı doğrulanmamış rubrik dışa aktarılamaz." }
        require(rubric.grade == classroom.grade) { "Rubrik farklı sınıf düzeyine ait." }

        val criteria = rubricDao.getCriteriaForRubric(rubricId)
        val assessments = assessmentDao.getAssessments(classroomId, rubricId).associateBy { it.studentId }
        val studentRows = studentDao.getStudentsForClassroom(classroomId).map { student ->
            val assessment = assessments[student.id]
            val scores = if (assessment != null) {
                assessmentDao.getScoresForAssessment(assessment.id).associate { it.criterionId to it.points }
            } else emptyMap()
            CsvStudent(
                number = student.studentNumber,
                firstName = student.firstName,
                lastName = student.lastName,
                scores = scores,
                definitiveTotal = assessment?.definitiveTotal,
                isCompleted = assessment?.isCompleted == true,
                scoredCount = assessment?.scoredCount ?: 0
            )
        }
        CsvExportFormatter.generate(criteria.map { CsvCriterion(it.id, it.title, it.maxPoints) }, studentRows)
    }

    // Full JSON backup/restore: every table, prevalidation and a single Room transaction.
    // Never treat a partially restored roster as a successful full backup.
    private val backupJson = Json { encodeDefaults = true; explicitNulls = true; ignoreUnknownKeys = false }

    suspend fun generateFullBackupJson(): String = db.withTransaction {
        val dao = db.backupDao()
        backupJson.encodeToString(BackupSnapshot(
            classrooms = dao.classrooms(),
            students = dao.students(),
            rubrics = dao.rubrics(),
            criteria = dao.criteria(),
            levels = dao.levels(),
            assessments = dao.assessments(),
            scores = dao.scores(),
            groups = dao.groups(),
            auditLogs = dao.auditLogs()
        ).also { it.validate() })
    }

    suspend fun restoreBackupFromJson(jsonString: String): Result<Int> {
        return try {
            // No writes before this completes. Unsupported old/partial formats are rejected.
            val snapshot = backupJson.decodeFromString<BackupSnapshot>(jsonString)
            snapshot.validate()
            db.withTransaction {
                val dao = db.backupDao()
                dao.clearScores()
                dao.clearAssessments()
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
                dao.putAssessments(snapshot.assessments)
                dao.putScores(snapshot.scores)
                dao.putGroups(snapshot.groups)
                dao.putLogs(snapshot.auditLogs)
            }
            Result.success(snapshot.students.size)
        } catch (cancel: kotlinx.coroutines.CancellationException) {
            throw cancel
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
