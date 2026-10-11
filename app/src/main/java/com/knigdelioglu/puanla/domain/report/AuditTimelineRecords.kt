package com.knigdelioglu.puanla.domain.report

import com.knigdelioglu.puanla.data.local.AuditLogEntity

data class AuditTimelineRecord(
    val action: String,
    val details: String,
    val timestamp: Long
)

/** Keeps the timeline honest: an empty audit log yields no synthetic event. */
fun auditTimelineRecords(logs: List<AuditLogEntity>, limit: Int = 8): List<AuditTimelineRecord> =
    logs.take(limit.coerceAtLeast(0)).map { AuditTimelineRecord(it.action, it.details, it.timestamp) }
