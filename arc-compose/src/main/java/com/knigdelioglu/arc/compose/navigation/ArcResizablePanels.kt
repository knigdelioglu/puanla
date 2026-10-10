package com.knigdelioglu.arc.compose.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.knigdelioglu.arc.compose.foundation.ArcMotion
import com.knigdelioglu.arc.compose.foundation.ArcTheme

/**
 * Arc Resizable Panels:
 * Essential for 11-inch tablet workspace. Divides the screen into resizable left/middle/right
 * panes (e.g. Student List <-> Rubric Criteria <-> Scoring Controls) with draggable dividers,
 * minimum constraints, and optional collapse toggles.
 */
@Composable
fun ArcResizablePanels(
    modifier: Modifier = Modifier,
    initialWeights: List<Float> = listOf(0.30f, 0.40f, 0.30f),
    minWeights: List<Float> = listOf(0.18f, 0.25f, 0.20f),
    firstPane: @Composable () -> Unit,
    secondPane: @Composable () -> Unit,
    thirdPane: (@Composable () -> Unit)? = null
) {
    val paneCount = if (thirdPane != null) 3 else 2
    var weights by remember {
        mutableStateOf(
            if (paneCount == 2) {
                listOf(initialWeights.getOrElse(0) { 0.4f }, initialWeights.getOrElse(1) { 0.6f })
            } else {
                initialWeights
            }
        )
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val totalWidthPx = with(LocalDensity.current) { maxWidth.toPx() }
        val dividerWidthDp = 12.dp
        val dividerWidthPx = with(LocalDensity.current) { dividerWidthDp.toPx() }

        Row(modifier = Modifier.fillMaxSize()) {
            // First Pane
            Box(
                modifier = Modifier
                    .weight(weights[0].coerceAtLeast(0.05f))
                    .fillMaxHeight()
            ) {
                firstPane()
            }

            // Divider 1
            ArcPanelDivider(
                onDelta = { deltaPx ->
                    val deltaRatio = deltaPx / totalWidthPx
                    val newW0 = (weights[0] + deltaRatio).coerceIn(minWeights[0], 0.6f)
                    val diff = newW0 - weights[0]
                    val newW1 = (weights[1] - diff).coerceAtLeast(minWeights.getOrElse(1) { 0.2f })
                    weights = if (paneCount == 2) {
                        listOf(newW0, newW1)
                    } else {
                        listOf(newW0, newW1, weights[2])
                    }
                },
                onDoubleTapReset = {
                    weights = if (paneCount == 2) listOf(0.4f, 0.6f) else listOf(0.30f, 0.40f, 0.30f)
                }
            )

            // Second Pane
            Box(
                modifier = Modifier
                    .weight(weights[1].coerceAtLeast(0.05f))
                    .fillMaxHeight()
            ) {
                secondPane()
            }

            // Optional Third Pane & Divider 2
            if (thirdPane != null && paneCount == 3) {
                ArcPanelDivider(
                    onDelta = { deltaPx ->
                        val deltaRatio = deltaPx / totalWidthPx
                        val newW1 = (weights[1] + deltaRatio).coerceIn(minWeights[1], 0.6f)
                        val diff = newW1 - weights[1]
                        val newW2 = (weights[2] - diff).coerceAtLeast(minWeights.getOrElse(2) { 0.18f })
                        weights = listOf(weights[0], newW1, newW2)
                    },
                    onDoubleTapReset = {
                        weights = listOf(0.30f, 0.40f, 0.30f)
                    }
                )

                Box(
                    modifier = Modifier
                        .weight(weights[2].coerceAtLeast(0.05f))
                        .fillMaxHeight()
                ) {
                    thirdPane()
                }
            }
        }
    }
}

@Composable
private fun ArcPanelDivider(
    onDelta: (Float) -> Unit,
    onDoubleTapReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableStateOf(false) }
    val colors = ArcTheme.colors

    Box(
        modifier = modifier
            .width(14.dp)
            .fillMaxHeight()
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { onDoubleTapReset() }
                )
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { isDragging = true },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onDelta(dragAmount.x)
                    },
                    onDragEnd = { isDragging = false },
                    onDragCancel = { isDragging = false }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Vertical hair line
        Box(
            modifier = Modifier
                .width(1.5.dp)
                .fillMaxHeight()
                .background(if (isDragging) colors.accent else colors.border)
        )

        // Center pill handle
        Surface(
            modifier = Modifier
                .size(width = 6.dp, height = 36.dp)
                .shadow(if (isDragging) 4.dp else 1.dp, CircleShape),
            shape = CircleShape,
            color = if (isDragging) colors.accent else colors.surfaceRaised,
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderStrong)
        ) {}
    }
}
