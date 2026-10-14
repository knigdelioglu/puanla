package com.knigdelioglu.puanla.domain.backup

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class MutationCoordinatorTest {
    @Test fun backupSnapshotWaitsForDebouncedObservationWrite() = runTest {
        val coordinator = MutationCoordinator()
        var persistedNote = ""
        coordinator.launchWrite(backgroundScope) {
            kotlinx.coroutines.delay(500)
            persistedNote = "Sahne yönergesini kullandı"
        }
        runCurrent()
        val snapshot = async { coordinator.snapshot { persistedNote } }
        runCurrent()
        assertFalse(snapshot.isCompleted)
        advanceTimeBy(500)
        runCurrent()
        assertEquals("Sahne yönergesini kullandı", snapshot.await())
    }

    @Test fun snapshotWaitsForQueuedWriteAndRejectsNewWrites() = runTest {
        val coordinator = MutationCoordinator()
        val allowedToFinish = CompletableDeferred<Unit>()
        val job = coordinator.launchWrite(backgroundScope) { allowedToFinish.await() }
        assertNotNull(job)
        runCurrent()
        val result = async { coordinator.snapshot { "complete" } }
        runCurrent()
        assertTrue(coordinator.isPaused())
        assertNull(coordinator.launchWrite(backgroundScope) { error("Must not execute") })
        assertFalse(result.isCompleted)
        allowedToFinish.complete(Unit)
        runCurrent()
        assertEquals("complete", result.await())
        assertFalse(coordinator.isPaused())
        assertNotNull(coordinator.launchWrite(backgroundScope) {})
    }

    @Test fun restoreWaitsForPriorMutationsBeforeReplacingDatabase() = runTest {
        val coordinator = MutationCoordinator()
        val allowedToFinish = CompletableDeferred<Unit>()
        var writeFinished = false
        coordinator.launchWrite(backgroundScope) {
            allowedToFinish.await()
            writeFinished = true
        }
        runCurrent()
        val restored = async {
            coordinator.restore {
                assertTrue(writeFinished)
                "restored"
            }
        }
        runCurrent()
        assertTrue(coordinator.isPaused())
        assertFalse(restored.isCompleted)
        allowedToFinish.complete(Unit)
        runCurrent()
        assertEquals("restored", restored.await())
        assertFalse(coordinator.isPaused())
    }

    @Test fun restoreFailureStillUnfreezesWrites() = runTest {
        val coordinator = MutationCoordinator()
        assertThrows(IllegalStateException::class.java) {
            kotlinx.coroutines.runBlocking {
                coordinator.restore<String> { error("bad backup") }
            }
        }
        assertFalse(coordinator.isPaused())
    }
}
