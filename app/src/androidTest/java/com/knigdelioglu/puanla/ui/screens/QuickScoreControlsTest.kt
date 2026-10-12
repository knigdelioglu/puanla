package com.knigdelioglu.puanla.ui.screens

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.knigdelioglu.arc.compose.foundation.ArcTheme
import com.knigdelioglu.puanla.ArcTestHostActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class QuickScoreControlsTest {
    @get:Rule
    val rule = createAndroidComposeRule<ArcTestHostActivity>()

    @Test
    fun oneTapGradeIsDistinctFromUnscoredAndSwitchCanDisableAutoAdvance() {
        val score = mutableStateOf<Int?>(null)
        val autoAdvance = mutableStateOf(true)
        var nextCriterionCalls = 0
        rule.setContent {
            ArcTheme(reducedMotion = true) {
                QuickScoreControls(
                    maxPoints = 20,
                    points = score.value,
                    levels = emptyList(),
                    autoAdvance = autoAdvance.value,
                    onAutoAdvanceChange = { autoAdvance.value = it },
                    hasNextCriterion = true,
                    onNextCriterion = { nextCriterionCalls += 1 },
                    onChooseScore = { score.value = it }
                )
            }
        }

        rule.onNodeWithText("Sonraki ölçüt").assertDoesNotExist()
        rule.onNodeWithText("0 P").performClick()
        rule.runOnIdle { assertEquals(0, score.value) }
        rule.onNodeWithText("15 P").performClick()
        rule.runOnIdle { assertEquals(15, score.value) }
        rule.onNodeWithText("Otomatik sonraki ölçüt").performClick()
        rule.runOnIdle { assertFalse(autoAdvance.value) }
        rule.onNodeWithText("Sonraki ölçüt").assertIsDisplayed().performClick()
        rule.runOnIdle { assertEquals(1, nextCriterionCalls) }
    }

    @Test
    fun rubricWithFivePointMaximumShowsBoundedNumericOptions() {
        rule.setContent {
            ArcTheme(reducedMotion = true) {
                QuickScoreControls(
                    maxPoints = 5,
                    points = null,
                    levels = emptyList(),
                    autoAdvance = true,
                    onAutoAdvanceChange = {},
                    hasNextCriterion = false,
                    onNextCriterion = {},
                    onChooseScore = {}
                )
            }
        }
        rule.onNodeWithText("5 P").assertIsDisplayed()
        rule.onNodeWithText("20 P").assertDoesNotExist()
    }
}
