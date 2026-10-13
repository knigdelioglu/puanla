package com.knigdelioglu.puanla.domain

import com.knigdelioglu.puanla.data.local.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class EvaluationRunBackupTest {
    private fun fixture(): BackupSnapshot {
        val classroom = ClassroomEntity("11c", 11, "C", "11-C", "2026-2027")
        val student = StudentEntity("s", "11c", "17", "Yağmur", "Çelik")
        val rubric = RubricEntity("r", "Drama", "", 11, "Canlandırma")
        val criterion = CriterionEntity("criterion", "r", "Anlatım", "", 100, 0)
        val first = AssessmentRunEntity("run1", "r", "Ekim")
        val second = AssessmentRunEntity("run2", "r", "Aralık")
        val firstAssessment = AssessmentEntity(
            "a1", "11c", "s", "r", true, 90, 1, runId = first.id
        )
        val secondAssessment = AssessmentEntity(
            "a2", "11c", "s", "r", true, 70, 1, runId = second.id
        )
        return BackupSnapshot(
            version = 3, classrooms = listOf(classroom), students = listOf(student),
            rubrics = listOf(rubric), criteria = listOf(criterion), levels = emptyList(),
            assessments = listOf(firstAssessment, secondAssessment),
            scores = listOf(
                CriterionScoreEntity("score1", "a1", "criterion", 90),
                CriterionScoreEntity("score2", "a2", "criterion", 70)
            ),
            groups = emptyList(), auditLogs = emptyList(),
            runs = listOf(first, second),
            runClassrooms = listOf(
                RunClassroomEntity("link1", first.id, classroom.id),
                RunClassroomEntity("link2", second.id, classroom.id)
            )
        )
    }

    @Test fun repeatedRubricForOneStudentIsValidWithDistinctRunIds() {
        val snapshot = fixture()
        snapshot.validate()
        val json = Json { encodeDefaults = true; explicitNulls = true }
        val roundtrip = json.decodeFromString<BackupSnapshot>(json.encodeToString(snapshot))
        roundtrip.validate()
        assertEquals(snapshot.assessments, roundtrip.assessments)
        assertEquals(snapshot.runs, roundtrip.runs)
    }

    @Test fun duplicateStudentWithinSameRunIsRejected() {
        val snapshot = fixture()
        assertThrows(IllegalArgumentException::class.java) {
            snapshot.copy(assessments = snapshot.assessments.map {
                it.copy(runId = "run1")
            }).validate()
        }
    }

    @Test fun missingClassroomAssignmentIsRejectedBeforeRestoration() {
        val snapshot = fixture()
        assertThrows(IllegalArgumentException::class.java) {
            snapshot.copy(runClassrooms = snapshot.runClassrooms.take(1)).validate()
        }
    }
}
