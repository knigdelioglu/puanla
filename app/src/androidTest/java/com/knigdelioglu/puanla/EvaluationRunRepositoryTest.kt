package com.knigdelioglu.puanla

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.knigdelioglu.puanla.data.local.ClassroomEntity
import com.knigdelioglu.puanla.data.local.CriterionEntity
import com.knigdelioglu.puanla.data.local.PuanlaDatabase
import com.knigdelioglu.puanla.data.local.RubricEntity
import com.knigdelioglu.puanla.data.local.StudentEntity
import com.knigdelioglu.puanla.data.repository.PuanlaRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EvaluationRunRepositoryTest {
    @Test fun repeatedRubricAndMultiClassAssignmentsKeepGradesIndependent() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, PuanlaDatabase::class.java)
            .allowMainThreadQueries().build()
        try {
            db.classroomDao().insertClassroom(ClassroomEntity("c1", 11, "A", "11-A", "2026-2027"))
            db.classroomDao().insertClassroom(ClassroomEntity("c2", 11, "B", "11-B", "2026-2027"))
            db.studentDao().insertStudent(StudentEntity("s1", "c1", "1", "Ayşe", "Çelik"))
            db.studentDao().insertStudent(StudentEntity("s2", "c2", "1", "Mert", "Öztürk"))
            db.rubricDao().insertRubric(RubricEntity("r", "Drama", "", 11, "Drama"))
            db.rubricDao().insertCriteria(listOf(CriterionEntity("criterion", "r", "Sunum", "", 100, 0)))
            val repository = PuanlaRepository(db) { "{}" }
            val october = repository.createAssessmentRun("r", "Ekim Drama", listOf("c1", "c2"))
            val december = repository.createAssessmentRun("r", "Aralık Drama", listOf("c1"))
            assertEquals(2, repository.getRunClassrooms(october.id).size)
            repository.saveCriterionScore("c1", "s1", "r", "criterion", 80, runId = october.id)
            repository.saveCriterionScore("c2", "s2", "r", "criterion", 100, runId = october.id)
            repository.saveCriterionScore("c1", "s1", "r", "criterion", 60, runId = december.id)
            assertEquals(80, repository.getRunCriterionScoresForStudent("s1", october.id)["criterion"])
            assertEquals(60, repository.getRunCriterionScoresForStudent("s1", december.id)["criterion"])
            assertEquals(100, repository.getRunCriterionScoresForStudent("s2", october.id)["criterion"])
            assertNull(repository.getRunAssessmentForStudent("s2", december.id))
            val csv = repository.generateCsvExport("c1", "r", december.id)
            assertTrue(csv.contains(";60;Tamamlandı"))
            assertThrows(IllegalArgumentException::class.java) {
                runBlocking { repository.generateCsvExport("c1", "r") }
            }
            assertThrows(IllegalArgumentException::class.java) {
                runBlocking { repository.generatePdfReportSnapshot("c1", "r") }
            }
            // Updating rubric metadata must never REPLACE and cascade-delete
            // an existing criterion score. Historical attempts still use
            // their frozen 100-point criterion despite the new 60-point template.
            db.rubricDao().insertRubric(RubricEntity("r", "Güncel Drama", "", 11, "Drama"))
            db.rubricDao().insertCriteria(
                listOf(CriterionEntity("criterion", "r", "Güncellenen Anlatım", "", 60, 0))
            )
            assertEquals(80, repository.getRunCriterionScoresForStudent("s1", october.id)["criterion"])
            repository.saveCriterionScore("c1", "s1", "r", "criterion", 90, runId = october.id)
            assertEquals(90, repository.getRunCriterionScoresForStudent("s1", october.id)["criterion"])
            repository.saveCriterionScore("c1", "s1", "r", "criterion", 80, runId = october.id)
            assertEquals(80, repository.getRunCriterionScoresForStudent("s1", october.id)["criterion"])
            repository.archiveRun(october.id, true)
            assertThrows(IllegalArgumentException::class.java) {
                runBlocking {
                    repository.saveCriterionScore("c1", "s1", "r", "criterion", 50, runId = october.id)
                }
            }
            assertEquals(80, repository.getRunCriterionScoresForStudent("s1", october.id)["criterion"])
            val octoberAssessment = requireNotNull(repository.getRunAssessmentForStudent("s1", october.id))
            val decemberAssessment = requireNotNull(repository.getRunAssessmentForStudent("s1", december.id))
            assertThrows(IllegalArgumentException::class.java) {
                runBlocking {
                    repository.clearAssessmentScores(octoberAssessment.id, "s1", "r")
                }
            }
            assertThrows(IllegalArgumentException::class.java) {
                runBlocking {
                    repository.clearAssessmentScores(decemberAssessment.id, "s2", "r")
                }
            }
            repository.clearAssessmentScores(decemberAssessment.id, "s1", "r")
            assertTrue(repository.getRunCriterionScoresForStudent("s1", december.id).isEmpty())
            assertEquals(80, repository.getRunCriterionScoresForStudent("s1", october.id)["criterion"])
            assertThrows(IllegalArgumentException::class.java) {
                runBlocking { repository.deleteClassroom(requireNotNull(repository.getClassroomById("c1"))) }
            }
        } finally {
            db.close()
        }
    }
}
