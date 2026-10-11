package com.knigdelioglu.puanla.domain.roster

import org.junit.Assert.*
import org.junit.Test

class RosterStatusFilterTest {
    @Test fun finishedAndPartialNeverOverlap() {
        assertTrue(RosterStatusFilter.matches("Tamamlanan", true, 4))
        assertFalse(RosterStatusFilter.matches("Kısmi", true, 4))
        assertTrue(RosterStatusFilter.matches("Kısmi", false, 2))
        assertFalse(RosterStatusFilter.matches("Tamamlanan", false, 2))
    }

    @Test fun missingAssessmentIsUnscoredNotGradedZero() {
        assertTrue(RosterStatusFilter.matches("Puanlanmadı", false, 0))
        assertFalse(RosterStatusFilter.matches("Kısmi", false, 0))
        assertFalse(RosterStatusFilter.matches("Puanlanmadı", false, 1))
        assertTrue(RosterStatusFilter.matches("Tümü", false, 0))
    }
}
