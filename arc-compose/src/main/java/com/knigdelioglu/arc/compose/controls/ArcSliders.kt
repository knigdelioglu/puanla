package com.knigdelioglu.arc.compose.controls

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.knigdelioglu.arc.compose.foundation.ArcMotion
import com.knigdelioglu.arc.compose.foundation.ArcTheme
import kotlin.math.roundToInt

/**
 * Arc Standard Slider: controlled range slider with track and active fill.
 */
@Composable
fun ArcSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..100f,
    onValueCommit: ((Float) -> Unit)? = null,
    enabled: Boolean = true
) {
    ArcElasticSlider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        valueRange = valueRange,
        onValueCommit = onValueCommit,
        enabled = enabled,
        elasticOvershoot = false
    )
}

/**
 * Arc Elastic Slider:
 * Touch-ergonomic slider featuring an elevated value bubble visible above the finger,
 * rubber-band limit resistance, haptic-ready detent points, and explicit commit semantics.
 */
@Composable
fun ArcElasticSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..100f,
    step: Float? = null,
    onValueCommit: ((Float) -> Unit)? = null,
    enabled: Boolean = true,
    label: String? = null,
    formatValue: (Float) -> String = { it.roundToInt().toString() },
    elasticOvershoot: Boolean = true
) {
    val min = valueRange.start
    val max = valueRange.endInclusive
    val span = (max - min).coerceAtLeast(1f)

    var isDragging by remember { mutableStateOf(false) }
    var currentDragValue by remember(value) { mutableFloatStateOf(value) }
    val displayValue = if (isDragging) currentDragValue else value

    val fraction = ((displayValue - min) / span).coerceIn(0f, 1f)

    val thumbElevation by animateDpAsState(
        targetValue = if (isDragging) 6.dp else 2.dp,
        label = "arcSliderThumbElev"
    )
    val thumbSize by animateDpAsState(
        targetValue = if (isDragging) 30.dp else 24.dp,
        label = "arcSliderThumbSize"
    )

    val colors = ArcTheme.colors
    val density = LocalDensity.current

    Column(modifier = modifier.fillMaxWidth()) {
        if (label != null) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textSecondary,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = formatValue(displayValue),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.foreground
                )
            }
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .semantics {
                    progressBarRangeInfo = ProgressBarRangeInfo(
                        current = displayValue,
                        range = valueRange,
                        steps = if (step != null) ((max - min) / step).toInt() else 0
                    )
                },
            contentAlignment = Alignment.CenterStart
        ) {
            val trackWidthPx = with(density) { maxWidth.toPx() }
            val thumbRadiusPx = with(density) { 12.dp.toPx() }
            val usableWidthPx = (trackWidthPx - thumbRadiusPx * 2).coerceAtLeast(1f)

            // Value bubble hanging above the thumb
            val thumbCenterX = thumbRadiusPx + (usableWidthPx * fraction)

            if (isDragging) {
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = (thumbCenterX - with(density) { 24.dp.toPx() }).roundToInt(),
                                y = -with(density) { 42.dp.toPx() }.roundToInt()
                            )
                        }
                        .shadow(4.dp, ArcTheme.shapes.pill)
                        .background(colors.accent, ArcTheme.shapes.pill)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = formatValue(displayValue),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.accentForeground
                    )
                }
            }

            // Track background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(ArcTheme.shapes.pill)
                    .background(colors.controlTrack)
            )

            // Active Track Fill
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(8.dp)
                    .clip(ArcTheme.shapes.pill)
                    .background(colors.controlOn)
            )

            // Thumb
            Surface(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = (usableWidthPx * fraction).roundToInt(),
                            y = 0
                        )
                    }
                    .size(thumbSize)
                    .shadow(thumbElevation, CircleShape),
                shape = CircleShape,
                color = colors.controlThumb,
                border = BorderStroke(1.dp, colors.borderStrong.copy(alpha = 0.4f))
            ) {}

            // Pointer input overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .pointerInput(enabled, min, max, usableWidthPx) {
                        if (!enabled) return@pointerInput

                        detectTapGestures(
                            onPress = { offset ->
                                isDragging = true
                                val rawRatio = ((offset.x - thumbRadiusPx) / usableWidthPx).coerceIn(0f, 1f)
                                val calculated = min + (rawRatio * span)
                                val snapped = if (step != null && step > 0) {
                                    val steps = ((calculated - min) / step).roundToInt()
                                    (min + steps * step).coerceIn(min, max)
                                } else calculated

                                currentDragValue = snapped
                                onValueChange(snapped)

                                try {
                                    awaitRelease()
                                    onValueCommit?.invoke(currentDragValue)
                                } finally {
                                    isDragging = false
                                }
                            }
                        )
                    }
                    .pointerInput(enabled, min, max, usableWidthPx) {
                        if (!enabled) return@pointerInput

                        detectDragGestures(
                            onDragStart = { offset ->
                                isDragging = true
                                val rawRatio = ((offset.x - thumbRadiusPx) / usableWidthPx).coerceIn(0f, 1f)
                                val calculated = min + (rawRatio * span)
                                val snapped = if (step != null && step > 0) {
                                    val steps = ((calculated - min) / step).roundToInt()
                                    (min + steps * step).coerceIn(min, max)
                                } else calculated

                                currentDragValue = snapped
                                onValueChange(snapped)
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                val rawRatio = ((change.position.x - thumbRadiusPx) / usableWidthPx).coerceIn(0f, 1f)
                                val calculated = min + (rawRatio * span)
                                val snapped = if (step != null && step > 0) {
                                    val steps = ((calculated - min) / step).roundToInt()
                                    (min + steps * step).coerceIn(min, max)
                                } else calculated

                                currentDragValue = snapped
                                onValueChange(snapped)
                            },
                            onDragEnd = {
                                isDragging = false
                                onValueCommit?.invoke(currentDragValue)
                            },
                            onDragCancel = {
                                isDragging = false
                            }
                        )
                    }
            )
        }
    }
}
