package com.knigdelioglu.puanla.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.knigdelioglu.arc.compose.foundation.ArcTheme
import com.knigdelioglu.puanla.ArcTestHostActivity
import com.knigdelioglu.puanla.data.local.CriterionEntity
import com.knigdelioglu.puanla.ui.preferences.QuickGradingPreference
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CriterionGradingEditorTest {
    @get:Rule val rule = createAndroidComposeRule<ArcTestHostActivity>()

    private val criterion = CriterionEntity(
        id = "content", rubricId = "rubric", title = "İçerik",
        description = "", maxPoints = 20, orderIndex = 0
    )

    @Test
    fun quickScoreZeroAdvancesButPrecisionEditsStayOnCurrentCriterion() {
        val score = mutableStateOf<Int?>(null)
        val nextCount = mutableStateOf(0)
        val context = mutableStateOf("individual/run/student/content")
        rule.setContent {
            ArcTheme(reducedMotion = true) {
                Column {
                    CriterionGradingEditor(
                        contextKey = context.value, criterion = criterion,
                        points = score.value, levels = emptyList(),
                        quickGrading = QuickGradingPreference(),
                        autoAdvance = true, onAutoAdvanceChange = {},
                        hasNextCriterion = true,
                        onNextCriterion = { nextCount.value += 1 },
                        onScore = { score.value = it },
                        onClearScore = { score.value = null }
                    )
                    Text("Next calls: ${nextCount.value}")
                }
            }
        }

        rule.onNodeWithText("0 P").performClick()
        rule.runOnIdle { assertEquals(0, score.value); assertEquals(1, nextCount.value) }

        rule.onNodeWithText("Hassas puan").performClick()
        rule.onNodeWithText("15 P").performClick()
        rule.runOnIdle { assertEquals(15, score.value); assertEquals(1, nextCount.value) }
        rule.onNodeWithText("Sonraki ölçüt").assertIsDisplayed()

        rule.onNodeWithText("Puanı kaldır").performClick()
        rule.runOnIdle { assertEquals(null, score.value); assertEquals(1, nextCount.value) }

        rule.runOnIdle { context.value = "group/session/group-1/content" }
        rule.onNodeWithText("Hassas puan").assertIsDisplayed()
        rule.onNodeWithText("Sonraki ölçüt").assertDoesNotExist()
    }

    @Test
    fun editingNoteStopsQuickAutoAdvanceWithoutChangingPreference() {
        val score = mutableStateOf<Int?>(null)
        val editingNote = mutableStateOf(true)
        val nextCount = mutableStateOf(0)
        rule.setContent {
            ArcTheme(reducedMotion = true) {
                Column {
                    Button(onClick = { editingNote.value = !editingNote.value }) {
                        Text("Not düzenlemeyi değiştir")
                    }
                    CriterionGradingEditor(
                        contextKey = "individual/run/student/content",
                        criterion = criterion, points = score.value,
                        levels = emptyList(), quickGrading = QuickGradingPreference(),
                        autoAdvance = true, onAutoAdvanceChange = {},
                        hasNextCriterion = true,
                        noteEditorOpen = editingNote.value,
                        onNextCriterion = { nextCount.value += 1 },
                        onScore = { score.value = it },
                        onClearScore = { score.value = null }
                    )
                }
            }
        }
        rule.onNodeWithText("5 P").performClick()
        rule.runOnIdle { assertEquals(0, nextCount.value) }
        rule.onNodeWithText("Sonraki ölçüt").assertIsDisplayed()
        rule.onNodeWithText("Not düzenlemeyi değiştir").performClick()
        rule.onNodeWithText("10 P").performClick()
        rule.runOnIdle { assertEquals(1, nextCount.value) }
    }
}
