package com.knigdelioglu.puanla.domain.ocr

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.math.abs
import kotlinx.coroutines.suspendCancellableCoroutine

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
    val sourceBottom: Int? = null,
    /** Dimensions of the oriented scan bitmap that produced the source boxes. */
    val imageWidth: Int? = null,
    val imageHeight: Int? = null,
    val firstNameCell: OcrCellRegion? = null,
    val lastNameCell: OcrCellRegion? = null,
    /** A secondary OCR reading is only a suggestion; it never silently replaces identity. */
    val firstNameSuggestion: String? = null,
    val lastNameSuggestion: String? = null
)

/** Disjoint image-space cell bounds; used for narrowly scoped second OCR passes. */
data class OcrCellRegion(val left: Int, val top: Int, val right: Int, val bottom: Int)

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

    private val TURKISH = Locale.forLanguageTag("tr-TR")
    private val NON_ALPHANUMERIC_UPPER = Regex("[^A-Z0-9]")
    private val WHITESPACE_REGEX = Regex("\\s+")

    private val GENDER_TOKENS = setOf("K", "E", "KIZ", "ERKEK")
    private val BOARDING_TOKENS = setOf("P", "G", "PANSİYONLU", "GÜNDÜZLÜ", "PANSIYONLU", "GUNDUZLU", "PANSİYON", "PANSIYON", "YATILI")
    private val HEADER_TOKENS = setOf("SIRA", "NO", "ÖĞRENCİ", "OGRENCI", "OKUL", "ADI", "SOYADI", "CİNSİYET", "CINSIYET", "PANSİYON", "PANSIYON", "SINIF", "ŞUBE", "SUBE")

    private val HEADER_ANCHOR_TOKENS = setOf(
        "ADI", "AD", "ADISOYADI", "SOYADI", "SOYAD", "NO", "OGRENCI",
        "OGRENCINO", "NUMARASI", "OKUL", "CINSIYET", "CINSIYETI",
        "PANSIYON", "PANSIYONDURUM"
    )

    private val METADATA_VALUES = setOf(
        "K", "E", "KIZ", "ERKEK", "P", "G", "YATILI",
        "PANSIYONLU", "GUNDUZLU", "KR", "YP"
    )

    private data class OverlapFiltering(
        val unique: List<OcrPositionedWord>,
        val conflicting: List<OcrPositionedWord>
    )

    /** Represents a single horizontal surname line in the surname column. */
    private data class SurnameLine(
        val words: List<OcrPositionedWord>,
        val projectedY: Double
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

    /**
     * Runs ML Kit on-device text recognition on a bitmap.
     */
    suspend fun processBitmap(bitmap: Bitmap): List<OcrStudentRow> {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        return try {
            suspend fun recognize(source: Bitmap, recycleWhenDone: Boolean = false): Text =
                suspendCancellableCoroutine { cont ->
                    recognizer.process(InputImage.fromBitmap(source, 0))
                        .addOnSuccessListener {
                            if (cont.isActive) cont.resume(it)
                        }
                        .addOnFailureListener {
                            if (cont.isActive) cont.resumeWith(Result.failure(it))
                        }
                        .addOnCompleteListener {
                            // Recycle only after ML Kit's native engine finishes processing,
                            // preventing native SIGSEGV if the coroutine is cancelled mid-flight.
                            if (recycleWhenDone && source !== bitmap && !source.isRecycled) {
                                source.recycle()
                            }
                        }
                }

            val recognized = recognize(bitmap, recycleWhenDone = false)
            var recognizedLineIndex = 0
            val words = recognized.textBlocks.flatMap { block ->
                block.lines.flatMap { line ->
                    val lineIndex = recognizedLineIndex++
                    line.elements.mapNotNull { element ->
                        element.boundingBox?.let { box ->
                            OcrPositionedWord(
                                element.text, box.left, box.top, box.width(), box.height(),
                                recognizedLineIndex = lineIndex
                            )
                        }
                    }
                }
            }
            val candidates = parsePositionedWords(words).map {
                it.copy(imageWidth = bitmap.width, imageHeight = bitmap.height)
            }
            // Only uncertain name cells are re-read. Keep the first result and
            // the original ambiguity flag: secondary OCR must never auto-approve
            // a pupil or borrow another physical row's words.
            var retries = 0
            suspend fun retry(region: OcrCellRegion?): String? {
                if (region == null || retries >= 12) return null
                val left = region.left.coerceIn(0, bitmap.width - 1)
                val top = region.top.coerceIn(0, bitmap.height - 1)
                val right = region.right.coerceIn(left + 1, bitmap.width)
                val bottom = region.bottom.coerceIn(top + 1, bitmap.height)
                if (right - left < 10 || bottom - top < 8) return null
                retries++
                val cellBitmap = Bitmap.createBitmap(bitmap, left, top, right - left, bottom - top)
                return try {
                    val raw = recognize(cellBitmap, recycleWhenDone = true).text
                    raw.replace(WHITESPACE_REGEX, " ").trim()
                        .takeIf { it.any(Char::isLetter) && it.length <= 100 }
                } catch (_: Exception) {
                    null // A failed retry must never discard the original OCR result.
                }
            }
            candidates.map { candidate ->
                if (!candidate.isAmbiguous) candidate else {
                    val rereadFirst = if (candidate.firstName.isBlank()) {
                        retry(candidate.firstNameCell)
                    } else null
                    val rereadLast = if (candidate.lastName.isBlank() ||
                        candidate.ambiguityReason?.contains("hizası") == true
                    ) {
                        retry(candidate.lastNameCell)
                    } else null
                    candidate.copy(
                        firstNameSuggestion = rereadFirst,
                        lastNameSuggestion = rereadLast
                    )
                }
            }
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

        fun normalizeToken(text: String): String =
            text.trim().uppercase(TURKISH)
                .replace('Ç', 'C').replace('Ğ', 'G').replace('İ', 'I')
                .replace('Ö', 'O').replace('Ş', 'S').replace('Ü', 'U')
                .replace(NON_ALPHANUMERIC_UPPER, "")

        fun median(values: List<Double>): Double {
            if (values.isEmpty()) return 0.0
            val sorted = values.sorted()
            val middle = sorted.size / 2
            return if (sorted.size % 2 == 0) (sorted[middle - 1] + sorted[middle]) / 2.0
            else sorted[middle]
        }

        val slope = estimatePageSlope(uniqueWords, ::normalizeToken, ::median)
        fun projectedY(word: OcrPositionedWord) = word.centerY - slope * word.centerX

        // ML Kit lines are useful for finding the header, but NOT for assigning
        // names to students: its line indices can span different physical rows.
        val headerLines = mutableListOf<MutableList<OcrPositionedWord>>()
        for (word in uniqueWords.sortedWith(compareBy<OcrPositionedWord> { projectedY(it) }.thenBy { it.x })) {
            val row = word.recognizedLineIndex?.let { lineIndex ->
                headerLines.firstOrNull { group -> group.any { it.recognizedLineIndex == lineIndex } }
            } ?: headerLines.minByOrNull { group ->
                abs(group.map { projectedY(it) }.average() - projectedY(word))
            }?.takeIf { group ->
                val height = group.map { it.height }.average()
                abs(group.map { projectedY(it) }.average() - projectedY(word)) <=
                    maxOf(6.0, height * 0.80)
            }
            if (row != null) row.add(word) else headerLines.add(mutableListOf(word))
        }
        headerLines.sortBy { group -> group.map { projectedY(it) }.average() }
        val headerIndex = headerLines.indexOfFirst { group ->
            val tokens = group.map { normalizeToken(it.text) }
            tokens.any { it in setOf("ADI", "AD", "ADISOYADI") } &&
                tokens.any { it in setOf("NO", "OGRENCI", "OGRENCINO", "NUMARASI", "OKUL") }
        }
        if (headerIndex < 0) return emptyList()
        val headers = headerLines[headerIndex].sortedBy { it.x }
        fun xOf(vararg labels: String): Double? = headers.firstOrNull { word ->
            val token = normalizeToken(word.text)
            labels.any { label -> token == label || token.startsWith(label) }
        }?.centerX
        val numberX = xOf("OGRENCI", "OKUL", "NUMARASI")
            ?: headers.filter { normalizeToken(it.text) == "NO" }.lastOrNull()?.centerX
            ?: return emptyList()
        val nameX = xOf("ADI", "ADISOYADI", "AD") ?: return emptyList()
        val surnameX = xOf("SOYADI", "SOYAD")
        val sequenceX = xOf("SNO", "SIRA")
            ?: headers.filter { normalizeToken(it.text) == "NO" && it.centerX < numberX - 25.0 }
                .minByOrNull { it.centerX }?.centerX
        val metadataAnchor = listOfNotNull(xOf("CINSIYET"), xOf("PANSIYON", "YATILI"))
            .filter { it > nameX }.minOrNull()

        // Do not assume a fixed number of pixels between the first- and
        // last-name headers: scaled-down scans are still separate columns.
        val nameHeader = headers.firstOrNull {
            normalizeToken(it.text) in setOf("ADI", "AD", "ADISOYADI")
        }
        val surnameHeader = headers.firstOrNull {
            normalizeToken(it.text) in setOf("SOYADI", "SOYAD")
        }
        val lastNameColumn = surnameHeader != null && nameHeader != null &&
            surnameHeader.x >= nameHeader.x + nameHeader.width &&
            surnameX != null && (metadataAnchor == null || surnameX < metadataAnchor)
        val metadataStart = metadataAnchor?.let {
            ((if (lastNameColumn) surnameX!! else nameX) + it) / 2.0
        } ?: Double.POSITIVE_INFINITY
        val schoolLeft = sequenceX?.let { (it + numberX) / 2.0 }
            ?: numberX - (nameX - numberX) / 2.0
        val schoolRight = (numberX + nameX) / 2.0
        val surnameBoundary = if (lastNameColumn) (nameX + surnameX!!) / 2.0 else metadataStart

        val headerBottom = headers.maxOf { projectedY(it) + it.height / 2.0 }
        val bodyWords = uniqueWords.filter { projectedY(it) > headerBottom + 2.0 }
        if (bodyWords.isEmpty()) return emptyList()
        val typicalHeight = median(bodyWords.map { it.height.toDouble() }.filter { it > 0.0 })
            .coerceAtLeast(6.0)
        val anchorMergeDistance = maxOf(3.0, typicalHeight * 0.38)
        fun isNumber(word: OcrPositionedWord) =
            word.text.length in 1..6 && word.text.all(Char::isDigit)

        // Each school-number box anchors a PHYSICAL row. Duplicate detections of
        // the same box collapse; school numbers never get inferred from row order.
        data class RowSeed(val word: OcrPositionedWord, val hasNumber: Boolean)
        val seeds = mutableListOf<RowSeed>()
        fun addSeed(word: OcrPositionedWord, hasNumber: Boolean) {
            val matchingIndex = seeds.indexOfFirst {
                abs(projectedY(it.word) - projectedY(word)) <= anchorMergeDistance
            }
            if (matchingIndex < 0) {
                seeds.add(RowSeed(word, hasNumber))
            } else {
                val previous = seeds[matchingIndex]
                if ((hasNumber && !previous.hasNumber) ||
                    (hasNumber == previous.hasNumber &&
                        abs(word.centerX - numberX) <
                        abs(previous.word.centerX - numberX))) {
                    seeds[matchingIndex] = RowSeed(word, hasNumber)
                }
            }
        }
        bodyWords.filter {
            isNumber(it) && it.centerX >= schoolLeft && it.centerX < schoolRight
        }.sortedBy(::projectedY).forEach { addSeed(it, true) }

        // If one school's number is unreadable, the printed sequence column can
        // still preserve its row. It must NOT fabricate a school number.
        if (sequenceX != null) {
            val sequenceRight = (sequenceX + numberX) / 2.0
            bodyWords.filter {
                isNumber(it) && it.centerX < sequenceRight &&
                    abs(it.centerX - sequenceX) <
                    abs(it.centerX - numberX)
            }.sortedBy(::projectedY).forEach { addSeed(it, false) }
        }

        fun pitch(): Double {
            val positions = seeds.map { projectedY(it.word) }.sorted()
            val gaps = positions.zipWithNext { a, b -> b - a }
                .filter { it > anchorMergeDistance * 1.5 }
            return median(gaps).takeIf { it > 0.0 } ?: typicalHeight * 1.8
        }

        // A missing number AND missing printed sequence must not shift later
        // students. Preserve an identifiable name row as an ambiguous candidate.
        val preliminaryPitch = pitch()
        val nameWords = bodyWords.filter {
            it.centerX >= schoolRight && it.centerX < surnameBoundary &&
                it.text.any(Char::isLetter)
        }
        val nameGroups = mutableListOf<MutableList<OcrPositionedWord>>()
        for (word in nameWords.sortedBy(::projectedY)) {
            val group = nameGroups.lastOrNull()?.takeIf {
                abs(projectedY(it.first()) - projectedY(word)) <=
                    maxOf(4.0, typicalHeight * 0.65)
            }
            if (group == null) nameGroups.add(mutableListOf(word)) else group.add(word)
        }
        for (group in nameGroups) {
            val representative = group.minByOrNull { abs(it.centerX - nameX) } ?: continue
            val y = median(group.map(::projectedY))
            if (seeds.none { abs(projectedY(it.word) - y) <=
                    maxOf(typicalHeight * 0.8, preliminaryPitch * 0.38) }) {
                addSeed(representative, false)
            }
        }
        seeds.sortBy { projectedY(it.word) }
        if (seeds.isEmpty()) return emptyList()
        val rowPitch = pitch().coerceAtLeast(typicalHeight * 0.9)

        fun looksLikeMetadata(word: OcrPositionedWord) =
            normalizeToken(word.text) in METADATA_VALUES

        // Pre-compute each row's base band, school number, first-name words, and
        // local vertical reference (anchored by the first-name column when available).
        data class RowBaseData(
            val index: Int,
            val center: Double,
            val top: Double,
            val bottom: Double,
            val baseRowWords: List<OcrPositionedWord>,
            val schoolCandidates: List<OcrPositionedWord>,
            val school: OcrPositionedWord?,
            val firstWords: List<OcrPositionedWord>,
            val metadataInSurnameZone: Boolean,
            val rowReferenceY: Double
        )

        val rowBases = seeds.mapIndexed { index, seed ->
            val center = projectedY(seed.word)
            val previous = seeds.getOrNull(index - 1)?.let { projectedY(it.word) }
            val next = seeds.getOrNull(index + 1)?.let { projectedY(it.word) }
            val top = maxOf(
                headerBottom + 2.0,
                previous?.let { (it + center) / 2.0 } ?: (center - rowPitch * 0.5)
            )
            val bottom = next?.let { (center + it) / 2.0 } ?: (center + rowPitch * 0.5)
            val baseRowWords = bodyWords.filter {
                val y = projectedY(it)
                y >= top && y < bottom
            }.sortedBy { it.x }

            val schoolCandidates = baseRowWords.filter {
                isNumber(it) && it.centerX >= schoolLeft && it.centerX < schoolRight
            }
            val school = schoolCandidates.minByOrNull { abs(it.centerX - numberX) }
            val firstWords = baseRowWords.filter {
                it.centerX >= schoolRight && it.centerX < surnameBoundary &&
                    it.text.any(Char::isLetter) && !looksLikeMetadata(it)
            }
            val metadataInSurnameZone = lastNameColumn && baseRowWords.any {
                it.centerX >= surnameBoundary && it.centerX < metadataStart &&
                    it.text.any(Char::isLetter) && looksLikeMetadata(it)
            }

            // Use the adjacent first-name Y as a local reference when it is well-behaved,
            // preventing accumulated horizontal slope drift from shifting surnames.
            val firstNameY = if (firstWords.isNotEmpty()) {
                median(firstWords.map(::projectedY))
            } else null
            val rowReferenceY = if (firstNameY != null && abs(firstNameY - center) <= rowPitch * 0.42) {
                center * 0.35 + firstNameY * 0.65
            } else {
                center
            }

            RowBaseData(
                index = index,
                center = center,
                top = top,
                bottom = bottom,
                baseRowWords = baseRowWords,
                schoolCandidates = schoolCandidates,
                school = school,
                firstWords = firstWords,
                metadataInSurnameZone = metadataInSurnameZone,
                rowReferenceY = rowReferenceY
            )
        }

        // Match surname lines 1-to-1 with rows so two vertically stacked surnames
        // can NEVER be merged onto a single student while leaving a neighbor empty.
        val assignedSurnamesByRow: Map<Int, List<OcrPositionedWord>> = if (lastNameColumn) {
            val allSurnameWords = bodyWords.filter {
                it.centerX >= surnameBoundary && it.centerX < metadataStart &&
                    it.text.any(Char::isLetter) && !looksLikeMetadata(it)
            }
            val surnameLines = buildSurnameLines(allSurnameWords, typicalHeight, ::projectedY)
            matchSurnameLinesToRows(
                rowReferenceYs = rowBases.map { it.rowReferenceY },
                surnameLines = surnameLines,
                typicalHeight = typicalHeight,
                rowPitch = rowPitch
            )
        } else {
            emptyMap()
        }

        val output = mutableListOf<OcrStudentRow>()
        for (base in rowBases) {
            val firstWords = base.firstWords
            val lastWords = assignedSurnamesByRow[base.index].orEmpty()

            val firstTokens = firstWords.map { it.text.trim() }.filter(String::isNotEmpty)
            val lastTokens = lastWords.map { it.text.trim() }.filter(String::isNotEmpty)
            val firstName = if (lastNameColumn) firstTokens.joinToString(" ")
            else firstTokens.dropLast(1).joinToString(" ")
            val lastName = if (lastNameColumn) lastTokens.joinToString(" ")
            else firstTokens.lastOrNull().orEmpty()

            // Extract optional metadata columns (gender & boarding status) from the row band.
            val metadataWords = base.baseRowWords.filter {
                it.centerX >= surnameBoundary && looksLikeMetadata(it)
            }
            var detectedGender: String? = null
            var detectedBoarding: String? = null
            for (metaWord in metadataWords) {
                val upper = metaWord.text.trim().uppercase(TURKISH)
                val norm = normalizeToken(upper)
                when {
                    upper in GENDER_TOKENS || norm in GENDER_TOKENS -> {
                        detectedGender = if (norm.startsWith("K")) "Kız" else "Erkek"
                    }
                    upper in BOARDING_TOKENS || norm in BOARDING_TOKENS -> {
                        detectedBoarding = if (norm.startsWith("P") || norm.startsWith("Y")) "Pansiyonlu" else "Gündüzlü"
                    }
                }
            }

            // Combine actual matched words for accurate row bounding boxes.
            val effectiveRowWords = (
                base.baseRowWords.filter { it.centerX < surnameBoundary || it.centerX >= metadataStart } +
                    lastWords
                ).distinct()
            if (effectiveRowWords.isEmpty()) continue

            val rowTop = effectiveRowWords.minOf { it.y }
            val rowBottom = effectiveRowWords.maxOf { it.y + it.height }
            val rowLeft = effectiveRowWords.minOf { it.x }
            val rowRight = effectiveRowWords.maxOf { it.x + it.width }
            val conflictingRead = filtered.conflicting.any { other ->
                other.y < rowBottom && other.y + other.height > rowTop &&
                    other.x < rowRight && other.x + other.width > rowLeft
            }

            // Check alignment against both the seed center (for first names) and
            // the local row reference Y (for surnames).
            val alignmentLimit = maxOf(typicalHeight * 0.65, rowPitch * 0.32)
            val firstMisaligned = firstWords.any {
                abs(projectedY(it) - base.center) > alignmentLimit
            }
            val lastMisaligned = lastWords.any {
                abs(projectedY(it) - base.rowReferenceY) > alignmentLimit
            }
            val misaligned = firstMisaligned || lastMisaligned

            val missingFields = base.school == null || firstName.isBlank() ||
                lastName.isBlank() || !lastNameColumn
            val ambiguous = missingFields || conflictingRead || misaligned ||
                base.metadataInSurnameZone || base.schoolCandidates.size > 1

            // Project the disjoint row band back to each column's local image
            // coordinates. On a skewed page this avoids cropping a neighbor's text.
            fun cellRegion(left: Double, right: Double, columnX: Double): OcrCellRegion? {
                if (!lastNameColumn || !left.isFinite() || !right.isFinite() ||
                    right <= left) return null
                val topY = (base.top + slope * columnX).toInt().coerceAtLeast(0)
                val bottomY = (base.bottom + slope * columnX).toInt()
                if (bottomY <= topY) return null
                return OcrCellRegion(
                    left.toInt().coerceAtLeast(0),
                    topY,
                    right.toInt(),
                    bottomY
                )
            }
            val lastRight = if (metadataStart.isFinite()) metadataStart
            else if (surnameX != null) surnameX + (surnameX - nameX)
            else surnameBoundary
            val firstCell = cellRegion(schoolRight, surnameBoundary, nameX)
            val lastCell = cellRegion(surnameBoundary, lastRight, surnameX ?: nameX)

            output.add(
                OcrStudentRow(
                    rawIndex = output.size + 1,
                    studentNumber = base.school?.text.orEmpty(),
                    firstName = firstName,
                    lastName = lastName.uppercase(TURKISH),
                    gender = detectedGender,
                    boardingStatus = detectedBoarding,
                    isAmbiguous = ambiguous,
                    ambiguityReason = when {
                        conflictingRead -> "Aynı görüntü bölgesinden farklı OCR okumaları geldi; fotoğrafla doğrulayın."
                        misaligned -> "Ad veya soyadın fiziksel satır hizası belirsiz; kaynak fotoğraftan doğrulayın."
                        base.schoolCandidates.size > 1 -> "Aynı satırda birden fazla okul numarası okundu."
                        base.metadataInSurnameZone -> "Soyadı alanında cinsiyet/yatılı bilgisi algılandı; kaynaktan doğrulayın."
                        missingFields -> "Eksik numara/ad/soyad veya birleşik sütun; öğretmen doğrulamalı."
                        else -> null
                    },
                    isApproved = false,
                    sourceLeft = rowLeft,
                    sourceTop = rowTop,
                    sourceRight = rowRight,
                    sourceBottom = rowBottom,
                    firstNameCell = firstCell,
                    lastNameCell = lastCell
                )
            )
        }
        return output
    }

    /**
     * Estimates page skew while ignoring outlier words in document titles or footers.
     */
    private fun estimatePageSlope(
        uniqueWords: List<OcrPositionedWord>,
        normalizeToken: (String) -> String,
        median: (List<Double>) -> Double
    ): Double {
        val allHeaderAnchors = uniqueWords.filter {
            normalizeToken(it.text) in HEADER_ANCHOR_TOKENS
        }
        // Keep only the densest horizontal band of header anchors so words like
        // "OKUL" or "ÖĞRENCİ" in the page title/footer cannot corrupt the regression.
        val inlierAnchors = if (allHeaderAnchors.size >= 3) {
            allHeaderAnchors.map { ref ->
                allHeaderAnchors.filter { other ->
                    val maxAllowedDeltaY = maxOf(40.0, abs(other.centerX - ref.centerX) * 0.14)
                    abs(other.centerY - ref.centerY) <= maxAllowedDeltaY
                }
            }.maxByOrNull { it.size }.orEmpty()
        } else {
            allHeaderAnchors
        }

        if (inlierAnchors.size >= 3) {
            val meanX = inlierAnchors.map { it.centerX }.average()
            val meanY = inlierAnchors.map { it.centerY }.average()
            val varianceX = inlierAnchors.sumOf { (it.centerX - meanX) * (it.centerX - meanX) }
            if (varianceX > 0.0) {
                return (inlierAnchors.sumOf { (it.centerX - meanX) * (it.centerY - meanY) } / varianceX)
                    .coerceIn(-0.12, 0.12)
            }
        }

        // Fallback: median of intra-line slopes from ML Kit recognized lines.
        val lineSlopes = uniqueWords
            .filter { it.recognizedLineIndex != null }
            .groupBy { it.recognizedLineIndex }
            .values
            .mapNotNull { lineWords ->
                if (lineWords.size < 2) return@mapNotNull null
                val sorted = lineWords.sortedBy { it.centerX }
                val first = sorted.first()
                val last = sorted.last()
                val dx = last.centerX - first.centerX
                if (dx >= 80.0) {
                    val s = (last.centerY - first.centerY) / dx
                    if (abs(s) <= 0.12) s else null
                } else null
            }

        return if (lineSlopes.isNotEmpty()) {
            median(lineSlopes).coerceIn(-0.12, 0.12)
        } else 0.0
    }

    /**
     * Groups surname words into distinct horizontal lines. Side-by-side words on the
     * same horizontal line stay together, whereas vertically stacked words become
     * separate [SurnameLine] entries.
     */
    private fun buildSurnameLines(
        surnameWords: List<OcrPositionedWord>,
        typicalHeight: Double,
        projectedY: (OcrPositionedWord) -> Double
    ): List<SurnameLine> {
        if (surnameWords.isEmpty()) return emptyList()
        val maxSameLineDeltaY = maxOf(4.0, typicalHeight * 0.45)
        val rawGroups = mutableListOf<MutableList<OcrPositionedWord>>()

        for (word in surnameWords.sortedWith(compareBy<OcrPositionedWord> { projectedY(it) }.thenBy { it.x })) {
            val wy = projectedY(word)
            val matchingGroup = rawGroups.lastOrNull()?.takeIf { group ->
                val groupMeanY = group.map(projectedY).average()
                val verticallyClose = abs(wy - groupMeanY) <= maxSameLineDeltaY
                val horizontallyOverlaps = group.any { existing ->
                    val xOverlap = (minOf(word.x + word.width, existing.x + existing.width) -
                        maxOf(word.x, existing.x)).coerceAtLeast(0)
                    val minW = minOf(word.width, existing.width)
                    minW > 0 && xOverlap.toDouble() / minW >= 0.30
                }
                verticallyClose && !horizontallyOverlaps
            }
            if (matchingGroup != null) {
                matchingGroup.add(word)
            } else {
                rawGroups.add(mutableListOf(word))
            }
        }

        return rawGroups.map { group ->
            val sortedByX = group.sortedBy { it.x }
            SurnameLine(
                words = sortedByX,
                projectedY = sortedByX.map(projectedY).average()
            )
        }.sortedBy { it.projectedY }
    }

    /**
     * Order-preserving 1-to-1 matching between student rows and horizontal surname lines.
     * Prevents a single student from receiving two vertically stacked surnames while
     * leaving an adjacent student row without a surname.
     */
    private fun matchSurnameLinesToRows(
        rowReferenceYs: List<Double>,
        surnameLines: List<SurnameLine>,
        typicalHeight: Double,
        rowPitch: Double
    ): Map<Int, List<OcrPositionedWord>> {
        val n = rowReferenceYs.size
        val m = surnameLines.size
        if (n == 0 || m == 0) return emptyMap()

        val maxMatchDist = maxOf(typicalHeight * 1.35, rowPitch * 0.78)
        val matchRewardBase = maxMatchDist * 2.5

        val dp = Array(n + 1) { DoubleArray(m + 1) }
        val choice = Array(n + 1) { IntArray(m + 1) } // 1 = skip row, 2 = skip line, 3 = match

        for (i in 1..n) {
            for (j in 1..m) {
                // Option 1: leave row (i - 1) without a surname line
                var bestScore = dp[i - 1][j]
                var bestChoice = 1

                // Option 2: skip surname line (j - 1)
                if (dp[i][j - 1] > bestScore) {
                    bestScore = dp[i][j - 1]
                    bestChoice = 2
                }

                // Option 3: match row (i - 1) with surname line (j - 1)
                val dist = abs(rowReferenceYs[i - 1] - surnameLines[j - 1].projectedY)
                if (dist <= maxMatchDist) {
                    val candidateScore = dp[i - 1][j - 1] + (matchRewardBase - dist)
                    if (candidateScore > bestScore) {
                        bestScore = candidateScore
                        bestChoice = 3
                    }
                }

                dp[i][j] = bestScore
                choice[i][j] = bestChoice
            }
        }

        val result = mutableMapOf<Int, List<OcrPositionedWord>>()
        var i = n
        var j = m
        while (i > 0 && j > 0) {
            when (choice[i][j]) {
                3 -> {
                    result[i - 1] = surnameLines[j - 1].words
                    i--
                    j--
                }
                1 -> i--
                2 -> j--
                else -> break
            }
        }
        return result
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
        val rawTokens = line.split(WHITESPACE_REGEX).filter { it.isNotBlank() }
        if (rawTokens.isEmpty()) return null

        // If line is a header line, skip
        val isHeader = rawTokens.any { token -> HEADER_TOKENS.contains(token.uppercase(TURKISH)) }
        if (isHeader) return null

        val filteredTokens = mutableListOf<String>()
        var detectedGender: String? = null
        var detectedBoarding: String? = null

        // Separate neighboring metadata columns
        for (token in rawTokens) {
            val upper = token.uppercase(TURKISH)
            when {
                GENDER_TOKENS.contains(upper) -> {
                    detectedGender = if (upper.startsWith("K")) "Kız" else "Erkek"
                }
                BOARDING_TOKENS.contains(upper) -> {
                    detectedBoarding = if (upper.startsWith("P") || upper.startsWith("Y")) "Pansiyonlu" else "Gündüzlü"
                }
                else -> {
                    filteredTokens.add(token)
                }
            }
        }

        if (filteredTokens.isEmpty()) return null

        // Locate student school number (digits sequence with 1-6 chars)
        var studentNumber: String? = null
        val nameTokens = mutableListOf<String>()

        for (token in filteredTokens) {
            val cleanDigits = token.filter { it.isDigit() }
            if (cleanDigits.length in 1..6 && studentNumber == null && cleanDigits == token) {
                // If it looks like index number 1..59 at the very start and there are more tokens, check next
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
                val upperClean = cleanWord.uppercase(TURKISH)
                if (cleanWord.isNotBlank() && !GENDER_TOKENS.contains(upperClean) && !BOARDING_TOKENS.contains(upperClean)) {
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
            lastName = lastName.uppercase(TURKISH),
            gender = detectedGender,
            boardingStatus = detectedBoarding,
            isAmbiguous = isAmbiguous,
            ambiguityReason = reason,
            isApproved = false
        )
    }

    private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
}