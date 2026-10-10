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
    val isApproved: Boolean = false
)

/** ML Kit element boundary data used for geometry-based column isolation and tests. */
data class OcrPositionedWord(val text: String, val x: Int, val y: Int, val width: Int, val height: Int) {
    val centerX: Double get() = x + width / 2.0
    val centerY: Double get() = y + height / 2.0
}

object OcrRosterParser {

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
            val words = recognized.textBlocks.flatMap { block ->
                block.lines.flatMap { line ->
                    line.elements.mapNotNull { element ->
                        element.boundingBox?.let { box ->
                            OcrPositionedWord(element.text, box.left, box.top, box.width(), box.height())
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
        val lines = mutableListOf<MutableList<OcrPositionedWord>>()
        for (word in words.sortedWith(compareBy<OcrPositionedWord> { it.centerY }.thenBy { it.x })) {
            val row = lines.lastOrNull()
            val height = row?.map { it.height }?.average() ?: word.height.toDouble()
            if (row != null && kotlin.math.abs(row.map { it.centerY }.average() - word.centerY) <=
                maxOf(6.0, height * 0.60)) {
                row.add(word)
            } else {
                lines.add(mutableListOf(word))
            }
        }
        val normalized: (String) -> String = { it.trim().uppercase(Locale("tr", "TR"))
            .replace(Regex("[^A-ZÇĞİÖŞÜ0-9]"), "") }
        val headerIndex = lines.indexOfFirst { row ->
            val tokens = row.map { normalized(it.text) }
            tokens.any { it in setOf("ADI", "AD", "ADISOYADI") } &&
                tokens.any { it in setOf("NO", "ÖĞRENCİ", "ÖĞRENCİNO", "NUMARASI", "OKUL") }
        }
        if (headerIndex < 0) return emptyList()
        val headers = lines[headerIndex].sortedBy { it.x }
        fun xOf(vararg labels: String): Double? = headers.firstOrNull {
            normalized(it.text) in labels
        }?.centerX
        val numberX = xOf("ÖĞRENCİ", "ÖĞRENCİNO", "OKUL", "NUMARASI")
            ?: headers.filter { normalized(it.text) == "NO" }.lastOrNull()?.centerX
            ?: return emptyList()
        val nameX = xOf("ADI", "AD", "ADISOYADI") ?: return emptyList()
        val surnameX = xOf("SOYADI", "SOYAD")
        val metadataStart = listOfNotNull(xOf("CİNSİYET", "CİNSİYETİ"), xOf("PANSİYON", "YATILI"))
            .filter { it > nameX }.minOrNull() ?: Double.POSITIVE_INFINITY
        val lastNameColumn = surnameX != null && surnameX > nameX && surnameX < metadataStart
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
            val ambiguous = school == null || firstName.isBlank() || lastName.isBlank() || !lastNameColumn
            output.add(
                OcrStudentRow(
                    rawIndex = output.size + 1,
                    studentNumber = school?.text.orEmpty(),
                    firstName = firstName,
                    lastName = lastName.uppercase(Locale("tr", "TR")),
                    isAmbiguous = ambiguous,
                    ambiguityReason = if (ambiguous)
                        "Eksik alan veya birleşik ad-soyad sütunu: öğretmen düzeltip onaylamalı." else null,
                    isApproved = false
                )
            )
        }
        return output
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
