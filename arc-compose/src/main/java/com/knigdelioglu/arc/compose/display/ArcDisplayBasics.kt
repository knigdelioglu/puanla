package com.knigdelioglu.arc.compose.display

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.knigdelioglu.arc.compose.controls.ArcButton
import com.knigdelioglu.arc.compose.controls.ArcButtonSize
import com.knigdelioglu.arc.compose.controls.ArcButtonVariant
import com.knigdelioglu.arc.compose.foundation.ArcTheme

/**
 * Arc Card: clean elevated card surface.
 */
@Composable
fun ArcCard(
    modifier: Modifier = Modifier,
    shape: Shape = ArcTheme.shapes.panel,
    color: Color = ArcTheme.colors.surface,
    border: BorderStroke? = BorderStroke(1.dp, ArcTheme.colors.borderSubtle),
    shadowElevation: Dp = 1.dp,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = color,
        border = border,
        shadowElevation = shadowElevation
    ) {
        content()
    }
}

/**
 * Arc Expandable Card: card with fluid expand/collapse toggle for long rubric descriptions or evidence notes.
 */
@Composable
fun ArcExpandableCard(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    badge: (@Composable () -> Unit)? = null,
    initiallyExpanded: Boolean = false,
    content: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    val colors = ArcTheme.colors

    ArcCard(
        modifier = modifier.animateContentSize()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = colors.foreground)
                        if (badge != null) {
                            Spacer(Modifier.width(8.dp))
                            badge()
                        }
                    }
                    if (subtitle != null) {
                        Spacer(Modifier.height(2.dp))
                        Text(subtitle, fontSize = 13.sp, color = colors.textSecondary)
                    }
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Daralt" else "Genişlet",
                    tint = colors.textMuted
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(Modifier.height(12.dp))
                    content()
                }
            }
        }
    }
}

/**
 * Arc Badge: status indicator badge (Tamamlandı, Kısmi, Başlanmadı, vb.).
 */
enum class ArcBadgeVariant {
    Default,
    Success,
    Warning,
    Danger,
    Accent
}

@Composable
fun ArcBadge(
    text: String,
    modifier: Modifier = Modifier,
    variant: ArcBadgeVariant = ArcBadgeVariant.Default
) {
    val colors = ArcTheme.colors
    val (bgColor, textColor) = when (variant) {
        ArcBadgeVariant.Default -> colors.surfaceMuted to colors.textSecondary
        ArcBadgeVariant.Success -> colors.success.copy(alpha = 0.15f) to colors.success
        ArcBadgeVariant.Warning -> colors.warning.copy(alpha = 0.15f) to colors.warning
        ArcBadgeVariant.Danger -> colors.danger.copy(alpha = 0.15f) to colors.danger
        ArcBadgeVariant.Accent -> colors.accent to colors.accentForeground
    }

    Surface(
        modifier = modifier,
        shape = ArcTheme.shapes.pill,
        color = bgColor
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
        )
    }
}

/**
 * Arc Avatar: student initials avatar without exposing unnecessary photos or PII.
 */
@Composable
fun ArcAvatar(
    initials: String,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    backgroundColor: Color = ArcTheme.colors.surfaceMuted
) {
    Surface(
        modifier = modifier.size(size),
        shape = CircleShape,
        color = backgroundColor,
        border = BorderStroke(1.dp, ArcTheme.colors.borderSubtle)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = initials.take(2).uppercase(),
                fontSize = (size.value * 0.4f).sp,
                fontWeight = FontWeight.Bold,
                color = ArcTheme.colors.foreground
            )
        }
    }
}

/**
 * Arc Avatar Group: clustered avatar row for group work members.
 */
@Composable
fun ArcAvatarGroup(
    membersInitials: List<String>,
    modifier: Modifier = Modifier,
    avatarSize: Dp = 32.dp,
    maxDisplay: Int = 4
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        val displayed = membersInitials.take(maxDisplay)
        displayed.forEachIndexed { index, initials ->
            Box(modifier = Modifier.offset(x = (-index * 8).dp)) {
                ArcAvatar(initials = initials, size = avatarSize)
            }
        }
        if (membersInitials.size > maxDisplay) {
            val extra = membersInitials.size - maxDisplay
            Box(modifier = Modifier.offset(x = (-displayed.size * 8).dp)) {
                Surface(
                    modifier = Modifier.size(avatarSize),
                    shape = CircleShape,
                    color = ArcTheme.colors.surfaceRaised,
                    border = BorderStroke(1.dp, ArcTheme.colors.border)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "+$extra",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ArcTheme.colors.textSecondary
                        )
                    }
                }
            }
        }
    }
}

/**
 * Arc Alert: notice bar for OCR uncertainty, storage status, or validation cautions.
 */
enum class ArcAlertVariant {
    Info,
    Success,
    Warning,
    Danger
}

typealias ArcAlertType = ArcAlertVariant

@Composable
fun ArcAlert(
    title: String,
    modifier: Modifier = Modifier,
    message: String? = null,
    variant: ArcAlertVariant = ArcAlertVariant.Info,
    type: ArcAlertVariant = variant,
    action: (@Composable () -> Unit)? = null
) {
    val effectiveVariant = if (type != ArcAlertVariant.Info) type else variant
    val colors = ArcTheme.colors
    val (barColor, icon) = when (effectiveVariant) {

        ArcAlertVariant.Info -> colors.info to Icons.Default.Info
        ArcAlertVariant.Success -> colors.success to Icons.Default.CheckCircle
        ArcAlertVariant.Warning -> colors.warning to Icons.Default.Warning
        ArcAlertVariant.Danger -> colors.danger to Icons.Default.Error
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = ArcTheme.shapes.panel,
        color = barColor.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, barColor.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = barColor, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = colors.foreground)
                if (message != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(message, fontSize = 13.sp, color = colors.textSecondary, lineHeight = 18.sp)
                }
            }
            if (action != null) {
                Spacer(Modifier.width(12.dp))
                action()
            }
        }
    }
}

/**
 * Arc Progress: linear progress bar showing rubric completion percentage or OCR steps.
 */
@Composable
fun ArcProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 8.dp,
    trackColor: Color = ArcTheme.colors.surfaceMuted,
    progressColor: Color = ArcTheme.colors.accent
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(ArcTheme.shapes.pill)
            .background(trackColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(height)
                .clip(ArcTheme.shapes.pill)
                .background(progressColor)
        )
    }
}

/**
 * Arc Skeleton: smooth shimmering placeholder while offline Room database loads.
 */
@Composable
fun ArcSkeleton(
    modifier: Modifier = Modifier,
    shape: Shape = ArcTheme.shapes.control
) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "skeletonAlpha"
    )

    Box(
        modifier = modifier
            .clip(shape)
            .background(ArcTheme.colors.surfaceMuted.copy(alpha = alpha))
    )
}

/**
 * Arc Empty State: guidance placeholder when classroom, rubric, or results are empty.
 */
@Composable
fun ArcEmptyState(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    action: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            Surface(
                modifier = Modifier.size(56.dp),
                shape = CircleShape,
                color = ArcTheme.colors.surfaceMuted
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(imageVector = icon, contentDescription = null, tint = ArcTheme.colors.textMuted, modifier = Modifier.size(28.dp))
                }
            }
            Spacer(Modifier.height(16.dp))
        }
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = ArcTheme.colors.foreground)
        Spacer(Modifier.height(6.dp))
        Text(
            text = description,
            fontSize = 14.sp,
            color = ArcTheme.colors.textSecondary,
            lineHeight = 20.sp,
            modifier = Modifier.fillMaxWidth(0.7f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        if (action != null) {
            Spacer(Modifier.height(20.dp))
            action()
        }
    }
}

/**
 * Arc Empty States: bundle template provider for classrooms, OCR, and rubrics.
 */
object ArcEmptyStates {
    @Composable
    fun NoClassrooms(onCreate: () -> Unit) {
        ArcEmptyState(
            title = "Henüz sınıf oluşturulmadı",
            description = "Değerlendirme yapmak için 9–12. sınıflardan bir şube ekleyin veya fotoğraftan öğrenci listesi aktarın.",
            action = {
                ArcButton(onClick = onCreate, variant = ArcButtonVariant.Primary) {
                    Text("Sınıf Oluştur")
                }
            }
        )
    }

    @Composable
    fun NoAssessments() {
        ArcEmptyState(
            title = "Değerlendirme Başlatılmadı",
            description = "Listeden bir öğrenci seçip rubrik ölçütlerine göre puanlamaya başlayın."
        )
    }
}

/**
 * Arc Accordion: collapsible multi-section guideline container.
 */
@Composable
fun ArcAccordion(
    items: List<Pair<String, String>>,
    modifier: Modifier = Modifier
) {
    var expandedIndex by remember { mutableStateOf<Int?>(null) }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEachIndexed { index, (header, body) ->
            val isExpanded = expandedIndex == index
            ArcCard {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedIndex = if (isExpanded) null else index },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(header, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = ArcTheme.colors.foreground)
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = ArcTheme.colors.textMuted
                        )
                    }
                    AnimatedVisibility(visible = isExpanded) {
                        Column {
                            Spacer(Modifier.height(8.dp))
                            Text(body, fontSize = 13.sp, color = ArcTheme.colors.textSecondary, lineHeight = 19.sp)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Arc Tree View: hierarchical criteria and task tree.
 */
data class ArcTreeNode(
    val id: String,
    val title: String,
    val children: List<ArcTreeNode> = emptyList()
) {
    constructor(title: String, children: List<ArcTreeNode> = emptyList()) : this(
        java.util.UUID.randomUUID().toString(),
        title,
        children
    )
}


@Composable
fun ArcTreeView(
    nodes: List<ArcTreeNode>,
    modifier: Modifier = Modifier,
    onNodeClick: ((ArcTreeNode) -> Unit)? = null
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        nodes.forEach { node ->
            ArcTreeNodeItem(node = node, depth = 0, onNodeClick = onNodeClick)
        }
    }
}

@Composable
private fun ArcTreeNodeItem(
    node: ArcTreeNode,
    depth: Int,
    onNodeClick: ((ArcTreeNode) -> Unit)?
) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = (depth * 18).dp)
                .clip(ArcTheme.shapes.small)
                .clickable {
                    if (node.children.isNotEmpty()) expanded = !expanded
                    onNodeClick?.invoke(node)
                }
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (node.children.isNotEmpty()) {
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = ArcTheme.colors.textMuted
                )
                Spacer(Modifier.width(4.dp))
            } else {
                Spacer(Modifier.width(20.dp))
            }
            Text(node.title, fontSize = 14.sp, color = ArcTheme.colors.foreground)
        }
        if (expanded) {
            node.children.forEach { child ->
                ArcTreeNodeItem(node = child, depth = depth + 1, onNodeClick = onNodeClick)
            }
        }
    }
}

/**
 * Arc Announcement Bar: persistent notification bar for important system/backup alerts.
 */
@Composable
fun ArcAnnouncementBar(
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = ArcTheme.colors.accent,
        contentColor = ArcTheme.colors.accentForeground
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(message, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            if (actionLabel != null && onActionClick != null) {
                Text(
                    text = actionLabel,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onActionClick() }
                )
            }
        }
    }
}

/**
 * Arc Theme Switch: clean light/dark mode switch.
 */
@Composable
fun ArcThemeSwitch(
    isDark: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(ArcTheme.shapes.pill)
            .clickable { onToggle(!isDark) },
        shape = ArcTheme.shapes.pill,
        color = ArcTheme.colors.surfaceRaised,
        border = BorderStroke(1.dp, ArcTheme.colors.border)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isDark) Icons.Default.DarkMode else Icons.Default.LightMode,
                contentDescription = "Tema Değiştir",
                tint = ArcTheme.colors.foreground,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = if (isDark) "Koyu" else "Açık",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = ArcTheme.colors.foreground
            )
        }
    }
}

/**
 * Arc Copy Button: copies student ID or report link to clipboard with brief check indicator.
 */
@Composable
fun ArcCopyButton(
    textToCopy: String,
    modifier: Modifier = Modifier,
    label: String? = null
) {
    var copied by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current

    ArcButton(
        onClick = {
            clipboardManager.setText(AnnotatedString(textToCopy))
            copied = true
        },
        modifier = modifier,
        variant = ArcButtonVariant.Secondary,
        size = ArcButtonSize.Sm
    ) {
        Icon(
            imageVector = if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
            contentDescription = "Kopyala",
            tint = if (copied) ArcTheme.colors.success else ArcTheme.colors.foreground,
            modifier = Modifier.size(14.dp)
        )
        if (label != null) {
            Spacer(Modifier.width(4.dp))
            Text(if (copied) "Kopyalandı" else label, fontSize = 12.sp)
        }
    }
}
