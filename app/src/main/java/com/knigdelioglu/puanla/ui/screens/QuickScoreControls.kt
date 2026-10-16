package com.knigdelioglu.puanla.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.knigdelioglu.arc.compose.controls.ArcButton
import com.knigdelioglu.arc.compose.controls.ArcButtonSize
import com.knigdelioglu.arc.compose.controls.ArcButtonVariant
import com.knigdelioglu.arc.compose.controls.ArcRadioCards
import com.knigdelioglu.arc.compose.controls.ArcSegmentedControl
import com.knigdelioglu.arc.compose.controls.ArcSwitch
import com.knigdelioglu.arc.compose.foundation.ArcTheme
import com.knigdelioglu.puanla.data.local.CriterionLevelEntity
import com.knigdelioglu.puanla.domain.scoring.quickScorePresets
import com.knigdelioglu.puanla.ui.preferences.QuickGradingPreference
import com.knigdelioglu.puanla.ui.preferences.qualitativeQuickOptions

/** Shared quick-score controls; advancing is coordinated by CriterionGradingEditor. */
@Composable
internal fun QuickScoreControls(
    maxPoints: Int,
    points: Int?,
    levels: List<CriterionLevelEntity>,
    autoAdvance: Boolean,
    onAutoAdvanceChange: (Boolean) -> Unit,
    hasNextCriterion: Boolean,
    onNextCriterion: () -> Unit,
    advancePaused: Boolean = false,
    onChooseScore: (Int) -> Unit,
    quickGrading: QuickGradingPreference = QuickGradingPreference()
) {
    val colors = ArcTheme.colors
    val options = remember(maxPoints, levels) {
        quickScorePresets(maxPoints, levels.map { it.points })
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            if (quickGrading.qualitativeEnabled) "Niteliksel hızlı puanlama"
            else if (levels.isNotEmpty()) "Hızlı düzey seçimi"
            else "Hızlı puan • tek dokunuş",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.textSecondary
        )
        Spacer(Modifier.height(7.dp))
        if (quickGrading.qualitativeEnabled) {
            val choices = remember(maxPoints, quickGrading.levelCount, quickGrading.appearance) {
                qualitativeQuickOptions(
                    maxPoints = maxPoints,
                    levelCount = quickGrading.levelCount,
                    appearance = quickGrading.appearance
                )
            }
            ArcRadioCards(
                options = choices,
                selectedOption = choices.firstOrNull { points != null && it.points == points },
                onOptionSelected = { onChooseScore(it.points) },
                modifier = Modifier.fillMaxWidth().testTag("qualitativeQuickScoreOptions"),
                titleProvider = { it.title(quickGrading.appearance) },
                badgeProvider = { "${it.points} / $maxPoints" }
            )
        } else {
            ArcSegmentedControl(
                items = options.map { "$it P" },
                selectedIndex = options.indexOf(points),
                onSelectIndex = { index -> options.getOrNull(index)?.let(onChooseScore) },
                modifier = Modifier.fillMaxWidth().testTag("quickScoreOptions")
            )
        }
        Spacer(Modifier.height(7.dp))
        ArcSwitch(
            checked = autoAdvance,
            onCheckedChange = onAutoAdvanceChange,
            label = "Otomatik sonraki ölçüt"
        )
        if (hasNextCriterion && (!autoAdvance || advancePaused)) {
            ArcButton(
                text = "Sonraki ölçüt",
                onClick = onNextCriterion,
                modifier = Modifier.fillMaxWidth(),
                variant = ArcButtonVariant.Ghost,
                size = ArcButtonSize.Sm
            )
        }
    }
}
