package com.knigdelioglu.puanla.ui.preferences

import com.knigdelioglu.arc.compose.foundation.ArcAccent

enum class MotionPreference(
    val label: String,
    val reducedMotionOverride: Boolean?,
    val storedValue: String
) {
    System("Sistem", null, "system"),
    Reduced("Azalt", true, "reduced"),
    Full("Açık", false, "full");

    companion object {
        fun fromStoredValue(value: String?, legacyBoolean: Boolean?): MotionPreference =
            entries.firstOrNull { it.storedValue == value } ?: when (legacyBoolean) {
                true -> Reduced
                false -> Full
                null -> System
            }
    }
}

object AppearancePreferenceRules {
    fun accentFromStoredValue(value: String?): ArcAccent =
        ArcAccent.entries.firstOrNull { it.name == value } ?: ArcAccent.Neutral
}
