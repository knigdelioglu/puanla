package com.knigdelioglu.puanla.domain.export

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PdfReportStatisticsTest {
    private val criteria = listOf(
        CsvCriterion("a", "İletişim", 40),
        CsvCriterion("b", "İfade", 60)
    )

    @Test fun completedGradesRequireEveryCriterionAndMatchingTotal() {
        val completed = CsvStudent(
            number = "12", firstName = "Ayşe", lastName = "Şahin",
            scores = mapOf("a" to 0, "b" to 60),
            definitiveTotal = 60, isCompleted = true, scoredCount = 2
        )
        assertEquals(60, PdfReportStatistics.definitiveScore(completed, criteria))
        assertEquals(PdfScoreState.COMPLETED, PdfReportStatistics.state(completed, criteria))
        assertNull(PdfReportStatistics.definitiveScore(completed.copy(definitiveTotal = 70), criteria))
        assertNull(PdfReportStatistics.definitiveScore(completed.copy(scores = mapOf("a" to null, "b" to 60)), criteria))
        assertNull(PdfReportStatistics.definitiveScore(completed.copy(scores = mapOf("a" to -1, "b" to 60)), criteria))
        assertNull(PdfReportStatistics.definitiveScore(completed.copy(isCompleted = false), criteria))
    }

    @Test fun reportDoesNotTurnMissingScoresIntoZeroOrDefinitiveGrades() {
        val complete = CsvStudent(
            "1", "Can", "Demir", mapOf("a" to 40, "b" to 60), 100, true, 2
        )
        val partial = CsvStudent(
            "2", "İpek", "Çelik", mapOf("a" to 0, "b" to null), null, false, 1
        )
        val empty = CsvStudent("3", "Ömer", "Güneş")
        val snapshot = PdfReportSnapshot("11-C", "2026-2027", "Drama", criteria, listOf(complete, partial, empty))
        val summary = PdfReportStatistics.summary(snapshot)
        assertEquals(3, summary.registered)
        assertEquals(1, summary.completed)
        assertEquals(1, summary.partial)
        assertEquals(1, summary.notStarted)
        assertEquals(100f, summary.average)
        assertEquals(100, summary.highest)
        assertEquals(100, summary.lowest)
        assertEquals(listOf(0, 0, 0, 1), summary.gradeDistribution)
        assertEquals(PdfScoreState.PARTIAL, PdfReportStatistics.state(partial, criteria))
        assertEquals(PdfScoreState.NOT_STARTED, PdfReportStatistics.state(empty, criteria))
    }

    @Test fun reportWithNoFinalizedGradesHasNoAverageOrExtrema() {
        val snapshot = PdfReportSnapshot("11-C", "2026-2027", "Drama", criteria, emptyList())
        val summary = PdfReportStatistics.summary(snapshot)
        assertNull(summary.average)
        assertNull(summary.highest)
        assertNull(summary.lowest)
        assertEquals(listOf(0, 0, 0, 0), summary.gradeDistribution)
    }
}
