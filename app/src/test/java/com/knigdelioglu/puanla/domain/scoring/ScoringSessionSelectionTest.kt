package com.knigdelioglu.puanla.domain.scoring

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ScoringSessionSelectionTest {
    private data class Session(val id: String, val active: Boolean = true)
    private val sessions = listOf(
        Session("archived", active = false),
        Session("active-a"),
        Session("active-b")
    )

    @Test fun currentSessionWinsOverPreviouslySavedChoice() {
        val selected = resolveSessionSelection(
            sessions, currentId = "active-b", preferredId = "active-a",
            idOf = { it.id }, isActive = { it.active }
        )
        assertEquals("active-b", selected?.id)
    }

    @Test fun previouslySavedSessionIsRestoredEvenIfArchived() {
        val selected = resolveSessionSelection(
            sessions, currentId = null, preferredId = "archived",
            idOf = { it.id }, isActive = { it.active }
        )
        assertEquals("archived", selected?.id)
    }

    @Test fun missingPreferenceFallsBackToFirstActiveSession() {
        val selected = resolveSessionSelection(
            sessions, currentId = "removed", preferredId = "removed",
            idOf = { it.id }, isActive = { it.active }
        )
        assertEquals("active-a", selected?.id)
    }

    @Test fun groupSessionsUseTheSameSelectionRulesWithoutArchiveStatus() {
        val selected = resolveSessionSelection(
            listOf(Session("group-1"), Session("group-2")),
            currentId = null, preferredId = "group-2", idOf = { it.id }
        )
        assertEquals("group-2", selected?.id)
    }

    @Test fun scopedEmptySessionListCannotRestoreAnotherContext() {
        val selected = resolveSessionSelection(
            emptyList<Session>(), currentId = "other-class",
            preferredId = "other-rubric", idOf = { it.id }
        )
        assertNull(selected)
    }
}
