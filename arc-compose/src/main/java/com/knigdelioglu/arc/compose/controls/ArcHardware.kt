package com.knigdelioglu.arc.compose.controls

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.knigdelioglu.arc.compose.foundation.ArcTheme

/**
 * Arc Shortcut Recorder: records or displays physical keyboard shortcuts (e.g. Cmd+K, Alt+S, Enter).
 */
@Composable
fun ArcShortcutRecorder(
    shortcut: String,
    onShortcutChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Klavye Kısayolu"
) {
    Surface(
        modifier = modifier,
        shape = ArcTheme.shapes.control,
        color = ArcTheme.colors.surfaceRaised,
        border = BorderStroke(1.dp, ArcTheme.colors.border)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontSize = 14.sp, color = ArcTheme.colors.foreground)
            Surface(
                shape = ArcTheme.shapes.small,
                color = ArcTheme.colors.surfaceMuted,
                border = BorderStroke(1.dp, ArcTheme.colors.borderStrong)
            ) {
                Text(
                    text = shortcut,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = ArcTheme.colors.foreground,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

/**
 * Arc Signature Pad: Android stylus / pen & finger signature surface for teacher verification.
 */
@Composable
fun ArcSignaturePad(
    onSignatureChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    strokeColor: Color = Color.Black,
    strokeWidth: Float = 4f
) {
    val points = remember { mutableStateListOf<Offset>() }
    val paths = remember { mutableStateListOf<List<Offset>>() }
    var currentPath by remember { mutableStateOf<List<Offset>>(emptyList()) }

    Surface(
        modifier = modifier.height(180.dp),
        shape = ArcTheme.shapes.panel,
        color = Color.White,
        border = BorderStroke(1.5.dp, ArcTheme.colors.borderStrong)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                currentPath = listOf(offset)
                                onSignatureChanged(true)
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                currentPath = currentPath + change.position
                            },
                            onDragEnd = {
                                if (currentPath.isNotEmpty()) {
                                    paths.add(currentPath)
                                    currentPath = emptyList()
                                }
                            },
                            onDragCancel = {
                                currentPath = emptyList()
                            }
                        )
                    }
            ) {
                // Draw existing paths
                paths.forEach { pathPoints ->
                    if (pathPoints.size > 1) {
                        val path = Path().apply {
                            moveTo(pathPoints.first().x, pathPoints.first().y)
                            for (i in 1 until pathPoints.size) {
                                lineTo(pathPoints[i].x, pathPoints[i].y)
                            }
                        }
                        drawPath(
                            path = path,
                            color = strokeColor,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                    }
                }

                // Draw in-progress path
                if (currentPath.size > 1) {
                    val path = Path().apply {
                        moveTo(currentPath.first().x, currentPath.first().y)
                        for (i in 1 until currentPath.size) {
                            lineTo(currentPath[i].x, currentPath[i].y)
                        }
                    }
                    drawPath(
                        path = path,
                        color = strokeColor,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
            }

            // Controls overlay
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
            ) {
                ArcButton(
                    onClick = {
                        paths.clear()
                        currentPath = emptyList()
                        onSignatureChanged(false)
                    },
                    variant = ArcButtonVariant.Ghost,
                    size = ArcButtonSize.Sm
                ) {
                    Icon(Icons.Default.Clear, contentDescription = "Temizle", modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Temizle", fontSize = 12.sp)
                }
            }

            if (paths.isEmpty() && currentPath.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Kalem veya parmak ile imzalayın",
                        fontSize = 14.sp,
                        color = Color.LightGray
                    )
                }
            }
        }
    }
}
