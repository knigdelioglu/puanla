package com.knigdelioglu.puanla.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.knigdelioglu.arc.compose.controls.ArcButton
import com.knigdelioglu.arc.compose.controls.ArcButtonSize
import com.knigdelioglu.arc.compose.controls.ArcButtonVariant
import com.knigdelioglu.arc.compose.controls.ArcElasticSlider
import com.knigdelioglu.arc.compose.controls.ArcNumberField
import com.knigdelioglu.arc.compose.foundation.ArcTheme
import com.knigdelioglu.puanla.data.local.CriterionEntity
import com.knigdelioglu.puanla.data.local.CriterionLevelEntity
import com.knigdelioglu.puanla.ui.preferences.QuickGradingPreference
import kotlin.math.roundToInt

/**
 * Only a committed quick-score tap advances to the next criterion.
 * Opening precision controls or editing an observation note suspends auto advance
 * without changing the teacher's saved preference.
 */
internal fun shouldAdvanceAfterQuickScore(
    autoAdvance: Boolean,
    hasNextCriterion: Boolean,
    precisionOpen: Boolean,
    noteEditorOpen: Boolean
): Boolean = autoAdvance && hasNextCriterion && !precisionOpen && !noteEditorOpen

/**
 * Shared grading UI for student and group scores.
 *
 * The caller supplies immutable context identity and delegates persistence.
 * Scoring here never writes to Room directly; there is no mixing of group
 * member projections with individual records.
 */
@Composable
internal fun CriterionGradingEditor(
    contextKey: String,
    criterion: CriterionEntity,
    points: Int?,
    levels: List<CriterionLevelEntity>,
    quickGrading: QuickGradingPreference,
    autoAdvance: Boolean,
    onAutoAdvanceChange: (Boolean) -> Unit,
    hasNextCriterion: Boolean,
    noteEditorOpen: Boolean = false,
    onNextCriterion: () -> Unit,
    onScore: (Int) -> Unit,
    onClearScore: () -> Unit
) {
    var precisionVisible by remember(contextKey) { mutableStateOf(false) }
    val advancementPaused = precisionVisible || noteEditorOpen

    Column(modifier = Modifier.fillMaxWidth()) {
        QuickScoreControls(
            maxPoints = criterion.maxPoints,
            points = points,
            levels = levels,
            autoAdvance = autoAdvance,
            onAutoAdvanceChange = onAutoAdvanceChange,
            hasNextCriterion = hasNextCriterion,
            onNextCriterion = onNextCriterion,
            advancePaused = advancementPaused,
            onChooseScore = { chosen ->
                onScore(chosen)
                if (shouldAdvanceAfterQuickScore(
                        autoAdvance = autoAdvance,
                        hasNextCriterion = hasNextCriterion,
                        precisionOpen = precisionVisible,
                        noteEditorOpen = noteEditorOpen
                    )) onNextCriterion()
            },
            quickGrading = quickGrading
        )

        Spacer(Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ArcButton(
                text = if (precisionVisible) "Hassas puanı gizle" else "Hassas puan",
                onClick = { precisionVisible = !precisionVisible },
                modifier = Modifier.weight(1f),
                size = ArcButtonSize.Sm,
                variant = ArcButtonVariant.Outline
            )
            ArcButton(
                text = "Puanı kaldır",
                onClick = onClearScore,
                enabled = points != null,
                size = ArcButtonSize.Sm,
                variant = ArcButtonVariant.Ghost
            )
        }

        if (precisionVisible) {
            Spacer(Modifier.height(12.dp))
            // Sliding is a preview only; the caller persists on thumb release.
            var preview by remember(contextKey) { mutableFloatStateOf((points ?: 0).toFloat()) }
            LaunchedEffect(contextKey, points) { preview = (points ?: 0).toFloat() }

            if (criterion.maxPoints > 0) {
                ArcElasticSlider(
                    value = preview,
                    onValueChange = { preview = it },
                    onValueCommit = { onScore(it.roundToInt()) },
                    step = 1f,
                    valueRange = 0f..criterion.maxPoints.toFloat(),
                    label = "Hassas puan",
                    formatValue = { "${it.roundToInt()} / ${criterion.maxPoints}" }
                )
            }
            ArcNumberField(
                value = points,
                onValueChange = { value ->
                    if (value == null) onClearScore() else onScore(value)
                },
                min = 0,
                max = criterion.maxPoints,
                label = "Tam sayı"
            )
            if (hasNextCriterion && autoAdvance) {
                Text(
                    "Hassas giriş sırasında otomatik ilerleme duraklatıldı.",
                    color = ArcTheme.colors.textSecondary
                )
            }
        }
    }
}
