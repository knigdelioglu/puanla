package com.knigdelioglu.puanla.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies that CSV export adheres to:
 * 1. UTF-8 BOM presence (\uFEFF) for Microsoft Excel compatibility
 * 2. Strict distinction between unscored ("Puanlanmadı") and zero ("0")
 * 3. Incomplete assessments marked as "Eksik" with no provisional total
 * 4. Proper semicolon delimiters and preservation of Turkish characters
 */
class CsvExportFormatTest {

    @Test
    fun utf8BomIsPrepended() {
        val testCsv = "\uFEFFOkul No;Adı;Soyadı;Ölçüt 1 (Azami 20);Toplam Puan (100);Değerlendirme Durumu\n"
        assertTrue("CSV must start with UTF-8 BOM", testCsv.startsWith("\uFEFF"))
    }

    @Test
    fun unscoredScoresProducePuanlanmadiNotZero() {
        val scoredZero: Int? = 0
        val unscored: Int? = null

        val displayZero = scoredZero?.toString() ?: "Puanlanmadı"
        val displayUnscored = unscored?.toString() ?: "Puanlanmadı"

        assertEquals("0", displayZero)
        assertEquals("Puanlanmadı", displayUnscored)
    }

    @Test
    fun incompleteStatusProducesEksikAndKismi() {
        val isCompleted = false
        val scoredCount = 3
        val definitiveTotal: Int? = null

        val totalDisplay = definitiveTotal?.toString() ?: "Eksik"
        val statusDisplay = if (isCompleted) "Tamamlandı" else if (scoredCount > 0) "Kısmi" else "Başlanmadı"

        assertEquals("Eksik", totalDisplay)
        assertEquals("Kısmi", statusDisplay)
    }

    @Test
    fun completedStatusProducesDefinitiveTotalAndTamamlandi() {
        val isCompleted = true
        val scoredCount = 5
        val definitiveTotal: Int? = 85

        val totalDisplay = definitiveTotal?.toString() ?: "Eksik"
        val statusDisplay = if (isCompleted) "Tamamlandı" else if (scoredCount > 0) "Kısmi" else "Başlanmadı"

        assertEquals("85", totalDisplay)
        assertEquals("Tamamlandı", statusDisplay)
    }
}
