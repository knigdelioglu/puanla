package com.knigdelioglu.puanla.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.knigdelioglu.arc.compose.controls.ArcActionButton
import com.knigdelioglu.arc.compose.controls.ArcButton
import com.knigdelioglu.arc.compose.controls.ArcButtonSize
import com.knigdelioglu.arc.compose.controls.ArcButtonVariant
import com.knigdelioglu.arc.compose.controls.ArcSelect
import com.knigdelioglu.arc.compose.controls.ArcRadioCards
import com.knigdelioglu.arc.compose.controls.ArcSegmentedControl
import com.knigdelioglu.arc.compose.navigation.ArcBottomSheet
import com.knigdelioglu.arc.compose.display.ArcTooltip
import com.knigdelioglu.arc.compose.display.ArcThemeSwitch
import com.knigdelioglu.arc.compose.display.ArcUserMenu
import com.knigdelioglu.arc.compose.navigation.ArcBreadcrumb
import com.knigdelioglu.arc.compose.navigation.ArcPageHeader
import com.knigdelioglu.puanla.ui.viewmodel.AppDestination
import com.knigdelioglu.puanla.ui.viewmodel.PuanlaViewModel

@Composable
fun AppTopBar(viewModel: PuanlaViewModel, onBackFromSettings: () -> Unit) {
    val currentDestination by viewModel.currentDestination.collectAsState()
    val classrooms by viewModel.classrooms.collectAsState()
    val selectedClassroom by viewModel.selectedClassroom.collectAsState()
    val rubrics by viewModel.rubrics.collectAsState()
    val selectedRubric by viewModel.selectedRubric.collectAsState()
    var showRubricCards by remember { mutableStateOf(false) }
    var showEvaluationRuns by remember { mutableStateOf(false) }
    val selectedRun by viewModel.selectedRun.collectAsState()
    val matchingRubrics = rubrics.filter { it.grade == selectedClassroom?.grade }

    ArcPageHeader(
        title = when (currentDestination) {
            AppDestination.WORKSPACE, AppDestination.GROUP_WORK ->
                selectedRubric?.title ?: "Puanlama"
            AppDestination.CLASSROOMS -> "Sınıflar"
            AppDestination.OCR_IMPORT -> "Fotoğraftan Aktarım"
            AppDestination.REPORTS -> "Raporlar"
            AppDestination.SETTINGS -> "Ayarlar"
        },
        actions = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Keep the classroom selector where the active context is useful.
                if (currentDestination != AppDestination.SETTINGS) {
                ArcSelect(
                    selectedItem = selectedClassroom,
                    items = classrooms,
                    onItemSelected = { viewModel.selectClassroom(it) },
                    labelProvider = { it.name },
                    placeholder = "Sınıf Seç",
                    modifier = Modifier.width(115.dp)
                )
                }

                // One rubric picker: the selected rubric is already visible in the title.
                if (currentDestination == AppDestination.WORKSPACE ||
                    currentDestination == AppDestination.GROUP_WORK ||
                    currentDestination == AppDestination.REPORTS
                ) {
                ArcButton(
                    text = "Rubrikler",
                    onClick = { showRubricCards = true },
                    variant = ArcButtonVariant.Outline,
                    size = ArcButtonSize.Sm,
                    enabled = selectedClassroom != null && matchingRubrics.isNotEmpty()
                )
                }

                if (currentDestination == AppDestination.WORKSPACE ||
                    currentDestination == AppDestination.REPORTS
                ) {
                    ArcButton(
                        text = selectedRun?.title?.let { if (it.length > 23) it.take(20) + "…" else it }
                            ?: "Değerlendirmeler",
                        onClick = { showEvaluationRuns = true },
                        variant = ArcButtonVariant.Outline,
                        size = ArcButtonSize.Sm,
                        enabled = selectedClassroom != null && selectedRubric != null
                    )
                }

                if (currentDestination == AppDestination.WORKSPACE ||
                    currentDestination == AppDestination.GROUP_WORK
                ) {
                    ArcSegmentedControl(
                        items = listOf("Bireysel", "Grup"),
                        selectedIndex = if (currentDestination == AppDestination.GROUP_WORK) 1 else 0,
                        onSelectIndex = { index ->
                            viewModel.currentDestination.value =
                                if (index == 1) AppDestination.GROUP_WORK else AppDestination.WORKSPACE
                        },
                        modifier = Modifier.width(168.dp)
                    )
                }

                ArcTooltip(tooltipText = "Ara ve komutları aç") {
                    ArcActionButton(onClick = { viewModel.isCommandPaletteOpen.value = true }, size = ArcButtonSize.Sm) {
                        Icon(Icons.Default.Search, contentDescription = "Ara")
                    }
                }
                ArcTooltip(
                    tooltipText = if (currentDestination == AppDestination.SETTINGS)
                        "Önceki sekmeye dön" else "Ayarlar"
                ) {
                    ArcActionButton(
                        onClick = {
                            if (currentDestination == AppDestination.SETTINGS) onBackFromSettings()
                            else viewModel.currentDestination.value = AppDestination.SETTINGS
                        },
                        size = ArcButtonSize.Sm
                    ) {
                        Icon(
                            if (currentDestination == AppDestination.SETTINGS) Icons.Default.ArrowBack
                            else Icons.Default.Settings,
                            contentDescription = if (currentDestination == AppDestination.SETTINGS)
                                "Önceki sekmeye dön" else "Ayarlar"
                        )
                    }
                }
            }
        }
    )
    EvaluationRunSheet(
        viewModel = viewModel,
        isOpen = showEvaluationRuns,
        onDismiss = { showEvaluationRuns = false }
    )
    ArcBottomSheet(
        isOpen = showRubricCards,
        onDismissRequest = { showRubricCards = false }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().heightIn(max = 460.dp).verticalScroll(rememberScrollState())
        ) {
            Text("Rubrik Seçimi", color = com.knigdelioglu.arc.compose.foundation.ArcTheme.colors.foreground)
            ArcRadioCards(
                options = matchingRubrics,
                selectedOption = selectedRubric,
                onOptionSelected = {
                    viewModel.selectRubric(it)
                    showRubricCards = false
                },
                titleProvider = { it.title },
                descriptionProvider = { "${it.grade}. sınıf rubriği" },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
