package com.knigdelioglu.puanla.domain.undo

import org.junit.Assert.*
import org.junit.Test

class UndoHistoryTest {
    @Test fun staleToastCannotUndoNewerScore() {
        val history = UndoHistory<String>()
        history.push("score-student-one", "Ayşe / anlatım")
        history.push("score-student-two", "Mert / sunum")
        assertNull(history.takeIfLatest("score-student-one"))
        assertEquals("score-student-two", history.latestId())
        assertEquals("Mert / sunum", history.takeIfLatest("score-student-two"))
        assertEquals("Ayşe / anlatım", history.takeIfLatest("score-student-one"))
    }

    @Test fun failedUndoCanBeRetriedAndRestoreClearsHistory() {
        val history = UndoHistory<Int>()
        history.push("a", 12)
        val previous = history.takeIfLatest("a")
        assertEquals(12, previous)
        history.restore("a", requireNotNull(previous))
        assertEquals("a", history.latestId())
        history.clear()
        assertNull(history.latestId())
        assertNull(history.takeIfLatest("a"))
    }

    @Test fun duplicateIdentifiersAreRejected() {
        val history = UndoHistory<String>()
        history.push("x", "one")
        assertThrows(IllegalArgumentException::class.java) { history.push("x", "two") }
    }
}
