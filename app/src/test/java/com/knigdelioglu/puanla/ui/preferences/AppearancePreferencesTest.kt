package com.knigdelioglu.puanla.ui.preferences

import com.knigdelioglu.arc.compose.foundation.ArcAccent
import org.junit.Assert.assertEquals
import org.junit.Test

class AppearancePreferencesTest {
    @Test
    fun missingMotionSettingFollowsSystem() {
        assertEquals(MotionPreference.System, MotionPreference.fromStoredValue(null, null))
    }

    @Test
    fun legacyMotionBooleansKeepTheirPreviousMeaning() {
        assertEquals(MotionPreference.Reduced, MotionPreference.fromStoredValue(null, true))
        assertEquals(MotionPreference.Full, MotionPreference.fromStoredValue(null, false))
    }

    @Test
    fun explicitMotionSettingTakesPrecedenceOverLegacyValue() {
        assertEquals(MotionPreference.System, MotionPreference.fromStoredValue("system", true))
    }

    @Test
    fun unknownAccentFallsBackToNeutral() {
        assertEquals(ArcAccent.Neutral, AppearancePreferenceRules.accentFromStoredValue("not-an-accent"))
        assertEquals(ArcAccent.Neutral, AppearancePreferenceRules.accentFromStoredValue(null))
        assertEquals(ArcAccent.Coral, AppearancePreferenceRules.accentFromStoredValue("Coral"))
    }
}
