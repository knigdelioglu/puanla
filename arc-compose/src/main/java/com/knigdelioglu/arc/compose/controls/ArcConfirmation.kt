package com.knigdelioglu.arc.compose.controls

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.knigdelioglu.arc.compose.foundation.ArcTheme
import kotlinx.coroutines.launch

/**
 * Arc Hold to Confirm: long-press button with filling progress ring to protect critical irreversible actions (e.g., delete classroom, restore backup).
 */
@Composable
fun ArcHoldToConfirm(
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Silmek için basılı tutun",
    confirmedLabel: String = "Tamamlandı",
    holdDurationMillis: Int = 1400,
    variant: ArcButtonVariant = ArcButtonVariant.Danger
) {
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var isConfirmed by remember { mutableStateOf(false) }
    val colors = ArcTheme.colors

    val buttonColor = if (isConfirmed) colors.success else if (variant == ArcButtonVariant.Danger) colors.danger else colors.accent

    Surface(
        modifier = modifier
            .height(48.dp)
            .pointerInput(isConfirmed) {
                if (isConfirmed) return@pointerInput
                detectTapGestures(
                    onPress = {
                        val animJob = scope.launch {
                            progress.animateTo(1f, animationSpec = tween(holdDurationMillis, easing = LinearEasing))
                            isConfirmed = true
                            onConfirm()
                        }
                        try {
                            awaitRelease()
                        } finally {
                            if (!isConfirmed) {
                                animJob.cancel()
                                scope.launch {
                                    progress.animateTo(0f, animationSpec = tween(200))
                                }
                            }
                        }
                    }
                )
            },
        shape = ArcTheme.shapes.control,
        color = buttonColor.copy(alpha = 0.12f),
        border = BorderStroke(1.5.dp, buttonColor)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (!isConfirmed) {
                    CircularProgressIndicator(
                        progress = { progress.value },
                        modifier = Modifier.size(20.dp),
                        color = buttonColor,
                        strokeWidth = 2.5.dp,
                        trackColor = buttonColor.copy(alpha = 0.2f)
                    )
                    Spacer(Modifier.width(10.dp))
                } else {
                    Icon(Icons.Default.Check, contentDescription = null, tint = colors.success, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                }

                Text(
                    text = if (isConfirmed) confirmedLabel else label,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = buttonColor
                )
            }
        }
    }
}

/**
 * Arc Confirm Morph: button that fluidly morphs into inline "Emin misiniz? [Evet] [İptal]" options.
 */
@Composable
fun ArcConfirmMorph(
    prompt: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    initialLabel: String = "Sil",
    confirmLabel: String = "Evet, Sil",
    cancelLabel: String = "Vazgeç",
    variant: ArcButtonVariant = ArcButtonVariant.Danger
) {
    var isConfirming by remember { mutableStateOf(false) }

    Box(modifier = modifier.animateContentSize()) {
        if (!isConfirming) {
            ArcButton(
                onClick = { isConfirming = true },
                variant = variant,
                size = ArcButtonSize.Sm
            ) {
                Text(initialLabel, fontWeight = FontWeight.SemiBold)
            }
        } else {
            Surface(
                shape = ArcTheme.shapes.control,
                color = ArcTheme.colors.surfaceRaised,
                border = BorderStroke(1.dp, ArcTheme.colors.border)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = prompt,
                        fontSize = 13.sp,
                        color = ArcTheme.colors.foreground,
                        fontWeight = FontWeight.Medium
                    )
                    ArcButton(
                        onClick = {
                            isConfirming = false
                            onConfirm()
                        },
                        variant = variant,
                        size = ArcButtonSize.Sm
                    ) {
                        Text(confirmLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    ArcButton(
                        onClick = { isConfirming = false },
                        variant = ArcButtonVariant.Ghost,
                        size = ArcButtonSize.Sm
                    ) {
                        Text(cancelLabel, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
