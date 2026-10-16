package com.knigdelioglu.puanla.ui.screens

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.knigdelioglu.arc.compose.foundation.ArcTheme
import com.knigdelioglu.puanla.ArcTestHostActivity
import com.knigdelioglu.puanla.data.local.ClassroomEntity
import com.knigdelioglu.puanla.domain.ocr.OcrStudentRow
import com.knigdelioglu.puanla.ui.viewmodel.PuanlaViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OcrVerificationScrollingTest {
    @get:Rule
    val rule = createAndroidComposeRule<ArcTestHostActivity>()

    @Test
    fun scrollingStudentsAlsoScrollsTitleStepperAndWarningsAway() {
        val application = InstrumentationRegistry.getInstrumentation()
            .targetContext.applicationContext as Application
        val viewModel = PuanlaViewModel(application)
        viewModel.selectedClassroom.value = ClassroomEntity(
            id = "ocr-scroll-test", grade = 11, section = "C",
            name = "11-C", academicYear = "2026-2027"
        )
        viewModel.ocrCandidates.value = (1..24).map { number ->
            OcrStudentRow(
                id = "student-$number",
                rawIndex = number,
                studentNumber = (100 + number).toString(),
                firstName = "Ad$number",
                lastName = "Soyad$number"
            )
        }
        viewModel.ocrStep.value = 2

        rule.setContent {
            ArcTheme(reducedMotion = true) {
                OcrImportScreen(viewModel)
            }
        }

        rule.onNodeWithText("Fotoğraftan Öğrenci Listesi Aktarımı").assertIsDisplayed()
        // The same LazyColumn owns the screen chrome and the last student.
        rule.onNodeWithTag("ocrVerificationScroll").performScrollToIndex(25)
        rule.onNodeWithTag("ocrStudent-student-24").assertIsDisplayed()
        rule.onNodeWithText("Fotoğraftan Öğrenci Listesi Aktarımı").assertDoesNotExist()

        // The confirmation action must remain visible after scrolling to the bottom.
        rule.onNodeWithTag("ocrImportAction").assertIsDisplayed().assertIsNotEnabled()

        rule.runOnIdle {
            val first = viewModel.ocrCandidates.value.first()
            viewModel.ocrCandidates.value = viewModel.ocrCandidates.value.toMutableList().apply {
                this[0] = first.copy(isApproved = true)
            }
        }
        rule.onNodeWithTag("ocrImportAction").assertIsDisplayed().assertIsEnabled().performClick()
        rule.onNodeWithText("Öğrencileri sınıfa aktar?").assertIsDisplayed()
        rule.onNodeWithText("Aktarımı Onayla").assertIsDisplayed()
        rule.onNodeWithText("Vazgeç").performClick()
        rule.onNodeWithText("Öğrencileri sınıfa aktar?").assertDoesNotExist()
    }

    @Test
    fun conflictingNumbersDisableStickyImportAndExplainWhy() {
        val application = InstrumentationRegistry.getInstrumentation()
            .targetContext.applicationContext as Application
        val viewModel = PuanlaViewModel(application)
        viewModel.selectedClassroom.value = ClassroomEntity(
            id = "ocr-conflict-test", grade = 11, section = "B",
            name = "11-B", academicYear = "2026-2027"
        )
        viewModel.ocrCandidates.value = listOf(
            OcrStudentRow(id = "first", rawIndex = 1, studentNumber = "203",
                firstName = "Deniz", lastName = "Bir", isApproved = true),
            OcrStudentRow(id = "second", rawIndex = 2, studentNumber = "203",
                firstName = "Ekin", lastName = "İki", isApproved = true)
        )
        viewModel.ocrStep.value = 2
        rule.setContent {
            ArcTheme(reducedMotion = true) { OcrImportScreen(viewModel) }
        }
        rule.onNodeWithTag("ocrImportAction").assertIsDisplayed().assertIsNotEnabled()
        rule.onNodeWithText("Çakışan okul numaralarını düzeltin.").assertIsDisplayed()
    }

    @Test
    fun existingPhotoPickerRemainsAlongsideOptionalPerspectiveScanner() {
        val application = InstrumentationRegistry.getInstrumentation()
            .targetContext.applicationContext as Application
        val viewModel = PuanlaViewModel(application)
        viewModel.selectedClassroom.value = ClassroomEntity(
            id = "ocr-scan-optional", grade = 11, section = "C",
            name = "11-C", academicYear = "2026-2027"
        )
        viewModel.ocrStep.value = 0
        rule.setContent {
            ArcTheme(reducedMotion = true) {
                OcrImportScreen(viewModel)
            }
        }
        // No Google Play scan SDK is launched by this test: prove the fallback
        // and optional perspective entry points coexist for all devices.
        rule.onNodeWithText("Belgeyi Tara / Perspektifi Düzelt").assertIsDisplayed()
        rule.onNodeWithText("Basılı Sınıf Listesi Fotoğrafı Seç").assertIsDisplayed()
    }

}
