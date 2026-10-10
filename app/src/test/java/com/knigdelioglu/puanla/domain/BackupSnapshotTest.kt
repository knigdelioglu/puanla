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
}
