package com.knigdelioglu.arc.compose.foundation

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Arc Library color tokens adapted for Jetpack Compose.
 * Preserves the light and dark neutral ramps and brand gradient definitions.
 */
@Immutable
data class ArcColors(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceRaised: Color,
    val surfaceMuted: Color,
    val foreground: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val border: Color,
    val borderSubtle: Color,
    val borderStrong: Color,
    val accent: Color,
    val accentStrong: Color,
    val accentSubtle: Color,
    val accentForeground: Color,
    val controlOn: Color,
    val controlGlyph: Color,
    val controlTrack: Color,
    val controlThumb: Color,
    val success: Color,
    val warning: Color,
    val danger: Color,
    val info: Color,
    val gradientFrom: Color,
    val gradientTo: Color,
    val gradientForeground: Color
) {
    val brandGradient: Brush
        get() = Brush.linearGradient(listOf(gradientFrom, gradientTo))

    val brandGradientSoft: Brush
        get() = Brush.linearGradient(
            listOf(
                gradientFrom.copy(alpha = if (isDark) 0.12f else 0.06f),
                gradientTo.copy(alpha = if (isDark) 0.12f else 0.06f)
            )
        )

    val brandGradientInk: Brush
        get() = Brush.linearGradient(
            listOf(
                if (isDark) Color(0xFFA78BFA) else Color(0xFF6D28D9),
                if (isDark) Color(0xFFFB923C) else Color(0xFFC2410C)
            )
        )
}

val ArcLightColors = ArcColors(
    isDark = false,
    background = Color(0xFFFFFFFF),
    surface = Color(0xFFFFFFFF),
    surfaceRaised = Color(0xFFFAFAFA),
    surfaceMuted = Color(0xFFF5F5F7),
    foreground = Color(0xFF141414),
    textSecondary = Color(0xFF555555),
    textMuted = Color(0xFF7D7D82),
    border = Color(0xFFE5E5EA),
    borderSubtle = Color(0xFFF0F0F3),
    borderStrong = Color(0xFFC7C7CC),
    accent = Color(0xFF2C2C2E),
    accentStrong = Color(0xFF1C1C1E),
    accentSubtle = Color(0x142C2C2E),
    accentForeground = Color(0xFFFFFFFF),
    controlOn = Color(0xFF1C1C1E),
    controlGlyph = Color(0xFFFFFFFF),
    controlTrack = Color(0xFFE5E5EA),
    controlThumb = Color(0xFFFFFFFF),
    success = Color(0xFF10B981),
    warning = Color(0xFFF59E0B),
    danger = Color(0xFFEF4444),
    info = Color(0xFF3B82F6),
    gradientFrom = Color(0xFFE9D5FF),
    gradientTo = Color(0xFFFFEDD5),
    gradientForeground = Color(0xFF2E1065)
)

val ArcDarkColors = ArcColors(
    isDark = true,
    background = Color(0xFF121214),
    surface = Color(0xFF1A1A1E),
    surfaceRaised = Color(0xFF24242A),
    surfaceMuted = Color(0xFF2C2C34),
    foreground = Color(0xFFF3F3F6),
    textSecondary = Color(0xFFA1A1AA),
    textMuted = Color(0xFF71717A),
    border = Color(0xFF2E2E36),
    borderSubtle = Color(0xFF23232A),
    borderStrong = Color(0xFF444450),
    accent = Color(0xFFE4E4E7),
    accentStrong = Color(0xFFFFFFFF),
    accentSubtle = Color(0x22E4E4E7),
    accentForeground = Color(0xFF18181B),
    controlOn = Color(0xFFF4F4F5),
    controlGlyph = Color(0xFF18181B),
    controlTrack = Color(0xFF3F3F46),
    controlThumb = Color(0xFFFFFFFF),
    success = Color(0xFF34D399),
    warning = Color(0xFFFBBF24),
    danger = Color(0xFFF87171),
    info = Color(0xFF60A5FA),
    gradientFrom = Color(0xFF581C87),
    gradientTo = Color(0xFF7C2D12),
    gradientForeground = Color(0xFFF3E8FF)
)
