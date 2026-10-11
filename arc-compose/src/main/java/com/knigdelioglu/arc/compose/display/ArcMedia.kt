package com.knigdelioglu.arc.compose.display

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.mimeTypes
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.knigdelioglu.arc.compose.controls.ArcButton
import com.knigdelioglu.arc.compose.controls.ArcButtonSize
import com.knigdelioglu.arc.compose.controls.ArcButtonVariant
import com.knigdelioglu.arc.compose.foundation.ArcTheme
import kotlin.math.roundToInt

/**
 * Arc File Dropzone: Android SAF picker + optional cross-app image URI drops.
 * For external drops the read grant stays alive until the suspending callback
 * finishes decoding the image; the component never uploads student data.
 */
private tailrec fun Context.arcActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.arcActivity()
    else -> null
}

@Composable
fun ArcFileDropzone(
    onSelectFile: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Sınıf Listesi Fotoğrafı Seçin",
    description: String = "Net ve aydınlık bir basılı liste fotoğrafı yükleyin. Veriler cihaz içinde işlenir.",
    icon: @Composable () -> Unit = {
        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = ArcTheme.colors.accent, modifier = Modifier.size(36.dp))
    },
    onFileDropped: (suspend (Uri) -> Unit)? = null
) {
    val context = LocalContext.current
    val activity = remember(context) { context.arcActivity() }
    val coroutineScope = rememberCoroutineScope()
    val latestDrop = rememberUpdatedState(onFileDropped)
    var dragHover by remember { mutableStateOf(false) }
    val target = remember(activity, coroutineScope) {
        object : DragAndDropTarget {
            override fun onEntered(event: DragAndDropEvent) { dragHover = true }
            override fun onExited(event: DragAndDropEvent) { dragHover = false }
            override fun onEnded(event: DragAndDropEvent) { dragHover = false }
            override fun onDrop(event: DragAndDropEvent): Boolean {
                dragHover = false
                val handler = latestDrop.value ?: return false
                val androidEvent = event.toAndroidDragEvent()
                val uri = androidEvent.clipData?.let { clip ->
                    (0 until clip.itemCount).firstNotNullOfOrNull { clip.getItemAt(it).uri }
                } ?: return false
                // Android grants for cross-application URIs may be transient.
                // Keep them until the app has finished reading, not only until drop.
                val grant = runCatching { activity?.requestDragAndDropPermissions(androidEvent) }.getOrNull()
                coroutineScope.launch {
                    try { handler(uri) } finally { grant?.release() }
                }
                return true
            }
        }
    }
    val receiving = if (onFileDropped == null) Modifier else Modifier.dragAndDropTarget(
        shouldStartDragAndDrop = { event ->
            event.mimeTypes().any { type ->
                type.startsWith("image/") || type == "text/uri-list" || type == "application/octet-stream"
            }
        },
        target = target
    )
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(receiving)
            .clip(ArcTheme.shapes.panel)
            .clickable { onSelectFile() },
        shape = ArcTheme.shapes.panel,
        color = if (dragHover) ArcTheme.colors.surfaceMuted else ArcTheme.colors.surfaceRaised,
        border = BorderStroke(1.5.dp, if (dragHover) ArcTheme.colors.accent else ArcTheme.colors.borderStrong)
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                modifier = Modifier.size(64.dp),
                shape = CircleShape,
                color = ArcTheme.colors.surfaceMuted
            ) {
                Box(contentAlignment = Alignment.Center) {
                    icon()
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ArcTheme.colors.foreground)
            Spacer(Modifier.height(4.dp))
            Text(
                text = description,
                fontSize = 13.sp,
                color = ArcTheme.colors.textSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
            Spacer(Modifier.height(16.dp))
            ArcButton(onClick = onSelectFile, variant = ArcButtonVariant.Secondary, size = ArcButtonSize.Sm) {
                Text("Dosya / Galeri Aç")
            }
        }
    }
}

/**
 * Arc File Upload: selected local file item card with name, size, and clear action.
 */
@Composable
fun ArcFileUpload(
    fileName: String,
    fileSizeString: String,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
    thumbnail: Bitmap? = null
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = ArcTheme.shapes.control,
        color = ArcTheme.colors.surfaceRaised,
        border = BorderStroke(1.dp, ArcTheme.colors.borderSubtle)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (thumbnail != null) {
                Image(
                    bitmap = thumbnail.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(ArcTheme.shapes.small),
                    contentScale = ContentScale.Crop
                )
            } else {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = ArcTheme.shapes.small,
                    color = ArcTheme.colors.surfaceMuted
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Description, contentDescription = null, tint = ArcTheme.colors.accent)
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(fileName, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = ArcTheme.colors.foreground)
                Text(fileSizeString, fontSize = 12.sp, color = ArcTheme.colors.textMuted)
            }
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Kaldır",
                tint = ArcTheme.colors.textMuted,
                modifier = Modifier
                    .size(18.dp)
                    .clickable { onClear() }
            )
        }
    }
}

/**
 * Arc Image Compare:
 * Dual image comparison slider (e.g. Scanned printed list original photo vs parsed OCR table visualization).
 * The user drags the handle to peel back one image and inspect the other.
 */
@Composable
fun ArcImageCompare(
    beforeContent: @Composable () -> Unit,
    afterContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    beforeLabel: String = "Orijinal Görsel",
    afterLabel: String = "OCR Algılama"
) {
    var splitFraction by remember { mutableFloatStateOf(0.5f) }
    val density = LocalDensity.current

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp),
        shape = ArcTheme.shapes.panel,
        border = BorderStroke(1.dp, ArcTheme.colors.borderSubtle)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val widthPx = with(density) { maxWidth.toPx() }
            val splitPx = widthPx * splitFraction

            // After content (Background full)
            Box(modifier = Modifier.fillMaxSize()) {
                afterContent()
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp),
                    shape = ArcTheme.shapes.pill,
                    color = Color.Black.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = afterLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Before content (Clipped to split fraction)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(with(density) { splitPx.toDp() })
                    .clipToBounds()
            ) {
                Box(modifier = Modifier.width(this@BoxWithConstraints.maxWidth).fillMaxHeight()) {
                    beforeContent()
                }
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp),
                    shape = ArcTheme.shapes.pill,
                    color = Color.Black.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = beforeLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Draggable divider line & handle
            Box(
                modifier = Modifier
                    .offset { IntOffset((splitPx - with(density) { 12.dp.toPx() }).roundToInt(), 0) }
                    .width(24.dp)
                    .fillMaxHeight()
                    .pointerInput(widthPx) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            // Apply each incremental delta to the latest state, not a
                            // splitPx captured at the beginning of the gesture.
                            splitFraction = (splitFraction + dragAmount.x / widthPx)
                                .coerceIn(0.05f, 0.95f)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(Color.White)
                )
                Surface(
                    modifier = Modifier
                        .size(32.dp)
                        .shadow(4.dp, CircleShape),
                    shape = CircleShape,
                    color = Color.White
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Compare,
                            contentDescription = "Karşılaştır",
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
