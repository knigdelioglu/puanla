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

    @Test
    fun matchesTurkishPrintableKeysAndRejectsModifierOnlyBindings() {
        assertTrue(KeyboardShortcutMatcher.matches("Ctrl+Ş", "ş", true, false, false, false))
        assertTrue(KeyboardShortcutMatcher.matches("Alt+I", "ı", false, true, false, false))
        assertFalse(KeyboardShortcutMatcher.matches("", "K", true, false, false, false))
        assertFalse(KeyboardShortcutMatcher.matches("Ctrl+", "K", true, false, false, false))
        assertFalse(KeyboardShortcutMatcher.matches("Ctrl", "Ctrl", false, false, false, false))
    }
}
