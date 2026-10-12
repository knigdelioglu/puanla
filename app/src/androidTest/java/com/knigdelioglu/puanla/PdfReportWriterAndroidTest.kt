package com.knigdelioglu.puanla

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.test.platform.app.InstrumentationRegistry
import com.knigdelioglu.puanla.domain.export.CsvCriterion
import com.knigdelioglu.puanla.domain.export.CsvStudent
import com.knigdelioglu.puanla.domain.export.PdfReportSnapshot
import com.knigdelioglu.puanla.domain.export.PdfReportWriter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate

/** Exercises the actual Android PdfDocument writer and a PDF reader. */
class PdfReportWriterAndroidTest {
    @Test fun multiPageReportWithTurkishNamesRendersSuccessfully() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val temp = File.createTempFile("puanla-report-", ".pdf", context.cacheDir)
        try {
            val criteria = (1..8).map { CsvCriterion("c$it", "Ölçüt $it – Sözlü anlatım ve iletişim", 10) }
            val students = (1..35).map { index ->
                CsvStudent(
                    number = index.toString(),
                    firstName = if (index % 2 == 0) "Çağrı" else "Şule",
                    lastName = "Öğrenci",
                    scores = criteria.associate { it.id to 10 },
                    definitiveTotal = 80,
                    isCompleted = true,
                    scoredCount = criteria.size
                )
            }
            val snapshot = PdfReportSnapshot("11-C", "2026-2027", "Türk Dili ve Edebiyatı", criteria, students)
            temp.outputStream().use {
                PdfReportWriter.write(
                    snapshot, "Türk Dili ve Edebiyatı", "Ayşe Öztürk", it, LocalDate.of(2026, 10, 12)
                )
            }
            assertTrue(temp.length() > 1000)
            ParcelFileDescriptor.open(temp, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
                PdfRenderer(descriptor).use { renderer ->
                    // 1 cover + 1 criterion index + 2 groups * 3 student pages
                    assertEquals(8, renderer.pageCount)
                    renderer.openPage(0).use { page ->
                        val image = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                        page.render(image, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        assertTrue(image.getPixel(50, 40) != Color.WHITE)
                        image.recycle()
                    }
                }
            }
        } finally {
            temp.delete()
        }
    }
}
