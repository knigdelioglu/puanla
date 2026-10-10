package com.knigdelioglu.arc.compose.foundation

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

@Immutable
data class ArcShapes(
    val control: RoundedCornerShape = RoundedCornerShape(18.dp),
    val panel: RoundedCornerShape = RoundedCornerShape(26.dp),
    val surface: RoundedCornerShape = RoundedCornerShape(34.dp),
    val pill: RoundedCornerShape = RoundedCornerShape(percent = 50),
    val small: RoundedCornerShape = RoundedCornerShape(10.dp)
)

val LocalArcShapes = staticCompositionLocalOf { ArcShapes() }
