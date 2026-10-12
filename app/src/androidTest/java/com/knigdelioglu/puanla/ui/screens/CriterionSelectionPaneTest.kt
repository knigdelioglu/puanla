package com.knigdelioglu.puanla.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.knigdelioglu.arc.compose.foundation.ArcTheme
import com.knigdelioglu.puanla.ArcTestHostActivity
import com.knigdelioglu.puanla.data.local.CriterionEntity
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CriterionSelectionPaneTest {
    @get:Rule
    val rule = createAndroidComposeRule<ArcTestHostActivity>()

    private fun criteria(count: Int) = (1..count).map { number ->
        CriterionEntity(
            id = "criterion-$number",
            rubricId = "rubric-test",
            title = "Ölçüt Başlığı $number",
            description = "Sadece sağ panelde gösterilecek detay $number",
            maxPoints = 20,
            orderIndex = number - 1
        )
    }

    @Test
    fun sevenCriteriaFitInTabletPaneWithoutScrollingAndSelectOnWholeRowTap() {
        val entries = criteria(7)
        val selected = mutableStateOf(entries.first())
        rule.setContent {
            ArcTheme(reducedMotion = true) {
                Box(Modifier.fillMaxWidth().height(540.dp)) {
                    CriteriaExplorerPane(
                        criteria = entries,
                        activeCriterion = selected.value,
                        onSelectCriterion = { selected.value = it }
                    )
                }
            }
        }

        entries.forEach { criterion ->
            rule.onNodeWithTag("rubricCriterion-${criterion.id}").assertIsDisplayed()
        }
        rule.onNodeWithText("Sadece sağ panelde gösterilecek detay 1").assertDoesNotExist()
        rule.onNodeWithTag("rubricCriterion-criterion-7").performClick().assertIsSelected()
        rule.runOnIdle { assertEquals("criterion-7", selected.value.id) }
    }

    @Test
    fun moreCriteriaInShorterPaneRemainReachableByScrolling() {
        val entries = criteria(12)
        val selected = mutableStateOf(entries.first())
        rule.setContent {
            ArcTheme(reducedMotion = true) {
                Box(Modifier.fillMaxWidth().height(360.dp)) {
                    CriteriaExplorerPane(
                        criteria = entries,
                        activeCriterion = selected.value,
                        onSelectCriterion = { selected.value = it }
                    )
                }
            }
        }

        rule.onNodeWithTag("rubricCriterionList").performScrollToIndex(11)
        rule.onNodeWithTag("rubricCriterion-criterion-12").assertIsDisplayed().performClick()
        rule.runOnIdle { assertEquals("criterion-12", selected.value.id) }
    }
}
