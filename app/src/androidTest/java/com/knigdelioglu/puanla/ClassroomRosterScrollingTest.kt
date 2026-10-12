package com.knigdelioglu.puanla

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
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
    fun rosterRowsScrollWithThePageInsteadOfBeingTrappedInAShortTable() {
        val students = (1..25).map { "Öğrenci $it" }
        composeRule.setContent {
            ArcTheme(reducedMotion = true) {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    Spacer(Modifier.height(400.dp))
                    ArcSortableDataTable(
                        items = students,
                        columns = listOf(ArcTableColumn<String>("Öğrenci") { Text(it) }),
                        pageSize = 25,
                        expandRowsInParentScroll = true
                    )
                }
            }
        }

        composeRule.onNodeWithText("Öğrenci 25").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun parentScrollingPreservesPaginationBeyondTwentyFiveStudents() {
        val students = (1..30).map { "Öğrenci $it" }
        composeRule.setContent {
            ArcTheme(reducedMotion = true) {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    ArcSortableDataTable(
                        items = students,
                        columns = listOf(ArcTableColumn<String>("Öğrenci") { Text(it) }),
                        pageSize = 25,
                        expandRowsInParentScroll = true
                    )
                }
            }
        }

        composeRule.onNodeWithText("Öğrenci 30").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Sonraki Sayfa").performScrollTo().performClick()
        composeRule.onNodeWithText("Öğrenci 30").performScrollTo().assertIsDisplayed()
    }
}
