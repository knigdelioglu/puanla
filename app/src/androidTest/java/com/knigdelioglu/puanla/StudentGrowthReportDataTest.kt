package com.knigdelioglu.puanla

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.knigdelioglu.puanla.data.local.AssessmentEntity
import com.knigdelioglu.puanla.data.local.AssessmentRunEntity
import com.knigdelioglu.puanla.data.local.ClassroomEntity
import com.knigdelioglu.puanla.data.local.CriterionEntity
import com.knigdelioglu.puanla.data.local.CriterionScoreEntity
import com.knigdelioglu.puanla.data.local.PuanlaDatabase
import com.knigdelioglu.puanla.data.local.RubricEntity
import com.knigdelioglu.puanla.data.local.StudentEntity
import com.knigdelioglu.puanla.data.repository.PuanlaRepository
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StudentGrowthReportDataTest {
    @Test
    fun progressHistoryUsesSessionDatesAndCriterionAveragesUseFrozenRubrics() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, PuanlaDatabase::class.java)
            .allowMainThreadQueries().build()
        try {
            val classroom = ClassroomEntity("growth-class", 11, "G", "11-G", "2026-2027")
            val student = StudentEntity("growth-student", classroom.id, "811", "Ada", "Örnek")
            val rubric = RubricEntity("growth-rubric", "Gözlem Rubriği", "", 11, "Fen")
            val criterionV1 = CriterionEntity("growth-criterion", rubric.id, "Düzeneği Kurma", "", 4, 0)
            val criterionV2 = criterionV1.copy(maxPoints = 8)
            database.classroomDao().insertClassroom(classroom)
            database.rubricDao().insertRubric(rubric)
            database.rubricDao().insertCriteria(listOf(criterionV1))
            database.studentDao().insertStudent(student)

            val firstRun = AssessmentRunEntity(
                "growth-run-1", rubric.id, "İlk Oturum",
                criteriaSnapshotJson = Json.encodeToString(listOf(criterionV1)),
                createdAt = 1_000L
            )
            val secondRun = AssessmentRunEntity(
                "growth-run-2", rubric.id, "İkinci Oturum",
                criteriaSnapshotJson = Json.encodeToString(listOf(criterionV2)),
                createdAt = 2_000L
            )
            val partialRun = AssessmentRunEntity(
                "growth-run-partial", rubric.id, "Eksik Oturum",
                criteriaSnapshotJson = Json.encodeToString(listOf(criterionV2)),
                createdAt = 3_000L
            )
            database.assessmentRunDao().insert(firstRun)
            database.assessmentRunDao().insert(secondRun)
            database.assessmentRunDao().insert(partialRun)

            database.assessmentDao().insertOrUpdateAssessment(
                AssessmentEntity(
                    "growth-assessment-1", classroom.id, student.id, rubric.id,
                    isCompleted = true, definitiveTotal = 3, scoredCount = 1,
                    observationNote = "İlk gözlem", lastModifiedAt = 9_000L, runId = firstRun.id
                )
            )
            database.assessmentDao().insertOrUpdateAssessment(
                AssessmentEntity(
                    "growth-assessment-2", classroom.id, student.id, rubric.id,
                    isCompleted = true, definitiveTotal = 6, scoredCount = 1,
                    observationNote = "İkinci gözlem", lastModifiedAt = 500L, runId = secondRun.id
                )
            )
            database.assessmentDao().insertOrUpdateAssessment(
                AssessmentEntity(
                    "growth-assessment-partial", classroom.id, student.id, rubric.id,
                    isCompleted = false, definitiveTotal = null, scoredCount = 1,
                    observationNote = "Eksik kayıt", runId = partialRun.id
                )
            )
            database.assessmentDao().insertOrUpdateScores(listOf(
                CriterionScoreEntity("growth-score-1", "growth-assessment-1", criterionV1.id, 3),
                CriterionScoreEntity("growth-score-2", "growth-assessment-2", criterionV1.id, 6),
                CriterionScoreEntity("growth-score-partial", "growth-assessment-partial", criterionV1.id, 8)
            ))

            val repository = PuanlaRepository(database) { "" }
            val history = repository.getCompletedRunScoresForStudent(classroom.id, student.id)
            val averages = repository.getStudentCriterionAverages(classroom.id, student.id)

            assertEquals(listOf(firstRun.id, secondRun.id), history.map { it.runId })
            assertEquals(listOf(1_000L, 2_000L), history.map { it.assessedAt })
            assertEquals(listOf("İlk gözlem", "İkinci gözlem"), history.map { it.observationNote })
            assertEquals(1, averages.size)
            assertEquals("Düzeneği Kurma", averages.single().title)
            assertEquals(75, averages.single().averagePercent)
            assertEquals(2, averages.single().observations)
        } finally {
            database.close()
        }
    }
}
