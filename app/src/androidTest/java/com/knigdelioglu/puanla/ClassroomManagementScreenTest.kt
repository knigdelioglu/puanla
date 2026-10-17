package com.knigdelioglu.puanla

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.knigdelioglu.arc.compose.foundation.ArcTheme
import com.knigdelioglu.puanla.data.local.ClassroomEntity
import com.knigdelioglu.puanla.data.local.PuanlaDatabase
import com.knigdelioglu.puanla.data.repository.PuanlaRepository
import com.knigdelioglu.puanla.ui.screens.ClassroomsScreen
import com.knigdelioglu.puanla.ui.viewmodel.PuanlaViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ClassroomManagementScreenTest {
    @get:Rule val composeRule = createAndroidComposeRule<ArcTestHostActivity>()

    @Test
    fun pastedRosterUsesTeacherReviewBeforeSavingStudents() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, PuanlaDatabase::class.java)
            .allowMainThreadQueries().build()
        val classroom = ClassroomEntity("paste-screen-class", 11, "P", "11-P", "2026-2027")
        runBlocking { database.classroomDao().insertClassroom(classroom) }
        val repository = PuanlaRepository(database) {
            context.assets.open("rubrics.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
        }
        val viewModel = PuanlaViewModel(context.applicationContext as Application, database, repository)
        val store = ViewModelStore().also { it.put("classroom-management", viewModel) }
        viewModel.setReducedMotion(com.knigdelioglu.puanla.ui.preferences.MotionPreference.Reduced)

        try {
            composeRule.setContent { ArcTheme(reducedMotion = true) { ClassroomsScreen(viewModel) } }
            composeRule.waitUntil(timeoutMillis = 15_000) {
                viewModel.selectedClassroom.value?.id == classroom.id
            }
            composeRule.onNodeWithText("Sınıf ve Öğrenci Yönetimi").assertIsDisplayed()
            composeRule.onNodeWithTag("classroomSelector").assertIsDisplayed()
            composeRule.onNodeWithText("Elle Öğrenci Ekle (11-P)").assertIsDisplayed()
            composeRule.onNodeWithText("11-P Öğrenci Listesi (0 Öğrenci)").assertIsDisplayed()

            composeRule.onNodeWithText("Metin / Liste Yapıştır").performClick()
            composeRule.onNodeWithTag("pastedRosterInput").performTextInput("501 - Ada Su Deneme")
            composeRule.onNodeWithText("Listeyi Ayrıştır ve İncele").performClick()
            composeRule.waitUntil(timeoutMillis = 10_000) {
                viewModel.ocrStep.value == 2 && viewModel.ocrCandidates.value.size == 1
            }
            assertFalse(viewModel.ocrCandidates.value.single().isApproved)
            assertEquals(0, runBlocking { database.studentDao().countForClassroom(classroom.id) })

            composeRule.onNodeWithText("Güvenilirleri Onayla (1)").performClick()
            composeRule.waitUntil(timeoutMillis = 5_000) { viewModel.ocrCandidates.value.single().isApproved }
            composeRule.onNodeWithText("Onaylananları Aktar (1)").performClick()
            composeRule.onNodeWithText("Aktarımı Onayla").performClick()
            composeRule.waitUntil(timeoutMillis = 10_000) {
                viewModel.ocrStep.value == 3 && viewModel.students.value.size == 1
            }
            val saved = runBlocking { database.studentDao().getStudentsForClassroom(classroom.id).single() }
            assertEquals("501", saved.studentNumber)
            assertEquals("Ada Su", saved.firstName)
            assertEquals("DENEME", saved.lastName)
        } finally {
            store.clear()
            database.close()
        }
    }
}
