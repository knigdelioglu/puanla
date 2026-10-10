package com.knigdelioglu.puanla.domain.report

import com.knigdelioglu.puanla.data.local.AssessmentEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Reporting uses a single lastModifiedAt per assessment, not historical revisions.
 * Inclusive civil-day filter in the device's zone; no database writes or time travel.
 */
object AssessmentDateFilter {
    fun isValidRange(startIso: String, endIso: String): Boolean = runCatching {
        val from = startIso.takeIf { it.isNotBlank() }?.let(LocalDate::parse)
        val through = endIso.takeIf { it.isNotBlank() }?.let(LocalDate::parse)
        from == null || through == null || !from.isAfter(through)
    }.getOrDefault(false)

    fun filter(
        assessments: List<AssessmentEntity>,
        startIso: String,
        endIso: String,
        zone: ZoneId = ZoneId.systemDefault()
    ): List<AssessmentEntity> {
        require(isValidRange(startIso, endIso)) { "Bitiş tarihi başlangıçtan önce olamaz." }
        val start = startIso.takeIf { it.isNotBlank() }?.let(LocalDate::parse)
        val end = endIso.takeIf { it.isNotBlank() }?.let(LocalDate::parse)
        return assessments.filter { assessment ->
            val day = Instant.ofEpochMilli(assessment.lastModifiedAt).atZone(zone).toLocalDate()
            (start == null || !day.isBefore(start)) && (end == null || !day.isAfter(end))
        }
    }
}
