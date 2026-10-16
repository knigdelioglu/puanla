package com.knigdelioglu.puanla.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.knigdelioglu.arc.compose.display.ArcBadge
import com.knigdelioglu.arc.compose.display.ArcBadgeVariant
import com.knigdelioglu.arc.compose.display.ArcEmptyState
import com.knigdelioglu.arc.compose.foundation.ArcTheme
import com.knigdelioglu.puanla.data.local.CriterionEntity

/** A single rubric explorer is used by both individual and group grading. */
@Composable
internal fun CriteriaExplorerPane(
    criteria: List<CriterionEntity>,
    activeCriterion: CriterionEntity?,
    scoreSummary: Map<String, String> = emptyMap(),
    onSelectCriterion: (CriterionEntity) -> Unit
) {
    val colors = ArcTheme.colors

    Surface(modifier = Modifier.fillMaxSize(), color = colors.surface) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Değerlendirme Ölçütleri",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.foreground
                )
                ArcBadge(text = "${criteria.size} Ölçüt", variant = ArcBadgeVariant.Accent)
            }

            Spacer(Modifier.height(12.dp))

            if (criteria.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    ArcEmptyState(
                        title = "Ölçüt Bulunamadı",
                        description = "Seçili rubrikte değerlendirilecek ölçüt bulunmuyor."
                    )
                }
            } else {
                BoxWithConstraints(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    val spacing = 6.dp
                    val minimumItemHeight = 56.dp * LocalDensity.current.fontScale.coerceAtLeast(1f)
                    // Show every criterion at once on tablets when the available height permits.
                    // Large system fonts and smaller windows fall back to scrolling.
                    val fitsAtOnce = maxHeight >= minimumItemHeight * criteria.size +
                        spacing * (criteria.size - 1)

                    if (fitsAtOnce) {
                        Column(
                            modifier = Modifier.fillMaxSize().testTag("rubricCriterionList"),
                            verticalArrangement = Arrangement.spacedBy(spacing)
                        ) {
                            criteria.forEach { criterion ->
                                CriterionSelectionRow(
                                    criterion = criterion,
                                    isActive = criterion.id == activeCriterion?.id,
                                    scoreSummary = scoreSummary[criterion.id],
                                    onClick = { onSelectCriterion(criterion) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().testTag("rubricCriterionList"),
                            verticalArrangement = Arrangement.spacedBy(spacing)
                        ) {
                            items(criteria, key = { it.id }) { criterion ->
                                CriterionSelectionRow(
                                    criterion = criterion,
                                    isActive = criterion.id == activeCriterion?.id,
                                    scoreSummary = scoreSummary[criterion.id],
                                    onClick = { onSelectCriterion(criterion) },
                                    modifier = Modifier.heightIn(min = minimumItemHeight)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CriterionSelectionRow(
    criterion: CriterionEntity,
    isActive: Boolean,
    scoreSummary: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ArcTheme.colors
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("rubricCriterion-${criterion.id}")
            .semantics { selected = isActive }
            .clickable(role = Role.Button, onClickLabel = "Ölçütü seç", onClick = onClick),
        shape = ArcTheme.shapes.control,
        color = if (isActive) colors.surfaceRaised else colors.surface,
        border = BorderStroke(
            if (isActive) 2.dp else 1.dp,
            if (isActive) colors.accent else colors.borderSubtle
        )
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${criterion.orderIndex + 1}. ${criterion.title}",
                fontSize = 15.sp,
                lineHeight = 19.sp,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                color = colors.foreground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (scoreSummary != null) {
                Spacer(Modifier.width(8.dp))
                Text(scoreSummary, fontSize = 12.sp, color = colors.textSecondary,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
