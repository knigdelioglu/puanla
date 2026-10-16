package com.knigdelioglu.puanla

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.knigdelioglu.puanla.data.local.*
import com.knigdelioglu.puanla.data.repository.PuanlaRepository
import com.knigdelioglu.puanla.domain.export.PdfReportStatistics
import com.knigdelioglu.puanla.domain.export.PdfScoreState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/**
 * P5 acceptance tests exercise the same repository paths used by both grading modes,
 * group projection, PDF/CSV and full backup. They deliberately do not mock Room.
 */
@RunWith(AndroidJUnit4::class)
class UnifiedScoringEndToEndAcceptanceTest {

    private fun database(): PuanlaDatabase =
        Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), PuanlaDatabase::class.java
        ).build()

    private fun repository(db: PuanlaDatabase) = PuanlaRepository(db) { "{}" }

    private suspend fun seed(db: PuanlaDatabase) {
        db.classroomDao().insertClassroom(ClassroomEntity("class", 11, "A", "11-A", "2026-2027"))
        db.studentDao().insertStudents(listOf(
            StudentEntity("s1", "class", "1", "Ayşe", "Yılmaz"),
            StudentEntity("s2", "class", "2", "Mert", "Demir")
        ))
        db.rubricDao().insertRubric(RubricEntity("rubric", "Sunum", "", 11, "Sunum", false))
        db.rubricDao().insertCriteria(listOf(
            CriterionEntity("content", "rubric", "İçerik", "", 20, 0),
            CriterionEntity("delivery", "rubric", "Sunum", "", 80, 1)
        ))
    }

    @Test fun individualGroupSessionNotesReportsAndFullBackupStayIndependent() = runBlocking {
        val source = database()
        val restored = database()
        try {
            seed(source)
            val repo = repository(source)
            val run = repo.createAssessmentRun("rubric", "Bireysel Sunum", listOf("class"))
            repo.saveCriterionScore("class", "s1", "rubric", "content", 0,
                evidenceNote = "Bireysel kanıt", runId = run.id)
            repo.saveCriterionScore("class", "s1", "rubric", "delivery", 60, runId = run.id)
            repo.saveCriterionScore("class", "s2", "rubric", "content", 10, runId = run.id)

            repo.saveGroupTask(GroupTaskEntity(
                "g", "class", "", "Grup A", "[\"s1\",\"s2\"]", "[]"
            ))
            val first = repo.createGroupSession("class", "rubric", "Grup Oturumu 1")
            val second = repo.createGroupSession("class", "rubric", "Grup Oturumu 2")
            repo.scoreGroup(first.id, "g", "content", 20)
            repo.saveGroupEvidenceNote(first.id, "g", "content", "Grup iş birliği kanıtı")
            repo.scoreGroup(first.id, "g", "delivery", 70)
            repo.scoreGroup(second.id, "g", "content", 0)

            val individual = repo.generatePdfReportSnapshot("class", "rubric", run.id)
            val groupFull = repo.generateGroupPdfReportSnapshot(first.id)
            val groupPartial = repo.generateGroupPdfReportSnapshot(second.id)
            assertEquals(listOf(60, null), individual.students.map { it.definitiveTotal })
            assertEquals(listOf(90, 90), groupFull.students.map { it.definitiveTotal })
            assertEquals(listOf(null, null), groupPartial.students.map { it.definitiveTotal })
            assertEquals(
                listOf(PdfScoreState.COMPLETED, PdfScoreState.PARTIAL),
                individual.students.map { PdfReportStatistics.state(it, individual.criteria) }
            )
            assertEquals(2, PdfReportStatistics.summary(groupFull).completed)
            assertEquals(2, PdfReportStatistics.summary(groupPartial).partial)
            assertTrue(repo.generateCsvExport("class", "rubric", run.id).contains("Eksik"))
            assertTrue(repo.generateGroupCsvExport(second.id).contains("Puanlanmadı"))
            assertTrue(repo.generateGroupCsvExport(first.id).contains("Grup A"))

            val backup = repo.generateFullBackupJson()
            val recovered = repository(restored)
            assertTrue(recovered.restoreBackupFromJson(backup).getOrThrow() == 2)
            assertEquals(individual, recovered.generatePdfReportSnapshot("class", "rubric", run.id))
            assertEquals(groupFull, recovered.generateGroupPdfReportSnapshot(first.id))
            assertEquals(groupPartial, recovered.generateGroupPdfReportSnapshot(second.id))
            assertEquals(repo.generateCsvExport("class", "rubric", run.id),
                recovered.generateCsvExport("class", "rubric", run.id))
            assertEquals(repo.generateGroupCsvExport(first.id),
                recovered.generateGroupCsvExport(first.id))

            val groupAssessment = requireNotNull(recovered.getGroupAssessment(first.id, "g"))
            val groupEvidence = recovered.getGroupScores(groupAssessment.id).single { it.criterionId == "content" }
            assertEquals("Grup iş birliği kanıtı", groupEvidence.evidenceNote)
            val individualAssessment = requireNotNull(
                recovered.getRunAssessmentForStudent("s1", run.id)
            )
            assertEquals("Bireysel kanıt", recovered.getScoresForAssessment(individualAssessment.id)
                .single { it.criterionId == "content" }.evidenceNote)

            // Undoing a group score invalidates its definitive grade without
            // touching its note, the other group session, or the individual grade.
            assertTrue(recovered.undoGroupScoreIfCurrent(
                first.id, "g", "content", 20, null
            ))
            val undone = requireNotNull(recovered.getGroupAssessment(first.id, "g"))
            assertNull(undone.definitiveTotal)
            assertEquals("Grup iş birliği kanıtı", recovered.getGroupScores(undone.id)
                .single { it.criterionId == "content" }.evidenceNote)
            assertEquals(0, recovered.getGroupScores(
                requireNotNull(recovered.getGroupAssessment(second.id, "g")).id
            ).single { it.criterionId == "content" }.points)
            assertEquals(60, recovered.generatePdfReportSnapshot("class", "rubric", run.id)
                .students.first { it.number == "1" }.definitiveTotal)
        } finally {
            source.close()
            restored.close()
        }
    }

    @Test fun membershipSyncChangesOnlyChosenSessionAndReports() = runBlocking {
        val db = database()
        try {
            seed(db)
            db.studentDao().insertStudent(StudentEntity("s3", "class", "3", "Ece", "Kaya"))
            val repo = repository(db)
            val g1 = GroupTaskEntity("g1", "class", "", "A", "[\"s1\",\"s2\"]", "[]")
            val g2 = GroupTaskEntity("g2", "class", "", "B", "[\"s3\"]", "[]")
            repo.saveGroupTask(g1)
            repo.saveGroupTask(g2)
            val session1 = repo.createGroupSession("class", "rubric", "Ekim 1")
            val session2 = repo.createGroupSession("class", "rubric", "Ekim 2")

            for (session in listOf(session1, session2)) {
                repo.scoreGroup(session.id, g1.id, "content", 20)
                repo.scoreGroup(session.id, g1.id, "delivery", 50)
                repo.scoreGroup(session.id, g2.id, "content", 10)
                repo.scoreGroup(session.id, g2.id, "delivery", 80)
            }
            val before = repo.generateGroupPdfReportSnapshot(session2.id)
            assertEquals(listOf(70, 70, 90), before.students.map { it.definitiveTotal })

            // The group manager changes current membership, but not stored
            // historical membership until an explicit session sync.
            repo.saveGroupTask(g1.copy(memberStudentIdsJson = "[\"s1\"]"))
            repo.saveGroupTask(g2.copy(memberStudentIdsJson = "[\"s2\",\"s3\"]"))
            assertEquals(before, repo.generateGroupPdfReportSnapshot(session2.id))
            assertEquals(2, repo.syncGroupSessionMembership(session1.id))
            assertEquals(listOf(70, 90, 90),
                repo.generateGroupPdfReportSnapshot(session1.id).students.map { it.definitiveTotal })
            assertEquals(before, repo.generateGroupPdfReportSnapshot(session2.id))
        } finally {
            db.close()
        }
    }
}
