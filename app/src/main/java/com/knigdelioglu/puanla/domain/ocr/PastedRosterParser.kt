package com.knigdelioglu.puanla.domain.ocr

import com.knigdelioglu.puanla.domain.student.StudentNameRules
import java.util.Locale

data class PastedRosterParseResult(
    val rows: List<OcrStudentRow>,
    val ignoredLineCount: Int
)

/** Parses explicit `school number - full name` lines without inventing missing identifiers. */
object PastedRosterParser {
    private val rowPattern = Regex("^\\s*(\\d{1,6})\\s*(?:[-–—:;,]\\s*|\\s+)(.+?)\\s*$")
    private val headerWords = setOf("NO", "OKUL", "OGRENCI", "NUMARA", "NUMARASI", "ADI", "SOYADI", "AD", "SOYAD")
    private val turkish = Locale.forLanguageTag("tr-TR")

    fun parse(text: String): PastedRosterParseResult {
        var ignored = 0
        val rows = text.lineSequence()
            .mapIndexedNotNull { lineIndex, rawLine ->
                val line = rawLine.trim()
                if (line.isEmpty()) return@mapIndexedNotNull null
                val match = rowPattern.matchEntire(line)
                if (match == null && line.uppercase(turkish)
                        .split(Regex("[^\\p{L}]+"))
                        .any { it in headerWords }
                ) return@mapIndexedNotNull null
                val number = match?.groupValues?.getOrNull(1)
                val fullName = match?.groupValues?.getOrNull(2)
                if (number == null || fullName.isNullOrBlank()) {
                    ignored++
                    return@mapIndexedNotNull null
                }
                val nameParts = runCatching { StudentNameRules.splitFullName(fullName) }.getOrNull()
                if (nameParts == null) {
                    ignored++
                    return@mapIndexedNotNull null
                }
                OcrStudentRow(
                    rawIndex = lineIndex + 1,
                    studentNumber = number,
                    firstName = nameParts.first,
                    lastName = nameParts.second,
                    isApproved = false
                )
            }
            .toList()
        return PastedRosterParseResult(rows = rows, ignoredLineCount = ignored)
    }
}
