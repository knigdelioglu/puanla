package com.knigdelioglu.arc.compose.controls

import org.junit.Assert.*
import org.junit.Test

class ArcNumberInputRulesTest {
    @Test fun acceptsZeroWithoutConfusingItWithUnscored() {
        assertEquals(0, ArcNumberInputRules.parse("0", 0, 100))
        assertNull(ArcNumberInputRules.parse("", 0, 100))
        assertEquals(100, ArcNumberInputRules.parse("100", 0, 100))
    }

    @Test fun rejectsPartialOverflowAndOutOfRangeValues() {
        assertNull(ArcNumberInputRules.parse("-", 0, 100))
        assertNull(ArcNumberInputRules.parse("101", 0, 100))
        assertNull(ArcNumberInputRules.parse("99999999999", 0, 100))
        assertNull(ArcNumberInputRules.parse("1.5", 0, 100))
        assertNull(ArcNumberInputRules.parse("20", 100, 0))
    }
}
