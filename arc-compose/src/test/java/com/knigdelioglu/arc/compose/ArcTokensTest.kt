package com.knigdelioglu.arc.compose

import com.knigdelioglu.arc.compose.foundation.ArcDarkColors
import com.knigdelioglu.arc.compose.foundation.ArcLightColors
import com.knigdelioglu.arc.compose.foundation.ArcMotionTokens
import com.knigdelioglu.arc.compose.foundation.ArcShapes
import com.knigdelioglu.arc.compose.foundation.ArcSpacing
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArcTokensTest {

    @Test
    fun lightColorsMatchExpectedPalette() {
        assertFalse(ArcLightColors.isDark)
        assertEquals(androidx.compose.ui.graphics.Color(0xFFFFFFFF), ArcLightColors.background)
        assertNotEquals(ArcLightColors.background, ArcLightColors.foreground)
    }

    @Test
    fun darkColorsAreDistinct() {
        assertTrue(ArcDarkColors.isDark)
        assertNotEquals(ArcLightColors.surface, ArcDarkColors.surface)
        assertNotEquals(ArcLightColors.foreground, ArcDarkColors.foreground)
    }

    @Test
    fun motionTokensSupportReducedMotion() {
        val normalMotion = ArcMotionTokens(reducedMotion = false)
        val reducedMotion = ArcMotionTokens(reducedMotion = true)

        assertFalse(normalMotion.reducedMotion)
        assertTrue(reducedMotion.reducedMotion)
    }

    @Test
    fun shapesConformToArcRadii() {
        val shapes = ArcShapes()
        // Control: 18dp, Panel: 26dp, Surface: 34dp
        assertTrue(shapes.control.topStart.toString().contains("18.0"))
        assertTrue(shapes.panel.topStart.toString().contains("26.0"))
        assertTrue(shapes.surface.topStart.toString().contains("34.0"))
    }

    @Test
    fun spacingMeetsMinimumTouchTarget() {
        val spacing = ArcSpacing()
        assertTrue(spacing.minTouchTarget.value >= 48f)
    }
}
