package com.knigdelioglu.puanla.domain

import com.knigdelioglu.puanla.domain.export.CsvCriterion
import com.knigdelioglu.puanla.domain.export.CsvExportFormatter
import com.knigdelioglu.puanla.domain.export.CsvStudent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tests the production CSV formatter itself, never hand-made expected logic. */
class CsvExportFormatTest {
    private val criteria = listOf(CsvCriterion("c1", "İçerik", 20), CsvCriterion("c2", "İfade", 80))

    @Test fun productionCsvIncludesBomAndCorrectTurkishText() {
        val csv = CsvExportFormatter.generate(criteria, listOf(
            CsvStudent("101", "Ayşe", "ÇELİK", mapOf("c1" to 20, "c2" to 65), 85, true, 2)
        ))
        assertTrue(csv.startsWith("\uFEFF"))
        assertEquals("\uFEFFOkul No;Adı;Soyadı;İçerik (Azami 20);İfade (Azami 80);Toplam Puan (100);Değerlendirme Durumu\r\n" +
            "101;Ayşe;ÇELİK;20;65;85;Tamamlandı\r\n", csv)
    }

    @Test fun missingIsNeverConfusedWithZeroOrDefinitiveGrade() {
        val csv = CsvExportFormatter.generate(criteria, listOf(
            CsvStudent("4", "Mert", "ŞAHİN", mapOf("c1" to 0), 99, true, 1),
            CsvStudent("5", "Can", "KAYA")
        ))
        assertTrue(csv.contains("4;Mert;ŞAHİN;0;Puanlanmadı;Eksik;Kısmi\r\n"))
        assertTrue(csv.contains("5;Can;KAYA;Puanlanmadı;Puanlanmadı;Eksik;Başlanmadı\r\n"))
        assertFalse(csv.contains(";99;Tamamlandı"))
    }

    @Test fun separatorsQuotesNewlinesAndFormulaInjectionAreEscaped() {
        val csv = CsvExportFormatter.generate(
            listOf(CsvCriterion("c1", "Metin; \"kanıt\"", 10)),
            listOf(CsvStudent("6", "=HYPERLINK(\"x\")", "YILMAZ;ÖZ", mapOf("c1" to 10), 10, true, 1))
        )
        assertTrue(csv.contains("\"Metin; \"\"kanıt\"\" (Azami 10)\""))
        assertTrue(csv.contains("6;\"'=HYPERLINK(\"\"x\"\")\";\"YILMAZ;ÖZ\";10;10;Tamamlandı"))
        assertEquals("'@SUM(1)", CsvExportFormatter.escapeField("@SUM(1)"))
        assertEquals("\"O\\nK\"".replace("\\n", "\n"), CsvExportFormatter.escapeField("O\nK"))
    }

    @Test fun headerReflectsCriterionMaximumInsteadOfHardcodedHundred() {
        val csv = CsvExportFormatter.generate(listOf(CsvCriterion("c", "Başarı", 8)), emptyList())
        assertTrue(csv.contains("Toplam Puan (8)"))
    }
}
