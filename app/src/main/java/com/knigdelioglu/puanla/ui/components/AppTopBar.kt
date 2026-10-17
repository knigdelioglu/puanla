package com.knigdelioglu.puanla.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.knigdelioglu.arc.compose.controls.ArcActionButton
import com.knigdelioglu.arc.compose.controls.ArcButton
import com.knigdelioglu.arc.compose.controls.ArcButtonSize
import com.knigdelioglu.arc.compose.controls.ArcButtonVariant
import com.knigdelioglu.arc.compose.controls.ArcRadioCards
import com.knigdelioglu.arc.compose.controls.ArcSelect
import com.knigdelioglu.arc.compose.display.ArcTooltip
import com.knigdelioglu.arc.compose.foundation.ArcTheme
import com.knigdelioglu.arc.compose.navigation.ArcBottomSheet
import com.knigdelioglu.puanla.data.local.AssessmentRunEntity
import com.knigdelioglu.puanla.data.local.GroupAssessmentSessionEntity
import com.knigdelioglu.puanla.ui.navigation.primaryNavigationDestinations
import com.knigdelioglu.puanla.ui.viewmodel.AppDestination
import com.knigdelioglu.puanla.ui.viewmodel.PuanlaViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppTopBar(
    viewModel: PuanlaViewModel,
    activePrimaryDestination: AppDestination,
    onNavigate: (AppDestination) -> Unit,
    onBackFromSettings: () -> Unit
) {
    val currentDestination by viewModel.currentDestination.collectAsState()
    val classrooms by viewModel.classrooms.collectAsState()
    val selectedClassroom by viewModel.selectedClassroom.collectAsState()
    val rubrics by viewModel.rubrics.collectAsState()
    val selectedRubric by viewModel.selectedRubric.collectAsState()
    val assessmentRuns by viewModel.assessmentRuns.collectAsState()
    val selectedRun by viewModel.selectedRun.collectAsState()
    val runClassProgress by viewModel.runClassProgress.collectAsState()
    val classAssessments by viewModel.classAssessments.collectAsState()
    val groupSessions by viewModel.groupSessions.collectAsState()
    val selectedGroupSession by viewModel.selectedGroupSession.collectAsState()
    val groupTasks by viewModel.groupTasks.collectAsState()
    val groupAssessments by viewModel.groupAssessments.collectAsState()
    val reportsGroupMode by viewModel.reportsGroupMode.collectAsState()
    val quickGrading by viewModel.quickGradingPreference.collectAsState()

    var showRubricCards by remember { mutableStateOf(false) }
    var showEvaluationRuns by remember { mutableStateOf(false) }
    var startInCreateMode by remember { mutableStateOf(false) }

    val isEvaluationDestination = currentDestination == AppDestination.WORKSPACE ||
        currentDestination == AppDestination.GROUP_WORK || currentDestination == AppDestination.REPORTS
    val groupMode = currentDestination == AppDestination.GROUP_WORK ||
        (currentDestination == AppDestination.REPORTS && reportsGroupMode)
    val matchingRubrics = rubrics.filter { it.grade == selectedClassroom?.grade }
    val individualProgress = runClassProgress.firstOrNull {
        it.classroomId == selectedClassroom?.id
    }
    val individualCompleted = individualProgress?.completed ?: 0
    val individualTotal = individualProgress?.total ?: 0
    val individualAverage = classAssessments.asSequence()
        .filter { it.isCompleted }
        .mapNotNull { it.definitiveTotal }
        .takeIf { it.any() }
        ?.average()
        ?.roundToInt()
    val groupCompleted = groupAssessments.count { it.isCompleted }
    val groupTotal = groupTasks.size
    val groupAverage = groupAssessments.asSequence()
        .filter { it.isCompleted }
        .mapNotNull { it.definitiveTotal }
        .takeIf { it.any() }
        ?.average()
        ?.roundToInt()

    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
        color = ArcTheme.colors.surface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, ArcTheme.colors.border)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Puanla",
                    color = ArcTheme.colors.foreground,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    primaryNavigationDestinations.forEach { destination ->
                        HeaderNavigationItem(
                            label = when (destination) {
                                AppDestination.WORKSPACE -> "Puanlama"
                                AppDestination.CLASSROOMS -> "Sınıflar & OCR"
                                AppDestination.RUBRICS -> "Rubrikler"
                                AppDestination.REPORTS -> "Raporlar"
                                else -> destination.label
                            },
                            selected = activePrimaryDestination == destination,
                            onClick = { onNavigate(destination) }
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ArcTooltip(tooltipText = "Ara ve komutları aç") {
                        ArcActionButton(
                            onClick = { viewModel.isCommandPaletteOpen.value = true },
                            size = ArcButtonSize.Sm
                        ) {
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

            if (currentDestination != AppDestination.SETTINGS && currentDestination != AppDestination.RUBRICS) {
                HorizontalDivider(color = ArcTheme.colors.borderSubtle)
                Spacer(Modifier.height(8.dp))
                if (isEvaluationDestination) {
                    val selectedMode = when {
                        groupMode -> 2
                        quickGrading.qualitativeEnabled -> 1
                        else -> 0
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            itemVerticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "AKTİF OTURUM",
                                modifier = Modifier.padding(horizontal = 4.dp),
                                color = ArcTheme.colors.textSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (groupMode) {
                                ArcSelect(
                                    selectedItem = selectedGroupSession,
                                    items = groupSessions,
                                    onItemSelected = viewModel::selectGroupSession,
                                    labelProvider = { groupSessionCardLabel(it, selectedClassroom?.name) },
                                    placeholder = "Grup oturumu seç",
                                    modifier = Modifier.width(390.dp)
                                )
                            } else {
                                ArcSelect(
                                    selectedItem = selectedRun,
                                    items = assessmentRuns,
                                    onItemSelected = viewModel::selectRun,
                                    labelProvider = { runCardLabel(it, selectedClassroom?.name) },
                                    placeholder = "Değerlendirme oturumu seç",
                                    modifier = Modifier.width(390.dp)
                                )
                            }
                            ArcButton(
                                text = "Yeni Oturum Başlat",
                                onClick = {
                                    startInCreateMode = true
                                    showEvaluationRuns = true
                                },
                                leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                                variant = ArcButtonVariant.Outline,
                                size = ArcButtonSize.Sm,
                                enabled = selectedClassroom != null && selectedRubric != null
                            )
                            ArcButton(
                                text = "Oturumlar",
                                onClick = {
                                    startInCreateMode = false
                                    showEvaluationRuns = true
                                },
                                variant = ArcButtonVariant.Ghost,
                                size = ArcButtonSize.Sm,
                                enabled = selectedClassroom != null && selectedRubric != null
                            )
                            if (groupMode) {
                                SessionStatusChip(
                                    text = groupSessionStatus(groupCompleted, groupTotal),
                                    complete = groupTotal > 0 && groupCompleted == groupTotal,
                                    inProgress = groupCompleted in 1 until groupTotal
                                )
                            } else {
                                SessionStatusChip(
                                    text = individualSessionStatus(selectedRun?.status, individualCompleted, individualTotal),
                                    complete = individualTotal > 0 && individualCompleted == individualTotal,
                                    inProgress = individualCompleted > 0 && individualCompleted < individualTotal
                                )
                            }
                            if (currentDestination != AppDestination.REPORTS) {
                                ArcButton(
                                    text = "Rapor ve Grafikler  →",
                                    onClick = {
                                        viewModel.reportsGroupMode.value = groupMode
                                        viewModel.currentDestination.value = AppDestination.REPORTS
                                    },
                                    variant = ArcButtonVariant.Outline,
                                    size = ArcButtonSize.Sm,
                                    enabled = if (groupMode) selectedGroupSession != null else selectedRun != null
                                )
                            }
                        }

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            itemVerticalAlignment = Alignment.CenterVertically
                        ) {
                            EvaluationModeSwitch(
                                selectedIndex = selectedMode,
                                groupCount = groupTotal,
                                modifier = Modifier.width(570.dp),
                                onSelectIndex = { index ->
                                    when (index) {
                                        0 -> {
                                            viewModel.setQualitativeQuickGrading(false)
                                            if (currentDestination == AppDestination.REPORTS) {
                                                viewModel.reportsGroupMode.value = false
                                            } else viewModel.currentDestination.value = AppDestination.WORKSPACE
                                        }
                                        1 -> {
                                            viewModel.setQualitativeQuickGrading(true)
                                            if (currentDestination == AppDestination.REPORTS) {
                                                viewModel.reportsGroupMode.value = false
                                            } else viewModel.currentDestination.value = AppDestination.WORKSPACE
                                        }
                                        else -> {
                                            if (currentDestination == AppDestination.REPORTS) {
                                                viewModel.reportsGroupMode.value = true
                                            } else viewModel.currentDestination.value = AppDestination.GROUP_WORK
                                        }
                                    }
                                }
                            )
                            ArcSelect(
                                selectedItem = selectedClassroom,
                                items = classrooms,
                                onItemSelected = viewModel::selectClassroom,
                                labelProvider = { it.name },
                                placeholder = "Sınıf seç",
                                modifier = Modifier.width(152.dp)
                            )
                            ArcButton(
                                text = selectedRubric?.title?.let { title ->
                                    "Rubrik: ${title.take(20)}${if (title.length > 20) "…" else ""}"
                                } ?: "Rubrik seç",
                                onClick = { showRubricCards = true },
                                variant = ArcButtonVariant.Outline,
                                size = ArcButtonSize.Sm,
                                enabled = selectedClassroom != null && matchingRubrics.isNotEmpty()
                            )
                            if (groupMode) {
                                MetricChip("İlerleme", "$groupCompleted/$groupTotal")
                                MetricChip("Ortalama", groupAverage?.let { "$it/100" } ?: "—")
                            } else {
                                MetricChip("İlerleme", "$individualCompleted/$individualTotal")
                                MetricChip("Ortalama", individualAverage?.let { "$it/100" } ?: "—")
                            }
                        }
                    }
                } else {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        itemVerticalAlignment = Alignment.CenterVertically
                    ) {
                        ArcSelect(
                            selectedItem = selectedClassroom,
                            items = classrooms,
                            onItemSelected = viewModel::selectClassroom,
                            labelProvider = { it.name },
                            placeholder = "Sınıf seç",
                            modifier = Modifier.width(180.dp)
                        )
                    }
                }
            }
        }
    }

    EvaluationRunSheet(
        viewModel = viewModel,
        isOpen = showEvaluationRuns,
        groupMode = groupMode,
        startInCreateMode = startInCreateMode,
        onDismiss = {
            showEvaluationRuns = false
            startInCreateMode = false
        }
    )

    ArcBottomSheet(
        isOpen = showRubricCards,
        onDismissRequest = { showRubricCards = false }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().heightIn(max = 460.dp)
                .verticalScroll(rememberScrollState()).padding(16.dp)
        ) {
            Text("Rubrik Seçimi", color = ArcTheme.colors.foreground, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
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

@Composable
private fun HeaderNavigationItem(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = ArcTheme.colors
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(role = Role.Tab, onClick = onClick)
            .semantics { this.selected = selected }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            color = if (selected) colors.foreground else colors.textSecondary,
            fontSize = 15.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(5.dp))
        Surface(
            modifier = Modifier.width(72.dp).height(2.dp),
            color = if (selected) colors.accent else Color.Transparent,
            shape = RoundedCornerShape(2.dp)
        ) {}
    }
}

@Composable
private fun SessionStatusChip(text: String, complete: Boolean, inProgress: Boolean = false) {
    val colors = ArcTheme.colors
    val color = when {
        complete -> colors.success
        inProgress -> colors.warning
        else -> colors.textSecondary
    }
    val fill = when {
        complete -> colors.success.copy(alpha = 0.10f)
        inProgress -> colors.warning.copy(alpha = 0.10f)
        else -> colors.surfaceMuted
    }
    val border = when {
        complete -> colors.success.copy(alpha = 0.35f)
        inProgress -> colors.warning.copy(alpha = 0.35f)
        else -> colors.borderSubtle
    }
    Surface(
        shape = ArcTheme.shapes.control,
        color = fill,
        border = BorderStroke(1.dp, border)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            color = color,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun EvaluationModeSwitch(
    selectedIndex: Int,
    groupCount: Int,
    modifier: Modifier = Modifier,
    onSelectIndex: (Int) -> Unit
) {
    val colors = ArcTheme.colors
    val modes = listOf(
        "Bireysel Değerlendirme" to Icons.Default.Person,
        "Hızlı Değerlendirme" to Icons.Default.Bolt,
        "Grup Değerlendirmesi ($groupCount)" to Icons.Default.Group
    )
    Surface(
        modifier = modifier.height(44.dp),
        shape = ArcTheme.shapes.pill,
        color = colors.surfaceMuted,
        border = BorderStroke(1.dp, colors.borderSubtle)
    ) {
        Row(
            modifier = Modifier.padding(3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            modes.forEachIndexed { index, (label, icon) ->
                val isSelected = index == selectedIndex
                Surface(
                    onClick = { onSelectIndex(index) },
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .semantics {
                            selected = isSelected
                            role = Role.Tab
                        },
                    shape = ArcTheme.shapes.pill,
                    color = if (isSelected) colors.surface else Color.Transparent,
                    border = if (isSelected) BorderStroke(1.dp, colors.border.copy(alpha = 0.5f)) else null,
                    shadowElevation = if (isSelected) 1.dp else 0.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(icon, contentDescription = null, tint = colors.textSecondary)
                        Spacer(Modifier.width(7.dp))
                        Text(
                            label,
                            color = if (isSelected) colors.foreground else colors.textSecondary,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricChip(label: String, value: String) {
    val colors = ArcTheme.colors
    Surface(
        shape = ArcTheme.shapes.control,
        color = colors.surfaceRaised,
        border = BorderStroke(1.dp, colors.borderSubtle)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = colors.textSecondary, fontSize = 14.sp)
            Text(value, color = colors.foreground, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

private fun individualSessionStatus(status: String?, completed: Int, total: Int): String = when {
    status == null -> "Oturum seçilmedi"
    total > 0 && completed == total -> "Oturum Tamamlandı"
    status == "ARCHIVED" -> "Arşivlendi · Eksik puan var"
    completed > 0 -> "Puanlama sürüyor"
    else -> "Yeni oturum"
}

private fun groupSessionStatus(completed: Int, total: Int): String = when {
    total > 0 && completed == total -> "Oturum Tamamlandı"
    completed > 0 -> "Puanlama sürüyor"
    else -> "Yeni oturum"
}

private fun runLabel(run: AssessmentRunEntity): String {
    val date = formatSessionDate(run.createdAt)
    val state = if (run.status == "ARCHIVED") "Arşivli" else "Aktif"
    return "${run.title.take(28)}${if (run.title.length > 28) "…" else ""} · $date · $state"
}

private fun runCardLabel(run: AssessmentRunEntity, classroomName: String?): String {
    val status = if (run.status == "ARCHIVED") "Arşivlendi" else "Aktif"
    return "${classroomName ?: "Sınıf"} — ${run.title} (${formatSessionDate(run.createdAt)} · $status)"
}

private fun groupSessionLabel(session: GroupAssessmentSessionEntity): String =
    "${session.title.take(28)}${if (session.title.length > 28) "…" else ""} · ${formatSessionDate(session.createdAt)}"

private fun groupSessionCardLabel(session: GroupAssessmentSessionEntity, classroomName: String?): String =
    "${classroomName ?: "Sınıf"} — ${session.title} (${formatSessionDate(session.createdAt)})"

private fun formatSessionDate(timestamp: Long): String {
    if (timestamp <= 0L) return "Eski kayıt"
    return Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.forLanguageTag("tr-TR")))
}
