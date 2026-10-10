package com.knigdelioglu.arc.compose.display

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Popup
import com.knigdelioglu.arc.compose.controls.ArcActionButton
import com.knigdelioglu.arc.compose.controls.ArcButton
import com.knigdelioglu.arc.compose.controls.ArcButtonSize
import com.knigdelioglu.arc.compose.controls.ArcButtonVariant
import com.knigdelioglu.arc.compose.foundation.ArcTheme
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * Toast data model.
 */
data class ArcToastMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val description: String? = null,
    val undoAction: (() -> Unit)? = null
)

/**
 * Arc Toast: single transient notification.
 */
@Composable
fun ArcToast(
    message: ArcToastMessage,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .shadow(8.dp, ArcTheme.shapes.panel)
            .width(360.dp),
        shape = ArcTheme.shapes.panel,
        color = ArcTheme.colors.surfaceRaised,
        border = BorderStroke(1.dp, ArcTheme.colors.border)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(message.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = ArcTheme.colors.foreground)
                if (message.description != null) {
                    Spacer(Modifier.height(2.dp))
                    Text(message.description, fontSize = 12.sp, color = ArcTheme.colors.textSecondary)
                }
            }
            if (message.undoAction != null) {
                ArcButton(
                    onClick = {
                        message.undoAction.invoke()
                        onDismiss()
                    },
                    variant = ArcButtonVariant.Secondary,
                    size = ArcButtonSize.Sm
                ) {
                    Text("Geri Al", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(6.dp))
            }
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Kapat",
                tint = ArcTheme.colors.textMuted,
                modifier = Modifier
                    .size(16.dp)
                    .clickable { onDismiss() }
            )
        }
    }
}

/**
 * Arc Toast Stack: stack of notifications with auto-dismiss and undo capability.
 */
@Composable
fun ArcToastStack(
    toasts: List<ArcToastMessage>,
    onDismissToast: (ArcToastMessage) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .padding(16.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            toasts.takeLast(3).forEach { toast ->
                LaunchedEffect(toast.id) {
                    delay(4000)
                    onDismissToast(toast)
                }
                ArcToast(
                    message = toast,
                    onDismiss = { onDismissToast(toast) }
                )
            }
        }
    }
}

/**
 * Arc Dialog: modal dialog for confirmations, settings, and critical decisions.
 */
@Composable
fun ArcDialog(
    isOpen: Boolean,
    onDismissRequest: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    confirmButton: (@Composable () -> Unit)? = null,
    dismissButton: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    if (isOpen) {
        Dialog(onDismissRequest = onDismissRequest) {
            Surface(
                modifier = modifier
                    .fillMaxWidth(0.92f),
                shape = ArcTheme.shapes.panel,
                color = ArcTheme.colors.surfaceRaised,
                border = BorderStroke(1.dp, ArcTheme.colors.border),
                shadowElevation = 16.dp
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = ArcTheme.colors.foreground)
                    Spacer(Modifier.height(14.dp))
                    content()
                    Spacer(Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        if (dismissButton != null) {
                            dismissButton()
                            Spacer(Modifier.width(10.dp))
                        }
                        if (confirmButton != null) {
                            confirmButton()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ArcDialog(
    isOpen: Boolean,
    onDismissRequest: () -> Unit,
    title: String,
    description: String,
    confirmText: String = "Onayla",
    cancelText: String = "Vazgeç",
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    ArcDialog(
        isOpen = isOpen,
        onDismissRequest = onDismissRequest,
        title = title,
        modifier = modifier,
        confirmButton = {
            ArcButton(
                text = confirmText,
                onClick = onConfirm,
                variant = ArcButtonVariant.Danger,
                size = ArcButtonSize.Sm
            )
        },
        dismissButton = {
            ArcButton(
                text = cancelText,
                onClick = onDismissRequest,
                variant = ArcButtonVariant.Ghost,
                size = ArcButtonSize.Sm
            )
        }
    ) {
        Text(
            text = description,
            fontSize = 14.sp,
            color = ArcTheme.colors.textSecondary,
            lineHeight = 20.sp
        )
    }
}


/**
 * Arc Popover: contextual anchor popover for tips and help.
 */
@Composable
fun ArcPopover(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    if (isOpen) {
        Popup(onDismissRequest = onDismiss) {
            Surface(
                modifier = modifier.shadow(8.dp, ArcTheme.shapes.panel),
                shape = ArcTheme.shapes.panel,
                color = ArcTheme.colors.surfaceRaised,
                border = BorderStroke(1.dp, ArcTheme.colors.border)
            ) {
                Box(modifier = Modifier.padding(14.dp)) {
                    content()
                }
            }
        }
    }
}

/**
 * Arc Tooltip: hover and long-press accessibility label.
 */
@Composable
fun ArcTooltip(
    tooltipText: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier) {
        content()
    }
}

/**
 * Arc Swipe Actions: revealable swipe action buttons on student row.
 */
@Composable
fun ArcSwipeActions(
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var offsetX by remember { mutableFloatStateOf(0f) }

    Box(modifier = modifier.fillMaxWidth()) {
        // Revealed Actions Behind
        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ArcActionButton(onClick = onEdit, size = ArcButtonSize.Sm) {
                Icon(Icons.Default.Edit, contentDescription = "Düzenle", modifier = Modifier.size(16.dp))
            }
            ArcActionButton(onClick = onDelete, size = ArcButtonSize.Sm, variant = ArcButtonVariant.Danger) {
                Icon(Icons.Default.Delete, contentDescription = "Sil", modifier = Modifier.size(16.dp))
            }
        }

        // Draggable Front Surface
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (offsetX < -80f) {
                                offsetX = -120f
                            } else {
                                offsetX = 0f
                            }
                        },
                        onDragCancel = { offsetX = 0f },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            offsetX = (offsetX + dragAmount).coerceIn(-120f, 0f)
                        }
                    )
                }
                .background(ArcTheme.colors.surface)
        ) {
            content()
        }
    }
}

/**
 * Arc Notification Center: list of local audit events (e.g. OCR import complete, backup saved).
 */
data class ArcNotificationItem(
    val id: String,
    val title: String,
    val timestamp: String,
    val details: String
)

@Composable
fun ArcNotificationCenter(
    notifications: List<ArcNotificationItem>,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = ArcTheme.shapes.panel,
        color = ArcTheme.colors.surfaceRaised,
        border = BorderStroke(1.dp, ArcTheme.colors.borderSubtle)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("İşlem Geçmişi", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ArcTheme.colors.foreground)
            Spacer(Modifier.height(10.dp))
            if (notifications.isEmpty()) {
                Text("Kayıtlı işlem yok.", fontSize = 13.sp, color = ArcTheme.colors.textMuted)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(notifications) { item ->
                        Surface(
                            shape = ArcTheme.shapes.control,
                            color = ArcTheme.colors.surface,
                            border = BorderStroke(1.dp, ArcTheme.colors.borderSubtle)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = ArcTheme.colors.foreground)
                                    Text(item.details, fontSize = 12.sp, color = ArcTheme.colors.textSecondary)
                                }
                                Text(item.timestamp, fontSize = 11.sp, color = ArcTheme.colors.textMuted)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Arc User Menu: profile, academic term, and quick teacher settings menu.
 */
@Composable
fun ArcUserMenu(
    teacherName: String = "Öğretmen",
    academicYear: String = "2026-2027",
    modifier: Modifier = Modifier,
    onSettingsClick: () -> Unit = {}
) {
    Surface(
        modifier = modifier
            .clip(ArcTheme.shapes.pill)
            .clickable { onSettingsClick() },
        shape = ArcTheme.shapes.pill,
        color = ArcTheme.colors.surfaceMuted,
        border = BorderStroke(1.dp, ArcTheme.colors.borderSubtle)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ArcAvatar(initials = teacherName, size = 26.dp)
            Spacer(Modifier.width(8.dp))
            Column {
                Text(teacherName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ArcTheme.colors.foreground)
                Text(academicYear, fontSize = 10.sp, color = ArcTheme.colors.textSecondary)
            }
        }
    }
}

/**
 * Arc Timeline: visual history flow of scoring sessions and backup events.
 */
data class ArcTimelineEntry(
    val title: String,
    val subtitle: String,
    val time: String,
    val icon: ImageVector = Icons.Default.Check
)

@Composable
fun ArcTimeline(
    entries: List<ArcTimelineEntry>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        entries.forEachIndexed { index, entry ->
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        modifier = Modifier.size(24.dp),
                        shape = CircleShape,
                        color = ArcTheme.colors.accent
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(entry.icon, contentDescription = null, tint = ArcTheme.colors.accentForeground, modifier = Modifier.size(14.dp))
                        }
                    }
                    if (index < entries.lastIndex) {
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height(32.dp)
                                .background(ArcTheme.colors.border)
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(entry.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = ArcTheme.colors.foreground)
                        Text(entry.time, fontSize = 12.sp, color = ArcTheme.colors.textMuted)
                    }
                    Text(entry.subtitle, fontSize = 13.sp, color = ArcTheme.colors.textSecondary)
                }
            }
        }
    }
}

/**
 * Arc Changelog Feed: version release notes for the Puanla application.
 */
@Composable
fun ArcChangelogFeed(
    modifier: Modifier = Modifier,
    version: String = "v0.1.0-dev",
    date: String = "10 Ekim 2026",
    changes: List<String> = listOf(
        "Tam Android Native Kotlin + Jetpack Compose mimarisi.",
        "Arc Library 99 bileşenin native Compose uyarlaması.",
        "11 inç tablet odaklı Resizable Panels çalışma alanı.",
        "Cihaz içi ML Kit OCR ile sütun korumalı sınıf listesi aktarımı.",
        "Room çevrimdışı veritabanı ve SAF tabanlı JSON yedekleme."
    )
) {
    ArcCard(modifier = modifier) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Sürüm Güncellemeleri", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ArcTheme.colors.foreground)
                ArcBadge(text = version, variant = ArcBadgeVariant.Accent)
            }
            Spacer(Modifier.height(4.dp))
            Text(date, fontSize = 12.sp, color = ArcTheme.colors.textMuted)
            Spacer(Modifier.height(12.dp))
            changes.forEach { item ->
                Text("• $item", fontSize = 13.sp, color = ArcTheme.colors.textSecondary, lineHeight = 19.sp)
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

/**
 * Arc FAQ Section: frequently asked questions accordion.
 */
@Composable
fun ArcFaqSection(
    modifier: Modifier = Modifier
) {
    val faqItems = listOf(
        "Puanlanmamış ölçüt ile 0 puan arasındaki fark nedir?" to "Puanlanmamış ölçüt henüz öğretmen tarafından değerlendirilmemiş demektir ve kesin nota dönüştürülmez. 0 puan ise ölçütün fiilen değerlendirilip sıfır takdir edildiğini gösterir.",
        "Fotoğraftan liste aktarımında sütunlar karışır mı?" to "Puanla, sütun geometrisi ayrıştırması ve ML Kit ile Sıra No, Okul No, Ad ve Soyad sütunlarını izole eder. Öğretmen doğrulamadan veritabanına kayıt yapılmaz.",
        "İnternet bağlantısı olmadan kullanılabilir mi?" to "Evet. Puanla tamamen çevrimdışı çalışır. Tüm veriler cihaz içi Room veritabanında saklanır."
    )
    ArcAccordion(items = faqItems, modifier = modifier)
}

/**
 * Arc JSON Viewer: diagnostic viewer for verified rubric JSON sources.
 */
@Composable
fun ArcJsonViewer(
    jsonText: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = ArcTheme.shapes.panel,
        color = Color(0xFF1E1E24)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Kaynak JSON Önizleme", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFA1A1AA))
            Spacer(Modifier.height(8.dp))
            Text(
                text = jsonText,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = Color(0xFF86EFAC),
                lineHeight = 18.sp
            )
        }
    }
}

/**
 * Arc Comparison Table: compare student scores across different rubrics or criteria.
 */
@Composable
fun ArcComparisonTable(
    headers: List<String>,
    rows: List<List<String>>,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = ArcTheme.shapes.panel,
        color = ArcTheme.colors.surfaceRaised,
        border = BorderStroke(1.dp, ArcTheme.colors.borderSubtle)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                headers.forEach { header ->
                    Text(
                        text = header,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = ArcTheme.colors.foreground,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Divider(color = ArcTheme.colors.borderSubtle)
            rows.forEach { row ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    row.forEach { cell ->
                        Text(
                            text = cell,
                            fontSize = 13.sp,
                            color = ArcTheme.colors.textSecondary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}
