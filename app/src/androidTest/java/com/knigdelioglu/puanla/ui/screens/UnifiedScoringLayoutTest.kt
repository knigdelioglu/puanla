package com.knigdelioglu.puanla.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
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
    @Test
    fun wideTabletLayoutKeepsAllThreePanesForBothModes() {
        val mode = mutableStateOf(ScoringTargetMode.INDIVIDUAL)
        rule.setContent {
            ArcTheme(reducedMotion = true) {
                // Force the wide-layout branch even on a phone-sized CI emulator;
                // a physical 11-inch tablet is checked separately.
                UnifiedScoringLayout(
                    mode = mode.value,
                    contextKey = "class/rubric/run-wide",
                    modifier = Modifier.requiredWidth(1000.dp).height(600.dp),
                    targetPane = { _ -> Text("Hedefler sütunu") },
                    criteriaPane = { _ -> Text("Ölçütler sütunu") },
                    scoringPane = { Text("Puanlama sütunu") }
                )
            }
        }
        rule.onNodeWithTag("unifiedScoringLayoutWide").assertExists()
        rule.onNodeWithText("Hedefler sütunu").assertExists()
        rule.onNodeWithText("Ölçütler sütunu").assertExists()
        rule.onNodeWithText("Puanlama sütunu").assertExists()
        rule.runOnIdle { mode.value = ScoringTargetMode.GROUP }
        rule.onNodeWithTag("unifiedScoringLayoutWide").assertExists()
        rule.onNodeWithText("Hedefler sütunu").assertExists()
        rule.onNodeWithText("Ölçütler sütunu").assertExists()
        rule.onNodeWithText("Puanlama sütunu").assertExists()
    }

    @Test
    fun resizingFromCompactToWideAndBackPreservesSelectedScoringPaneButNewSessionResetsIt() {
        val paneWidth = mutableStateOf(820.dp)
        val session = mutableStateOf("class/rubric/run-one")
        val mode = mutableStateOf(ScoringTargetMode.INDIVIDUAL)
        rule.setContent {
            ArcTheme(reducedMotion = true) {
                UnifiedScoringLayout(
                    mode = mode.value,
                    contextKey = session.value,
                    modifier = Modifier.requiredWidth(paneWidth.value).height(600.dp),
                    targetPane = { onSelected ->
                        Button(onClick = onSelected) { Text("Hedef seç") }
                    },
                    criteriaPane = { onSelected ->
                        Button(onClick = onSelected) { Text("Ölçüt seç") }
                    },
                    scoringPane = { Text("Puanlama ekranı") }
                )
            }
        }

        rule.onNodeWithTag("unifiedScoringLayoutCompact").assertExists()
        rule.onNodeWithText("Hedef seç").performClick()
        rule.onNodeWithText("Puanlama ekranı").assertIsDisplayed()

        rule.runOnIdle { paneWidth.value = 1000.dp }
        rule.onNodeWithTag("unifiedScoringLayoutWide").assertExists()
        rule.onNodeWithText("Hedef seç").assertExists()
        rule.onNodeWithText("Ölçüt seç").assertExists()
        rule.onNodeWithText("Puanlama ekranı").assertExists()

        rule.runOnIdle { paneWidth.value = 820.dp }
        rule.onNodeWithTag("unifiedScoringLayoutCompact").assertExists()
        rule.onNodeWithText("Puanlama ekranı").assertIsDisplayed()
        rule.onNodeWithText("Hedef seç").assertDoesNotExist()

        rule.runOnIdle { session.value = "class/rubric/run-two" }
        rule.onNodeWithText("Hedef seç").assertIsDisplayed()
        rule.onNodeWithText("Puanlama ekranı").assertDoesNotExist()

        rule.onNodeWithText("Hedef seç").performClick()
        rule.runOnIdle { mode.value = ScoringTargetMode.GROUP }
        rule.onNodeWithText("Gruplar").assertExists()
        rule.onNodeWithText("Hedef seç").assertIsDisplayed()
    }

}
