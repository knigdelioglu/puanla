package com.knigdelioglu.puanla.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import com.knigdelioglu.arc.compose.navigation.ArcCommandAction
import com.knigdelioglu.arc.compose.navigation.ArcCommandPalette
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
    val scope = rememberCoroutineScope()

    val actions = mutableListOf<ArcCommandAction>()

    // Navigation Actions
    actions.add(
        ArcCommandAction(
            id = "nav_workspace",
            title = "Değerlendirme Çalışma Alanına Git",
            category = "Gezinme",
            shortcut = "1",
            onExecute = { viewModel.currentDestination.value = AppDestination.WORKSPACE }
        )
    )
    actions.add(
        ArcCommandAction(
            id = "nav_classrooms",
            title = "Sınıflar & Öğrenciler Ekranına Git",
            category = "Gezinme",
            shortcut = "2",
            onExecute = { viewModel.currentDestination.value = AppDestination.CLASSROOMS }
        )
    )
    actions.add(
        ArcCommandAction(
            id = "nav_ocr",
            title = "Fotoğraftan Liste Aktarımı (OCR)",
            category = "Gezinme",
            shortcut = "3",
            onExecute = { viewModel.currentDestination.value = AppDestination.OCR_IMPORT }
        )
    )
    actions.add(
        ArcCommandAction(
            id = "nav_group",
            title = "Grup Çalışmaları & Hazırlık Takibi",
            category = "Gezinme",
            shortcut = "4",
            onExecute = { viewModel.currentDestination.value = AppDestination.GROUP_WORK }
        )
    )
    actions.add(
        ArcCommandAction(
            id = "nav_reports",
            title = "Raporlar & Analiz / CSV Dışa Aktar",
            category = "Gezinme",
            shortcut = "5",
            onExecute = { viewModel.currentDestination.value = AppDestination.REPORTS }
        )
    )
    actions.add(
        ArcCommandAction(
            id = "nav_settings",
            title = "Ayarlar & Veri Güvenliği",
            category = "Gezinme",
            shortcut = "6",
            onExecute = { viewModel.currentDestination.value = AppDestination.SETTINGS }
        )
    )
    actions.add(
        ArcCommandAction(
            id = "nav_catalog",
            title = "Arc Compose 99 Bileşen Kataloğu",
            category = "Gezinme",
            shortcut = "7",
            onExecute = { viewModel.currentDestination.value = AppDestination.CATALOG }
        )
    )

    // Quick System Actions
    actions.add(
        ArcCommandAction(
            id = "action_toggle_dark",
            title = if (darkMode == true) "Aydınlık Moda Geç" else "Karanlık Moda Geç",
            category = "Ayarlar",
            onExecute = { viewModel.darkMode.value = !(darkMode ?: false) }
        )
    )
    actions.add(
        ArcCommandAction(
            id = "action_undo",
            title = "Son Puanı Geri Al",
            category = "İşlemler",
            shortcut = "Cmd+Z",
            onExecute = {
                scope.launch { viewModel.undoLastAction() }
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
    rubrics.forEach { rubric ->
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
