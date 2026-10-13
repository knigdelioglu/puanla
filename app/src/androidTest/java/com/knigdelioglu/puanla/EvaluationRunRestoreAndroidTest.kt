package com.knigdelioglu.puanla

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.knigdelioglu.puanla.data.local.*
import com.knigdelioglu.puanla.data.repository.PuanlaRepository
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EvaluationRunRestoreAndroidTest {
    private fun newDatabase() = Room.inMemoryDatabaseBuilder(
        InstrumentationRegistry.getInstrumentation().targetContext,
        PuanlaDatabase::class.java
    ).allowMainThreadQueries().build()

    private suspend fun seed(db: PuanlaDatabase) {
        db.classroomDao().insertClassroom(ClassroomEntity("c1", 11, "A", "11-A", "2026-2027"))
        db.studentDao().insertStudent(StudentEntity("s1", "c1", "42", "Öykü", "Şen"))
        db.rubricDao().insertRubric(RubricEntity("rubric", "Drama", "", 11, "Canlandırma"))
        db.rubricDao().insertCriteria(listOf(CriterionEntity("criterion", "rubric", "Anlatım", "", 100, 0)))
    }

    @Test fun fullV3BackupRestoresDistinctAttemptScoresAndArchiveStatus() = runBlocking {
        val source = newDatabase()
        val destination = newDatabase()
        try {
            seed(source)
            val repo = PuanlaRepository(source) { "{}" }
            val first = repo.createAssessmentRun("rubric", "İlk değerlendirme", listOf("c1"))
            val second = repo.createAssessmentRun("rubric", "İkinci değerlendirme", listOf("c1"))
            repo.saveCriterionScore("c1", "s1", "rubric", "criterion", 90, runId = first.id)
            repo.saveCriterionScore("c1", "s1", "rubric", "criterion", 70, runId = second.id)
            repo.archiveRun(first.id, true)
            val backup = repo.generateFullBackupJson()
            val restored = PuanlaRepository(destination) { "{}" }
            assertTrue(restored.restoreBackupFromJson(backup).isSuccess)
            assertEquals(90, restored.getRunCriterionScoresForStudent("s1", first.id)["criterion"])
            assertEquals(70, restored.getRunCriterionScoresForStudent("s1", second.id)["criterion"])
            assertEquals("ARCHIVED", restored.getRun(first.id)?.status)
            assertEquals(1, restored.getRunClassrooms(second.id).size)
            assertEquals(2, destination.backupDao().runs().size)
            assertEquals(2, destination.backupDao().assessments().size)
        } finally {
            destination.close()
            source.close()
        }
    }

    @Test fun legacyV2BackupCreatesHistoricalRunWithoutInventingDate() = runBlocking {
        val destination = newDatabase()
        try {
            val classroom = ClassroomEntity("oldClass", 11, "B", "11-B", "2026-2027")
            val student = StudentEntity("student", classroom.id, "7", "İpek", "Şahin")
            val rubric = RubricEntity("oldRubric", "Drama", "", 11, "Drama")
            val criterion = CriterionEntity("oldCriterion", rubric.id, "Beceri", "", 100, 0)
            val assessment = AssessmentEntity(
                id = "existing", classroomId = classroom.id, studentId = student.id,
                rubricId = rubric.id, isCompleted = true, definitiveTotal = 80, scoredCount = 1
            )
            val legacy = BackupSnapshot(
                version = 2,
                classrooms = listOf(classroom), students = listOf(student),
                rubrics = listOf(rubric), criteria = listOf(criterion), levels = emptyList(),
                assessments = listOf(assessment),
                scores = listOf(CriterionScoreEntity("oldScore", assessment.id, criterion.id, 80)),
                groups = emptyList(), auditLogs = emptyList()
            )
            val json = Json { encodeDefaults = true; explicitNulls = true }.encodeToString(legacy)
            val repo = PuanlaRepository(destination) { "{}" }
            assertTrue(repo.restoreBackupFromJson(json).isSuccess)
            val restored = destination.backupDao().assessments().single()
            assertEquals("existing", restored.id)
            assertEquals(80, repo.getRunCriterionScoresForStudent(student.id, restored.runId)[criterion.id])
            assertEquals(0L, repo.getRun(restored.runId)?.createdAt)
        } finally {
            destination.close()
        }
    }
}
