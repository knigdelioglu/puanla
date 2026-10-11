package com.knigdelioglu.puanla

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.knigdelioglu.arc.compose.advanced.ArcAdvancedItem
import com.knigdelioglu.arc.compose.advanced.ArcGlassTabBar
import com.knigdelioglu.arc.compose.editor.ArcRichTextEditor
import com.knigdelioglu.arc.compose.foundation.ArcTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ArcDesignSystemIntegrationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ArcTestHostActivity>()

    @Test
    fun glassNavigationReturnsTheSelectedDestinationKey() {
        val selectedKey = mutableStateOf("workspace")
        val items = listOf(
            ArcAdvancedItem(key = "workspace", label = "Değerlendirme"),
            ArcAdvancedItem(key = "classrooms", label = "Sınıflar")
        )
        composeRule.setContent {
            ArcTheme(reducedMotion = true) {
                ArcGlassTabBar(
                    tabs = items,
                    selectedKey = selectedKey.value,
                    onTabSelected = { selectedKey.value = it }
                )
            }
        }

        composeRule.onNodeWithText("Sınıflar").assertIsDisplayed().performClick()
        composeRule.runOnIdle { assertEquals("classrooms", selectedKey.value) }
    }

    @Test
    fun richTextEditorPersistsFormattingAsPlainMarkdownText() {
        val note = mutableStateOf("")
        composeRule.setContent {
            ArcTheme(reducedMotion = true) {
                ArcRichTextEditor(
                    value = note.value,
                    onValueChange = { note.value = it },
                    placeholder = "Ölçüte özel not"
                )
            }
        }

        composeRule.onNode(hasSetTextAction()).performTextInput("kanıt")
        composeRule.onNodeWithContentDescription("Kalın").performClick()
        composeRule.runOnIdle { assertEquals("kanıt****", note.value) }
    }
}
