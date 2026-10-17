package com.knigdelioglu.puanla

import android.app.Application
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.knigdelioglu.arc.compose.foundation.ArcTheme
import com.knigdelioglu.puanla.data.local.AssessmentEntity
import com.knigdelioglu.puanla.data.local.AssessmentRunEntity
import com.knigdelioglu.puanla.data.local.CriterionScoreEntity
import com.knigdelioglu.puanla.data.local.PuanlaDatabase
import com.knigdelioglu.puanla.data.local.RubricEntity
import com.knigdelioglu.puanla.data.local.ClassroomEntity
import com.knigdelioglu.puanla.data.local.StudentEntity
import com.knigdelioglu.puanla.data.repository.PuanlaRepository
import com.knigdelioglu.puanla.ui.screens.ReportsScreen
import com.knigdelioglu.puanla.ui.viewmodel.PuanlaViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReportsCenterScreenTest {
    @get:Rule val composeRule = createAndroidComposeRule<ArcTestHostActivity>()

    @Test
    fun showsRealStudentGrowthAndSelectedSessionReport(): Unit = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, PuanlaDatabase::class.java)
            .allowMainThreadQueries().build()
        val classroom = ClassroomEntity("reports-center-class", 11, "R", "11-R", "2026-2027")
        val student = StudentEntity("reports-center-student", classroom.id, "911", "Ada", "Deneme")
        database.classroomDao().insertClassroom(classroom)
        database.studentDao().insertStudent(student)

        val repository = PuanlaRepository(database) {
            context.assets.open("rubrics.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
        }
        val viewModel = PuanlaViewModel(context.applicationContext as Application, database, repository)
        val store = ViewModelStore().also { it.put("reports-center", viewModel) }
        try {
            composeRule.setContent { ArcTheme(reducedMotion = true) { ReportsScreen(viewModel) } }
            composeRule.waitUntil(timeoutMillis = 15_000) {
                viewModel.selectedClassroom.value?.id == classroom.id &&
                    viewModel.rubrics.value.any { it.grade == classroom.grade }
            }
            val rubric = requireNotNull(viewModel.rubrics.value.firstOrNull { it.grade == classroom.grade })
            viewModel.selectRubric(rubric)
            composeRule.waitUntil(timeoutMillis = 10_000) { viewModel.selectedRubric.value?.id == rubric.id }
            val criteria = repository.getCriteriaForRubric(rubric.id)
            assertTrue(criteria.isNotEmpty())

            val run1 = repository.createAssessmentRun(rubric.id, "Gelişim Öncesi", listOf(classroom.id))
            val run2 = repository.createAssessmentRun(rubric.id, "Gelişim Sonrası", listOf(classroom.id))
            listOf(run1, run2).forEachIndexed { index, run ->
                val total = criteria.sumOf { it.maxPoints }
                val achieved = if (index == 0) 60 else 90
                val scores = criteria.map { criterion ->
                    val points = (criterion.maxPoints * achieved / 100).coerceIn(0, criterion.maxPoints)
                    criterion.id to points
                }
                val definitiveTotal = scores.sumOf { it.second }
                val assessmentId = "reports-center-assessment-${index + 1}"
                database.assessmentDao().insertOrUpdateAssessment(
                    AssessmentEntity(
                        id = assessmentId,
                        classroomId = classroom.id,
                        studentId = student.id,
                        rubricId = rubric.id,
                        isCompleted = true,
                        definitiveTotal = definitiveTotal,
                        scoredCount = criteria.size,
                        observationNote = if (index == 0) "İlk oturum gözlemi" else "Son oturum gözlemi",
                        runId = run.id
                    )
                )
                database.assessmentDao().insertOrUpdateScores(scores.mapIndexed { scoreIndex, pair ->
                    CriterionScoreEntity(
                        id = "reports-center-score-$index-$scoreIndex",
                        assessmentId = assessmentId,
                        criterionId = pair.first,
                        points = pair.second
                    )
                })
                assertTrue(total > 0)
            }

            composeRule.waitUntil(timeoutMillis = 10_000) {
                viewModel.assessmentRuns.value.any { it.id == run2.id }
            }
            viewModel.selectRun(run2)

            composeRule.waitUntil(timeoutMillis = 20_000) {
                composeRule.onAllNodesWithTag("studentGrowthChart").fetchSemanticsNodes().isNotEmpty()
            }
            composeRule.waitUntil(timeoutMillis = 20_000) {
                composeRule.onAllNodesWithText("İlk oturum gözlemi", substring = true).fetchSemanticsNodes().isNotEmpty()
            }
            composeRule.onNodeWithText("Öğrenci Gelişim Grafikleri ve Raporlama Merkezi").assertExists()
            composeRule.onNodeWithText("Ada Deneme (#911)").assertExists()
            composeRule.onNodeWithTag("studentGrowthChart").assertExists()
            composeRule.onNodeWithText("Ölçüt Bazlı Ortalama Yetkinlik Düzeyi").assertExists()
            composeRule.onNodeWithText("İlk oturum gözlemi", substring = true).assertExists()
            composeRule.onNodeWithText("2. Gelişim Sonrası").assertExists()
            composeRule.onNodeWithText("Sınıf Değerlendirme Sonuçları").assertExists()
        } finally {
            store.clear()
            database.close()
        }
    }
}
