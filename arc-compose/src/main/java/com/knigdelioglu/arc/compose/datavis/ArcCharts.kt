package com.knigdelioglu.arc.compose.datavis

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.knigdelioglu.arc.compose.display.ArcCard
import com.knigdelioglu.arc.compose.foundation.ArcTheme

/**
 * Arc Bar Chart: distribution of scores across grade intervals.
 */
data class ArcBarData(
    val label: String,
    val value: Float,
    val color: Color? = null
)

@Composable
fun ArcBarChart(
    data: List<ArcBarData>,
    modifier: Modifier = Modifier,
    barHeight: Dp = 140.dp
) {
    val maxValue = (data.maxOfOrNull { it.value } ?: 1f).coerceAtLeast(1f)
    val colors = ArcTheme.colors

    ArcCard(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Puan Dağılımı", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = colors.foreground)
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(barHeight),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.Bottom
            ) {
                data.forEach { item ->
                    val fraction = (item.value / maxValue).coerceIn(0f, 1f)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Text(item.value.toInt().toString(), fontSize = 11.sp, color = colors.textMuted)
                        Spacer(Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .width(28.dp)
                                .fillMaxHeight(fraction)
                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                .background(item.color ?: colors.accent)
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(item.label, fontSize = 11.sp, color = colors.textSecondary)
                    }
                }
            }
        }
    }
}

/**
 * Arc Donut Chart: completion distribution (Tamamlanan / Kısmi / Başlanmadı).
 */
data class ArcDonutSegment(
    val label: String,
    val value: Float,
    val color: Color
)

@Composable
fun ArcDonutChart(
    segments: List<ArcDonutSegment>,
    modifier: Modifier = Modifier,
    centerTitle: String = "Toplam",
    centerSubtitle: String = ""
) {
    val total = (segments.sumOf { it.value.toDouble() }).toFloat().coerceAtLeast(1f)
    val colors = ArcTheme.colors

    ArcCard(modifier = modifier) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(130.dp), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    var startAngle = -90f
                    val strokeWidth = 22.dp.toPx()
                    val diameter = size.minDimension - strokeWidth
                    val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
                    val arcSize = Size(diameter, diameter)

                    segments.forEach { seg ->
                        val sweep = (seg.value / total) * 360f
                        drawArc(
                            color = seg.color,
                            startAngle = startAngle,
                            sweepAngle = sweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                        )
                        startAngle += sweep
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(centerTitle, fontSize = 11.sp, color = colors.textMuted)
                    Text(centerSubtitle, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colors.foreground)
                }
            }
            Spacer(Modifier.width(20.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                segments.forEach { seg ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(modifier = Modifier.size(10.dp), shape = CircleShape, color = seg.color) {}
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "${seg.label}: ${seg.value.toInt()}",
                            fontSize = 13.sp,
                            color = colors.foreground
                        )
                    }
                }
            }
        }
    }
}

/**
 * Arc Gauge: arc progress meter showing completion percentage.
 */
@Composable
fun ArcGauge(
    percentage: Float,
    label: String = "Tamamlanma Oranı",
    modifier: Modifier = Modifier
) {
    val colors = ArcTheme.colors
    ArcCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = colors.textSecondary)
            Spacer(Modifier.height(10.dp))
            Box(modifier = Modifier.size(120.dp, 70.dp), contentAlignment = Alignment.BottomCenter) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 14.dp.toPx()
                    val diameter = size.width - strokeWidth
                    val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
                    val arcSize = Size(diameter, diameter)

                    // Track
                    drawArc(
                        color = colors.surfaceMuted,
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                    // Progress
                    drawArc(
                        color = colors.accent,
                        startAngle = 180f,
                        sweepAngle = 180f * percentage.coerceIn(0f, 1f),
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
                Text(
                    text = "${(percentage * 100).toInt()}%",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.foreground
                )
            }
        }
    }
}

/**
 * Arc Sparkline: lightweight historical score curve.
 */
@Composable
fun ArcSparkline(
    points: List<Float>,
    modifier: Modifier = Modifier,
    lineColor: Color = ArcTheme.colors.accent
) {
    if (points.size < 2) return

    val min = points.minOrNull() ?: 0f
    val max = (points.maxOrNull() ?: 100f).coerceAtLeast(min + 1f)

    Canvas(modifier = modifier.height(36.dp).width(100.dp)) {
        val width = size.width
        val height = size.height
        val stepX = width / (points.size - 1)

        val path = Path()
        points.forEachIndexed { i, p ->
            val x = i * stepX
            val y = height - (((p - min) / (max - min)) * height)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path = path, color = lineColor, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
    }
}

/**
 * Arc Usage Meter: capacity / progress meter with threshold indicators.
 */
@Composable
fun ArcUsageMeter(
    used: Int,
    total: Int,
    label: String,
    modifier: Modifier = Modifier
) {
    val fraction = if (total > 0) (used.toFloat() / total).coerceIn(0f, 1f) else 0f
    Column(modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontSize = 13.sp, color = ArcTheme.colors.textSecondary)
            Text("$used / $total", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = ArcTheme.colors.foreground)
        }
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(ArcTheme.shapes.pill)
                .background(ArcTheme.colors.surfaceMuted)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(8.dp)
                    .clip(ArcTheme.shapes.pill)
                    .background(if (fraction >= 1f) ArcTheme.colors.success else ArcTheme.colors.accent)
            )
        }
    }
}

/**
 * Arc Line Chart: score progress across assessment sessions.
 */
@Composable
fun ArcLineChart(
    sessionScores: List<Pair<String, Float>>,
    modifier: Modifier = Modifier
) {
    val colors = ArcTheme.colors
    ArcCard(modifier = modifier) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text("Oturum Not Eğilimi", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = colors.foreground)
            Spacer(Modifier.height(14.dp))
            if (sessionScores.size < 2) {
                Text("Eğilim için en az 2 oturum gereklidir.", fontSize = 13.sp, color = colors.textMuted)
            } else {
                Canvas(modifier = Modifier.fillMaxWidth().height(120.dp)) {
                    val max = (sessionScores.maxOf { it.second }).coerceAtLeast(100f)
                    val stepX = size.width / (sessionScores.size - 1)
                    val path = Path()

                    sessionScores.forEachIndexed { i, (_, score) ->
                        val x = i * stepX
                        val y = size.height - ((score / max) * size.height)
                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        drawCircle(color = colors.accent, radius = 4.dp.toPx(), center = Offset(x, y))
                    }
                    drawPath(path = path, color = colors.accent, style = Stroke(width = 2.5.dp.toPx()))
                }
            }
        }
    }
}

/**
 * Arc Waffle Chart: 10x10 block grid displaying student completion percentage.
 */
@Composable
fun ArcWaffleChart(
    percentage: Int, // 0..100
    modifier: Modifier = Modifier
) {
    val colors = ArcTheme.colors
    ArcCard(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Sınıf İlerleme Matrisi", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.foreground)
                Text("%$percentage", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.accent)
            }
            Spacer(Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                for (row in 0 until 10) {
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        for (col in 0 until 10) {
                            val index = row * 10 + col
                            val filled = index < percentage
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(if (filled) colors.accent else colors.surfaceMuted)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Arc Slope Chart: compare pre vs post performance slope.
 */
@Composable
fun ArcSlopeChart(
    items: List<Pair<String, Pair<Float, Float>>>, // Name to (InitialScore, FinalScore)
    modifier: Modifier = Modifier
) {
    val colors = ArcTheme.colors
    ArcCard(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Gelişim Eğrileri", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = colors.foreground)
            Spacer(Modifier.height(12.dp))
            Canvas(modifier = Modifier.fillMaxWidth().height(140.dp)) {
                val leftX = 30.dp.toPx()
                val rightX = size.width - 30.dp.toPx()

                items.forEach { (_, scores) ->
                    val y1 = size.height - (scores.first / 100f * size.height)
                    val y2 = size.height - (scores.second / 100f * size.height)
                    drawLine(
                        color = if (y2 <= y1) colors.success else colors.danger,
                        start = Offset(leftX, y1),
                        end = Offset(rightX, y2),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawCircle(color = colors.foreground, radius = 3.dp.toPx(), center = Offset(leftX, y1))
                    drawCircle(color = colors.foreground, radius = 3.dp.toPx(), center = Offset(rightX, y2))
                }
            }
        }
    }
}

/**
 * Arc Activity Heatmap: weekly activity density calendar for scoring history.
 */
@Composable
fun ArcActivityHeatmap(
    daysIntensity: List<Int>, // 0..4
    modifier: Modifier = Modifier
) {
    val colors = ArcTheme.colors
    ArcCard(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Değerlendirme Yoğunluğu", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.foreground)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                daysIntensity.take(28).chunked(7).forEach { week ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        week.forEach { level ->
                            val color = when (level) {
                                1 -> colors.accent.copy(alpha = 0.25f)
                                2 -> colors.accent.copy(alpha = 0.50f)
                                3 -> colors.accent.copy(alpha = 0.75f)
                                4 -> colors.accent
                                else -> colors.surfaceMuted
                            }
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(color)
                            )
                        }
                    }
                }
            }
        }
    }
}
