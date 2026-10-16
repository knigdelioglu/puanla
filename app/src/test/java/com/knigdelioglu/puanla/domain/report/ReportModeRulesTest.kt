package com.knigdelioglu.puanla.domain.report

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportModeRulesTest {
    @Test fun groupExportWorksWithoutAnyIndividualRun() {
        assertTrue(canExportSelectedReport(
            groupMode = true, classroomId = "c", rubricId = "r",
            individualRunId = null, groupSessionId = "group-session"
        ))
        assertFalse(canExportSelectedReport(
            groupMode = false, classroomId = "c", rubricId = "r",
            individualRunId = null, groupSessionId = "group-session"
        ))
    }

    @Test fun individualExportCannotAccidentallyReuseGroupSession() {
        assertTrue(canExportSelectedReport(false, "c", "r", "run", null))
        assertFalse(canExportSelectedReport(true, "c", "r", "run", null))
    }

    @Test fun bothExportModesNeedClassroomAndRubric() {
        assertFalse(canExportSelectedReport(true, null, "r", "run", "group"))
        assertFalse(canExportSelectedReport(false, "c", null, "run", "group"))
        assertFalse(canExportSelectedReport(true, "c", "r", "run", null))
    }
}
