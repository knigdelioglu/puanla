package com.knigdelioglu.puanla.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Grading
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.knigdelioglu.arc.compose.controls.ArcExpandingButtonGroup
import com.knigdelioglu.arc.compose.navigation.ArcResizablePanels

/** Presentation mode only; each caller still owns its session, records and undo history. */
internal enum class ScoringTargetMode { INDIVIDUAL, GROUP }

/**
 * One layout contract for individual and group scoring.
 *
 * The first two panes receive an onSelected callback to navigate to scoring
 * on narrow screens; on wide tablets all panes remain visible at once.
 * Nothing in this component reads or mutates grading data.
 */
@Composable
internal fun UnifiedScoringLayout(
    mode: ScoringTargetMode,
    contextKey: String,
    modifier: Modifier = Modifier,
    targetPane: @Composable (onSelected: () -> Unit) -> Unit,
    criteriaPane: @Composable (onSelected: () -> Unit) -> Unit,
    scoringPane: @Composable () -> Unit
) {
    // Keep compact navigation across temporary wide layouts (rotation,
    // split-screen resizing and DeX), but reset for a different session/mode.
    var selectedTab by rememberSaveable(mode, contextKey) { mutableIntStateOf(0) }
    BoxWithConstraints(modifier = modifier) {
        if (maxWidth >= 900.dp) {
            ArcResizablePanels(
                modifier = Modifier.fillMaxSize().testTag("unifiedScoringLayoutWide"),
                initialWeights = listOf(0.25f, 0.25f, 0.50f),
                minWeights = listOf(0.20f, 0.20f, 0.30f),
                firstPane = { targetPane({}) },
                secondPane = { criteriaPane({}) },
                thirdPane = scoringPane
            )
        } else {
            // The tab is scoped to mode + classroom/rubric/session, not a target:
            // "Next student/group" must not throw the teacher out of scoring.
            Column(modifier = Modifier.fillMaxSize().testTag("unifiedScoringLayoutCompact")) {
                ArcExpandingButtonGroup(
                    items = listOf(
                        (if (mode == ScoringTargetMode.GROUP) "Gruplar" else "Öğrenciler") to {
                            Icon(
                                if (mode == ScoringTargetMode.GROUP) Icons.Default.Group
                                else Icons.Default.FormatListNumbered,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        "Ölçütler" to {
                            Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(18.dp))
                        },
                        "Puanlama" to {
                            Icon(Icons.Default.Grading, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    ),
                    selectedIndex = selectedTab,
                    onSelectIndex = { selectedTab = it },
                    modifier = Modifier.padding(12.dp)
                )
                Box(modifier = Modifier.weight(1f)) {
                    when (selectedTab) {
                        0 -> targetPane { selectedTab = 2 }
                        1 -> criteriaPane { selectedTab = 2 }
                        else -> scoringPane()
                    }
                }
            }
        }
    }
}
