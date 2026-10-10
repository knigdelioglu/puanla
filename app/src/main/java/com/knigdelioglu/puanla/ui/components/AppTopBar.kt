package com.knigdelioglu.puanla.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
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
                    items = rubrics.filter { it.grade == selectedClassroom?.grade },
                    onItemSelected = { viewModel.selectRubric(it) },
                    labelProvider = { it.title },
                    placeholder = "Rubrik Seç",
                    modifier = Modifier.width(180.dp)
                )

                // Command Palette Shortcut Button
                ArcButton(
                    text = "Ctrl/⌘+K Ara",
                    onClick = { viewModel.isCommandPaletteOpen.value = true },
                    variant = ArcButtonVariant.Secondary,
                    size = ArcButtonSize.Sm
                )

                // Real local audit events, not simulated notifications.
                ArcActionButton(
                    onClick = { viewModel.isNotificationCenterOpen.value = true },
                    size = ArcButtonSize.Sm
                ) {
                    Icon(Icons.Default.History, contentDescription = "İşlem Geçmişini Aç")
                }

                // Theme Switch
                ArcThemeSwitch(
                    isDark = darkMode ?: false,
                    onToggle = { viewModel.darkMode.value = it }
                )
            }
        }
    )
}
