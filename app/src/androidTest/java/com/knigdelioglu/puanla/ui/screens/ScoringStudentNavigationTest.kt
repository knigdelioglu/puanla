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
import com.knigdelioglu.puanla.ArcTestHostActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScoringStudentNavigationTest {
    @get:Rule
    val rule = createAndroidComposeRule<ArcTestHostActivity>()

    @Test
    fun switchingStudentStartsAtFirstCriterionWhileSameStudentKeepsSelection() {
        val studentId = mutableStateOf("student-1")
        val rubricId = mutableStateOf("rubric-1")
        rule.setContent {
            val activeCriterionIndex = rememberStudentCriterionIndex(studentId.value, rubricId.value)
            Column {
                Text("Aktif ölçüt: ${activeCriterionIndex.intValue + 1}")
                Button(onClick = { activeCriterionIndex.intValue = 2 }) { Text("Üçüncü ölçüt") }
                Button(onClick = { studentId.value = "student-2" }) { Text("Sonraki öğrenci") }
                Button(onClick = { rubricId.value = "rubric-2" }) { Text("Başka rubrik") }
            }
        }

        rule.onNodeWithText("Aktif ölçüt: 1").assertIsDisplayed()
        rule.onNodeWithText("Üçüncü ölçüt").performClick()
        rule.onNodeWithText("Aktif ölçüt: 3").assertIsDisplayed()
        // Student identity changes without changing rubric, as on the scoring tablet.
        rule.onNodeWithText("Sonraki öğrenci").performClick()
        rule.onNodeWithText("Aktif ölçüt: 1").assertIsDisplayed()
        rule.onNodeWithText("Üçüncü ölçüt").performClick()
        rule.onNodeWithText("Aktif ölçüt: 3").assertIsDisplayed()
        // Changing the rubric also restarts at the first criterion.
        rule.onNodeWithText("Başka rubrik").performClick()
        rule.onNodeWithText("Aktif ölçüt: 1").assertIsDisplayed()
    }
}
