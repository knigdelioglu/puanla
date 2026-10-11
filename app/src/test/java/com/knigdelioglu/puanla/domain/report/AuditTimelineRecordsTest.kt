package com.knigdelioglu.puanla.domain.report

import com.knigdelioglu.puanla.data.local.AuditLogEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class AuditTimelineRecordsTest {
    @Test
    fun `empty audit log remains empty`() {
        assertEquals(emptyList<AuditTimelineRecord>(), auditTimelineRecords(emptyList()))
    }

    @Test
    fun `only persisted events are shown and the requested limit is respected`() {
        val events = (1..3).map { index ->
            AuditLogEntity("$index", index.toLong(), "ACTION_$index", "details")
        }

        assertEquals(
            listOf(AuditTimelineRecord("ACTION_1", "details", 1L), AuditTimelineRecord("ACTION_2", "details", 2L)),
            auditTimelineRecords(events, limit = 2)
        )
    }
}
