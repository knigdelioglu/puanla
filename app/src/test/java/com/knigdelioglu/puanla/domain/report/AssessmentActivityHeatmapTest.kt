package com.knigdelioglu.puanla.domain.report

import com.knigdelioglu.puanla.data.local.AuditLogEntity
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class AssessmentActivityHeatmapTest {
    private val today = LocalDate.of(2026, 10, 11)
    private val zone = ZoneId.of("Europe/Istanbul")

    @Test
    fun `only real score changes in range contribute and blank days stay empty`() {
        val logs = buildList {
            addAll(logs("CRITERION_SCORE_CHANGED", today, 4))
            addAll(logs("CRITERION_SCORE_CHANGED", today.minusDays(1), 2))
            addAll(logs("CREATE_CLASSROOM", today, 12))
            addAll(logs("CRITERION_SCORE_CHANGED", today.minusDays(28), 1))
            addAll(logs("CRITERION_SCORE_CHANGED", today.plusDays(1), 1))
        }

        val actual = assessmentActivityByDay(logs, today, zone)

        assertEquals(28, actual.size)
        assertEquals(0, actual.first())
        assertEquals(2, actual[26])
        assertEquals(3, actual.last())
        assertEquals(0, actual[10])
    }

    private fun logs(action: String, date: LocalDate, count: Int) =
        (1..count).map { index ->
            AuditLogEntity(
                id = "$action-${date}-$index",
                timestamp = date.atTime(12, 0).atZone(zone).toInstant().toEpochMilli(),
                action = action,
                details = "test"
            )
        }
}
