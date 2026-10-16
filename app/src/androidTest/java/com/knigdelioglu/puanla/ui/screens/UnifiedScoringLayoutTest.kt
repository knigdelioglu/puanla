package com.knigdelioglu.puanla.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.knigdelioglu.arc.compose.foundation.ArcTheme
import com.knigdelioglu.puanla.ArcTestHostActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UnifiedScoringLayoutTest {
    @get:Rule val rule = createAndroidComposeRule<ArcTestHostActivity>()

    @Test
    fun compactIndividualAndGroupModesUseIdenticalSelectionNavigation() {
        val mode = mutableStateOf(ScoringTargetMode.INDIVIDUAL)
        val context = mutableStateOf("class/rubric/run-1")
        rule.setContent {
            ArcTheme(reducedMotion = true) {
                Box(Modifier.fillMaxSize()) {
                    UnifiedScoringLayout(
                        mode = mode.value,
                        contextKey = context.value,
                        modifier = Modifier.fillMaxSize(),
                        targetPane = { onSelected ->
                            Button(onClick = onSelected) { Text("Birimi seç") }
                        },
                        criteriaPane = { onSelected ->
                            Button(onClick = onSelected) { Text("Ölçütü seç") }
                        },
                        scoringPane = { Text("Puanlama alanı") }
                    )
                }
            }
        }

        rule.onNodeWithTag("unifiedScoringLayoutCompact").assertExists()
        rule.onNodeWithText("Öğrenciler").assertExists()
        rule.onNodeWithText("Birimi seç").performClick()
        rule.onNodeWithText("Puanlama alanı").assertIsDisplayed()

        rule.runOnIdle { mode.value = ScoringTargetMode.GROUP }
        rule.onNodeWithText("Gruplar").assertExists()
        rule.onNodeWithText("Birimi seç").assertIsDisplayed()
        rule.onNodeWithText("Birimi seç").performClick()
        rule.onNodeWithText("Puanlama alanı").assertIsDisplayed()

        // A different session restores the target list rather than keeping
        // a scoring panel from the previous session.
        rule.runOnIdle { context.value = "class/rubric/run-2" }
        rule.onNodeWithText("Birimi seç").assertIsDisplayed()
    }
}
