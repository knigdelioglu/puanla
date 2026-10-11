package com.knigdelioglu.puanla

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assertIsDisplayed
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.knigdelioglu.arc.compose.controls.ArcCalendar
import com.knigdelioglu.arc.compose.controls.ArcConfirmMorph
import com.knigdelioglu.arc.compose.datavis.ArcActivityHeatmap
import com.knigdelioglu.arc.compose.foundation.ArcTheme
import com.knigdelioglu.arc.compose.navigation.ArcCarousel
import com.knigdelioglu.arc.compose.navigation.ArcPagination
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ArcP1InteractionsTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ArcTestHostActivity>()

    @Test
    fun calendarSelectionReturnsTheSelectedIsoDate() {
        val selected = mutableStateOf("")
        composeRule.setContent {
            ArcTheme {
                ArcCalendar(
                    selectedDay = null,
                    monthName = "Ekim 2026",
                    onSelectDay = {},
                    onSelectDate = { selected.value = it }
                )
            }
        }

        composeRule.onNodeWithText("11").performClick()
        composeRule.runOnIdle { assertEquals("2026-10-11", selected.value) }
    }

    @Test
    fun confirmationRequiresTheExplicitSecondAction() {
        val confirmationCount = mutableStateOf(0)
        composeRule.setContent {
            ArcTheme {
                ArcConfirmMorph(
                    prompt = "Silinsin mi?",
                    initialLabel = "Sil",
                    confirmLabel = "Evet, sil",
                    onConfirm = { confirmationCount.value += 1 }
                )
            }
        }

        composeRule.onNodeWithText("Sil").performClick()
        composeRule.onNodeWithText("Evet, sil").assertIsDisplayed().performClick()
        composeRule.runOnIdle { assertEquals(1, confirmationCount.value) }
    }

    @Test
    fun carouselAdvancesOneRealItemAtATime() {
        composeRule.setContent {
            ArcTheme {
                ArcCarousel(items = listOf("Öğrenci A", "Öğrenci B")) { item ->
                    androidx.compose.material3.Text(item)
                }
            }
        }

        composeRule.onNodeWithText("Öğrenci A").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Sonraki").performClick()
        composeRule.onNodeWithText("Öğrenci B").assertIsDisplayed()
        composeRule.onNodeWithText("2 / 2").assertIsDisplayed()
    }

    @Test
    fun paginationCallsBackWithTheAdjacentPage() {
        val currentPage = mutableStateOf(1)
        composeRule.setContent {
            ArcTheme {
                ArcPagination(
                    currentPage = currentPage.value,
                    totalPages = 3,
                    onPageChange = { currentPage.value = it }
                )
            }
        }

        composeRule.onNodeWithContentDescription("Sonraki Sayfa").performClick()
        composeRule.runOnIdle { assertEquals(2, currentPage.value) }
        composeRule.onNodeWithText("2 / 3").assertIsDisplayed()
    }

    @Test
    fun activityHeatmapExposesRealDayLabelsToAccessibility() {
        composeRule.setContent {
            ArcTheme {
                ArcActivityHeatmap(
                    daysIntensity = listOf(0, 1) + List(26) { 0 },
                    dayLabels = listOf("14 Eyl: puan değişikliği etkinliği, seviye 0/4", "15 Eyl: puan değişikliği etkinliği, seviye 1/4")
                )
            }
        }

        composeRule.onNodeWithContentDescription("15 Eyl: puan değişikliği etkinliği, seviye 1/4").assertExists()
    }
}
