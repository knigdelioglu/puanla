package com.knigdelioglu.arc.compose.controls

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.knigdelioglu.arc.compose.foundation.ArcMotion
import com.knigdelioglu.arc.compose.foundation.ArcTheme

enum class ArcButtonVariant {
    Primary,
    Secondary,
    Outline,
    Ghost,
    Danger,
    Success
}

enum class ArcButtonSize(val height: Dp, val horizontalPadding: Dp, val fontSize: Int, val iconSize: Dp) {
    Sm(36.dp, 12.dp, 13, 16.dp),
    Md(46.dp, 18.dp, 15, 20.dp),
    Lg(54.dp, 24.dp, 16, 22.dp)
}

/**
 * Arc Button adapted for Compose.
 * Includes physical spring press, tactile padding, subtle outline/fill variants, and accessible state.
 */
@Composable
fun ArcButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ArcButtonVariant = ArcButtonVariant.Primary,
    size: ArcButtonSize = ArcButtonSize.Md,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    shape: Shape = ArcTheme.shapes.control,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable () -> Unit
) {
    val pressed by interactionSource.collectIsPressedAsState()
    val motion = ArcMotion.current
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled && !loading) 0.97f else 1f,
        animationSpec = motion.springSnappy(),
        label = "arcButtonPress"
    )

    val colors = ArcTheme.colors
    val backgroundColor = when (variant) {
        ArcButtonVariant.Primary -> if (enabled) colors.accent else colors.surfaceMuted
        ArcButtonVariant.Secondary -> if (enabled) colors.surfaceRaised else colors.surfaceMuted
        ArcButtonVariant.Outline -> if (enabled) colors.surface else colors.surfaceMuted
        ArcButtonVariant.Ghost -> Color.Transparent
        ArcButtonVariant.Danger -> if (enabled) colors.danger else colors.surfaceMuted
        ArcButtonVariant.Success -> if (enabled) colors.success else colors.surfaceMuted
    }

    val contentColor = when (variant) {
        ArcButtonVariant.Primary -> if (enabled) colors.accentForeground else colors.textMuted
        ArcButtonVariant.Secondary -> if (enabled) colors.foreground else colors.textMuted
        ArcButtonVariant.Outline -> if (enabled) colors.foreground else colors.textMuted
        ArcButtonVariant.Ghost -> if (enabled) colors.foreground else colors.textMuted
        ArcButtonVariant.Danger -> if (enabled) Color.White else colors.textMuted
        ArcButtonVariant.Success -> if (enabled) Color.White else colors.textMuted
    }

    val border = when (variant) {
        ArcButtonVariant.Outline -> BorderStroke(1.dp, if (enabled) colors.border else colors.borderSubtle)
        ArcButtonVariant.Secondary -> BorderStroke(1.dp, colors.borderSubtle)
        else -> null
    }

    Surface(
        modifier = modifier
            .scale(scale)
            .defaultMinSize(minHeight = size.height, minWidth = size.height)
            .semantics {
                role = Role.Button
                if (!enabled || loading) disabled()
            },
        shape = shape,
        color = backgroundColor,
        contentColor = contentColor,
        border = border,
        shadowElevation = if (variant == ArcButtonVariant.Primary && enabled && !pressed) 1.dp else 0.dp
    ) {
        Box(
            modifier = Modifier
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled && !loading,
                    onClick = onClick
                )
                .padding(horizontal = size.horizontalPadding),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.animateContentSize()
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(size.iconSize),
                        strokeWidth = 2.dp,
                        color = contentColor
                    )
                    Spacer(Modifier.width(8.dp))
                } else if (leadingIcon != null) {
                    Box(modifier = Modifier.size(size.iconSize), contentAlignment = Alignment.Center) {
                        leadingIcon()
                    }
                    Spacer(Modifier.width(8.dp))
                }

                CompositionLocalProvider(LocalContentColor provides contentColor) {
                    content()
                }

                if (!loading && trailingIcon != null) {
                    Spacer(Modifier.width(8.dp))
                    Box(modifier = Modifier.size(size.iconSize), contentAlignment = Alignment.Center) {
                        trailingIcon()
                    }
                }
            }
        }
    }
}

@Composable
fun ArcButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ArcButtonVariant = ArcButtonVariant.Primary,
    size: ArcButtonSize = ArcButtonSize.Md,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    shape: Shape = ArcTheme.shapes.control
) {
    ArcButton(
        onClick = onClick,
        modifier = modifier,
        variant = variant,
        size = size,
        enabled = enabled,
        loading = loading,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        shape = shape
    ) {
        Text(text, fontSize = size.fontSize.sp, fontWeight = FontWeight.SemiBold)
    }
}


/**
 * Arc Action Button: compact, icon-friendly touch button for tablet tools.
 */
@Composable
fun ArcActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ArcButtonVariant = ArcButtonVariant.Secondary,
    size: ArcButtonSize = ArcButtonSize.Md,
    enabled: Boolean = true,
    shape: Shape = ArcTheme.shapes.control,
    content: @Composable () -> Unit
) {
    ArcButton(
        onClick = onClick,
        modifier = modifier,
        variant = variant,
        size = size,
        enabled = enabled,
        shape = shape,
        content = content
    )
}

/**
 * Arc Split Button: combined primary action with drop down option trigger.
 */
@Composable
fun ArcSplitButton(
    onPrimaryClick: () -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ArcButtonVariant = ArcButtonVariant.Primary,
    size: ArcButtonSize = ArcButtonSize.Md,
    enabled: Boolean = true,
    primaryContent: @Composable () -> Unit
) {
    val shape = ArcTheme.shapes.control
    val leftShape = remember(shape) {
        RoundedCornerShape(topStart = 18.dp, bottomStart = 18.dp, topEnd = 4.dp, bottomEnd = 4.dp)
    }
    val rightShape = remember(shape) {
        RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp, topEnd = 18.dp, bottomEnd = 18.dp)
    }

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        ArcButton(
            onClick = onPrimaryClick,
            variant = variant,
            size = size,
            enabled = enabled,
            shape = leftShape,
            content = primaryContent
        )
        Spacer(Modifier.width(2.dp))
        ArcButton(
            onClick = onMenuClick,
            variant = variant,
            size = size,
            enabled = enabled,
            shape = rightShape,
            content = {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Diğer seçenekler",
                    modifier = Modifier.size(size.iconSize)
                )
            }
        )
    }
}

@Composable
fun ArcSplitButton(
    primaryText: String,
    onPrimaryClick: () -> Unit,
    menuItems: List<String>,
    onMenuItemClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    variant: ArcButtonVariant = ArcButtonVariant.Primary,
    size: ArcButtonSize = ArcButtonSize.Md
) {
    var menuOpen by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        ArcSplitButton(
            onPrimaryClick = onPrimaryClick,
            onMenuClick = { menuOpen = true },
            variant = variant,
            size = size,
            primaryContent = { Text(primaryText, fontWeight = FontWeight.SemiBold) }
        )
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            menuItems.forEachIndexed { index, item ->
                DropdownMenuItem(
                    text = { Text(item) },
                    onClick = {
                        menuOpen = false
                        onMenuItemClick(index)
                    }
                )
            }
        }
    }
}

/**
 * Arc Button Group: joined linear group of actions.
 */
@Composable
fun ArcButtonGroup(
    modifier: Modifier = Modifier,
    shape: Shape = ArcTheme.shapes.control,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = ArcTheme.colors.surfaceMuted,
        border = BorderStroke(1.dp, ArcTheme.colors.borderSubtle)
    ) {
        Row(
            modifier = Modifier.padding(3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            content()
        }
    }
}

@Composable
fun ArcButtonGroup(
    buttons: List<Pair<String, () -> Unit>>,
    modifier: Modifier = Modifier,
    shape: Shape = ArcTheme.shapes.control
) {
    ArcButtonGroup(modifier = modifier, shape = shape) {
        buttons.forEach { (text, action) ->
            ArcButton(
                text = text,
                onClick = action,
                variant = ArcButtonVariant.Ghost,
                size = ArcButtonSize.Sm
            )
        }
    }
}


/**
 * Arc Floating Button Group: elevated toolbar pill for persistent tablet navigation (Previous / Undo / Next).
 */
@Composable
fun ArcFloatingButtonGroup(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier
            .shadow(12.dp, ArcTheme.shapes.pill, spotColor = Color(0x33000000))
            .padding(horizontal = 6.dp, vertical = 4.dp),
        shape = ArcTheme.shapes.pill,
        color = ArcTheme.colors.surfaceRaised,
        border = BorderStroke(1.dp, ArcTheme.colors.borderStrong.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            content()
        }
    }
}

/**
 * Arc Expanding Button Group: items expand label smoothly when selected or focused.
 */
@Composable
fun ArcExpandingButtonGroup(
    items: List<Pair<String, @Composable () -> Unit>>,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    ArcButtonGroup(modifier = modifier, shape = ArcTheme.shapes.pill) {
        items.forEachIndexed { index, (label, icon) ->
            val selected = index == selectedIndex
            Surface(
                modifier = Modifier
                    .clickable { onSelectIndex(index) }
                    .animateContentSize(),
                shape = ArcTheme.shapes.pill,
                color = if (selected) ArcTheme.colors.accent else Color.Transparent,
                contentColor = if (selected) ArcTheme.colors.accentForeground else ArcTheme.colors.foreground
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    icon()
                    AnimatedVisibility(visible = selected) {
                        Row {
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = label,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}
