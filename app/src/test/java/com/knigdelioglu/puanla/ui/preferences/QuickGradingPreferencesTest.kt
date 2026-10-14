package com.knigdelioglu.puanla.ui.preferences

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class QuickGradingPreferencesTest {
    @Test
    fun defaultRetainsExistingNumericQuickScoring() {
        val preference = QuickGradingPreference()
        assertEquals(false, preference.qualitativeEnabled)
        assertEquals(4, preference.levelCount)
        assertEquals(QuickGradeAppearance.Text, preference.appearance)
    }

    @Test
    fun threeLevelsUseExplicitBoundedScores() {
        val choices = qualitativeQuickOptions(20, 3, QuickGradeAppearance.Text)
        assertEquals(listOf("Geliştirilmeli", "Yeterli", "İyi"), choices.map { it.label })
        assertEquals(listOf(0, 10, 20), choices.map { it.points })
    }

    @Test
    fun fourLevelsScaleToCriterionLimitIncludingFivePoints() {
        val choices = qualitativeQuickOptions(5, 4, QuickGradeAppearance.Stars)
        assertEquals(listOf(0, 2, 3, 5), choices.map { it.points })
        assertEquals(listOf("★", "★★", "★★★", "★★★★"), choices.map { it.icon })
        assertEquals(listOf("Yetersiz", "Geliştirilmeli", "Yeterli", "İyi"), choices.map { it.label })
    }

    @Test
    fun lowMaximumOffersOnlyUniqueIntegerScoresForBothModes() {
        assertEquals(listOf(0, 1), qualitativeQuickOptions(1, 4, QuickGradeAppearance.Text).map { it.points })
        assertEquals(listOf(0, 1, 2), qualitativeQuickOptions(2, 4, QuickGradeAppearance.Text).map { it.points })
        assertEquals(listOf(0), qualitativeQuickOptions(0, 4, QuickGradeAppearance.Text).map { it.points })
        assertEquals(listOf(0, 1), qualitativeQuickOptions(1, 3, QuickGradeAppearance.Faces).map { it.points })
    }

    @Test
    fun smileFacesRetainTheSameNumericScores() {
        val stars = qualitativeQuickOptions(10, 3, QuickGradeAppearance.Stars)
        val faces = qualitativeQuickOptions(10, 3, QuickGradeAppearance.Faces)
        assertEquals(stars.map { it.points }, faces.map { it.points })
        assertEquals(listOf(0, 5, 10), faces.map { it.points })
        assertTrue(faces.all { it.icon.isNotBlank() })
    }

    @Test
    fun corruptStyleFallsBackToReadableLabels() {
        assertEquals(QuickGradeAppearance.Text, QuickGradeAppearance.fromStored("unknown"))
        assertEquals(QuickGradeAppearance.Text, QuickGradeAppearance.fromStored(null))
        assertEquals(QuickGradeAppearance.Faces, QuickGradeAppearance.fromStored("Faces"))
    }

    @Test
    fun invalidBoundsAreRejectedAndLargeValuesAvoidOverflow() {
        assertThrows(IllegalArgumentException::class.java) {
            qualitativeQuickOptions(-1, 3, QuickGradeAppearance.Text)
        }
        assertThrows(IllegalArgumentException::class.java) {
            qualitativeQuickOptions(20, 5, QuickGradeAppearance.Text)
        }
        assertEquals(Int.MAX_VALUE,
            qualitativeQuickOptions(Int.MAX_VALUE, 4, QuickGradeAppearance.Text).last().points)
    }

    @Test
    fun zeroScoreRemainsActualZeroAndNotUnscored() {
        assertEquals(0, qualitativeQuickOptions(20, 4, QuickGradeAppearance.Text).first().points)
        assertEquals(0, qualitativeQuickOptions(0, 3, QuickGradeAppearance.Text).last().points)
    }
}
