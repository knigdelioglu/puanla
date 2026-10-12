package com.knigdelioglu.puanla.domain

import com.knigdelioglu.puanla.domain.ocr.OcrImportDuplicateDetector
import com.knigdelioglu.puanla.domain.ocr.OcrRosterIdentity
import com.knigdelioglu.puanla.domain.ocr.OcrStudentRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OcrImportDuplicateDetectorTest {
    @Test
    fun recognizesTurkishNameWithMultipleOcrTyposAsPossibleDuplicate() {
        val rows = listOf(
            row("100", "Şevin", "Yücem"),
            row("110", "Sevrn", "Ytckm")
        )

        val hints = OcrImportDuplicateDetector.findPotentialDuplicates(rows, emptyList())

        assertFalse(hints.containsKey(0))
        assertEquals("Şevin Yücem", hints[1]?.matchedName)
        assertFalse(hints[1]?.sameStudentNumber ?: true)
    }

    @Test
    fun matchesAgainstExistingClassRosterEvenWhenSchoolNumberWasMisread() {
        val hints = OcrImportDuplicateDetector.findPotentialDuplicates(
            candidates = listOf(row("999", "Sevrn", "Ytckm")),
            currentRoster = listOf(OcrRosterIdentity("100", "Şevin", "Yücem"))
        )

        assertEquals("Şevin Yücem", hints[0]?.matchedName)
        assertFalse(hints[0]?.sameStudentNumber ?: true)
    }

    @Test
    fun duplicateSchoolNumberIsFlaggedEvenIfNamesDiffer() {
        val hints = OcrImportDuplicateDetector.findPotentialDuplicates(
            candidates = listOf(row("100", "Lorin", "Şerel")),
            currentRoster = listOf(OcrRosterIdentity("100", "Noyan", "Güzel"))
        )

        assertEquals("Noyan Güzel", hints[0]?.matchedName)
        assertTrue(hints[0]?.sameStudentNumber ?: false)
    }

    @Test
    fun sharedFirstNameAloneDoesNotCreateDuplicateWarning() {
        val hints = OcrImportDuplicateDetector.findPotentialDuplicates(
            candidates = listOf(row("110", "Şevin", "Demir")),
            currentRoster = listOf(OcrRosterIdentity("100", "Şevin", "Yücem"))
        )

        assertTrue(hints.isEmpty())
    }

    @Test
    fun selectedFuzzyMatchRequiresExplicitDistinctStudentConfirmation() {
        val candidates = listOf(
            row("100", "Şevin", "Yücem"),
            row("110", "Sevrn", "Ytckm").copy(isApproved = true)
        )

        val pending = OcrImportDuplicateDetector.validateSelectedCandidates(candidates, emptyList())
        val confirmed = OcrImportDuplicateDetector.validateSelectedCandidates(
            candidates.mapIndexed { index, candidate ->
                if (index == 1) candidate.copy(isConfirmedDistinctFromPossibleMatch = true) else candidate
            },
            emptyList()
        )

        assertEquals(setOf(1), pending.unconfirmedPossibleMatches)
        assertFalse(pending.canImport)
        assertTrue(confirmed.canImport)
    }

    @Test
    fun repeatedSchoolNumberOnlyBlocksWhenBothRowsWouldBeImported() {
        val candidates = listOf(
            row("100", "Nehir", "Aksoy"),
            row("100", "Nehrr", "Aksoi").copy(isApproved = true)
        )

        val singleSelectedRow = OcrImportDuplicateDetector.validateSelectedCandidates(candidates, emptyList())
        val bothSelectedRows = OcrImportDuplicateDetector.validateSelectedCandidates(
            candidates.map { it.copy(isApproved = true) },
            emptyList()
        )

        assertTrue(singleSelectedRow.canImport)
        assertEquals(setOf(1), bothSelectedRows.schoolNumberConflicts)
        assertFalse(bothSelectedRows.canImport)
    }


    @Test
    fun flagsAyseYilmazAndCorruptedAesyeYklmnzWithoutMatchingSchoolNumber() {
        val candidates = listOf(
            row("245", "Ayşe", "Yılmaz"),
            row("999", "aesye", "yklmnz").copy(isApproved = true)
        )
        val hints = OcrImportDuplicateDetector.findPotentialDuplicates(candidates, emptyList())
        assertEquals("Ayşe Yılmaz", hints[1]?.matchedName)
        assertFalse(hints[1]?.sameStudentNumber ?: true)
        assertFalse(OcrImportDuplicateDetector.validateSelectedCandidates(candidates, emptyList()).canImport)
    }

    @Test
    fun twoCandidatesFromIdenticalPhotoRowCannotBothBeImportedEvenWhenNamesDiffer() {
        val candidates = listOf(
            row("245", "Ayşe", "Yılmaz").copy(
                isApproved = true, sourceLeft = 20, sourceTop = 50,
                sourceRight = 450, sourceBottom = 70
            ),
            row("999", "Mehmet", "Demir").copy(
                isApproved = true, sourceLeft = 22, sourceTop = 51,
                sourceRight = 453, sourceBottom = 71,
                isConfirmedDistinctFromPossibleMatch = true
            )
        )
        val validation = OcrImportDuplicateDetector.validateSelectedCandidates(candidates, emptyList())
        assertEquals(setOf(1), validation.sourceRowConflicts)
        assertFalse(validation.canImport)
        val single = OcrImportDuplicateDetector.validateSelectedCandidates(
            candidates.mapIndexed { index, candidate ->
                if (index == 1) candidate.copy(isApproved = false) else candidate
            }, emptyList()
        )
        assertTrue(single.canImport)
    }

    @Test
    fun differentPhotoRowsAreNeverTreatedAsSameSource() {
        val candidates = listOf(
            row("245", "Ayşe", "Yılmaz").copy(
                isApproved = true, sourceLeft = 20, sourceTop = 50,
                sourceRight = 450, sourceBottom = 70
            ),
            row("246", "Mehmet", "Demir").copy(
                isApproved = true, sourceLeft = 20, sourceTop = 85,
                sourceRight = 450, sourceBottom = 105
            )
        )
        assertTrue(OcrImportDuplicateDetector.validateSelectedCandidates(candidates, emptyList()).canImport)
    }

    private fun row(number: String, firstName: String, lastName: String) = OcrStudentRow(
        rawIndex = number.toInt(),
        studentNumber = number,
        firstName = firstName,
        lastName = lastName
    )
}
