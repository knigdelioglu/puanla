package com.knigdelioglu.puanla

import android.database.Cursor
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.knigdelioglu.puanla.data.local.PuanlaDatabase
import com.knigdelioglu.puanla.data.repository.PuanlaRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RubricCatalogImportTest {
    @Test
    fun approvedWorkbookCatalogIsSeededLocallyAndIdempotently() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, PuanlaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val repository = PuanlaRepository(database) {
                context.assets.open("rubrics.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
            }

            repository.initialize()

            val rubrics = database.rubricDao().getAllRubrics()
            assertEquals(8, rubrics.size)
            assertTrue(rubrics.all { it.grade == 11 })
            val criteria = rubrics.flatMap { rubric -> database.rubricDao().getCriteriaForRubric(rubric.id) }
            assertEquals(48, criteria.size)
            rubrics.forEach { rubric ->
                val rubricCriteria = criteria.filter { it.rubricId == rubric.id }
                assertEquals(100, rubricCriteria.sumOf { it.maxPoints })
                assertTrue(database.rubricDao().getLevelsForRubric(rubric.id).isEmpty())
            }

            assertEquals(
                listOf(
                    "Konu ve amaç",
                    "Gözlem / inceleme / araştırma",
                    "Yöntem ve strateji",
                    "Süre ve hedef kitle",
                    "Araçlar / canlandırma biçimi",
                    "Mekân / teknik / görüntü / ses",
                    "İletişim engellerini giderme"
                ),
                repository.getGroupPreparationChecklist("rubric_11_iletisim_engelleri")
            )
            assertTrue(repository.getGroupPreparationChecklist("rubric_11_e_posta").isEmpty())

            assertEquals(0, countRows(database, "classrooms"))
            assertEquals(0, countRows(database, "students"))

            repository.initialize()
            assertEquals(8, database.rubricDao().getAllRubrics().size)
            assertEquals(48, rubrics.sumOf { database.rubricDao().getCriteriaForRubric(it.id).size })
        } finally {
            database.close()
        }
    }

    private fun countRows(database: PuanlaDatabase, table: String): Int {
        require(table == "classrooms" || table == "students")
        val cursor: Cursor = database.openHelper.readableDatabase.query("SELECT COUNT(*) FROM $table")
        return cursor.use {
            check(it.moveToFirst())
            it.getInt(0)
        }
    }
}
