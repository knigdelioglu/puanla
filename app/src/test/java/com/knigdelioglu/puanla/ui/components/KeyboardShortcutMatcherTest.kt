package com.knigdelioglu.puanla.ui.components

import android.view.KeyEvent
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyboardShortcutMatcherTest {
    @Test
    fun matchesRecordedCtrlAndMetaShortcuts() {
        assertTrue(KeyboardShortcutMatcher.matches("Ctrl+K", "k", true, false, false, false))
        assertTrue(KeyboardShortcutMatcher.matches("Meta+K", "K", false, false, false, true))
    }

    @Test
    fun rejectsMissingOrAdditionalModifiers() {
        assertFalse(KeyboardShortcutMatcher.matches("Ctrl+K", "K", false, false, false, false))
        assertFalse(KeyboardShortcutMatcher.matches("Ctrl+K", "K", true, false, true, false))
    }

    @Test
    fun uncustomizedBindingKeepsBothExistingPlatformShortcuts() {
        assertTrue(KeyboardShortcutMatcher.matchesCommandPaletteShortcut("Ctrl+K", false, "K", true, false, false, false))
        assertTrue(KeyboardShortcutMatcher.matchesCommandPaletteShortcut("Ctrl+K", false, "K", false, false, false, true))
        assertFalse(KeyboardShortcutMatcher.matchesCommandPaletteShortcut("Ctrl+K", true, "K", false, false, false, true))
    }

    @Test
    fun matchesSpecialKeyLabelsFromRecorder() {
        assertTrue(
            KeyboardShortcutMatcher.matches(
                "Ctrl+Enter",
                KeyboardShortcutMatcher.keyLabel(KeyEvent.KEYCODE_ENTER, 0),
                true,
                false,
                false,
                false
            )
        )
    }
}
