package com.knigdelioglu.puanla.domain.undo

/**
 * A toast's undo callback must identify its own recorded edit, not whichever
 * edit happens to be last when the callback executes.
 */
class UndoHistory<T> {
    private data class Entry<T>(val id: String, val action: T)
    private val entries = mutableListOf<Entry<T>>()

    fun push(id: String, action: T) {
        require(id.isNotBlank() && entries.none { it.id == id }) { "Geçersiz veya yinelenen geri alma kimliği." }
        entries.add(Entry(id, action))
    }

    fun latestId(): String? = entries.lastOrNull()?.id

    /** Stale toast actions cannot undo another edit, even on a different pupil. */
    fun takeIfLatest(id: String): T? {
        if (entries.lastOrNull()?.id != id) return null
        return entries.removeAt(entries.lastIndex).action
    }

    fun restore(id: String, action: T) {
        push(id, action)
    }

    fun clear() = entries.clear()
}
