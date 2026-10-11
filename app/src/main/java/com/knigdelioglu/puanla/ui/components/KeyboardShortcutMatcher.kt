package com.knigdelioglu.puanla.ui.components

import android.view.KeyEvent
import java.util.Locale

/** Matches the display format produced by ArcShortcutRecorder against a hardware key event. */
object KeyboardShortcutMatcher {
    private val modifiers = setOf("CTRL", "ALT", "SHIFT", "META")

    fun matches(
        shortcut: String,
        keyLabel: String,
        ctrlPressed: Boolean,
        altPressed: Boolean,
        shiftPressed: Boolean,
        metaPressed: Boolean
    ): Boolean {
        val parsed = parse(shortcut) ?: return false
        val activeModifiers = buildSet {
            if (ctrlPressed) add("CTRL")
            if (altPressed) add("ALT")
            if (shiftPressed) add("SHIFT")
            if (metaPressed) add("META")
        }
        return parsed.first == activeModifiers && parsed.second == normalize(keyLabel)
    }

    fun matchesCommandPaletteShortcut(
        shortcut: String,
        isCustom: Boolean,
        keyLabel: String,
        ctrlPressed: Boolean,
        altPressed: Boolean,
        shiftPressed: Boolean,
        metaPressed: Boolean
    ): Boolean {
        if (isCustom) {
            return matches(shortcut, keyLabel, ctrlPressed, altPressed, shiftPressed, metaPressed)
        }
        return matches("Ctrl+K", keyLabel, ctrlPressed, altPressed, shiftPressed, metaPressed) ||
            matches("Meta+K", keyLabel, ctrlPressed, altPressed, shiftPressed, metaPressed)
    }

    /** Same special-key and printable-key labels as ArcShortcutRecorder. */
    fun keyLabel(keyCode: Int, unicodeChar: Int): String {
        val special = when (keyCode) {
            KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> "Enter"
            KeyEvent.KEYCODE_SPACE -> "Space"
            KeyEvent.KEYCODE_TAB -> "Tab"
            KeyEvent.KEYCODE_DEL -> "Backspace"
            KeyEvent.KEYCODE_FORWARD_DEL -> "Delete"
            KeyEvent.KEYCODE_DPAD_UP -> "Up"
            KeyEvent.KEYCODE_DPAD_DOWN -> "Down"
            KeyEvent.KEYCODE_DPAD_LEFT -> "Left"
            KeyEvent.KEYCODE_DPAD_RIGHT -> "Right"
            in KeyEvent.KEYCODE_F1..KeyEvent.KEYCODE_F12 -> "F${keyCode - KeyEvent.KEYCODE_F1 + 1}"
            else -> null
        }
        if (special != null) return special
        if (unicodeChar != 0 && !Character.isISOControl(unicodeChar)) {
            return String(Character.toChars(unicodeChar)).uppercase(Locale.ROOT)
        }
        return KeyEvent.keyCodeToString(keyCode)
            .removePrefix("KEYCODE_")
            .replace('_', ' ')
    }

    private fun parse(shortcut: String): Pair<Set<String>, String>? {
        val pieces = shortcut.split('+')
        if (pieces.isEmpty()) return null
        val expectedModifiers = linkedSetOf<String>()
        var index = 0
        while (index < pieces.lastIndex) {
            val modifier = normalize(pieces[index])
            if (modifier !in modifiers || !expectedModifiers.add(modifier)) return null
            index++
        }
        val key = pieces.drop(index).joinToString("+").trim()
        if (key.isEmpty()) return null
        return expectedModifiers to normalize(key)
    }

    private fun normalize(value: String): String = value.trim().uppercase(Locale.ROOT)
}
