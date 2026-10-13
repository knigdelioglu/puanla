package com.knigdelioglu.puanla.domain.export

/**
 * A transactionally-read, immutable report snapshot. Individual and group
 * assessments are intentionally not merged: they have different provenance.
 */
data class PdfReportSnapshot(
    val classroomName: String,
    val academicYear: String,
    val rubricTitle: String,
    val criteria: List<CsvCriterion>,
    val students: List<CsvStudent>
)

enum class PdfScoreState { COMPLETED, PARTIAL, NOT_STARTED }

data class PdfReportSummary(
    val registered: Int,
    val completed: Int,
    val partial: Int,
    val notStarted: Int,
    val average: Float?,
    val highest: Int?,
    val lowest: Int?,
    val gradeDistribution: List<Int>
)

/** Same definitive-grade invariants as the CSV exporter. Null never means zero. */
object PdfReportStatistics {
    fun definitiveScore(student: CsvStudent, criteria: List<CsvCriterion>): Int? {
        if (!student.isCompleted || criteria.isEmpty()) return null
        if (criteria.any { criterion ->
                student.scores[criterion.id]?.let { it in 0..criterion.maxPoints } != true
            }) return null
        val total = criteria.sumOf { student.scores[it.id] ?: 0 }
        return total.takeIf { it == student.definitiveTotal }
    }

    fun state(student: CsvStudent, criteria: List<CsvCriterion>): PdfScoreState = when {
        definitiveScore(student, criteria) != null -> PdfScoreState.COMPLETED
        student.scoredCount > 0 || student.scores.values.any { it != null } -> PdfScoreState.PARTIAL
        else -> PdfScoreState.NOT_STARTED
    }

    fun summary(snapshot: PdfReportSnapshot): PdfReportSummary {
        val definitive = snapshot.students.mapNotNull { definitiveScore(it, snapshot.criteria) }
        val states = snapshot.students.map { state(it, snapshot.criteria) }
        return PdfReportSummary(
            registered = snapshot.students.size,
            completed = states.count { it == PdfScoreState.COMPLETED },
            partial = states.count { it == PdfScoreState.PARTIAL },
            notStarted = states.count { it == PdfScoreState.NOT_STARTED },
            average = definitive.takeIf { it.isNotEmpty() }?.average()?.toFloat(),
            highest = definitive.maxOrNull(),
            lowest = definitive.minOrNull(),
            gradeDistribution = listOf(
                definitive.count { it < 50 },
                definitive.count { it in 50..69 },
                definitive.count { it in 70..84 },
                definitive.count { it >= 85 }
            )
        )
    }
}
