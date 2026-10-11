package com.knigdelioglu.puanla.domain.backup

import com.knigdelioglu.puanla.data.local.BackupSnapshot
import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/** An untrusted backup must be inspected before the user can replace device data. */
data class BackupPreview(
    val exportedAt: Long,
    val classrooms: Int,
    val students: Int,
    val rubrics: Int,
    val assessments: Int,
    val scores: Int,
    val groups: Int
)

object BackupImport {
    const val MAX_BYTES = 16 * 1024 * 1024
    private val json = Json { encodeDefaults = true; explicitNulls = true; ignoreUnknownKeys = false }

    /**
     * Limit reads from SAF providers: readText() can otherwise exhaust memory on
     * unexpected huge files. Caller retains ownership of closing the stream.
     */
    fun readLimited(input: InputStream, maxBytes: Int = MAX_BYTES): String {
        require(maxBytes > 0)
        val bytes = ByteArrayOutputStream()
        val chunk = ByteArray(8192)
        var read = input.read(chunk)
        while (read != -1) {
            require(bytes.size().toLong() + read <= maxBytes) {
                "Yedek dosyası çok büyük (en fazla ${maxBytes / (1024 * 1024)} MB)."
            }
            bytes.write(chunk, 0, read)
            read = input.read(chunk)
        }
        return bytes.toString(Charsets.UTF_8.name())
    }

    /** Uses the same strict schema and referential-integrity validation as restore. */
    fun inspect(source: String): BackupPreview {
        val snapshot = json.decodeFromString<BackupSnapshot>(source)
        snapshot.validate()
        return BackupPreview(
            exportedAt = snapshot.exportedAt,
            classrooms = snapshot.classrooms.size,
            students = snapshot.students.size,
            rubrics = snapshot.rubrics.size,
            assessments = snapshot.assessments.size,
            scores = snapshot.scores.size,
            groups = snapshot.groups.size
        )
    }
}
