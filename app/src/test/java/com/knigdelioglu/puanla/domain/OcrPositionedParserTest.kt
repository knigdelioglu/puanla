package com.knigdelioglu.puanla.domain

import com.knigdelioglu.puanla.domain.ocr.OcrPositionedWord
import com.knigdelioglu.puanla.domain.ocr.OcrRosterParser
import org.junit.Assert.*
import org.junit.Test

class OcrPositionedParserTest {
    @Test fun splitAndUnaccentedColumnLabelsAreReunitedAcrossPerspective() {
        val words = listOf(
            OcrPositionedWord("S.NO", 10, 10, 30, 16),
            OcrPositionedWord("OGRENCI", 100, 13, 60, 16),
            OcrPositionedWord("NO", 162, 16, 18, 16),
            OcrPositionedWord("ADI", 240, 19, 30, 16),
            OcrPositionedWord("SOYADI", 395, 22, 60, 16),
            OcrPositionedWord("CINSIYETI", 580, 25, 75, 16),
            OcrPositionedWord("PANSIYON DURUM", 730, 28, 130, 16),
            OcrPositionedWord("1", 20, 70, 9, 16),
            OcrPositionedWord("245", 128, 72, 30, 16),
            OcrPositionedWord("AYŞE", 242, 75, 39, 16),
            OcrPositionedWord("NUR", 290, 78, 30, 16),
            OcrPositionedWord("ÇELİK", 396, 81, 49, 16),
            OcrPositionedWord("Kız", 582, 84, 25, 16),
            OcrPositionedWord("Yatılı", 732, 87, 35, 16)
        )

        val student = OcrRosterParser.parsePositionedWords(words).single()

        assertEquals("245", student.studentNumber)
        assertEquals("AYŞE NUR", student.firstName)
        assertEquals("ÇELİK", student.lastName)
        assertFalse(student.isAmbiguous)
        assertFalse(student.isApproved)
    }

    @Test fun photographedClassListKeepsSkewedHeaderAndTurkishColumnsTogether() {
        val words = listOf(
            OcrPositionedWord("S.No", 10, 10, 30, 16, recognizedLineIndex = 0),
            OcrPositionedWord("Öğrenci", 100, 13, 60, 16, recognizedLineIndex = 0),
            OcrPositionedWord("No", 162, 16, 18, 16, recognizedLineIndex = 0),
            OcrPositionedWord("Adı", 240, 19, 30, 16, recognizedLineIndex = 0),
            OcrPositionedWord("Soyadı", 395, 22, 60, 16, recognizedLineIndex = 0),
            OcrPositionedWord("Cinsiyeti", 580, 25, 75, 16, recognizedLineIndex = 0),
            OcrPositionedWord("Pansiyon", 730, 28, 75, 16, recognizedLineIndex = 0),
            OcrPositionedWord("Durum", 820, 31, 50, 16, recognizedLineIndex = 0),
            OcrPositionedWord("1", 20, 70, 9, 16, recognizedLineIndex = 1),
            OcrPositionedWord("245", 128, 72, 30, 16, recognizedLineIndex = 1),
            OcrPositionedWord("AYŞE", 242, 75, 39, 16, recognizedLineIndex = 1),
            OcrPositionedWord("NUR", 290, 78, 30, 16, recognizedLineIndex = 1),
            OcrPositionedWord("ÇELİK", 396, 81, 49, 16, recognizedLineIndex = 1),
            OcrPositionedWord("Kız", 582, 84, 25, 16, recognizedLineIndex = 1),
            OcrPositionedWord("Yatılı", 732, 87, 35, 16, recognizedLineIndex = 1)
        )

        val student = OcrRosterParser.parsePositionedWords(words).single()

        assertEquals("245", student.studentNumber)
        assertEquals("AYŞE NUR", student.firstName)
        assertEquals("ÇELİK", student.lastName)
        assertFalse(student.isAmbiguous)
        assertFalse(student.isApproved)
    }

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

    @Test fun overlappingMlKitReadingsProduceOnlyOneAmbiguousCandidate() {
        val words = listOf(
            OcrPositionedWord("ÖĞRENCİ", 100, 10, 60, 16, recognizedLineIndex = 0),
            OcrPositionedWord("NO", 162, 10, 18, 16, recognizedLineIndex = 0),
            OcrPositionedWord("ADI", 240, 10, 30, 16, recognizedLineIndex = 0),
            OcrPositionedWord("SOYADI", 395, 10, 60, 16, recognizedLineIndex = 0),
            OcrPositionedWord("245", 128, 65, 30, 16, recognizedLineIndex = 1),
            OcrPositionedWord("AYŞE", 242, 65, 39, 16, recognizedLineIndex = 1),
            OcrPositionedWord("YILMAZ", 396, 65, 60, 16, recognizedLineIndex = 1),
            // The engine described the same pixels twice using another recognized line.
            OcrPositionedWord("999", 129, 66, 30, 16, recognizedLineIndex = 2),
            OcrPositionedWord("aesye", 243, 66, 39, 16, recognizedLineIndex = 2),
            OcrPositionedWord("yklmnz", 397, 66, 60, 16, recognizedLineIndex = 2)
        )
        val students = OcrRosterParser.parsePositionedWords(words)
        assertEquals(1, students.size)
        assertEquals("245", students.single().studentNumber)
        assertEquals("AYŞE", students.single().firstName)
        assertEquals("YILMAZ", students.single().lastName)
        assertTrue(students.single().isAmbiguous)
        assertNotNull(students.single().sourceTop)
    }

    @Test fun adjacentPhysicalRowsNeverMerge() {
        val header = listOf(
            OcrPositionedWord("ÖĞRENCİ", 100, 10, 60, 16),
            OcrPositionedWord("NO", 162, 10, 18, 16),
            OcrPositionedWord("ADI", 240, 10, 30, 16),
            OcrPositionedWord("SOYADI", 395, 10, 60, 16)
        )
        val studentWords = listOf(
            OcrPositionedWord("245", 128, 65, 30, 16),
            OcrPositionedWord("AYŞE", 242, 65, 39, 16),
            OcrPositionedWord("YILMAZ", 396, 65, 60, 16),
            OcrPositionedWord("246", 128, 95, 30, 16),
            OcrPositionedWord("AYŞE", 242, 95, 39, 16),
            OcrPositionedWord("YILDIRIM", 396, 95, 72, 16)
        )
        val students = OcrRosterParser.parsePositionedWords(header + studentWords)
        assertEquals(2, students.size)
        assertEquals(listOf("245", "246"), students.map { it.studentNumber })
    }

    @Test fun misleadingMlKitLineIndicesDoNotMoveSurnamesToAdjacentStudents() {
        val header = listOf(
            OcrPositionedWord("ÖĞRENCİ", 100, 10, 60, 16, recognizedLineIndex = 0),
            OcrPositionedWord("NO", 162, 10, 18, 16, recognizedLineIndex = 0),
            OcrPositionedWord("ADI", 240, 10, 30, 16, recognizedLineIndex = 0),
            OcrPositionedWord("SOYADI", 395, 10, 60, 16, recognizedLineIndex = 0)
        )
        // A bad ML Kit line association reports every surname as belonging to the
        // next student's text line. Pixel geometry, not this ID, must win.
        val students = (0 until 20).flatMap { index ->
            val y = 65 + index * 29
            listOf(
                OcrPositionedWord((245 + index).toString(), 128, y, 30, 16,
                    recognizedLineIndex = index + 1),
                OcrPositionedWord("AD$index", 242, y, 48, 16,
                    recognizedLineIndex = index + 1),
                OcrPositionedWord("SOYAD$index", 396, y, 70, 16,
                    recognizedLineIndex = index + 2)
            )
        }
        val rows = OcrRosterParser.parsePositionedWords(header + students)
        assertEquals(20, rows.size)
        rows.forEachIndexed { index, row ->
            assertEquals((245 + index).toString(), row.studentNumber)
            assertEquals("AD$index", row.firstName)
            assertEquals("SOYAD$index", row.lastName)
            assertFalse(row.isAmbiguous)
            assertFalse(row.isApproved)
        }
    }

    @Test fun missingSchoolNumberUsesSequenceAnchorWithoutBorrowingNeighbors() {
        val header = listOf(
            OcrPositionedWord("S.NO", 10, 10, 32, 16),
            OcrPositionedWord("ÖĞRENCİ", 100, 10, 60, 16),
            OcrPositionedWord("NO", 162, 10, 18, 16),
            OcrPositionedWord("ADI", 240, 10, 30, 16),
            OcrPositionedWord("SOYADI", 395, 10, 60, 16)
        )
        val body = listOf(
            OcrPositionedWord("1", 20, 65, 9, 16),
            OcrPositionedWord("245", 128, 65, 30, 16),
            OcrPositionedWord("AYŞE", 242, 65, 40, 16),
            OcrPositionedWord("YILMAZ", 396, 65, 65, 16),
            OcrPositionedWord("2", 20, 95, 9, 16),
            OcrPositionedWord("MEHMET", 242, 95, 60, 16),
            OcrPositionedWord("DEMİR", 396, 95, 58, 16),
            OcrPositionedWord("3", 20, 125, 9, 16),
            OcrPositionedWord("247", 128, 125, 30, 16),
            OcrPositionedWord("ELİF", 242, 125, 40, 16),
            OcrPositionedWord("ÇELİK", 396, 125, 58, 16)
        )
        val rows = OcrRosterParser.parsePositionedWords(header + body)
        assertEquals(3, rows.size)
        assertEquals(listOf("245", "", "247"), rows.map { it.studentNumber })
        assertEquals(listOf("YILMAZ", "DEMİR", "ÇELİK"), rows.map { it.lastName })
        assertEquals("MEHMET", rows[1].firstName)
        assertTrue(rows[1].isAmbiguous)
    }

    @Test fun missingBothNumberAndSequenceCreatesAmbiguousGapRow() {
        val header = listOf(
            OcrPositionedWord("ÖĞRENCİ", 100, 10, 60, 16),
            OcrPositionedWord("NO", 162, 10, 18, 16),
            OcrPositionedWord("ADI", 240, 10, 30, 16),
            OcrPositionedWord("SOYADI", 395, 10, 60, 16)
        )
        val body = listOf(
            OcrPositionedWord("245", 128, 65, 30, 16),
            OcrPositionedWord("AYŞE", 242, 65, 40, 16),
            OcrPositionedWord("YILMAZ", 396, 65, 65, 16),
            OcrPositionedWord("MEHMET", 242, 95, 60, 16),
            OcrPositionedWord("DEMİR", 396, 95, 58, 16),
            OcrPositionedWord("247", 128, 125, 30, 16),
            OcrPositionedWord("ELİF", 242, 125, 40, 16),
            OcrPositionedWord("ÇELİK", 396, 125, 58, 16)
        )
        val rows = OcrRosterParser.parsePositionedWords(header + body)
        assertEquals(3, rows.size)
        assertEquals(listOf("AYŞE", "MEHMET", "ELİF"), rows.map { it.firstName })
        assertEquals(listOf("YILMAZ", "DEMİR", "ÇELİK"), rows.map { it.lastName })
        assertEquals("", rows[1].studentNumber)
        assertTrue(rows[1].isAmbiguous)
    }

    @Test fun missingSurnameIsNeverCompletedFromFollowingRow() {
        val header = listOf(
            OcrPositionedWord("ÖĞRENCİ", 100, 10, 60, 16),
            OcrPositionedWord("NO", 162, 10, 18, 16),
            OcrPositionedWord("ADI", 240, 10, 30, 16),
            OcrPositionedWord("SOYADI", 395, 10, 60, 16)
        )
        val body = listOf(
            OcrPositionedWord("245", 128, 65, 30, 16),
            OcrPositionedWord("AYŞE", 242, 65, 40, 16),
            OcrPositionedWord("YILMAZ", 396, 65, 60, 16),
            OcrPositionedWord("246", 128, 95, 30, 16),
            OcrPositionedWord("MEHMET", 242, 95, 60, 16),
            OcrPositionedWord("247", 128, 125, 30, 16),
            OcrPositionedWord("ELİF", 242, 125, 40, 16),
            OcrPositionedWord("ÇELİK", 396, 125, 58, 16)
        )
        val rows = OcrRosterParser.parsePositionedWords(header + body)
        assertEquals(3, rows.size)
        assertEquals("", rows[1].lastName)
        assertTrue(rows[1].isAmbiguous)
        assertEquals("ÇELİK", rows[2].lastName)
    }


    @Test fun nameAndSurnameCellRegionsStayWithinSeparatePhysicalRows() {
        val header = listOf(
            OcrPositionedWord("ÖĞRENCİ", 100, 10, 60, 16),
            OcrPositionedWord("NO", 162, 10, 18, 16),
            OcrPositionedWord("ADI", 240, 10, 30, 16),
            OcrPositionedWord("SOYADI", 395, 10, 60, 16)
        )
        val students = (0 until 5).flatMap { index ->
            val y = 65 + 29 * index
            listOf(
                OcrPositionedWord((245 + index).toString(), 128, y, 30, 16),
                OcrPositionedWord("AD$index", 242, y, 40, 16),
                OcrPositionedWord("SOYAD$index", 396, y, 70, 16)
            )
        }
        val rows = OcrRosterParser.parsePositionedWords(header + students)
        assertEquals(5, rows.size)
        rows.forEach { row ->
            val first = row.firstNameCell!!
            val last = row.lastNameCell!!
            assertTrue(first.right <= last.left)
            assertTrue(first.bottom > first.top)
            assertTrue(last.bottom > last.top)
            assertNull(row.firstNameSuggestion)
            assertNull(row.lastNameSuggestion)
        }
        rows.zipWithNext { a, b ->
            assertTrue(a.firstNameCell!!.bottom <= b.firstNameCell!!.top)
            assertTrue(a.lastNameCell!!.bottom <= b.lastNameCell!!.top)
        }
    }

    @Test fun combinedNameColumnNeverCreatesUnsafeSurnameRetryCrop() {
        val words = listOf(
            OcrPositionedWord("ÖĞRENCİ", 100, 10, 60, 16),
            OcrPositionedWord("NO", 162, 10, 18, 16),
            OcrPositionedWord("ADI", 240, 10, 30, 16),
            OcrPositionedWord("245", 128, 65, 30, 16),
            OcrPositionedWord("AYŞE", 242, 65, 40, 16),
            OcrPositionedWord("YILMAZ", 288, 65, 60, 16)
        )
        val row = OcrRosterParser.parsePositionedWords(words).single()
        assertTrue(row.isAmbiguous)
        assertNull(row.firstNameCell)
        assertNull(row.lastNameCell)
    }

    @Test fun numberedRowWithUnreadableBothNamesIsKeptForReview() {
        val words = listOf(
            OcrPositionedWord("ÖĞRENCİ", 100, 10, 60, 16),
            OcrPositionedWord("NO", 162, 10, 18, 16),
            OcrPositionedWord("ADI", 240, 10, 30, 16),
            OcrPositionedWord("SOYADI", 395, 10, 60, 16),
            OcrPositionedWord("245", 128, 65, 30, 16),
            OcrPositionedWord("AYŞE", 242, 65, 40, 16),
            OcrPositionedWord("YILMAZ", 396, 65, 60, 16),
            OcrPositionedWord("246", 128, 95, 30, 16),
            OcrPositionedWord("247", 128, 125, 30, 16),
            OcrPositionedWord("ELİF", 242, 125, 40, 16),
            OcrPositionedWord("ÇELİK", 396, 125, 58, 16)
        )
        val rows = OcrRosterParser.parsePositionedWords(words)
        assertEquals(3, rows.size)
        assertEquals("246", rows[1].studentNumber)
        assertEquals("", rows[1].firstName)
        assertEquals("", rows[1].lastName)
        assertTrue(rows[1].isAmbiguous)
        assertFalse(rows[1].isApproved)
    }

}
