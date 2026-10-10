package com.knigdelioglu.puanla.domain

import com.knigdelioglu.puanla.domain.ocr.OcrPositionedWord
import com.knigdelioglu.puanla.domain.ocr.OcrRosterParser
import org.junit.Assert.*
import org.junit.Test

class OcrPositionedParserTest {
    @Test fun differentSurnameGenderAndBoardingColumnsNeverMix() {
        val words = listOf(
            OcrPositionedWord("SIRA", 8, 10, 32, 16),
            OcrPositionedWord("NO", 50, 10, 20, 16),
            OcrPositionedWord("ÖĞRENCİ", 110, 10, 65, 16),
            OcrPositionedWord("NO", 177, 10, 19, 16),
            OcrPositionedWord("ADI", 240, 10, 30, 16),
            OcrPositionedWord("SOYADI", 390, 10, 58, 16),
            OcrPositionedWord("CİNSİYET", 550, 10, 72, 16),
            OcrPositionedWord("PANSİYON", 670, 10, 75, 16),
            OcrPositionedWord("1", 20, 48, 9, 16),
            OcrPositionedWord("245", 140, 48, 27, 16),
            OcrPositionedWord("AYŞE", 242, 48, 39, 16),
            OcrPositionedWord("NUR", 290, 48, 30, 16),
            OcrPositionedWord("ÇELİK", 396, 48, 49, 16),
            OcrPositionedWord("K", 570, 48, 14, 16),
            OcrPositionedWord("KR.", 680, 48, 24, 16)
        )
        val rows = OcrRosterParser.parsePositionedWords(words)
        assertEquals(1, rows.size)
        val student = rows.single()
        assertEquals("245", student.studentNumber)
        assertEquals("AYŞE NUR", student.firstName)
        assertEquals("ÇELİK", student.lastName)
        assertFalse(student.isApproved)
        assertFalse(student.lastName.contains("KR"))
        assertFalse(student.lastName.contains("KIZ"))
    }

    @Test fun withoutDetectedHeadersDoesNotFabricateStudentRows() {
        val words = listOf(
            OcrPositionedWord("123", 30, 20, 24, 16),
            OcrPositionedWord("ALİ", 100, 20, 30, 16),
            OcrPositionedWord("YILMAZ", 240, 20, 60, 16)
        )
        assertTrue(OcrRosterParser.parsePositionedWords(words).isEmpty())
    }
}
