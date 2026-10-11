package com.knigdelioglu.puanla.domain.report

import com.knigdelioglu.puanla.data.local.AuditLogEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Maps real score-change audit entries to the most recent 28 calendar days.
 * Empty dates remain zero; only score changes contribute to the evaluation view.
 */
fun assessmentActivityByDay(
    logs: List<AuditLogEntity>,
    today: LocalDate,
    zone: ZoneId
): List<Int> {
    val firstDay = today.minusDays(27)
    val counts = logs.asSequence()
        .filter { it.action == "CRITERION_SCORE_CHANGED" }
        .map { Instant.ofEpochMilli(it.timestamp).atZone(zone).toLocalDate() }
        .filter { it in firstDay..today }
        .groupingBy { it }
        .eachCount()

    return (0L..27L).map { offset ->
        when (val count = counts[today.minusDays(27L - offset)] ?: 0) {
            0 -> 0
            1 -> 1
            in 2..3 -> 2
            in 4..7 -> 3
            else -> 4
        }
    }
}
