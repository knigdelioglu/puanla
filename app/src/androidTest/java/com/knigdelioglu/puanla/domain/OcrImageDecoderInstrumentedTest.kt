package com.knigdelioglu.puanla.domain

import android.graphics.Bitmap
import android.media.ExifInterface
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.knigdelioglu.puanla.domain.ocr.OcrImageDecoder
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OcrImageDecoderInstrumentedTest {
    @Test
    fun scanDecoderHonorsExifQuarterTurnBeforeOcrCoordinatesAreComputed() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File.createTempFile("ocr-rotated-", ".jpg", context.cacheDir)
        val original = Bitmap.createBitmap(160, 96, Bitmap.Config.ARGB_8888)
        try {
            FileOutputStream(file).use { stream ->
                check(original.compress(Bitmap.CompressFormat.JPEG, 95, stream))
            }
            ExifInterface(file.absolutePath).apply {
                setAttribute(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_ROTATE_90.toString()
                )
                saveAttributes()
            }
            val rotated = OcrImageDecoder.decode(context, Uri.fromFile(file), 2400)
            try {
                assertEquals(96, rotated.width)
                assertEquals(160, rotated.height)
            } finally {
                rotated.recycle()
            }
        } finally {
            original.recycle()
            file.delete()
        }
    }
}
