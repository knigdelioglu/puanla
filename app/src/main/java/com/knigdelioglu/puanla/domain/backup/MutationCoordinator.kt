package com.knigdelioglu.puanla.domain.backup

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Coordinates ALL ViewModel database mutations with full backup / restore.
 * A snapshot waits for submitted writes (including debounced notes).
 * A restore cancels and joins writes before replacing the database.
 *
 * Registration and pausing are synchronized, so even a caller on IO cannot
 * race a newly submitted write on the Compose main thread.
 */
class MutationCoordinator {
    private val monitor = Any()
    private val transferMutex = Mutex()
    private val active = linkedSetOf<Job>()
    private var paused = false

    fun isPaused(): Boolean = synchronized(monitor) { paused }
    fun hasPendingWrites(): Boolean = synchronized(monitor) { active.any { it.isActive } }

    fun launchWrite(scope: CoroutineScope, action: suspend CoroutineScope.() -> Unit): Job? {
        val job = scope.launch(start = CoroutineStart.LAZY, block = action)
        job.invokeOnCompletion { synchronized(monitor) { active.remove(job) } }
        val accepted = synchronized(monitor) {
            if (paused) false else {
                active.add(job)
                true
            }
        }
        if (!accepted) {
            job.cancel()
            return null
        }
        job.start()
        return job
    }

    /** Used by suspend undo actions already running in a coroutine. */
    fun track(job: Job): Boolean {
        val accepted = synchronized(monitor) {
            if (paused) false else {
                active.add(job)
                true
            }
        }
        if (accepted) job.invokeOnCompletion { synchronized(monitor) { active.remove(job) } }
        return accepted
    }

    fun untrack(job: Job) {
        synchronized(monitor) { active.remove(job) }
    }

    suspend fun <T> snapshot(read: suspend () -> T): T =
        transferMutex.withLock {
            val pending = synchronized(monitor) {
                paused = true
                active.toList()
            }
            try {
                pending.joinAll()
                read()
            } finally {
                synchronized(monitor) { paused = false }
            }
        }

    suspend fun <T> restore(replace: suspend () -> T): T =
        transferMutex.withLock {
            val pending = synchronized(monitor) {
                paused = true
                active.toList()
            }
            try {
                pending.forEach { it.cancel() }
                pending.joinAll()
                replace()
            } finally {
                synchronized(monitor) { paused = false }
            }
        }
}
