package com.knigdelioglu.puanla.domain.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Shared, bounded decoder for OCR and its source-image review.
 *
 * Both paths must display the same orientation, otherwise ML Kit's bounding
 * boxes refer to different physical areas of a photographed roster.
 */
object OcrImageDecoder {
    suspend fun decode(context: Context, uri: Uri, maxDimension: Int): Bitmap =
        withContext(Dispatchers.IO) {
            require(maxDimension > 0)
            val resolver = context.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Görsel okunamadı." }

            val orientation = resolver.openInputStream(uri)?.use { stream ->
                runCatching {
                    ExifInterface(stream).getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                    )
                }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
            } ?: ExifInterface.ORIENTATION_NORMAL

            var sample = 1
            while (maxOf(bounds.outWidth / sample, bounds.outHeight / sample) > maxDimension) {
                sample *= 2
            }
            val options = BitmapFactory.Options().apply { inSampleSize = sample }
            val decoded = resolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            } ?: error("Görsel çözümlenemedi.")

            val transform = orientationMatrix(orientation)
            if (transform.isIdentity) {
                decoded
            } else {
                try {
                    val oriented = Bitmap.createBitmap(
                        decoded, 0, 0, decoded.width, decoded.height, transform, true
                    )
                    if (oriented !== decoded) decoded.recycle()
                    oriented
                } catch (failure: Exception) {
                    decoded.recycle()
                    throw failure
                }
            }
        }

    private fun orientationMatrix(orientation: Int) = Matrix().apply {
        // EXIF transforms map decoded pixel coordinates into the displayed page.
        // Android's createBitmap adjusts the result translation and dimensions.
        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> setScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> setRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> setScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> setValues(
                floatArrayOf(0f, 1f, 0f, 1f, 0f, 0f, 0f, 0f, 1f)
            )
            ExifInterface.ORIENTATION_ROTATE_90 -> setRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> setValues(
                floatArrayOf(0f, -1f, 0f, -1f, 0f, 0f, 0f, 0f, 1f)
            )
            ExifInterface.ORIENTATION_ROTATE_270 -> setRotate(270f)
        }
    }
}
