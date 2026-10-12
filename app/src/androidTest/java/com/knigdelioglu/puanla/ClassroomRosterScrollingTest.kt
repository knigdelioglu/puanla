package com.knigdelioglu.puanla

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
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
