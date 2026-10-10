package com.knigdelioglu.arc.compose.foundation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArcMotionTest {

    @Test
    fun reducedMotionDefaultsToFalse() {
        val motion = ArcMotionTokens()
        assertFalse(motion.reducedMotion)
        assertEquals(240, motion.durationStandard)
        assertEquals(160, motion.durationFast)
    }

    @Test
    fun reducedMotionEnabledHasHighStiffnessAndSnapTransitions() {
        val motion = ArcMotionTokens(reducedMotion = true)
        assertTrue(motion.reducedMotion)

        val responsiveSpec = motion.springResponsive<Float>()
        assertEquals(androidx.compose.animation.core.Spring.StiffnessHigh, responsiveSpec.stiffness, 0.001f)

        val tweenSpec = motion.standardTween<Float>()
        // When reduced motion is active, standardTween returns a SnapSpec
        assertTrue(tweenSpec is androidx.compose.animation.core.SnapSpec)
    }
}
