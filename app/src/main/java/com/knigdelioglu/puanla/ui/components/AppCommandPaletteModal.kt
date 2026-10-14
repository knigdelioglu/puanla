package com.knigdelioglu.puanla.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import com.knigdelioglu.arc.compose.navigation.ArcCommandAction
import com.knigdelioglu.arc.compose.navigation.ArcCommandPalette
import com.knigdelioglu.arc.compose.foundation.ArcTheme
import com.knigdelioglu.puanla.data.local.fullName
import com.knigdelioglu.puanla.ui.viewmodel.AppDestination
import com.knigdelioglu.puanla.ui.viewmodel.PuanlaViewModel
import kotlinx.coroutines.launch

@Composable
fun AppCommandPaletteModal(viewModel: PuanlaViewModel) {
    val isOpen by viewModel.isCommandPaletteOpen.collectAsState()
    val classrooms by viewModel.classrooms.collectAsState()
    val rubrics by viewModel.rubrics.collectAsState()
    val students by viewModel.students.collectAsState()
    val darkMode by viewModel.darkMode.collectAsState()
    val selectedClassroom by viewModel.selectedClassroom.collectAsState()
    val scope = rememberCoroutineScope()

    val actions = mutableListOf<ArcCommandAction>()

    // Navigation Actions
    actions.add(
        ArcCommandAction(
            id = "nav_workspace",
            title = "Puanlamaya Git (Bireysel)",
            category = "Gezinme",
            onExecute = { viewModel.currentDestination.value = AppDestination.WORKSPACE }
        )
    )
    actions.add(
        ArcCommandAction(
            id = "nav_classrooms",
            title = "Sınıflara Git",
            category = "Gezinme",
            onExecute = { viewModel.currentDestination.value = AppDestination.CLASSROOMS }
        )
    )
    actions.add(
        ArcCommandAction(
            id = "nav_group",
            title = "Puanlama / Grup Çalışmaları",
            category = "Gezinme",
            onExecute = { viewModel.currentDestination.value = AppDestination.GROUP_WORK }
        )
    )
    actions.add(
        ArcCommandAction(
            id = "nav_reports",
            title = "Raporlara Git / CSV Dışa Aktar",
            category = "Gezinme",
            onExecute = { viewModel.currentDestination.value = AppDestination.REPORTS }
        )
    )
    actions.add(
        ArcCommandAction(
            id = "nav_settings",
            title = "Ayarlar",
            category = "Gezinme",
            onExecute = { viewModel.currentDestination.value = AppDestination.SETTINGS }
        )
    )

    // Quick System Actions
    actions.add(
        ArcCommandAction(
            id = "action_toggle_dark",
            title = when (darkMode) {
                true -> "Aydınlık Moda Geç"
                false -> "Karanlık Moda Geç"
                null -> "Sistem temasını değiştir (${if (ArcTheme.colors.isDark) "koyu" else "açık"} → ${if (ArcTheme.colors.isDark) "açık" else "koyu"})"
            },
            category = "Ayarlar",
            onExecute = viewModel::toggleDarkMode
        )
    )
    actions.add(
        ArcCommandAction(
            id = "action_undo",
            title = "Son Puanı Geri Al",
            category = "İşlemler",
            onExecute = {
                scope.launch {
                    if (viewModel.currentDestination.value == AppDestination.GROUP_WORK) {
                        viewModel.undoLastGroupAction()
                    } else viewModel.undoLastAction()
                }
            }
        )
    )

    // Classrooms
    classrooms.forEach { cls ->
        actions.add(
            ArcCommandAction(
                id = "cls_${cls.id}",
                title = "Sınıf Seç: ${cls.name}",
                category = "Sınıflar",
                onExecute = {
                    viewModel.selectClassroom(cls)
                    viewModel.currentDestination.value = AppDestination.WORKSPACE
                }
            )
        )
    }

    // Rubrics
    rubrics.filter { rubric -> selectedClassroom?.grade == rubric.grade }.forEach { rubric ->
        actions.add(
            ArcCommandAction(
                id = "rubric_${rubric.id}",
                title = "Rubrik Seç: ${rubric.title}",
                category = "Rubrikler",
                onExecute = {
                    viewModel.selectRubric(rubric)
                    viewModel.currentDestination.value = AppDestination.WORKSPACE
                }
            )
        )
    }

    // Students in active classroom
    students.forEach { std ->
        actions.add(
            ArcCommandAction(
                id = "std_${std.id}",
                title = "Öğrenci Seç: ${std.fullName} (${std.studentNumber})",
                category = "Öğrenciler",
                onExecute = {
                    viewModel.selectStudent(std)
                    viewModel.currentDestination.value = AppDestination.WORKSPACE
                }
            )
        )
    }

    ArcCommandPalette(
        isOpen = isOpen,
        onClose = { viewModel.isCommandPaletteOpen.value = false },
        actions = actions
    )
}
