package com.knigdelioglu.puanla

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.knigdelioglu.puanla.data.local.AssessmentRunEntity
import com.knigdelioglu.puanla.data.local.PuanlaDatabase
import com.knigdelioglu.puanla.data.local.RubricEntity
import com.knigdelioglu.puanla.data.service.RubricManagementService
import com.knigdelioglu.puanla.domain.rubric.RubricCriterionDraft
import com.knigdelioglu.puanla.domain.rubric.RubricDraft
import com.knigdelioglu.puanla.domain.rubric.RubricLevelDraft
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RubricManagementServiceTest {
    @Test
    fun createDuplicateAndDeleteUseIndependentRubricAndCriterionIds() = runBlocking<Unit> {
        val db = inMemoryDatabase()
        try {
            val service = RubricManagementService(db)
            val created = service.save(validDraft())
            val originalDetails = service.getDetails(created.id)

            val duplicate = service.duplicate(created.id)
            val duplicateDetails = service.getDetails(duplicate.id)

            assertNotEquals(created.id, duplicate.id)
            assertNotEquals(originalDetails.criteria.single().id, duplicateDetails.criteria.single().id)
            assertEquals(created.title + " (Kopya)", duplicate.title)
            assertEquals(originalDetails.levels.map { it.description }, duplicateDetails.levels.map { it.description })

            service.delete(duplicate.id)
            assertNull(db.rubricDao().getRubricById(duplicate.id))
            assertEquals(created.id, db.rubricDao().getRubricById(created.id)?.id)
        } finally {
            db.close()
        }
    }

    @Test
    fun rubricWithEvaluationHistoryCannotBeEditedOrDeleted() = runBlocking<Unit> {
        val db = inMemoryDatabase()
        try {
            val service = RubricManagementService(db)
            val created = service.save(validDraft())
            val detailsBefore = service.getDetails(created.id)
            db.assessmentRunDao().insert(AssessmentRunEntity("run-1", created.id, "Oturum"))

            assertThrows(IllegalArgumentException::class.java) {
                runBlocking { service.delete(created.id) }
            }
            assertThrows(IllegalArgumentException::class.java) {
                runBlocking { service.save(validDraft().copy(title = "Değişiklik"), created.id) }
            }

            val detailsAfter = service.getDetails(created.id)
            assertEquals(detailsBefore.rubric, detailsAfter.rubric)
            assertEquals(detailsBefore.criteria, detailsAfter.criteria)
            assertEquals(1, detailsAfter.usageCount)
        } finally {
            db.close()
        }
    }

    @Test
    fun approvedSourceRubricCannotBeDeleted() = runBlocking<Unit> {
        val db = inMemoryDatabase()
        try {
            db.rubricDao().insertRubric(RubricEntity("source", "Kaynak", "", 11, "Fen", isBuiltIn = true))
            val service = RubricManagementService(db)
            assertThrows(IllegalArgumentException::class.java) {
                runBlocking { service.delete("source") }
            }
            assertEquals("Kaynak", db.rubricDao().getRubricById("source")?.title)
        } finally {
            db.close()
        }
    }

    private fun inMemoryDatabase(): PuanlaDatabase = Room.inMemoryDatabaseBuilder(
        InstrumentationRegistry.getInstrumentation().targetContext,
        PuanlaDatabase::class.java
    ).allowMainThreadQueries().build()

    private fun validDraft() = RubricDraft(
        title = "Sunum Rubriği",
        subject = "Türkçe",
        description = "Sunum performansını değerlendirir.",
        grade = 11,
        criteria = listOf(
            RubricCriterionDraft(
                title = "Hazırlık",
                category = "Süreç",
                maxPoints = 100,
                levels = listOf(
                    RubricLevelDraft(25, "Başlangıç"),
                    RubricLevelDraft(50, "Gelişiyor"),
                    RubricLevelDraft(75, "Yetkin"),
                    RubricLevelDraft(100, "Örnek")
                )
            )
        )
    )
}
