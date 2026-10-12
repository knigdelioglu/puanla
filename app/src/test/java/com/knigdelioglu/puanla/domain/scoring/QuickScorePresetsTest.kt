package com.knigdelioglu.puanla.domain.scoring

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class QuickScorePresetsTest {
    @Test
    fun numericChoicesCoverRealRubricBounds() {
        assertEquals(listOf(0, 5, 10, 15, 20), quickScorePresets(20))
        assertEquals(listOf(0, 3, 5, 8, 10), quickScorePresets(10))
        assertEquals(listOf(0, 1, 3, 4, 5), quickScorePresets(5))
        assertEquals(listOf(0), quickScorePresets(0))
    }

    @Test
    fun approvedChoicesAreUsedInsteadOfInventingQualitativeGrades() {
        assertEquals(listOf(0, 7, 13, 20), quickScorePresets(20, listOf(20, 7, 0, 13, 7)))
    }

    @Test
    fun invalidAndTooManyLevelsFallBackToBoundedNumericChoices() {
        assertEquals(listOf(0, 5, 10, 15, 20), quickScorePresets(20, listOf(-1, 21, 999)))
        assertEquals(listOf(0, 5, 10, 15, 20), quickScorePresets(20, listOf(0, 1, 2, 3, 4, 5)))
    }

    @Test
    fun negativeMaximumIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { quickScorePresets(-1) }
    }
}
