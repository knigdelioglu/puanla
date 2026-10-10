package com.knigdelioglu.puanla.domain.ocr

import android.graphics.Bitmap
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
    val isApproved: Boolean = true
)

object OcrRosterParser {

    private val GENDER_TOKENS = setOf("K", "E", "KIZ", "ERKEK", "Kız", "Erkek", "kız", "erkek")
    private val BOARDING_TOKENS = setOf("P", "G", "PANSİYONLU", "GÜNDÜZLÜ", "PANSİYON", "YATILI", "Pansiyonlu", "Gündüzlü")
    private val HEADER_TOKENS = setOf("SIRA", "NO", "ÖĞRENCİ", "OKUL", "ADI", "SOYADI", "CİNSİYET", "PANSİYON", "SINIF", "ŞUBE")

    /**
     * Runs ML Kit on-device text recognition on a bitmap.
     */
    suspend fun processBitmap(bitmap: Bitmap): List<OcrStudentRow> {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val image = InputImage.fromBitmap(bitmap, 0)

        val visionText = suspendCancellableCoroutine<Text> { cont ->
            recognizer.process(image)
                .addOnSuccessListener { cont.resume(it) }
                .addOnFailureListener {
                    // Fallback to empty text on failure
                    cont.resume(Text("", emptyList<Text.TextBlock>()))
                }
        }

        return parseVisionText(visionText.text)
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
            isApproved = !isAmbiguous
        )
    }

    private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
}
