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
import androidx.compose.material.icons.filled.History
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
import com.knigdelioglu.arc.compose.navigation.ArcBottomSheet
import com.knigdelioglu.arc.compose.display.ArcTooltip
import com.knigdelioglu.arc.compose.display.ArcBadge
import com.knigdelioglu.arc.compose.display.ArcBadgeVariant
import com.knigdelioglu.arc.compose.display.ArcThemeSwitch
import com.knigdelioglu.arc.compose.navigation.ArcBreadcrumb
import com.knigdelioglu.arc.compose.navigation.ArcPageHeader
import com.knigdelioglu.puanla.ui.viewmodel.AppDestination
import com.knigdelioglu.puanla.ui.viewmodel.PuanlaViewModel

@Composable
fun AppTopBar(viewModel: PuanlaViewModel) {
    val currentDestination by viewModel.currentDestination.collectAsState()
    val classrooms by viewModel.classrooms.collectAsState()
    val selectedClassroom by viewModel.selectedClassroom.collectAsState()
    val rubrics by viewModel.rubrics.collectAsState()
    val selectedRubric by viewModel.selectedRubric.collectAsState()
    val darkMode by viewModel.darkMode.collectAsState()
    var showRubricCards by remember { mutableStateOf(false) }
    val matchingRubrics = rubrics.filter { it.grade == selectedClassroom?.grade }

    val breadcrumbItems = mutableListOf("Puanla")
    selectedClassroom?.let { breadcrumbItems.add(it.name) }
    selectedRubric?.let { breadcrumbItems.add(it.title) }

    ArcPageHeader(
        title = when (currentDestination) {
            AppDestination.WORKSPACE -> selectedRubric?.title ?: "Değerlendirme"
            AppDestination.CLASSROOMS -> "Sınıflar & Öğrenciler"
            AppDestination.OCR_IMPORT -> "Fotoğraftan Aktarım (OCR)"
            AppDestination.GROUP_WORK -> "Grup Çalışmaları"
            AppDestination.REPORTS -> "Raporlar & Analiz"
            AppDestination.SETTINGS -> "Ayarlar & Güvenlik"
            AppDestination.CATALOG -> "Arc Bileşen Kataloğu"
        },
        breadcrumb = {
            ArcBreadcrumb(items = breadcrumbItems)
        },
        statusBadge = {
            ArcBadge(text = "Çevrimdışı Güvenli", variant = ArcBadgeVariant.Success)
        },
        actions = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Classroom Selector
                ArcSelect(
                    selectedItem = selectedClassroom,
                    items = classrooms,
                    onItemSelected = { viewModel.selectClassroom(it) },
                    labelProvider = { it.name },
                    placeholder = "Sınıf Seç",
                    modifier = Modifier.width(115.dp)
                )

                // Rubric Selector
                ArcSelect(
                    selectedItem = selectedRubric,
                    items = matchingRubrics,
                    onItemSelected = { viewModel.selectRubric(it) },
                    labelProvider = { it.title },
                    placeholder = "Rubrik Seç",
                    modifier = Modifier.width(180.dp)
                )

                // Tablet/compact fallback for long rubric titles: real Arc radio-card picker.
                ArcButton(
                    text = "Rubrikler",
                    onClick = { showRubricCards = true },
                    variant = ArcButtonVariant.Outline,
                    size = ArcButtonSize.Sm,
                    enabled = selectedClassroom != null && matchingRubrics.isNotEmpty()
                )

                // Command Palette Shortcut Button
                ArcButton(
                    text = "Ctrl/⌘+K Ara",
                    onClick = { viewModel.isCommandPaletteOpen.value = true },
                    variant = ArcButtonVariant.Secondary,
                    size = ArcButtonSize.Sm
                )

                // Real local audit events, not simulated notifications.
                ArcTooltip(tooltipText = "Yerel işlem geçmişini aç") {
                    ArcActionButton(
                        onClick = { viewModel.isNotificationCenterOpen.value = true },
                        size = ArcButtonSize.Sm
                    ) {
                        Icon(Icons.Default.History, contentDescription = "İşlem Geçmişini Aç")
                    }
                }

                // Theme Switch
                ArcThemeSwitch(
                    isDark = darkMode ?: false,
                    onToggle = { viewModel.setDarkMode(it) }
                )
            }
        }
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
