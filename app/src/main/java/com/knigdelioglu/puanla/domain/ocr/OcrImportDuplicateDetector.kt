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
                    compareBy<Match> { it.sameNumber }
                        .thenBy { it.matchedCandidateIndex == null }
                        .thenBy { it.sameSourceRow }
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

    /**
     * Validates ALL identity conflicts independently.
     *
     * The UI intentionally shows one best duplicate hint per row. A single hint
     * must not be used for persistence validation: a photo-row duplicate can
     * otherwise hide a different same-number match in the saved classroom.
     */
    fun validateSelectedCandidates(
        candidates: List<OcrStudentRow>,
        currentRoster: List<OcrRosterIdentity>
    ): OcrDuplicateValidation {
        val selectedIndices = candidates.indices.filter { candidates[it].isApproved }
        val numberConflicts = linkedSetOf<Int>()
        val sourceConflicts = linkedSetOf<Int>()
        val unconfirmedMatches = linkedSetOf<Int>()

        for (index in selectedIndices) {
            val candidate = candidates[index]
            val number = candidate.studentNumber.trim()

            // Even a candidate masked by a better source-row match cannot
            // duplicate an already saved classroom number.
            if (number.isNotBlank() &&
                currentRoster.any { number == it.studentNumber.trim() }
            ) {
                numberConflicts.add(index)
            }

            // Do not use only a single "best" previous OCR candidate.
            // Every selected/selected pair must be checked.
            for (priorIndex in selectedIndices.filter { it < index }) {
                val prior = candidates[priorIndex]
                if (number.isNotBlank() && number == prior.studentNumber.trim()) {
                    numberConflicts.add(index)
                }
                if (sameSourceImageRow(candidate, prior)) {
                    sourceConflicts.add(index)
                }
            }

            if (candidate.isConfirmedDistinctFromPossibleMatch) continue

            // Fuzzy warnings remain conservative, including potential OCR
            // repeats that were not individually selected for import.
            val savedFuzzyMatch = currentRoster.any { existing ->
                (number.isBlank() || number != existing.studentNumber.trim()) &&
                    nameSimilarity(candidate, existing) >= NAME_SIMILARITY_THRESHOLD
            }
            val capturedFuzzyMatch = candidates.take(index).any { prior ->
                val sameNumber = number.isNotBlank() && number == prior.studentNumber.trim()
                !sameNumber && !sameSourceImageRow(candidate, prior) &&
                    nameSimilarity(
                        candidate,
                        OcrRosterIdentity(prior.studentNumber, prior.firstName, prior.lastName)
                    ) >= NAME_SIMILARITY_THRESHOLD
            }
            if (savedFuzzyMatch || capturedFuzzyMatch) {
                unconfirmedMatches.add(index)
            }
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
