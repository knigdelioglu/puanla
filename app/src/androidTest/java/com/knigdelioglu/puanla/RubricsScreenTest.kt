package com.knigdelioglu.puanla

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.knigdelioglu.puanla.data.local.PuanlaDatabase
import com.knigdelioglu.puanla.data.repository.PuanlaRepository
import com.knigdelioglu.puanla.ui.preferences.MotionPreference
import com.knigdelioglu.puanla.ui.viewmodel.AppDestination
import com.knigdelioglu.puanla.ui.viewmodel.PuanlaViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RubricsScreenTest {
    @get:Rule val composeRule = createAndroidComposeRule<ArcTestHostActivity>()

    @Test
    fun rubricsTabOpensDesignerAndSavesASeparateRubric() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, PuanlaDatabase::class.java)
            .allowMainThreadQueries().build()
        val repository = PuanlaRepository(database) {
            context.assets.open("rubrics.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
        }
        val viewModel = PuanlaViewModel(context.applicationContext as Application, database, repository)
        val store = ViewModelStore().also { it.put("rubrics-screen", viewModel) }
        viewModel.setReducedMotion(MotionPreference.Reduced)

        try {
            composeRule.setContent { PuanlaApp(viewModel = viewModel) }
            composeRule.waitUntil(timeoutMillis = 15_000) { viewModel.rubrics.value.isNotEmpty() }
            composeRule.waitForIdle()

            composeRule.onNodeWithText("Puanla").assertIsDisplayed()
            composeRule.onNodeWithText("Rubrikler").performClick()
            composeRule.waitUntil(timeoutMillis = 5_000) {
                viewModel.currentDestination.value == AppDestination.RUBRICS
            }
            composeRule.onNodeWithText("Dereceli Puanlama Anahtarı (Rubrik) Tasarımcısı").assertIsDisplayed()
            composeRule.onNodeWithTag("newRubricButton").performClick()
            composeRule.onNodeWithText("Rubrik Düzenleyici: Yeni Değerlendirme Rubriği").assertIsDisplayed()
            composeRule.onNodeWithTag("rubricTitleField").performTextReplacement("Tablet Sunum Rubriği")
            composeRule.onNodeWithTag("rubricSaveButton").performClick()

            composeRule.waitUntil(timeoutMillis = 10_000) {
                viewModel.rubrics.value.any { it.title == "Tablet Sunum Rubriği" }
            }
            composeRule.onNodeWithText("Tablet Sunum Rubriği").assertIsDisplayed()
            runBlocking {
                val saved = viewModel.rubrics.value.first { it.title == "Tablet Sunum Rubriği" }
                val criteria = viewModel.repository.getCriteriaForRubric(saved.id)
                assertEquals(2, criteria.size)
                assertEquals(100, criteria.sumOf { it.maxPoints })
            }
        } finally {
            store.clear()
            database.close()
        }
    }
}
