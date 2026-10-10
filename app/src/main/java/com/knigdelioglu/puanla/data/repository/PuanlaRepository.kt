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
import com.knigdelioglu.puanla.domain.CriterionScore
import com.knigdelioglu.puanla.domain.summarizeScores
import androidx.room.withTransaction
import com.knigdelioglu.puanla.data.local.BackupSnapshot
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

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
        val trimmedNum = studentNumber.trim()
        val existing = studentDao.getStudentByNumber(classroomId, trimmedNum)
        if (existing != null) {
            return Result.failure(IllegalArgumentException("Bu okul numarasına ($trimmedNum) sahip öğrenci zaten mevcut."))
        }

        val student = StudentEntity(
            id = UUID.randomUUID().toString(),
            classroomId = classroomId,
            studentNumber = trimmedNum,
            firstName = firstName.trim(),
            lastName = lastName.trim().uppercase(),
            gender = gender?.trim(),
            boardingStatus = boardingStatus?.trim()
        )
        studentDao.insertStudent(student)
        return Result.success(student)
    }

    suspend fun addStudents(students: List<StudentEntity>) = db.withTransaction {
        require(students.isNotEmpty()) { "Aktarılacak onaylı öğrenci yok." }
        require(students.all {
            it.studentNumber.isNotBlank() && it.firstName.isNotBlank() && it.lastName.isNotBlank()
        }) { "Eksik öğrenci bilgisi var." }
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

    suspend fun updateStudent(student: StudentEntity) {
        studentDao.updateStudent(student)
    }

    suspend fun deleteStudent(student: StudentEntity) {
        studentDao.deleteStudent(student)
    }

    // Rubrics
    fun getAllRubricsFlow(): Flow<List<RubricEntity>> = rubricDao.getAllRubricsFlow()

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

    // Audit logs
    fun getRecentLogsFlow(limit: Int = 30): Flow<List<AuditLogEntity>> =
        auditLogDao.getRecentLogsFlow(limit)

    // CSV Export (Excel compatible UTF-8 BOM, comma/semicolon delimited)
    suspend fun generateCsvExport(classroomId: String, rubricId: String): String {
        val classroom = classroomDao.getClassroomById(classroomId) ?: return ""
        val rubric = rubricDao.getRubricById(rubricId) ?: return ""
        val students = studentDao.getStudentsForClassroom(classroomId)
        val criteria = rubricDao.getCriteriaForRubric(rubricId)
        val assessments = assessmentDao.getAssessments(classroomId, rubricId).associateBy { it.studentId }

        val sb = StringBuilder()
        // UTF-8 BOM so Excel opens Turkish characters seamlessly
        sb.append('\uFEFF')

        // Header line
        val headers = mutableListOf("Okul No", "Adı", "Soyadı")
        criteria.forEach { headers.add("${it.title} (Azami ${it.maxPoints})") }
        headers.add("Toplam Puan (100)")
        headers.add("Değerlendirme Durumu")
        sb.append(headers.joinToString(";")).append("\n")

        // Rows
        for (student in students) {
            val assess = assessments[student.id]
            val row = mutableListOf<String>()
            row.add(student.studentNumber)
            row.add(student.firstName)
            row.add(student.lastName)

            if (assess != null) {
                val scores = assessmentDao.getScoresForAssessment(assess.id).associateBy { it.criterionId }
                criteria.forEach { crit ->
                    val pts = scores[crit.id]?.points
                    row.add(pts?.toString() ?: "Puanlanmadı")
                }
                row.add(assess.definitiveTotal?.toString() ?: "Eksik")
                row.add(if (assess.isCompleted) "Tamamlandı" else if (assess.scoredCount > 0) "Kısmi" else "Başlanmadı")
            } else {
                criteria.forEach { _ -> row.add("Puanlanmadı") }
                row.add("Eksik")
                row.add("Başlanmadı")
            }
            sb.append(row.joinToString(";")).append("\n")
        }

        return sb.toString()
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
