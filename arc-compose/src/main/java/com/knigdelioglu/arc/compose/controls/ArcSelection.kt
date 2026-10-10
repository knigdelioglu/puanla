package com.knigdelioglu.arc.compose.controls

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.knigdelioglu.arc.compose.foundation.ArcMotion
import com.knigdelioglu.arc.compose.foundation.ArcTheme

/**
 * Arc Checkbox: crisp rounded square with animated check mark.
 */
@Composable
fun ArcCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: String? = null
) {
    val motion = ArcMotion.current
    val scale by animateFloatAsState(
        targetValue = if (checked) 1f else 0.85f,
        animationSpec = motion.springSnappy(),
        label = "arcCheckboxCheck"
    )

    val colors = ArcTheme.colors
    val backgroundColor by animateColorAsState(
        targetValue = if (checked) colors.controlOn else Color.Transparent,
        animationSpec = motion.fastTween(),
        label = "arcCheckboxBg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (checked) colors.controlOn else colors.borderStrong,
        animationSpec = motion.fastTween(),
        label = "arcCheckboxBorder"
    )

    Row(
        modifier = modifier
            .semantics { role = Role.Checkbox }
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(backgroundColor)
                .background(
                    if (!checked) colors.surfaceRaised else Color.Transparent,
                    RoundedCornerShape(6.dp)
                )
                .then(
                    Modifier.background(
                        color = Color.Transparent,
                        shape = RoundedCornerShape(6.dp)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier.size(22.dp),
                shape = RoundedCornerShape(6.dp),
                color = backgroundColor,
                border = BorderStroke(1.5.dp, borderColor)
            ) {
                if (checked) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = colors.controlGlyph,
                            modifier = Modifier
                                .size(14.dp)
                                .scale(scale)
                        )
                    }
                }
            }
        }

        if (label != null) {
            Spacer(Modifier.width(10.dp))
            Text(
                text = label,
                fontSize = 15.sp,
                color = if (enabled) colors.foreground else colors.textMuted
            )
        }
    }
}

/**
 * Arc Switch: iOS/macOS inspired physical spring toggle with white thumb.
 */
@Composable
fun ArcSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: String? = null
) {
    val motion = ArcMotion.current
    val trackWidth = 48.dp
    val trackHeight = 28.dp
    val thumbSize = 22.dp
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 22.dp else 2.dp,
        animationSpec = motion.springSnappy(),
        label = "arcSwitchThumb"
    )

    val colors = ArcTheme.colors
    val trackColor by animateColorAsState(
        targetValue = if (checked) colors.controlOn else colors.controlTrack,
        animationSpec = motion.fastTween(),
        label = "arcSwitchTrack"
    )

    val switchBox = @Composable {
        Box(
            modifier = Modifier
                .size(trackWidth, trackHeight)
                .clip(ArcTheme.shapes.pill)
                .background(trackColor)
                .semantics { role = Role.Switch }
                .then(
                    if (label == null) Modifier.clickable(enabled = enabled) { onCheckedChange(!checked) }
                    else Modifier
                )
                .padding(vertical = 3.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Surface(
                modifier = Modifier
                    .offset(x = thumbOffset)
                    .size(thumbSize)
                    .shadow(2.dp, CircleShape),
                shape = CircleShape,
                color = colors.controlThumb
            ) {}
        }
    }

    if (label != null) {
        Row(
            modifier = modifier.clickable(enabled = enabled) { onCheckedChange(!checked) },
            verticalAlignment = Alignment.CenterVertically
        ) {
            switchBox()
            Spacer(Modifier.width(10.dp))
            Text(
                text = label,
                fontSize = 15.sp,
                color = if (enabled) colors.foreground else colors.textMuted
            )
        }
    } else {
        Box(modifier = modifier) {
            switchBox()
        }
    }
}

/**
 * Arc Radio Group: linear or vertical radio option set.
 */
@Composable
fun <T> ArcRadioGroup(
    options: List<T>,
    selectedOption: T,
    onOptionSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    labelProvider: (T) -> String = { it.toString() }
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            val selected = option == selectedOption
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOptionSelected(option) }
                    .padding(vertical = 6.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(20.dp),
                    shape = CircleShape,
                    border = BorderStroke(
                        if (selected) 6.dp else 1.5.dp,
                        if (selected) ArcTheme.colors.controlOn else ArcTheme.colors.borderStrong
                    ),
                    color = ArcTheme.colors.surface
                ) {}
                Spacer(Modifier.width(12.dp))
                Text(
                    text = labelProvider(option),
                    fontSize = 15.sp,
                    color = ArcTheme.colors.foreground,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}

/**
 * Arc Radio Cards: cards behaving as single-selection radio options with rich titles/descriptions.
 */
@Composable
fun <T> ArcRadioCards(
    options: List<T>,
    selectedOption: T?,
    onOptionSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    titleProvider: (T) -> String,
    descriptionProvider: ((T) -> String)? = null,
    badgeProvider: ((T) -> String)? = null
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        options.forEach { option ->
            val selected = option == selectedOption
            val colors = ArcTheme.colors
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOptionSelected(option) },
                shape = ArcTheme.shapes.panel,
                color = if (selected) colors.surfaceRaised else colors.surface,
                border = BorderStroke(
                    if (selected) 2.dp else 1.dp,
                    if (selected) colors.accent else colors.borderSubtle
                ),
                shadowElevation = if (selected) 2.dp else 0.dp
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(20.dp),
                        shape = CircleShape,
                        border = BorderStroke(
                            if (selected) 6.dp else 1.5.dp,
                            if (selected) colors.controlOn else colors.borderStrong
                        ),
                        color = colors.surface
                    ) {}
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = titleProvider(option),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.foreground
                            )
                            if (badgeProvider != null) {
                                val badge = badgeProvider(option)
                                if (badge.isNotEmpty()) {
                                    Spacer(Modifier.width(8.dp))
                                    Surface(
                                        shape = ArcTheme.shapes.pill,
                                        color = colors.surfaceMuted
                                    ) {
                                        Text(
                                            text = badge,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = colors.textSecondary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                        if (descriptionProvider != null) {
                            val desc = descriptionProvider(option)
                            if (desc.isNotEmpty()) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = desc,
                                    fontSize = 13.sp,
                                    color = colors.textSecondary,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Arc Segmented Control: sliding active pill for 3-4 qualitative choices or view modes.
 */
@Composable
fun ArcSegmentedControl(
    items: List<String>,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ArcTheme.colors
    Surface(
        modifier = modifier.height(44.dp),
        shape = ArcTheme.shapes.pill,
        color = colors.surfaceMuted,
        border = BorderStroke(1.dp, colors.borderSubtle)
    ) {
        Row(
            modifier = Modifier.padding(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, title ->
                val selected = index == selectedIndex
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clickable { onSelectIndex(index) },
                    shape = ArcTheme.shapes.pill,
                    color = if (selected) colors.surface else Color.Transparent,
                    border = if (selected) BorderStroke(1.dp, colors.border.copy(alpha = 0.5f)) else null,
                    shadowElevation = if (selected) 1.dp else 0.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = title,
                            fontSize = 14.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (selected) colors.foreground else colors.textSecondary
                        )
                    }
                }
            }
        }
    }
}
