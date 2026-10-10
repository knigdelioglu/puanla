package com.knigdelioglu.arc.compose.foundation

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.DurationBasedAnimationSpec
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Arc Library motion tokens adapted for Compose.
 * Supports system reduced motion by scaling or snapping transitions.
 */
@Immutable
data class ArcMotionTokens(
    val reducedMotion: Boolean = false,
    val durationInstant: Int = 120,
    val durationFast: Int = 160,
    val durationExit: Int = 180,
    val durationStandard: Int = 240,
    val durationConsidered: Int = 480,
    val easeEnter: Easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f),
    val easeStandard: Easing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f),
    val easeExit: Easing = CubicBezierEasing(0.7f, 0f, 0.84f, 0f),
    val easeInOut: Easing = CubicBezierEasing(0.65f, 0f, 0.35f, 1f)
) {
    fun <T> springResponsive(): SpringSpec<T> {
        if (reducedMotion) return spring(stiffness = Spring.StiffnessHigh)
        return spring(dampingRatio = 0.75f, stiffness = 520f)
    }

    fun <T> springGentle(): SpringSpec<T> {
        if (reducedMotion) return spring(stiffness = Spring.StiffnessHigh)
        return spring(dampingRatio = 0.85f, stiffness = 340f)
    }

    fun <T> springSnappy(): SpringSpec<T> {
        if (reducedMotion) return spring(stiffness = Spring.StiffnessHigh)
        return spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMedium)
    }

    fun <T> springSmooth(): SpringSpec<T> {
        if (reducedMotion) return spring(stiffness = Spring.StiffnessHigh)
        return spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow)
    }

    fun <T> springMorph(): SpringSpec<T> {
        if (reducedMotion) return spring(stiffness = Spring.StiffnessHigh)
        return spring(dampingRatio = 0.84f, stiffness = Spring.StiffnessMediumLow)
    }

    fun <T> standardTween(delayMillis: Int = 0): DurationBasedAnimationSpec<T> {
        if (reducedMotion) return snap(delayMillis)
        return tween(durationMillis = durationStandard, delayMillis = delayMillis, easing = easeStandard)
    }

    fun <T> fastTween(delayMillis: Int = 0): DurationBasedAnimationSpec<T> {
        if (reducedMotion) return snap(delayMillis)
        return tween(durationMillis = durationFast, delayMillis = delayMillis, easing = easeStandard)
    }
}

val LocalArcMotion = staticCompositionLocalOf { ArcMotionTokens() }

object ArcMotion {
    val current: ArcMotionTokens
        @Composable
        get() = LocalArcMotion.current
}
