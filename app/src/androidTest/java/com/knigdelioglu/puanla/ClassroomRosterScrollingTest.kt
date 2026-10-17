package com.knigdelioglu.puanla

import android.app.Application
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.knigdelioglu.puanla.data.local.ClassroomEntity
import com.knigdelioglu.puanla.data.local.StudentEntity
import com.knigdelioglu.puanla.data.local.PuanlaDatabase
import com.knigdelioglu.puanla.data.repository.PuanlaRepository
import com.knigdelioglu.puanla.ui.screens.ClassroomsScreen
import com.knigdelioglu.puanla.ui.viewmodel.PuanlaViewModel
import kotlinx.coroutines.runBlocking
import com.knigdelioglu.arc.compose.datavis.ArcSortableDataTable
import com.knigdelioglu.arc.compose.datavis.ArcTableColumn
import com.knigdelioglu.arc.compose.foundation.ArcTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ClassroomRosterScrollingTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ArcTestHostActivity>()

    // ViewModel collectors may finish cancellation after a test; keep DB open
    // until instrumentation finishes, like the other repository UI tests.
    companion object {
        private val databases = mutableListOf<PuanlaDatabase>()
    }

    @Test
    fun realClassroomScreenShowsCompactControlsAndScrollableRoster() {
        runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, PuanlaDatabase::class.java)
            .allowMainThreadQueries().build().also { databases.add(it) }
        val classroom = ClassroomEntity(
            id = "roster-layout-class", grade = 11, section = "T",
            name = "11-T", academicYear = "2026-2027"
        )
        db.classroomDao().insertClassroom(classroom)
        repeat(30) { index ->
            db.studentDao().insertStudent(
                StudentEntity(
                    id = "roster-layout-$index", classroomId = classroom.id,
                    studentNumber = (100 + index).toString(),
                    firstName = "Ad$index", lastName = "Soyad$index"
                )
            )
        }
        val repository = PuanlaRepository(db) {
            context.assets.open("rubrics.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
        }
        val viewModel = PuanlaViewModel(context.applicationContext as Application, db, repository)
        val store = ViewModelStore().also { it.put("roster-layout", viewModel) }

        try {
            composeRule.setContent {
                ArcTheme(reducedMotion = true) { ClassroomsScreen(viewModel) }
            }
            composeRule.waitUntil(timeoutMillis = 15_000) {
                viewModel.students.value.size == 30 &&
                    viewModel.selectedClassroom.value?.id == classroom.id
            }
            // The reference layout keeps a clear page heading and class tabs.
            composeRule.onNodeWithText("Sınıf ve Öğrenci Yönetimi").assertIsDisplayed()
            composeRule.onNodeWithTag("classroomSelector").assertExists()
            composeRule.onNodeWithTag("studentRosterControls").assertExists()
            // The whole page scrolls to the full-height roster viewport.
            composeRule.onNodeWithTag("studentRosterTable").performScrollTo().assertIsDisplayed()
            composeRule.onNodeWithContentDescription("Sonraki Sayfa").performScrollTo().performClick()
            composeRule.onNode(hasScrollToIndexAction() and hasAnyAncestor(hasTestTag("studentRosterTable")))
                .performScrollToIndex(4)
            composeRule.onNodeWithText("Ad29", substring = true).assertIsDisplayed()
        } finally {
            store.clear()
        }
        }
    }

    @Test
    fun pageAndRosterCanBothScrollWithoutRestrictingRosterToBottomStrip() {
        val students = (1..25).map { "Öğrenci $it" }
        composeRule.setContent {
            ArcTheme(reducedMotion = true) {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    Text("Sınıf Yönetimi")
                    Spacer(Modifier.height(400.dp))
                    ArcSortableDataTable(
                        items = students,
                        columns = listOf(ArcTableColumn<String>("Öğrenci") { Text(it) }),
                        modifier = Modifier.height(620.dp),
                        pageSize = 25
                    )
                    Text("Sayfa Altı")
                }
            }
        }

        composeRule.onNodeWithText("Sayfa Altı").performScrollTo().assertIsDisplayed()
        composeRule.onNode(hasScrollToIndexAction()).performScrollToIndex(24)
        composeRule.onNodeWithText("Öğrenci 25").assertIsDisplayed()
    }

    @Test
    fun rosterPaginationStillWorksForMoreThanTwentyFiveStudents() {
        val students = (1..30).map { "Öğrenci $it" }
        composeRule.setContent {
            ArcTheme(reducedMotion = true) {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    ArcSortableDataTable(
                        items = students,
                        columns = listOf(ArcTableColumn<String>("Öğrenci") { Text(it) }),
                        modifier = Modifier.height(620.dp),
                        pageSize = 25
                    )
                }
            }
        }

        composeRule.onNodeWithText("Öğrenci 30").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Sonraki Sayfa").performScrollTo().performClick()
        composeRule.onNode(hasScrollToIndexAction()).performScrollToIndex(4)
        composeRule.onNodeWithText("Öğrenci 30").assertIsDisplayed()
    }
}
