package com.knigdelioglu.puanla

import android.app.Application
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.knigdelioglu.puanla.data.local.ClassroomEntity
import com.knigdelioglu.puanla.data.local.PuanlaDatabase
import com.knigdelioglu.puanla.data.local.StudentEntity
import com.knigdelioglu.puanla.data.repository.PuanlaRepository
import com.knigdelioglu.puanla.ui.preferences.MotionPreference
import com.knigdelioglu.puanla.ui.viewmodel.AppDestination
import com.knigdelioglu.puanla.ui.viewmodel.PuanlaViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppHeaderScrollTest {
    @get:Rule val composeRule = createAndroidComposeRule<ArcTestHostActivity>()

    companion object {
        private val openDatabases = mutableListOf<PuanlaDatabase>()
    }

    @Test
    fun sharedHeaderHidesOnScrollAndReturnsWhenScrollingBackOnClassroomScreen() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, PuanlaDatabase::class.java)
            .allowMainThreadQueries().build().also { openDatabases.add(it) }
        val classroom = ClassroomEntity("header-scroll-class", 11, "H", "11-H", "2026-2027")
        runBlocking {
            database.classroomDao().insertClassroom(classroom)
            repeat(30) { index ->
                database.studentDao().insertStudent(
                    StudentEntity("header-scroll-$index", classroom.id, index.toString(), "Ad$index", "Soyad$index")
                )
            }
        }
        val repository = PuanlaRepository(database) {
            context.assets.open("rubrics.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
        }
        val viewModel = PuanlaViewModel(context.applicationContext as Application, database, repository)
        val store = ViewModelStore().also { it.put("header-scroll", viewModel) }
        viewModel.setReducedMotion(MotionPreference.Reduced)
        viewModel.currentDestination.value = AppDestination.CLASSROOMS

        try {
            composeRule.setContent { PuanlaApp(viewModel = viewModel) }
            composeRule.waitUntil(timeoutMillis = 15_000) {
                viewModel.selectedClassroom.value?.id == classroom.id && viewModel.students.value.size == 30
            }

            composeRule.onNodeWithText("Puanla").assertIsDisplayed()
            composeRule.onNodeWithTag("classroomManagementScroll")
                .performTouchInput { swipeUp() }
            composeRule.onNodeWithText("Puanla").assertDoesNotExist()

            composeRule.onNodeWithTag("classroomManagementScroll")
                .performTouchInput { swipeDown() }
            composeRule.onNodeWithText("Puanla").assertIsDisplayed()

            composeRule.onNodeWithText("Puanlama").performClick()
            composeRule.waitUntil(timeoutMillis = 5_000) {
                viewModel.currentDestination.value == AppDestination.WORKSPACE &&
                    viewModel.selectedRubric.value != null
            }
            composeRule.onNodeWithText("Yeni Oturum Başlat").performClick()
            composeRule.onNodeWithText("Yeni Değerlendirme Oturumu").assertIsDisplayed()
        } finally {
            store.clear()
        }
    }
}
