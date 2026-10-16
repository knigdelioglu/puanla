package com.knigdelioglu.puanla.domain.report

/**
 * Both export modes use the same actions but retain separate session identities.
 * An individual report never falls back to a group session (and vice versa).
 */
fun canExportSelectedReport(
    groupMode: Boolean,
    classroomId: String?,
    rubricId: String?,
    individualRunId: String?,
    groupSessionId: String?
): Boolean {
    if (classroomId == null || rubricId == null) return false
    return if (groupMode) groupSessionId != null else individualRunId != null
}
