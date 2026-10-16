package com.knigdelioglu.puanla.domain.scoring

/**
 * Stable session restoration shared by individual and group scoring.
 * The caller must provide sessions scoped to the current classroom/rubric.
 * No session from another context is ever synthesized.
 */
fun <T> resolveSessionSelection(
    items: List<T>,
    currentId: String?,
    preferredId: String?,
    idOf: (T) -> String,
    isActive: (T) -> Boolean = { true }
): T? {
    return items.firstOrNull { idOf(it) == currentId }
        ?: items.firstOrNull { idOf(it) == preferredId }
        ?: items.firstOrNull(isActive)
        ?: items.firstOrNull()
}
