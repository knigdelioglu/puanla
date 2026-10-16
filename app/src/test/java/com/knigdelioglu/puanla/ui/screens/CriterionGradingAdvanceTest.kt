package com.knigdelioglu.puanla.ui.screens

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CriterionGradingAdvanceTest {
    @Test fun quickScoreAutomaticallyMovesOnlyWhenSafe() {
        assertTrue(shouldAdvanceAfterQuickScore(true, true, false, false))
        assertFalse(shouldAdvanceAfterQuickScore(true, false, false, false))
        assertFalse(shouldAdvanceAfterQuickScore(false, true, false, false))
        assertFalse(shouldAdvanceAfterQuickScore(true, true, true, false))
        assertFalse(shouldAdvanceAfterQuickScore(true, true, false, true))
        assertFalse(shouldAdvanceAfterQuickScore(true, true, true, true))
    }
}
