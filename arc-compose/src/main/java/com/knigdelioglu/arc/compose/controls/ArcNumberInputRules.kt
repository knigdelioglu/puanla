package com.knigdelioglu.arc.compose.controls

/** Numeric scoring accepts an integer only on explicit commit and within bounds. */
object ArcNumberInputRules {
    fun parse(text: String, minimum: Int, maximum: Int): Int? {
        if (minimum > maximum || text.isEmpty() || !text.all(Char::isDigit)) return null
        return text.toIntOrNull()?.takeIf { it in minimum..maximum }
    }
}
