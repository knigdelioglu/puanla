package com.knigdelioglu.puanla

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.knigdelioglu.arc.compose.foundation.ArcTheme
import com.knigdelioglu.puanla.data.local.ClassroomEntity
import com.knigdelioglu.puanla.data.local.CriterionEntity
import com.knigdelioglu.puanla.data.local.CriterionLevelEntity
import com.knigdelioglu.puanla.data.local.GroupTaskEntity
import com.knigdelioglu.puanla.data.local.PuanlaDatabase
import com.knigdelioglu.puanla.data.local.RubricEntity
import com.knigdelioglu.puanla.data.local.StudentEntity
import com.knigdelioglu.puanla.data.repository.PuanlaRepository
import com.knigdelioglu.puanla.ui.screens.GroupWorkScreen
import com.knigdelioglu.puanla.ui.viewmodel.PuanlaViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GroupEvaluationScreenTest {
    @get:Rule val composeRule = createAndroidComposeRule<ArcTestHostActivity>()

    @Test
    fun wideGroupBoardScoresCriterionAndPublishesOnlyGroupResult(): Unit = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, PuanlaDatabase::class.java)
            .allowMainThreadQueries().build()
        val classroom = ClassroomEntity("group-screen-class", 11, "T", "11-T", "2026-2027")
        val student = StudentEntity("group-screen-student", classroom.id, "901", "Deneme", "Öğrenci")
        val rubric = RubricEntity("group-screen-rubric", "Ekran Rubriği", "", 11, "Sunum", false)
        val criterion = CriterionEntity("group-screen-criterion", rubric.id, "Sunum Becerisi", "", 4, 0)
        val group = GroupTaskEntity(
            id = "group-screen-team",
            classroomId = classroom.id,
            rubricId = "",
            title = "Deneme Ekibi",
            memberStudentIdsJson = "[\"${student.id}\"]",
            checklistJson = "[]"
        )
        database.classroomDao().insertClassroom(classroom)
        database.studentDao().insertStudent(student)
        database.rubricDao().insertRubric(rubric)
        database.rubricDao().insertCriteria(listOf(criterion))
        database.rubricDao().insertLevels((1..4).map { points ->
            CriterionLevelEntity(
                id = "group-screen-level-$points",
                criterionId = criterion.id,
                points = points,
                description = "Düzey $points açıklaması",
                orderIndex = points - 1
            )
        })

        val repository = PuanlaRepository(database) {
            context.assets.open("rubrics.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
        }
        val viewModel = PuanlaViewModel(context.applicationContext as Application, database, repository)
        val store = ViewModelStore().also { it.put("group-evaluation-screen", viewModel) }
        try {
            composeRule.setContent {
                ArcTheme(reducedMotion = true) {
                    Box(Modifier.fillMaxSize()) {
                        Box(Modifier.requiredWidth(1120.dp).height(820.dp)) {
                            GroupWorkScreen(viewModel)
                        }
                    }
                }
            }
            composeRule.waitUntil(timeoutMillis = 15_000) {
                viewModel.classrooms.value.any { it.id == classroom.id } &&
                    viewModel.rubrics.value.any { it.id == rubric.id }
            }
            viewModel.selectClassroom(classroom)
            composeRule.waitUntil(timeoutMillis = 10_000) {
                viewModel.selectedClassroom.value?.id == classroom.id
            }
            viewModel.selectRubric(rubric)
            composeRule.waitUntil(timeoutMillis = 10_000) {
                viewModel.selectedRubric.value?.id == rubric.id
            }
            repository.saveGroupTask(group)
            val session = repository.createGroupSession(classroom.id, rubric.id, "Sunum Değerlendirmesi")
            composeRule.waitUntil(timeoutMillis = 10_000) {
                viewModel.groupTasks.value.any { it.id == group.id } &&
                    viewModel.groupSessions.value.any { it.id == session.id }
            }
            viewModel.selectGroupSession(session)
            composeRule.waitUntil(timeoutMillis = 10_000) {
                viewModel.selectedGroup.value?.id == group.id &&
                    viewModel.selectedGroupSession.value?.id == session.id &&
                    viewModel.groupCriteria.value.any { it.id == criterion.id }
            }

            composeRule.onNodeWithTag("groupEvaluationWideLayout").assertExists()
            composeRule.onNodeWithTag("groupEvaluationBoard").assertExists()
            composeRule.onNodeWithText("Çalışma Grupları (1)").assertExists()
            composeRule.onNodeWithText("Grup Değerlendirmesi · 1 Öğrenci").assertExists()
            composeRule.onNodeWithText("01. Sunum Becerisi").assertIsDisplayed()
            composeRule.onNodeWithText("Destek Gerekli").assertExists()
            composeRule.onNodeWithText("Grup Gözlem Notu · Sunum Becerisi").assertExists()

            composeRule.onNodeWithText("4 Puan").performClick()
            composeRule.waitUntil(timeoutMillis = 10_000) {
                runBlocking {
                    repository.getGroupAssessment(session.id, group.id)?.isCompleted == true
                }
            }
            composeRule.onNodeWithText("Grup Puanlarını Üyelere Aktar").performClick()
            composeRule.waitUntil(timeoutMillis = 10_000) {
                runBlocking {
                    repository.getGroupAssessment(session.id, group.id)?.let { assessment ->
                        repository.getGroupMemberResults(assessment.id).singleOrNull()?.isCompleted == true
                    } == true
                }
            }
            val assessment = requireNotNull(repository.getGroupAssessment(session.id, group.id))
            assertEquals(4, assessment.definitiveTotal)
            assertTrue(repository.getGroupMemberResults(assessment.id).single().isCompleted)
            assertNull(repository.getAssessmentForStudent(student.id, rubric.id))
        } finally {
            store.clear()
            database.close()
        }
    }
}
