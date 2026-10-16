package com.knigdelioglu.puanla.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.knigdelioglu.arc.compose.controls.ArcButton
import com.knigdelioglu.arc.compose.controls.ArcButtonSize
import com.knigdelioglu.arc.compose.controls.ArcButtonVariant
import com.knigdelioglu.arc.compose.controls.ArcFloatingButtonGroup

/** The same target-navigation and undo controls for a student or a group. */
@Composable
internal fun ScoringTargetToolbar(
    mode: ScoringTargetMode,
    onPrevious: () -> Unit,
    onUndo: () -> Unit,
    onNext: () -> Unit,
    canPrevious: Boolean = true,
    canUndo: Boolean = true,
    canNext: Boolean = true
) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        ArcFloatingButtonGroup {
            ArcButton(
                text = if (mode == ScoringTargetMode.GROUP) "Önceki Grup" else "Önceki",
                onClick = onPrevious,
                enabled = canPrevious,
                variant = ArcButtonVariant.Outline,
                size = ArcButtonSize.Sm
            )
            ArcButton(
                text = "Geri Al",
                onClick = onUndo,
                enabled = canUndo,
                variant = ArcButtonVariant.Secondary,
                size = ArcButtonSize.Sm
            )
            ArcButton(
                text = if (mode == ScoringTargetMode.GROUP) "Sonraki Grup" else "Sonraki",
                onClick = onNext,
                enabled = canNext,
                variant = ArcButtonVariant.Primary,
                size = ArcButtonSize.Sm
            )
        }
    }
}
