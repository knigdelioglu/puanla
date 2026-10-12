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
    val version: Int = 2,
    val exportedAt: Long = System.currentTimeMillis(),
    val classrooms: List<ClassroomEntity>,
    val students: List<StudentEntity>,
    val rubrics: List<RubricEntity>,
    val criteria: List<CriterionEntity>,
    val levels: List<CriterionLevelEntity>,
    val assessments: List<AssessmentEntity>,
    val scores: List<CriterionScoreEntity>,
    val groups: List<GroupTaskEntity>,
    val auditLogs: List<AuditLogEntity>,
    val groupSessions: List<GroupAssessmentSessionEntity> = emptyList(),
    val groupAssessments: List<GroupAssessmentEntity> = emptyList(),
    val groupScores: List<GroupCriterionScoreEntity> = emptyList(),
    val groupMemberResults: List<GroupMemberResultEntity> = emptyList()
) {
    fun validate() {
        require(version in 1..2) { "Desteklenmeyen yedek sürümü: $version" }
        if (version == 1) require(groupSessions.isEmpty() && groupAssessments.isEmpty() &&
            groupScores.isEmpty() && groupMemberResults.isEmpty()) { "Eski yedekte yeni grup notları olamaz." }
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
        unique(groupSessions) { it.id }
        unique(groupAssessments) { it.id }
        unique(groupScores) { it.id }
        unique(groupMemberResults) { it.id }
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
            assessment != null && criterion != null && criterion.rubricId == assessment.rubricId &&
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
                (it.rubricId.isBlank() || rubricById[it.rubricId]?.grade == classById[it.classroomId]?.grade)
        }) { "Grup ilişki bilgileri geçersiz." }
        val groupById = groups.associateBy { it.id }
        val sessions = groupSessions.associateBy { it.id }
        val evaluations = groupAssessments.associateBy { it.id }
        require(groupSessions.all {
            classById.containsKey(it.classroomId) &&
                rubricById[it.rubricId]?.grade == classById[it.classroomId]?.grade
        }) { "Grup oturumu geçersiz." }
        require(groupAssessments.all {
            val s = sessions[it.sessionId]
            s != null && groupById[it.groupId]?.classroomId == s.classroomId
        }) { "Grup değerlendirmesi geçersiz." }
        unique(groupAssessments) { it.sessionId + "/" + it.groupId }
        require(groupScores.all {
            val e = evaluations[it.groupAssessmentId]
            val criterion = criterionById[it.criterionId]
            e != null && criterion?.rubricId == sessions[e.sessionId]?.rubricId &&
                (it.points == null || it.points in 0..criterion.maxPoints)
        }) { "Grup ölçüt puanı geçersiz." }
        unique(groupScores) { it.groupAssessmentId + "/" + it.criterionId }
        require(groupMemberResults.all {
            val e = evaluations[it.groupAssessmentId]
            val s = e?.let { evaluation -> sessions[evaluation.sessionId] }
            s != null && studentById[it.studentId]?.classroomId == s.classroomId &&
                it.isCompleted == e.isCompleted && it.definitiveTotal == e.definitiveTotal
        }) { "Grup öğrenci sonucu geçersiz." }
        unique(groupMemberResults) { it.groupAssessmentId + "/" + it.studentId }
        for (e in groupAssessments) {
            val session = requireNotNull(sessions[e.sessionId])
            val cs = criteria.filter { it.rubricId == session.rubricId }
            val scored = groupScores.filter { it.groupAssessmentId == e.id }
                .associate { it.criterionId to it.points }
            val summary = summarizeScores(cs.map { CriterionScore(it.maxPoints, scored[it.id]) })
            require(e.isCompleted == summary.completed && e.definitiveTotal == summary.definitiveTotal &&
                e.scoredCount == summary.scoredCount) { "Grup puan toplamı tutarsız." }
            val ids = try {
                kotlinx.serialization.json.Json.parseToJsonElement(e.memberStudentIdsJson)
                    .let { element -> element as kotlinx.serialization.json.JsonArray }
                    .map { element -> (element as kotlinx.serialization.json.JsonPrimitive).content }
            } catch (ex: Exception) { throw IllegalArgumentException("Grup üyeleri geçersiz.", ex) }
            require(ids.isNotEmpty() && ids.distinct().size == ids.size &&
                ids.all { studentById[it]?.classroomId == session.classroomId }) { "Grup üyeleri tutarsız." }
            require(groupMemberResults.filter { it.groupAssessmentId == e.id }.map { it.studentId }.toSet() ==
                ids.toSet()) { "Grup üye notları eksik." }
        }
    }
}
