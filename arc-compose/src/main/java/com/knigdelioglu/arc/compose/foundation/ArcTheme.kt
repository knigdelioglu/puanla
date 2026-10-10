package com.knigdelioglu.arc.compose.foundation

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

val LocalArcColors = staticCompositionLocalOf { ArcLightColors }

object ArcTheme {
    val colors: ArcColors
        @Composable
        @ReadOnlyComposable
        get() = LocalArcColors.current

    val motion: ArcMotionTokens
        @Composable
        @ReadOnlyComposable
        get() = LocalArcMotion.current

    val shapes: ArcShapes
        @Composable
        @ReadOnlyComposable
        get() = LocalArcShapes.current

    val spacing: ArcSpacing
        @Composable
        @ReadOnlyComposable
        get() = LocalArcSpacing.current
}

@Composable
fun ArcTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    reducedMotion: Boolean = false,
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) ArcDarkColors else ArcLightColors
    val motion = ArcMotionTokens(reducedMotion = reducedMotion)
    val shapes = ArcShapes()
    val spacing = ArcSpacing()

    val m3Colors = if (darkTheme) {
        darkColorScheme(
            primary = colors.accent,
            onPrimary = colors.accentForeground,
            primaryContainer = colors.surfaceRaised,
            onPrimaryContainer = colors.foreground,
            secondary = colors.accentSubtle,
            onSecondary = colors.foreground,
            background = colors.background,
            onBackground = colors.foreground,
            surface = colors.surface,
            onSurface = colors.foreground,
            surfaceVariant = colors.surfaceMuted,
            onSurfaceVariant = colors.textSecondary,
            outline = colors.border,
            outlineVariant = colors.borderSubtle,
            error = colors.danger,
            onError = colors.background
        )
    } else {
        lightColorScheme(
            primary = colors.accent,
            onPrimary = colors.accentForeground,
            primaryContainer = colors.surfaceRaised,
            onPrimaryContainer = colors.foreground,
            secondary = colors.accentSubtle,
            onSecondary = colors.foreground,
            background = colors.background,
            onBackground = colors.foreground,
            surface = colors.surface,
            onSurface = colors.foreground,
            surfaceVariant = colors.surfaceMuted,
            onSurfaceVariant = colors.textSecondary,
            outline = colors.border,
            outlineVariant = colors.borderSubtle,
            error = colors.danger,
            onError = colors.background
        )
    }

    CompositionLocalProvider(
        LocalArcColors provides colors,
        LocalArcMotion provides motion,
        LocalArcShapes provides shapes,
        LocalArcSpacing provides spacing
    ) {
        MaterialTheme(
            colorScheme = m3Colors,
            shapes = Shapes(
                extraSmall = shapes.small,
                small = shapes.small,
                medium = shapes.control,
                large = shapes.panel,
                extraLarge = shapes.surface
            ),
            content = content
        )
    }
}
