package com.knigdelioglu.puanla.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScoringRulesTest {
    @Test fun zeroIsNotUnscored() {
        val result = summarizeScores(listOf(CriterionScore(10, 0), CriterionScore(20, 15)))
        assertTrue(result.completed)
        assertEquals(15, result.definitiveTotal)
    }

    @Test fun missingScoreIsNeverFinal() {
        val result = summarizeScores(listOf(CriterionScore(10, 0), CriterionScore(20, null)))
        assertFalse(result.completed)
        assertNull(result.definitiveTotal)
        assertEquals(1, result.scoredCount)
    }

    @Test(expected = IllegalArgumentException::class)
    fun scoreCannotExceedCriterionMaximum() {
        CriterionScore(10, 11)
    }

    @Test fun emptyRubricIsNotCompleted() {
        assertFalse(summarizeScores(emptyList()).completed)
    }
}
