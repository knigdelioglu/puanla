package com.knigdelioglu.puanla.domain

import com.knigdelioglu.puanla.domain.ocr.OcrRosterParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OcrRosterParserTest {

    @Test
    fun standardRosterLineParsedCorrectlyWithoutLeakage() {
        val line = "1 245 AHMET YILMAZ E G"
        val row = OcrRosterParser.parseLine(line, 1)

        assertNotNull(row)
        assertEquals("245", row!!.studentNumber)
        assertEquals("AHMET", row.firstName)
        assertEquals("YILMAZ", row.lastName)
        assertEquals("Erkek", row.gender)
        assertEquals("Gündüzlü", row.boardingStatus)
        assertFalse(row.isAmbiguous)
    }

    @Test
    fun multiWordFirstNameParsedCorrectly() {
        val line = "12 518 AYŞE NUR DEMİR K P"
        val row = OcrRosterParser.parseLine(line, 12)

        assertNotNull(row)
        assertEquals("518", row!!.studentNumber)
        assertEquals("AYŞE NUR", row.firstName)
        assertEquals("DEMİR", row.lastName)
        assertEquals("Kız", row.gender)
        assertEquals("Pansiyonlu", row.boardingStatus)
    }

    @Test
    fun headerLineIsSkipped() {
        val header = "SIRA NO ÖĞRENCİ NO ADI SOYADI CİNSİYETİ PANSİYON"
        val row = OcrRosterParser.parseLine(header, 1)
        assertNull(row)
    }

    @Test
    fun singleWordNameFlaggedAsAmbiguous() {
        val line = "302 ELİF"
        val row = OcrRosterParser.parseLine(line, 1)

        assertNotNull(row)
        assertTrue(row!!.isAmbiguous)
        assertEquals("ELİF", row.firstName)
        assertEquals("", row.lastName)
    }

    @Test
    fun fullRosterTextExtraction() {
        val roster = """
            SIRA NO ÖĞRENCİ NO ADI SOYADI CİNSİYET PANSİYON
            1 101 MEHMET KAYA E G
            2 102 ZEYNEP KOÇ K P
            3 103 ALİ CAN ŞAHİN E G
        """.trimIndent()

        val rows = OcrRosterParser.parseVisionText(roster)
        assertEquals(3, rows.size)
        assertEquals("101", rows[0].studentNumber)
        assertEquals("MEHMET", rows[0].firstName)
        assertEquals("KAYA", rows[0].lastName)

        assertEquals("102", rows[1].studentNumber)
        assertEquals("ZEYNEP", rows[1].firstName)
        assertEquals("KOÇ", rows[1].lastName)

        assertEquals("103", rows[2].studentNumber)
        assertEquals("ALİ CAN", rows[2].firstName)
        assertEquals("ŞAHİN", rows[2].lastName)
    }
}
