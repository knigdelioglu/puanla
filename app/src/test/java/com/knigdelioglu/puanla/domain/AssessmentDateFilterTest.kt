package com.knigdelioglu.puanla.domain

import com.knigdelioglu.puanla.data.local.AssessmentEntity
import com.knigdelioglu.puanla.domain.report.AssessmentDateFilter
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class AssessmentDateFilterTest {
    private val zone = ZoneId.of("Europe/Istanbul")
    private fun item(id: String, day: String, hour: Int = 12): AssessmentEntity {
        val whenMillis = LocalDate.parse(day).atTime(hour, 0).atZone(zone).toInstant().toEpochMilli()
        return AssessmentEntity(
            id = id, classroomId = "c", studentId = id, rubricId = "r",
            lastModifiedAt = whenMillis
        )
    }

    @Test fun inclusiveDaysAndOpenBounds() {
        val rows = listOf(item("before", "2026-10-09"), item("first", "2026-10-10"),
            item("last", "2026-10-11", 23), item("after", "2026-10-12"))
        assertEquals(listOf("first", "last"),
            AssessmentDateFilter.filter(rows, "2026-10-10", "2026-10-11", zone).map { it.id })
        assertEquals(listOf("before", "first"),
            AssessmentDateFilter.filter(rows, "", "2026-10-10", zone).map { it.id })
        assertEquals(4, AssessmentDateFilter.filter(rows, "", "", zone).size)
    }

    @Test fun invalidDatesAreRejected() {
        assertFalse(AssessmentDateFilter.isValidRange("2026-10-12", "2026-10-11"))
        assertFalse(AssessmentDateFilter.isValidRange("invalid", ""))
        assertThrows(IllegalArgumentException::class.java) {
            AssessmentDateFilter.filter(emptyList(), "2026-10-12", "2026-10-11", zone)
        }
    }
}
