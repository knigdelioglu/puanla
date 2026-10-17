package com.knigdelioglu.puanla.domain.ocr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PastedRosterParserTest {
    @Test
    fun parsesExplicitNumbersAndLeavesRowsUnapproved() {
        val result = PastedRosterParser.parse(
            "No - Ad Soyad\n101 - Ada Su Deneme\n102 Eren Örnek"
        )

        assertEquals(2, result.rows.size)
        assertEquals(0, result.ignoredLineCount)
        assertEquals("101", result.rows[0].studentNumber)
        assertEquals("Ada Su", result.rows[0].firstName)
        assertEquals("DENEME", result.rows[0].lastName)
        assertEquals("102", result.rows[1].studentNumber)
        assertTrue(result.rows.none { it.isApproved })
        assertTrue(result.rows.none { it.isAmbiguous })
    }

    @Test
    fun ignoresMissingNumbersAndIncompleteNamesInsteadOfGuessing() {
        val result = PastedRosterParser.parse(
            "Ada Deneme\n103 - Eren\n104 - Geçerli Örnek"
        )

        assertEquals(1, result.rows.size)
        assertEquals("104", result.rows.single().studentNumber)
        assertEquals(2, result.ignoredLineCount)
        assertFalse(result.rows.single().isApproved)
    }
}
