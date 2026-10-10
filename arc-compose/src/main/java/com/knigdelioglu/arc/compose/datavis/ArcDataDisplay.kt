package com.knigdelioglu.arc.compose.datavis

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.knigdelioglu.arc.compose.display.ArcBadge
import com.knigdelioglu.arc.compose.display.ArcBadgeVariant
import com.knigdelioglu.arc.compose.display.ArcCard
import com.knigdelioglu.arc.compose.foundation.ArcMotion
import com.knigdelioglu.arc.compose.foundation.ArcTheme

/**
 * Arc Metric Card: displays single metric with label, value, trend, or subtitle.
 */
@Composable
fun ArcMetricCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trendPositive: Boolean? = null,
    badgeText: String? = null,
    badgeVariant: ArcBadgeVariant = ArcBadgeVariant.Default
) {
    val colors = ArcTheme.colors
    ArcCard(modifier = modifier) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(title, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = colors.textSecondary)
                if (badgeText != null) {
                    ArcBadge(text = badgeText, variant = badgeVariant)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = value,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = colors.foreground,
                letterSpacing = (-0.5).sp
            )
            if (subtitle != null) {
                Spacer(Modifier.height(4.dp))
                Text(subtitle, fontSize = 12.sp, color = colors.textMuted)
            }
        }
    }
}

/**
 * Arc Stats Band: horizontal block displaying Tamamlanan, Kısmi, Başlanmadı and Sınıf Ortalaması.
 */
data class ArcStatItem(
    val label: String,
    val value: String,
    val detail: String? = null
)

@Composable
fun ArcStatsBand(
    stats: List<ArcStatItem>,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = ArcTheme.shapes.panel,
        color = ArcTheme.colors.surfaceRaised,
        border = BorderStroke(1.dp, ArcTheme.colors.borderSubtle)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            stats.forEachIndexed { index, stat ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stat.label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = ArcTheme.colors.textSecondary)
                    Spacer(Modifier.height(4.dp))
                    Text(stat.value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = ArcTheme.colors.foreground)
                    if (stat.detail != null) {
                        Spacer(Modifier.height(2.dp))
                        Text(stat.detail, fontSize = 11.sp, color = ArcTheme.colors.textMuted)
                    }
                }
                if (index < stats.lastIndex) {
                    Box(
                        modifier = Modifier
                            .height(36.dp)
                            .width(1.dp)
                            .background(ArcTheme.colors.borderSubtle)
                    )
                }
            }
        }
    }
}

/**
 * Arc Animated Counter: smooth integer counting animation on value change.
 */
@Composable
fun ArcAnimatedCounter(
    targetValue: Int = 0,
    modifier: Modifier = Modifier,
    suffix: String = "",
    count: Int = targetValue,
    fontSize: TextUnit = 24.sp,
    color: Color = ArcTheme.colors.foreground
) {
    val motion = ArcMotion.current
    val effectiveTarget = if (count != 0) count else targetValue
    val animatedValue by animateIntAsState(
        targetValue = effectiveTarget,
        animationSpec = motion.standardTween(),
        label = "animatedCounter"
    )

    Text(
        text = "$animatedValue$suffix",
        fontSize = fontSize,
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = modifier
    )
}

/**
 * Arc Slot Text: number reel / slot animation effect.
 */
@Composable
fun ArcSlotText(
    value: String,
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = value,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "arcSlotText",
        modifier = modifier
    ) { target ->
        Text(
            text = target,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = ArcTheme.colors.foreground
        )
    }
}

/**
 * Arc Text Reveal: smooth initial text reveal for introductions.
 */
@Composable
fun ArcTextReveal(
    text: String,
    modifier: Modifier = Modifier
) {
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(text) {
        alpha.animateTo(1f, animationSpec = tween(600, easing = LinearEasing))
    }
    Text(
        text = text,
        color = ArcTheme.colors.foreground.copy(alpha = alpha.value),
        fontSize = 15.sp,
        modifier = modifier
    )
}

/**
 * Arc Text Morph: morphing label (e.g. "Kaydediliyor..." -> "Kaydedildi").
 */
@Composable
fun ArcTextMorph(
    label: String,
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = label,
        transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
        label = "arcTextMorph",
        modifier = modifier
    ) { currentLabel ->
        Text(
            text = currentLabel,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = ArcTheme.colors.textSecondary
        )
    }
}

/**
 * Arc Text Shimmer: gentle shimmer pulse for ongoing OCR processing.
 */
@Composable
fun ArcTextShimmer(
    text: String,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmerAlpha"
    )

    Text(
        text = text,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        color = ArcTheme.colors.accent.copy(alpha = alpha),
        modifier = modifier
    )
}

/**
 * Arc Chip Group: horizontal filter chips for rubrics, classes, and assessment status.
 */
@Composable
fun <T> ArcChipGroup(
    items: List<T>,
    selectedItem: T?,
    onItemSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    labelProvider: (T) -> String = { it.toString() }
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items) { item ->
            val isSelected = item == selectedItem
            val colors = ArcTheme.colors
            Surface(
                modifier = Modifier
                    .clip(ArcTheme.shapes.pill)
                    .clickable { onItemSelected(item) },
                shape = ArcTheme.shapes.pill,
                color = if (isSelected) colors.accent else colors.surfaceRaised,
                border = BorderStroke(1.dp, if (isSelected) colors.accent else colors.borderSubtle)
            ) {
                Text(
                    text = labelProvider(item),
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) colors.accentForeground else colors.foreground,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                )
            }
        }
    }
}

/**
 * Arc Filter Toolbar: combined search, status chips, and sorting controls.
 */
@Composable
fun ArcFilterToolbar(
    modifier: Modifier = Modifier,
    searchSlot: (@Composable () -> Unit)? = null,
    filterChipsSlot: (@Composable () -> Unit)? = null,
    actionsSlot: (@Composable () -> Unit)? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (searchSlot != null) {
                Box(modifier = Modifier.weight(1f, fill = false)) {
                    searchSlot()
                }
            }
            if (actionsSlot != null) {
                Spacer(Modifier.width(8.dp))
                actionsSlot()
            }
        }
        if (filterChipsSlot != null) {
            Spacer(Modifier.height(8.dp))
            filterChipsSlot()
        }
    }
}

/**
 * Arc Sortable Data Table:
 * Essential for Student Roster, OCR Verification review, and Result / CSV inspection.
 */
data class ArcTableColumn<T>(
    val title: String,
    val weight: Float = 1f,
    val comparator: Comparator<T>? = null,
    val content: @Composable (T) -> Unit
)

@Composable
fun <T> ArcSortableDataTable(
    items: List<T>,
    columns: List<ArcTableColumn<T>>,
    modifier: Modifier = Modifier,
    onRowClick: ((T) -> Unit)? = null
) {
    var sortColumnIndex by remember { mutableStateOf<Int?>(null) }
    var sortAscending by remember { mutableStateOf(true) }

    val sortedItems = remember(items, sortColumnIndex, sortAscending) {
        val colIndex = sortColumnIndex
        if (colIndex != null) {
            val comp = columns.getOrNull(colIndex)?.comparator
            if (comp != null) {
                if (sortAscending) items.sortedWith(comp) else items.sortedWith(comp.reversed())
            } else items
        } else items
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = ArcTheme.shapes.panel,
        color = ArcTheme.colors.surface,
        border = BorderStroke(1.dp, ArcTheme.colors.borderSubtle)
    ) {
        Column {
            // Header Row
            Surface(color = ArcTheme.colors.surfaceRaised) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    columns.forEachIndexed { index, col ->
                        Row(
                            modifier = Modifier
                                .weight(col.weight)
                                .then(
                                    if (col.comparator != null) {
                                        Modifier.clickable {
                                            if (sortColumnIndex == index) {
                                                sortAscending = !sortAscending
                                            } else {
                                                sortColumnIndex = index
                                                sortAscending = true
                                            }
                                        }
                                    } else Modifier
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = col.title,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = ArcTheme.colors.foreground
                            )
                            if (sortColumnIndex == index) {
                                Spacer(Modifier.width(4.dp))
                                Icon(
                                    imageVector = if (sortAscending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                    contentDescription = null,
                                    tint = ArcTheme.colors.accent,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }

            Divider(color = ArcTheme.colors.borderSubtle)

            // Items Rows
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(sortedItems) { item ->
                    Surface(
                        color = Color.Transparent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (onRowClick != null) Modifier.clickable { onRowClick(item) } else Modifier)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            columns.forEach { col ->
                                Box(modifier = Modifier.weight(col.weight)) {
                                    col.content(item)
                                }
                            }
                        }
                    }
                    Divider(color = ArcTheme.colors.borderSubtle.copy(alpha = 0.5f))
                }
            }
        }
    }
}
