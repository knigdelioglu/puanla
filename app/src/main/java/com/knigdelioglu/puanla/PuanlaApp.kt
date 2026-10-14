package com.knigdelioglu.puanla

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.lifecycleScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.knigdelioglu.arc.compose.display.ArcToastMessage
import com.knigdelioglu.arc.compose.display.ArcToastStack
import com.knigdelioglu.arc.compose.display.ArcNotificationItem
import com.knigdelioglu.arc.compose.display.ArcNotificationCenter
import com.knigdelioglu.arc.compose.controls.ArcButton
import com.knigdelioglu.arc.compose.controls.ArcButtonVariant
import com.knigdelioglu.arc.compose.advanced.ArcAdvancedItem
import com.knigdelioglu.arc.compose.advanced.ArcGlassTabBar
import com.knigdelioglu.arc.compose.foundation.ArcTheme
import com.knigdelioglu.puanla.ui.components.AppCommandPaletteModal
import com.knigdelioglu.puanla.ui.components.KeyboardShortcutMatcher
import com.knigdelioglu.puanla.ui.components.AppTopBar
import com.knigdelioglu.puanla.ui.screens.ClassroomsScreen
import com.knigdelioglu.puanla.ui.screens.GroupWorkScreen
import com.knigdelioglu.puanla.ui.screens.OcrImportScreen
import com.knigdelioglu.puanla.ui.screens.ReportsScreen
import com.knigdelioglu.puanla.ui.screens.ScoringWorkspaceScreen
import com.knigdelioglu.puanla.ui.screens.SettingsScreen
import com.knigdelioglu.puanla.ui.navigation.primaryNavigationDestinations
import com.knigdelioglu.puanla.ui.navigation.primaryTab
import com.knigdelioglu.puanla.ui.viewmodel.AppDestination
import com.knigdelioglu.puanla.ui.viewmodel.PuanlaViewModel

/**
 * Puanla Tablet Native Uygulaması:
 * Tablet öncelikli sınıf ve değerlendirme uygulaması.
 */
@Composable
fun PuanlaApp(
    viewModel: PuanlaViewModel = viewModel(),
    onSystemBarAppearanceChanged: (Boolean) -> Unit = {}
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                lifecycleOwner.lifecycleScope.launch { viewModel.flushPendingEvidenceNotes() }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val darkModePreference by viewModel.darkMode.collectAsState()
    val reduceMotion by viewModel.reduceMotion.collectAsState()
    val accent by viewModel.accent.collectAsState()
    val commandPaletteShortcut by viewModel.commandPaletteShortcut.collectAsState()
    val commandPaletteShortcutIsCustom by viewModel.commandPaletteShortcutIsCustom.collectAsState()
    val isSystemDark = isSystemInDarkTheme()
    val isDark = darkModePreference ?: isSystemDark

    // The system bars follow the same resolved theme as the Compose UI.
    SideEffect { onSystemBarAppearanceChanged(isDark) }

    val currentDestination by viewModel.currentDestination.collectAsState()
    val toastMessages by viewModel.toastMessages.collectAsState()
    val showHistory by viewModel.isNotificationCenterOpen.collectAsState()
    val recentLogs by viewModel.recentLogs.collectAsState()

    val destinations = remember { primaryNavigationDestinations }
    var lastMainTab by remember { mutableStateOf(AppDestination.WORKSPACE) }
    LaunchedEffect(currentDestination) {
        lastMainTab = currentDestination.primaryTab(lastMainTab)
    }
    val navigationItems = remember {
        destinations.map { destination ->
            ArcAdvancedItem(
                key = destination.name,
                label = destination.label,
                contentDescription = destination.label
            )
        }
    }

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
        reducedMotion = reduceMotion.reducedMotionOverride,
        accent = accent
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = ArcTheme.colors.background
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    // Keep interactive UI clear of status/navigation bars and display cutouts.
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .onPreviewKeyEvent { event ->
                    val keyEvent = event.nativeKeyEvent
                    if (event.type == KeyEventType.KeyDown &&
                        KeyboardShortcutMatcher.matchesCommandPaletteShortcut(
                            shortcut = commandPaletteShortcut,
                            isCustom = commandPaletteShortcutIsCustom,
                            keyLabel = KeyboardShortcutMatcher.keyLabel(keyEvent.keyCode, keyEvent.unicodeChar),
                            ctrlPressed = event.isCtrlPressed,
                            altPressed = event.isAltPressed,
                            shiftPressed = event.isShiftPressed,
                            metaPressed = event.isMetaPressed
                        )) {
                        viewModel.isCommandPaletteOpen.value = true
                        true
                    } else false
                }
            ) {
                Scaffold(
                    // The root already consumes safe drawing insets for all screen content
                    // and floating controls; Scaffold must not apply them a second time.
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    topBar = {
                        Column {
                            AppTopBar(
                                viewModel = viewModel,
                                onBackFromSettings = {
                                    viewModel.currentDestination.value = lastMainTab
                                }
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                ArcGlassTabBar(
                                    tabs = navigationItems,
                                    selectedKey = currentDestination.primaryTab(lastMainTab).name,
                                    onTabSelected = { key ->
                                        destinations.firstOrNull { it.name == key }?.let {
                                            lastMainTab = it
                                            viewModel.currentDestination.value = it
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
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

                // Real Room audit log records shown through the Arc P1 notification center.
                if (showHistory) {
                    Dialog(onDismissRequest = { viewModel.isNotificationCenterOpen.value = false }) {
                        Surface(
                            modifier = Modifier.width(480.dp).height(430.dp),
                            shape = ArcTheme.shapes.panel,
                            color = ArcTheme.colors.surfaceRaised
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "İşlem Geçmişi",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ArcTheme.colors.foreground
                                    )
                                    ArcButton(
                                        text = "Kapat",
                                        onClick = { viewModel.isNotificationCenterOpen.value = false },
                                        variant = ArcButtonVariant.Ghost
                                    )
                                }
                                val dateFormat = remember { SimpleDateFormat("dd.MM HH:mm", Locale("tr", "TR")) }
                                val items = remember(recentLogs) {
                                    recentLogs.map {
                                        ArcNotificationItem(
                                            id = it.id, title = it.action,
                                            timestamp = dateFormat.format(Date(it.timestamp)),
                                            details = it.details
                                        )
                                    }
                                }
                                ArcNotificationCenter(
                                    notifications = items,
                                    modifier = Modifier.weight(1f).fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                // Global Command Palette Modal (Cmd+K)
                AppCommandPaletteModal(viewModel = viewModel)
            }
        }
    }
}
