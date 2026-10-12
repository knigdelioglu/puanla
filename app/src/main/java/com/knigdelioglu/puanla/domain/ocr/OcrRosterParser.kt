package com.knigdelioglu.puanla.domain.ocr

import android.graphics.Bitmap
import android.graphics.Rect
import java.util.Locale
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

data class OcrStudentRow(
    val id: String = java.util.UUID.randomUUID().toString(),
    val rawIndex: Int,
    val studentNumber: String,
    val firstName: String,
    val lastName: String,
    val gender: String? = null,
    val boardingStatus: String? = null,
    val isAmbiguous: Boolean = false,
    val ambiguityReason: String? = null,
    val isApproved: Boolean = false,
    val isConfirmedDistinctFromPossibleMatch: Boolean = false,
    /** Source-image row bounds; kept through review for geometric duplicate detection. */
    val sourceLeft: Int? = null,
    val sourceTop: Int? = null,
    val sourceRight: Int? = null,
    val sourceBottom: Int? = null
)

/** ML Kit element boundary data used for geometry-based column isolation and tests. */
data class OcrPositionedWord(
    val text: String,
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    /** Original ML Kit line; keeps words together when a photographed page is skewed. */
    val recognizedLineIndex: Int? = null
) {
    val centerX: Double get() = x + width / 2.0
    val centerY: Double get() = y + height / 2.0
}

/** Rows can be the same source even when OCR produces unrelated names or numbers. */
internal fun sameSourceImageRow(first: OcrStudentRow, second: OcrStudentRow): Boolean {
    val leftA = first.sourceLeft ?: return false
    val rightA = first.sourceRight ?: return false
    val topA = first.sourceTop ?: return false
    val bottomA = first.sourceBottom ?: return false
    val leftB = second.sourceLeft ?: return false
    val rightB = second.sourceRight ?: return false
    val topB = second.sourceTop ?: return false
    val bottomB = second.sourceBottom ?: return false
    val width = minOf(rightA - leftA, rightB - leftB)
    val height = minOf(bottomA - topA, bottomB - topB)
    if (width <= 0 || height <= 0) return false
    val xOverlap = (minOf(rightA, rightB) - maxOf(leftA, leftB)).coerceAtLeast(0)
    val yOverlap = (minOf(bottomA, bottomB) - maxOf(topA, topB)).coerceAtLeast(0)
    return xOverlap.toDouble() / width >= 0.65 && yOverlap.toDouble() / height >= 0.65
}

object OcrRosterParser {

    private data class OverlapFiltering(
        val unique: List<OcrPositionedWord>,
        val conflicting: List<OcrPositionedWord>
    )

    /** Suppress two ML Kit elements describing the same pixels, not nearby separate words. */
    private fun filterRepeatedDetections(words: List<OcrPositionedWord>): OverlapFiltering {
        val unique = mutableListOf<OcrPositionedWord>()
        val conflicting = mutableListOf<OcrPositionedWord>()
        for (word in words) {
            val duplicate = unique.firstOrNull { existing ->
                val xOverlap = (minOf(word.x + word.width, existing.x + existing.width) -
                    maxOf(word.x, existing.x)).coerceAtLeast(0)
                val yOverlap = (minOf(word.y + word.height, existing.y + existing.height) -
                    maxOf(word.y, existing.y)).coerceAtLeast(0)
                val minWidth = minOf(word.width, existing.width)
                val minHeight = minOf(word.height, existing.height)
                minWidth > 0 && minHeight > 0 &&
                    xOverlap.toDouble() / minWidth >= 0.75 &&
                    yOverlap.toDouble() / minHeight >= 0.70
            }
            if (duplicate == null) unique.add(word)
            else if (!duplicate.text.equals(word.text, ignoreCase = true)) conflicting.add(word)
        }
        return OverlapFiltering(unique, conflicting)
    }

    private val GENDER_TOKENS = setOf("K", "E", "KIZ", "ERKEK", "Kız", "Erkek", "kız", "erkek")
    private val BOARDING_TOKENS = setOf("P", "G", "PANSİYONLU", "GÜNDÜZLÜ", "PANSİYON", "YATILI", "Pansiyonlu", "Gündüzlü")
    private val HEADER_TOKENS = setOf("SIRA", "NO", "ÖĞRENCİ", "OKUL", "ADI", "SOYADI", "CİNSİYET", "PANSİYON", "SINIF", "ŞUBE")

    /**
     * Runs ML Kit on-device text recognition on a bitmap.
     */
    suspend fun processBitmap(bitmap: Bitmap): List<OcrStudentRow> {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        return try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val recognized = suspendCancellableCoroutine<Text> { cont ->
                recognizer.process(image)
                    .addOnSuccessListener { if (cont.isActive) cont.resume(it) }
                    .addOnFailureListener { if (cont.isActive) cont.resumeWith(Result.failure(it)) }
            }
            var recognizedLineIndex = 0
            val words = recognized.textBlocks.flatMap { block ->
                block.lines.flatMap { line ->
                    val lineIndex = recognizedLineIndex++
                    line.elements.mapNotNull { element ->
                        element.boundingBox?.let { box ->
                            OcrPositionedWord(
                                element.text,
                                box.left,
                                box.top,
                                box.width(),
                                box.height(),
                                recognizedLineIndex = lineIndex
                            )
                        }
                    }
                }
            }
            parsePositionedWords(words)
        } finally {
            recognizer.close()
        }
    }

    /**
     * Actual coordinate-aware parser. A recognizable column header is mandatory;
     * without it there are NO guessed/final student records.
     *
     * Even when geometry looks reliable every OCR row starts UNAPPROVED.
     */
    fun parsePositionedWords(words: List<OcrPositionedWord>): List<OcrStudentRow> {
        if (words.isEmpty()) return emptyList()
        val filtered = filterRepeatedDetections(words)
        val uniqueWords = filtered.unique

        // Turkish headings are sometimes returned without diacritics, and a
        // photographed sheet can make separate column labels land at different
        // raw Y coordinates. Normalize both before rebuilding table rows.
        val normalized: (String) -> String = { text ->
            text.trim().uppercase(Locale("tr", "TR"))
                .replace('Ç', 'C').replace('Ğ', 'G').replace('İ', 'I')
                .replace('Ö', 'O').replace('Ş', 'S').replace('Ü', 'U')
                .replace(Regex("[^A-Z0-9]"), "")
        }
        val headerAnchors = uniqueWords.filter {
            normalized(it.text) in setOf(
                "ADI", "AD", "ADISOYADI", "SOYADI", "SOYAD", "NO", "OGRENCI",
                "OGRENCINO", "NUMARASI", "OKUL", "CINSIYET", "CINSIYETI",
                "PANSIYON", "PANSIYONDURUM"
            )
        }
        val meanX = headerAnchors.map { it.centerX }.average()
        val meanY = headerAnchors.map { it.centerY }.average()
        val varianceX = headerAnchors.sumOf { (it.centerX - meanX) * (it.centerX - meanX) }
        val slope = if (headerAnchors.size >= 3 && varianceX > 0.0) {
            (headerAnchors.sumOf { (it.centerX - meanX) * (it.centerY - meanY) } / varianceX)
                .coerceIn(-0.12, 0.12)
        } else 0.0
        fun projectedY(word: OcrPositionedWord) = word.centerY - slope * word.centerX

        val lines = mutableListOf<MutableList<OcrPositionedWord>>()
        for (word in uniqueWords.sortedWith(compareBy<OcrPositionedWord> { projectedY(it) }.thenBy { it.x })) {
            // ML Kit already groups elements into text lines. Keep that signal so
            // perspective/skew across a wide printed table cannot split its header
            // into unrelated rows. The projected Y also reunites a visual row when
            // ML Kit returns its separate columns as different lines or blocks.
            val row = word.recognizedLineIndex?.let { lineIndex ->
                lines.firstOrNull { group -> group.any { it.recognizedLineIndex == lineIndex } }
            } ?: lines.minByOrNull { group -> kotlin.math.abs(group.map { projectedY(it) }.average() - projectedY(word)) }
                ?.takeIf { group ->
                    val height = group.map { it.height }.average()
                    kotlin.math.abs(group.map { projectedY(it) }.average() - projectedY(word)) <=
                        maxOf(6.0, height * 0.80)
                }
            if (row != null) {
                row.add(word)
            } else {
                lines.add(mutableListOf(word))
            }
        }
        lines.sortBy { row -> row.map { projectedY(it) }.average() }
        val headerIndex = lines.indexOfFirst { row ->
            val tokens = row.map { normalized(it.text) }
            tokens.any { it in setOf("ADI", "AD", "ADISOYADI") } &&
                tokens.any { it in setOf("NO", "OGRENCI", "OGRENCINO", "NUMARASI", "OKUL") }
        }
        if (headerIndex < 0) return emptyList()
        val headers = lines[headerIndex].sortedBy { it.x }
        fun xOf(vararg labels: String): Double? = headers.firstOrNull { word ->
            val token = normalized(word.text)
            labels.any { label -> token == label || token.startsWith(label) }
        }?.centerX
        val numberX = xOf("OGRENCI", "OKUL", "NUMARASI")
            ?: headers.filter { normalized(it.text) == "NO" }.lastOrNull()?.centerX
            ?: return emptyList()
        val nameX = xOf("ADI", "ADISOYADI", "AD") ?: return emptyList()
        val surnameX = xOf("SOYADI", "SOYAD")
        val metadataAnchor = listOfNotNull(xOf("CINSIYET"), xOf("PANSIYON", "YATILI"))
            .filter { it > nameX }.minOrNull()
        // Boundaries are halfway BETWEEN header centres, not the start of the
        // following header. This keeps K/E, KR, boarding metadata out of surnames.
        // Closely adjacent "Adı Soyadı" header words may describe ONE combined
        // column; in that case all candidates remain ambiguous until reviewed.
        val lastNameColumn = surnameX != null && surnameX - nameX > 70.0 &&
            (metadataAnchor == null || surnameX < metadataAnchor)
        val metadataStart = metadataAnchor?.let {
            ((if (lastNameColumn) surnameX!! else nameX) + it) / 2.0
        } ?: Double.POSITIVE_INFINITY
        val schoolRight = (numberX + nameX) / 2.0
        val surnameBoundary = if (lastNameColumn) (nameX + surnameX!!) / 2.0 else metadataStart
        val output = mutableListOf<OcrStudentRow>()
        for (row in lines.drop(headerIndex + 1)) {
            val sorted = row.sortedBy { it.x }
            val school = sorted.filter { it.centerX < schoolRight }.filter {
                it.text.all(Char::isDigit) && it.text.length in 1..6
            }.minByOrNull { kotlin.math.abs(it.centerX - numberX) }
            val firstTokens = sorted.filter { it.centerX >= schoolRight && it.centerX < surnameBoundary }
                .map { it.text.trim() }.filter(String::isNotEmpty)
            val lastTokens = if (lastNameColumn)
                sorted.filter { it.centerX >= surnameBoundary && it.centerX < metadataStart }
                    .map { it.text.trim() }.filter(String::isNotEmpty)
            else emptyList()
            if (school == null && firstTokens.isEmpty() && lastTokens.isEmpty()) continue
            val firstName = if (lastNameColumn) firstTokens.joinToString(" ")
                else firstTokens.dropLast(1).joinToString(" ")
            val lastName = if (lastNameColumn) lastTokens.joinToString(" ")
                else firstTokens.lastOrNull().orEmpty()
            if (firstName.isEmpty() && lastName.isEmpty()) continue
            val rowTop = sorted.minOf { it.y }
            val rowBottom = sorted.maxOf { it.y + it.height }
            val rowLeft = sorted.minOf { it.x }
            val rowRight = sorted.maxOf { it.x + it.width }
            val conflictingRead = filtered.conflicting.any { other ->
                other.y < rowBottom && other.y + other.height > rowTop &&
                    other.x < rowRight && other.x + other.width > rowLeft
            }
            val missingFields = school == null || firstName.isBlank() || lastName.isBlank() || !lastNameColumn
            val ambiguous = missingFields || conflictingRead
            output.add(
                OcrStudentRow(
                    rawIndex = output.size + 1,
                    studentNumber = school?.text.orEmpty(),
                    firstName = firstName,
                    lastName = lastName.uppercase(Locale("tr", "TR")),
                    isAmbiguous = ambiguous,
                    ambiguityReason = when {
                        conflictingRead -> "Aynı görüntü bölgesinden farklı OCR okumaları geldi; fotoğrafla doğrulayın."
                        missingFields -> "Eksik alan veya birleşik ad-soyad sütunu: öğretmen düzeltip onaylamalı."
                        else -> null
                    },
                    isApproved = false,
                    sourceLeft = rowLeft,
                    sourceTop = rowTop,
                    sourceRight = rowRight,
                    sourceBottom = rowBottom
                )
            )
        }
        // A single physical student row must never become two import candidates.
        // Conflicting readings are retained as uncertainty rather than silently trusted.
        val collapsed = mutableListOf<OcrStudentRow>()
        for (candidate in output) {
            val existingIndex = collapsed.indexOfFirst { sameSourceImageRow(it, candidate) }
            if (existingIndex < 0) {
                collapsed.add(candidate)
            } else {
                val previous = collapsed[existingIndex]
                val priorCompleteness = listOf(previous.studentNumber, previous.firstName, previous.lastName)
                    .count { it.isNotBlank() }
                val nextCompleteness = listOf(candidate.studentNumber, candidate.firstName, candidate.lastName)
                    .count { it.isNotBlank() }
                val preferred = if (nextCompleteness > priorCompleteness) candidate else previous
                collapsed[existingIndex] = preferred.copy(
                    isAmbiguous = true,
                    ambiguityReason = "Aynı fotoğraf satırı birden fazla okundu; kaynak fotoğraftan doğrulayın."
                )
            }
        }
        return collapsed.mapIndexed { index, candidate -> candidate.copy(rawIndex = index + 1) }
    }

    /**
     * Parses multiline OCR text output with column isolation and noise filtering.
     */
    fun parseVisionText(text: String): List<OcrStudentRow> {
        val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }
        val parsedRows = mutableListOf<OcrStudentRow>()

        var indexCounter = 1
        for (line in lines) {
            val row = parseLine(line, indexCounter)
            if (row != null) {
                parsedRows.add(row)
                indexCounter++
            }
        }

        return parsedRows
    }

    /**
     * Parses a single line from a printed student roster.
     * Prevents leakage of gender (K/E/KIZ/ERKEK), boarding status (P/G), or index numbers into names.
     */
    fun parseLine(line: String, defaultIndex: Int): OcrStudentRow? {
        val rawTokens = line.split("\\s+".toRegex()).filter { it.isNotBlank() }
        if (rawTokens.isEmpty()) return null

        // If line is a header line, skip
        val isHeader = rawTokens.any { token -> HEADER_TOKENS.contains(token.uppercase()) }
        if (isHeader) return null

        val filteredTokens = mutableListOf<String>()
        var detectedGender: String? = null
        var detectedBoarding: String? = null

        // Separate neighboring metadata columns
        for (token in rawTokens) {
            val upper = token.uppercase()
            when {
                GENDER_TOKENS.contains(upper) -> {
                    detectedGender = if (upper.startsWith("K")) "Kız" else "Erkek"
                }
                BOARDING_TOKENS.contains(upper) -> {
                    detectedBoarding = if (upper.startsWith("P")) "Pansiyonlu" else "Gündüzlü"
                }
                else -> {
                    filteredTokens.add(token)
                }
            }
        }

        if (filteredTokens.isEmpty()) return null

        // Locate student school number (digits sequence with 1-5 chars)
        var studentNumber: String? = null
        val nameTokens = mutableListOf<String>()

        for (token in filteredTokens) {
            val cleanDigits = token.filter { it.isDigit() }
            if (cleanDigits.length in 1..6 && studentNumber == null && cleanDigits == token) {
                // If it looks like index number 1..99 at the very start and there are more tokens, check next
                if (cleanDigits.toIntOrNull() != null && cleanDigits.toInt() < 60 && nameTokens.isEmpty() && filteredTokens.size > 2) {
                    val potentialNextNumber = filteredTokens.getOrNull(1)?.filter { it.isDigit() }
                    if (potentialNextNumber != null && potentialNextNumber.length in 2..6) {
                        // This first token was just the sequence index, skip it
                        continue
                    }
                }
                studentNumber = cleanDigits
            } else {
                // Strip punctuation and neighbor column fragments
                val cleanWord = token.filter { it.isLetter() || it == '-' || it == '\'' }
                if (cleanWord.isNotBlank() && !GENDER_TOKENS.contains(cleanWord.uppercase()) && !BOARDING_TOKENS.contains(cleanWord.uppercase())) {
                    nameTokens.add(cleanWord)
                }
            }
        }

        if (nameTokens.isEmpty()) return null

        val (firstName, lastName, isAmbiguous, reason) = when {
            nameTokens.size == 1 -> {
                // Only one word found
                Quadruple(nameTokens[0], "", true, "Soyadı bulunamadı, kontrol gerekli.")
            }
            nameTokens.size == 2 -> {
                Quadruple(nameTokens[0], nameTokens[1], studentNumber == null, if (studentNumber == null) "Okul numarası okunamadı." else null)
            }
            else -> {
                // Multi-word first name (e.g., AHMET CAN + YILMAZ, AYŞE NUR + DEMİR)
                val last = nameTokens.last()
                val first = nameTokens.subList(0, nameTokens.size - 1).joinToString(" ")
                Quadruple(first, last, studentNumber == null, if (studentNumber == null) "Okul numarası okunamadı." else null)
            }
        }

        val finalNumber = studentNumber ?: defaultIndex.toString()

        return OcrStudentRow(
            rawIndex = defaultIndex,
            studentNumber = finalNumber,
            firstName = firstName,
            lastName = lastName.uppercase(),
            gender = detectedGender,
            boardingStatus = detectedBoarding,
            isAmbiguous = isAmbiguous,
            ambiguityReason = reason,
            isApproved = false
        )
    }

    private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
}
