package com.knigdelioglu.puanla.domain.backup

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class MutationCoordinatorTest {
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

    @Test fun restoreCancelsAndJoinsPriorMutations() = runTest {
        val coordinator = MutationCoordinator()
        val finallyCalled = CompletableDeferred<Unit>()
        coordinator.launchWrite(backgroundScope) {
            try {
                awaitCancellation()
            } finally {
                finallyCalled.complete(Unit)
            }
        }
        runCurrent()
        val restored = async {
            coordinator.restore {
                assertTrue(finallyCalled.isCompleted)
                "restored"
            }
        }
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
