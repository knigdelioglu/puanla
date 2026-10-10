package com.knigdelioglu.puanla

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.knigdelioglu.arc.compose.display.ArcToastMessage
import com.knigdelioglu.arc.compose.display.ArcToastStack
import com.knigdelioglu.arc.compose.foundation.ArcTheme
import com.knigdelioglu.arc.compose.navigation.ArcTabs
import com.knigdelioglu.puanla.ui.components.AppCommandPaletteModal
import com.knigdelioglu.puanla.ui.components.AppTopBar
import com.knigdelioglu.puanla.ui.screens.CatalogGalleryScreen
import com.knigdelioglu.puanla.ui.screens.ClassroomsScreen
import com.knigdelioglu.puanla.ui.screens.GroupWorkScreen
import com.knigdelioglu.puanla.ui.screens.OcrImportScreen
import com.knigdelioglu.puanla.ui.screens.ReportsScreen
import com.knigdelioglu.puanla.ui.screens.ScoringWorkspaceScreen
import com.knigdelioglu.puanla.ui.screens.SettingsScreen
import com.knigdelioglu.puanla.ui.viewmodel.AppDestination
import com.knigdelioglu.puanla.ui.viewmodel.PuanlaViewModel

/**
 * Puanla Tablet Native Uygulaması:
 * Arc Library'nin 99 bileşenini içeren, tablet öncelikli Jetpack Compose çalışma alanı.
 */
@Composable
fun PuanlaApp(
    viewModel: PuanlaViewModel = viewModel()
) {
    val darkModePreference by viewModel.darkMode.collectAsState()
    val reduceMotion by viewModel.reduceMotion.collectAsState()
    val isSystemDark = isSystemInDarkTheme()
    val isDark = darkModePreference ?: isSystemDark

    val currentDestination by viewModel.currentDestination.collectAsState()
    val toastMessages by viewModel.toastMessages.collectAsState()

    val destinations = remember { AppDestination.values() }
    val destinationLabels = remember { destinations.map { it.label } }

    val arcToastMessages = remember(toastMessages) {
        toastMessages.map { (text, undo) ->
            ArcToastMessage(
                title = text,
                undoAction = undo
            )
        }
    }

    ArcTheme(
        darkTheme = isDark,
        reducedMotion = reduceMotion
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = ArcTheme.colors.background
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Scaffold(
                    topBar = {
                        Column {
                            AppTopBar(viewModel = viewModel)
                            ArcTabs(
                                tabs = destinationLabels,
                                selectedTabIndex = destinations.indexOf(currentDestination).coerceAtLeast(0),
                                onTabSelected = { index ->
                                    viewModel.currentDestination.value = destinations[index]
                                },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (currentDestination) {
                            AppDestination.WORKSPACE -> ScoringWorkspaceScreen(viewModel = viewModel)
                            AppDestination.CLASSROOMS -> ClassroomsScreen(viewModel = viewModel)
                            AppDestination.OCR_IMPORT -> OcrImportScreen(viewModel = viewModel)
                            AppDestination.GROUP_WORK -> GroupWorkScreen(viewModel = viewModel)
                            AppDestination.REPORTS -> ReportsScreen(viewModel = viewModel)
                            AppDestination.SETTINGS -> SettingsScreen(viewModel = viewModel)
                            AppDestination.CATALOG -> CatalogGalleryScreen()
                        }
                    }
                }

                // Global Toast Stack with Undo
                if (arcToastMessages.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(24.dp)
                    ) {
                        ArcToastStack(
                            toasts = arcToastMessages,
                            onDismissToast = { viewModel.dismissToast() }
                        )
                    }
                }

                // Global Command Palette Modal (Cmd+K)
                AppCommandPaletteModal(viewModel = viewModel)
            }
        }
    }
}
