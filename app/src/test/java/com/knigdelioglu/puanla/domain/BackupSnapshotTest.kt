package com.knigdelioglu.puanla.domain

import com.knigdelioglu.puanla.data.local.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class BackupSnapshotTest {
    private fun basicSnapshot(): BackupSnapshot {
        val classroom = ClassroomEntity("c1", 11, "C", "11-C", "2026-2027")
        val student = StudentEntity("s1", "c1", "100", "Ayşe", "Çelik")
        val rubric = RubricEntity("r1", "Kaynağı Onaylı Test Rubriği", "", 11, "Sınama")
        val criterion = CriterionEntity("k1", "r1", "Ölçüt", "", 10, 0)
        val assess = AssessmentEntity("a1", "c1", "s1", "r1", false, null, 0)
        return BackupSnapshot(
            classrooms = listOf(classroom),
            students = listOf(student),
            rubrics = listOf(rubric),
            criteria = listOf(criterion),
            levels = emptyList(),
            assessments = listOf(assess),
            scores = emptyList(),
            groups = emptyList(),
            auditLogs = emptyList()
        )
    }

    @Test fun fullSnapshotRoundTripsAllEntityTypes() {
        val src = basicSnapshot()
        src.validate()
        val json = Json { encodeDefaults = true; explicitNulls = true }
        val restored = json.decodeFromString<BackupSnapshot>(json.encodeToString(src))
        restored.validate()
        assertEquals(src.classrooms, restored.classrooms)
        assertEquals(src.students, restored.students)
        assertEquals(src.assessments, restored.assessments)
        assertEquals(src.rubrics, restored.rubrics)
    }

    @Test fun orphanScoreFailsValidation() {
        val original = basicSnapshot()
        val damaged = original.copy(scores = listOf(CriterionScoreEntity("sc1", "unknown", "k1", 5)))
        assertThrows(IllegalArgumentException::class.java) { damaged.validate() }
    }

    @Test fun duplicateStudentNumberFailsValidation() {
        val original = basicSnapshot()
        val damaged = original.copy(students = original.students +
            StudentEntity("s2", "c1", "100", "Mert", "Demir"))
        assertThrows(IllegalArgumentException::class.java) { damaged.validate() }
    }

    @Test fun incorrectScoreSummaryFailsValidation() {
        val original = basicSnapshot()
        val damaged = original.copy(assessments = listOf(original.assessments.single().copy(
            isCompleted = true, definitiveTotal = 10, scoredCount = 1
        )))
        assertThrows(IllegalArgumentException::class.java) { damaged.validate() }
    }
    @Test fun legacyVersionOneBackupCanBeImportedWithoutGroupAssessmentFields() {
        val original = basicSnapshot().copy(version = 1)
        val json = Json { encodeDefaults = true; explicitNulls = true }
        val encoded = json.encodeToString(original)
        val objectWithoutNewKeys = Json.parseToJsonElement(encoded).let { it as kotlinx.serialization.json.JsonObject }
            .filterKeys { it !in setOf("groupSessions", "groupAssessments", "groupScores", "groupMemberResults") }
        val restored = json.decodeFromString<BackupSnapshot>(
            kotlinx.serialization.json.JsonObject(objectWithoutNewKeys).toString()
        )
        restored.validate()
        assertEquals(1, restored.version)
        assertTrue(restored.groupSessions.isEmpty())
        assertEquals(original.assessments, restored.assessments)
    }

    @Test fun groupGradeBackupRejectsInconsistentMemberGrades() {
        val original = basicSnapshot()
        val group = GroupTaskEntity("g1", "c1", "", "Grup 1", "[\\"s1\\"]", "[]")
        val session = GroupAssessmentSessionEntity("session1", "c1", "r1", "Drama")
        val evaluation = GroupAssessmentEntity(
            id = "eval1", sessionId = session.id, groupId = group.id,
            groupTitleSnapshot = group.title, memberStudentIdsJson = group.memberStudentIdsJson,
            isCompleted = true, definitiveTotal = 8, scoredCount = 1
        )
        val score = GroupCriterionScoreEntity("groupScore1", evaluation.id, "k1", 8)
        val studentGrade = GroupMemberResultEntity("member1", evaluation.id, "s1", 8, true)
        val valid = original.copy(
            groups = listOf(group),
            groupSessions = listOf(session),
            groupAssessments = listOf(evaluation),
            groupScores = listOf(score),
            groupMemberResults = listOf(studentGrade)
        )
        valid.validate()
        assertThrows(IllegalArgumentException::class.java) {
            valid.copy(groupMemberResults = listOf(studentGrade.copy(definitiveTotal = 7))).validate()
        }
    }

}
