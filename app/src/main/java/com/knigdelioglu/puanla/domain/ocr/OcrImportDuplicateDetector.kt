package com.knigdelioglu.puanla.domain.ocr

import java.util.Locale

data class OcrRosterIdentity(
    val studentNumber: String,
    val firstName: String,
    val lastName: String
) {
    val fullName: String get() = "$firstName $lastName"
}

data class OcrDuplicateHint(
    val matchedName: String,
    /** Equal same-class school numbers cannot both be imported. */
    val sameStudentNumber: Boolean,
    /** Both OCR candidates originate from the same physical image row. */
    val sameSourceRow: Boolean = false,
    /** Non-null when the match is another OCR row; null means a saved roster entry. */
    val matchedCandidateIndex: Int?
)

data class OcrDuplicateValidation(
    val schoolNumberConflicts: Set<Int>,
    val unconfirmedPossibleMatches: Set<Int>,
    val sourceRowConflicts: Set<Int> = emptySet()
) {
    val canImport: Boolean
        get() = schoolNumberConflicts.isEmpty() && unconfirmedPossibleMatches.isEmpty() && sourceRowConflicts.isEmpty()
}

/** Finds likely repeated OCR rows without silently merging or correcting student records. */
object OcrImportDuplicateDetector {
    fun findPotentialDuplicates(
        candidates: List<OcrStudentRow>,
        currentRoster: List<OcrRosterIdentity>
    ): Map<Int, OcrDuplicateHint> {
        val hints = linkedMapOf<Int, OcrDuplicateHint>()

        candidates.forEachIndexed { index, candidate ->
            val comparisons: List<Pair<OcrRosterIdentity, Int?>> = currentRoster.map { it to null } +
                candidates.take(index).mapIndexed { priorIndex, row ->
                    OcrRosterIdentity(row.studentNumber, row.firstName, row.lastName) to priorIndex
                }
            val match = comparisons
                .mapNotNull { (existing, matchedCandidateIndex) ->
                    val sameNumber = candidate.studentNumber.isNotBlank() &&
                        candidate.studentNumber.trim() == existing.studentNumber.trim()
                    val sameSource = matchedCandidateIndex != null &&
                        sameSourceImageRow(candidate, candidates[matchedCandidateIndex])
                    val similarity = if (sameNumber || sameSource) 1.0 else nameSimilarity(candidate, existing)
                    if (sameNumber || sameSource || similarity >= NAME_SIMILARITY_THRESHOLD) {
                        Match(existing, sameNumber, sameSource, similarity, matchedCandidateIndex)
                    } else null
                }
                .maxWithOrNull(
                    compareBy<Match> { it.sameSourceRow }
                        .thenBy { it.sameNumber }
                        .thenBy { it.matchedCandidateIndex == null }
                        .thenBy { it.similarity }
                )

            if (match != null) {
                hints[index] = OcrDuplicateHint(
                    matchedName = match.identity.fullName,
                    sameStudentNumber = match.sameNumber,
                    sameSourceRow = match.sameSourceRow,
                    matchedCandidateIndex = match.matchedCandidateIndex
                )
            }
        }
        return hints
    }

    fun validateSelectedCandidates(
        candidates: List<OcrStudentRow>,
        currentRoster: List<OcrRosterIdentity>
    ): OcrDuplicateValidation {
        val hints = findPotentialDuplicates(candidates, currentRoster)
        val selectedIndices = candidates.indices.filter { candidates[it].isApproved }
        val numberConflicts = selectedIndices.filterTo(linkedSetOf()) { index ->
            val hint = hints[index] ?: return@filterTo false
            hint.sameStudentNumber && (hint.matchedCandidateIndex == null ||
                candidates[hint.matchedCandidateIndex].isApproved)
        }
        val sourceConflicts = selectedIndices.filterTo(linkedSetOf()) { index ->
            val hint = hints[index] ?: return@filterTo false
            hint.sameSourceRow && hint.matchedCandidateIndex != null &&
                candidates[hint.matchedCandidateIndex].isApproved
        }
        val unconfirmedMatches = selectedIndices.filterTo(linkedSetOf()) { index ->
            val hint = hints[index]
            hint != null && !hint.sameStudentNumber && !hint.sameSourceRow &&
                !candidates[index].isConfirmedDistinctFromPossibleMatch
        }
        return OcrDuplicateValidation(numberConflicts, unconfirmedMatches, sourceConflicts)
    }

    private data class Match(
        val identity: OcrRosterIdentity,
        val sameNumber: Boolean,
        val sameSourceRow: Boolean,
        val similarity: Double,
        val matchedCandidateIndex: Int?
    )

    private fun nameSimilarity(candidate: OcrStudentRow, existing: OcrRosterIdentity): Double {
        val candidateFirst = normalize(candidate.firstName)
        val existingFirst = normalize(existing.firstName)
        val candidateLast = normalize(candidate.lastName)
        val existingLast = normalize(existing.lastName)
        if (candidateFirst.isEmpty() || existingFirst.isEmpty() || candidateLast.isEmpty() || existingLast.isEmpty()) {
            return 0.0
        }

        val firstSimilarity = similarity(candidateFirst, existingFirst)
        val lastSimilarity = similarity(candidateLast, existingLast)
        // Require evidence from both names so a common first name alone cannot flag a student.
        return if (firstSimilarity >= MIN_FIRST_NAME_SIMILARITY &&
            lastSimilarity >= MIN_LAST_NAME_SIMILARITY
        ) {
            (firstSimilarity + lastSimilarity) / 2.0
        } else 0.0
    }

    private fun normalize(value: String): String = value.trim()
        .lowercase(TURKISH)
        .replace('ç', 'c').replace('ğ', 'g').replace('ı', 'i')
        .replace('ö', 'o').replace('ş', 's').replace('ü', 'u')
        .replace(Regex("[^a-z0-9]"), "")

    private fun similarity(left: String, right: String): Double {
        if (left == right) return 1.0
        val longest = maxOf(left.length, right.length)
        if (longest == 0) return 1.0
        val previous = IntArray(right.length + 1) { it }
        val current = IntArray(right.length + 1)
        for (leftIndex in left.indices) {
            current[0] = leftIndex + 1
            for (rightIndex in right.indices) {
                val substitutionCost = if (left[leftIndex] == right[rightIndex]) 0 else 1
                current[rightIndex + 1] = minOf(
                    current[rightIndex] + 1,
                    previous[rightIndex + 1] + 1,
                    previous[rightIndex] + substitutionCost
                )
            }
            current.copyInto(previous)
        }
        val editSimilarity = 1.0 - previous[right.length].toDouble() / longest
        // Jaro-Winkler tolerates swapped, skipped and repeatedly misread OCR letters.
        return maxOf(editSimilarity, jaroWinkler(left, right))
    }

    private fun jaroWinkler(left: String, right: String): Double {
        if (left == right) return 1.0
        if (left.isEmpty() || right.isEmpty()) return 0.0
        val window = (maxOf(left.length, right.length) / 2 - 1).coerceAtLeast(0)
        val leftMatched = BooleanArray(left.length)
        val rightMatched = BooleanArray(right.length)
        var matches = 0
        for (i in left.indices) {
            val start = (i - window).coerceAtLeast(0)
            val end = (i + window + 1).coerceAtMost(right.length)
            for (j in start until end) {
                if (!rightMatched[j] && left[i] == right[j]) {
                    leftMatched[i] = true
                    rightMatched[j] = true
                    matches++
                    break
                }
            }
        }
        if (matches == 0) return 0.0
        var transpositions = 0
        var cursor = 0
        for (i in left.indices) {
            if (!leftMatched[i]) continue
            while (!rightMatched[cursor]) cursor++
            if (left[i] != right[cursor]) transpositions++
            cursor++
        }
        val m = matches.toDouble()
        val jaro = (m / left.length + m / right.length + (m - transpositions / 2.0) / m) / 3.0
        if (jaro <= 0.7) return jaro
        var prefix = 0
        while (prefix < minOf(4, left.length, right.length) && left[prefix] == right[prefix]) prefix++
        return jaro + prefix * 0.1 * (1.0 - jaro)
    }

    private val TURKISH = Locale.forLanguageTag("tr-TR")
    private const val MIN_FIRST_NAME_SIMILARITY = 0.78
    private const val MIN_LAST_NAME_SIMILARITY = 0.75
    private const val NAME_SIMILARITY_THRESHOLD = 0.79
}
