package com.knigdelioglu.puanla.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.Divider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.knigdelioglu.arc.compose.controls.ArcButton
import com.knigdelioglu.arc.compose.controls.ArcButtonSize
import com.knigdelioglu.arc.compose.controls.ArcButtonVariant
import com.knigdelioglu.arc.compose.controls.ArcCheckbox
import com.knigdelioglu.arc.compose.controls.ArcConfirmMorph
import com.knigdelioglu.arc.compose.controls.ArcInput
import com.knigdelioglu.arc.compose.controls.ArcMultiSelect
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import com.knigdelioglu.arc.compose.controls.ArcSelect
import com.knigdelioglu.arc.compose.navigation.ArcResizablePanels
import com.knigdelioglu.puanla.data.local.GroupAssessmentEntity
import com.knigdelioglu.puanla.data.local.GroupAssessmentSessionEntity
import com.knigdelioglu.puanla.data.local.GroupMemberResultEntity
import com.knigdelioglu.puanla.data.local.GroupCriterionScoreEntity
import com.knigdelioglu.puanla.data.local.CriterionEntity
import com.knigdelioglu.arc.compose.display.ArcAvatarGroup
import com.knigdelioglu.arc.compose.display.ArcBadge
import com.knigdelioglu.arc.compose.display.ArcBadgeVariant
import com.knigdelioglu.arc.compose.display.ArcCard
import com.knigdelioglu.arc.compose.display.ArcEmptyState
import com.knigdelioglu.arc.compose.display.ArcExpandableCard
import com.knigdelioglu.arc.compose.display.ArcProgress
import com.knigdelioglu.arc.compose.foundation.ArcTheme
import com.knigdelioglu.puanla.data.local.GroupTaskEntity
import com.knigdelioglu.puanla.data.local.StudentEntity
import com.knigdelioglu.puanla.data.local.fullName
import com.knigdelioglu.puanla.data.local.initials
import com.knigdelioglu.puanla.ui.viewmodel.PuanlaViewModel
import org.json.JSONArray


/**
 * The existing Puanlama > Grup mode shares rubric and score controls with individual
 * grading, but owns separate session/group state and never rewrites individual grades.
 */
@Composable
fun GroupWorkScreen(viewModel: PuanlaViewModel) {
    val classroom by viewModel.selectedClassroom.collectAsState()
    val rubric by viewModel.selectedRubric.collectAsState()
    val students by viewModel.students.collectAsState()
    val groups by viewModel.groupTasks.collectAsState()
    val selectedGroup by viewModel.selectedGroup.collectAsState()
    val sessions by viewModel.groupSessions.collectAsState()
    val session by viewModel.selectedGroupSession.collectAsState()
    val groupAssessments by viewModel.groupAssessments.collectAsState()
    val assessment by viewModel.currentGroupAssessment.collectAsState()
    val scores by viewModel.currentGroupScores.collectAsState()
    val memberResults by viewModel.currentGroupMembers.collectAsState()
    val criteria by viewModel.criteria.collectAsState()
    val levels by viewModel.levels.collectAsState()
    val quickGrading by viewModel.quickGradingPreference.collectAsState()

    var createGroup by remember { mutableStateOf(false) }
    var createSession by remember { mutableStateOf(false) }
    var showPreparation by remember { mutableStateOf(false) }
    var activeCriterionIndex by remember(selectedGroup?.id, session?.id, rubric?.id) { mutableIntStateOf(0) }
    var autoAdvance by rememberSaveable { mutableStateOf(true) }
    val activeCriterion = criteria.getOrNull(activeCriterionIndex) ?: criteria.firstOrNull()

    Column(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Grup değerlendirmesi",
                fontWeight = FontWeight.Bold, fontSize = 18.sp,
                color = ArcTheme.colors.foreground, modifier = Modifier.weight(1f)
            )
            ArcButton(
                text = if (showPreparation) "Puanlamaya dön" else "Hazırlık",
                onClick = { showPreparation = !showPreparation },
                size = ArcButtonSize.Sm, variant = ArcButtonVariant.Outline
            )
            ArcButton(
                text = "Yeni Grup", onClick = { createGroup = true },
                size = ArcButtonSize.Sm, enabled = classroom != null
            )
        }

        if (classroom == null || rubric == null) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                ArcEmptyState(
                    title = "Sınıf veya rubrik seçilmedi",
                    description = "Üst çubuktan sınıfı ve değerlendirilecek rubriği seçin."
                )
            }
        } else if (showPreparation) {
            if (groups.isEmpty()) {
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    ArcEmptyState(title = "Henüz grup yok", description = "Yeni Grup ile öğrencileri gruplandırın.")
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(groups, key = { it.id }) { group ->
                        GroupTaskCard(
                            task = group, students = students,
                            onToggleChecklist = { idx -> viewModel.toggleChecklistItem(group, idx) }
                        )
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (sessions.isNotEmpty()) {
                    ArcSelect(
                        selectedItem = session,
                        items = sessions,
                        onItemSelected = { viewModel.selectGroupSession(it) },
                        labelProvider = { it.title },
                        placeholder = "Oturum seçin",
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Text(
                        "Bu rubrikle henüz değerlendirme oturumu yok.",
                        modifier = Modifier.weight(1f), fontSize = 13.sp,
                        color = ArcTheme.colors.textSecondary
                    )
                }
                ArcButton(
                    text = "Yeni Oturum", onClick = { createSession = true },
                    size = ArcButtonSize.Sm, variant = ArcButtonVariant.Primary
                )
            }

            if (groups.isEmpty() || session == null) {
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    ArcEmptyState(
                        title = if (groups.isEmpty()) "Grup oluşturun" else "Oturum oluşturun",
                        description = "Rubrik bağımsız grupları ve bir değerlendirme oturumunu seçerek başlayın."
                    )
                }
            } else {
                BoxWithConstraints(Modifier.weight(1f)) {
                    if (maxWidth >= 900.dp) {
                        ArcResizablePanels(
                            modifier = Modifier.fillMaxSize(),
                            initialWeights = listOf(0.28f, 0.38f, 0.34f),
                            minWeights = listOf(0.20f, 0.25f, 0.25f),
                            firstPane = {
                                GroupRosterPane(groups, selectedGroup, groupAssessments) {
                                    viewModel.selectGroup(it)
                                }
                            },
                            secondPane = {
                                CriteriaExplorerPane(
                                    criteria = criteria, activeCriterion = activeCriterion,
                                    onSelectCriterion = {
                                        activeCriterionIndex = criteria.indexOfFirst { c -> c.id == it.id }.coerceAtLeast(0)
                                    }
                                )
                            },
                            thirdPane = {
                                GroupScoringPane(
                                    group = selectedGroup, assessment = assessment,
                                    criterion = activeCriterion, criteria = criteria,
                                    scores = scores, students = students, memberResults = memberResults,
                                    quickGrading = quickGrading,
                                    autoAdvance = autoAdvance, onAutoAdvance = { autoAdvance = it },
                                    onScore = { points ->
                                        activeCriterion?.let { criterion ->
                                            viewModel.setGroupScore(criterion.id, points)
                                            if (autoAdvance && activeCriterionIndex < criteria.lastIndex) activeCriterionIndex++
                                        }
                                    },
                                    onClear = { activeCriterion?.let { viewModel.setGroupScore(it.id, null) } },
                                    onNext = {
                                        if (activeCriterionIndex < criteria.lastIndex) activeCriterionIndex++
                                    },
                                    onNextGroup = {
                                        val i = groups.indexOfFirst { it.id == selectedGroup?.id }
                                        groups.getOrNull(i + 1)?.let { viewModel.selectGroup(it) }
                                    }
                                )
                            }
                        )
                    } else {
                        Column(
                            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ArcSelect(
                                selectedItem = selectedGroup,
                                items = groups,
                                onItemSelected = { viewModel.selectGroup(it) },
                                labelProvider = { it.title },
                                placeholder = "Grup seç",
                                modifier = Modifier.fillMaxWidth()
                            )
                            ArcSelect(
                                selectedItem = activeCriterion,
                                items = criteria,
                                onItemSelected = { item ->
                                    activeCriterionIndex = criteria.indexOfFirst { it.id == item.id }.coerceAtLeast(0)
                                },
                                labelProvider = { it.title },
                                placeholder = "Ölçüt seç",
                                modifier = Modifier.fillMaxWidth()
                            )
                            GroupScoringPane(
                                group = selectedGroup, assessment = assessment,
                                criterion = activeCriterion, criteria = criteria,
                                scores = scores, students = students, memberResults = memberResults,
                                quickGrading = quickGrading,
                                autoAdvance = autoAdvance, onAutoAdvance = { autoAdvance = it },
                                onScore = { points ->
                                    activeCriterion?.let { criterion ->
                                        viewModel.setGroupScore(criterion.id, points)
                                        if (autoAdvance && activeCriterionIndex < criteria.lastIndex) activeCriterionIndex++
                                    }
                                },
                                onClear = { activeCriterion?.let { viewModel.setGroupScore(it.id, null) } },
                                onNext = {
                                    if (activeCriterionIndex < criteria.lastIndex) activeCriterionIndex++
                                },
                                onNextGroup = {
                                    val i = groups.indexOfFirst { it.id == selectedGroup?.id }
                                    groups.getOrNull(i + 1)?.let { viewModel.selectGroup(it) }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (createGroup) {
        CreateGroupDialog(
            students = students,
            onDismiss = { createGroup = false },
            onConfirm = { title, memberIds ->
                viewModel.addGroupTask(title, memberIds)
                createGroup = false
            }
        )
    }
    if (createSession) {
        var title by remember { mutableStateOf("") }
        Dialog(onDismissRequest = { createSession = false }) {
            Surface(
                shape = ArcTheme.shapes.panel, color = ArcTheme.colors.surfaceRaised,
                modifier = Modifier.width(440.dp)
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Yeni grup değerlendirmesi", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    ArcInput(value = title, onValueChange = { title = it },
                        label = "Oturum adı", placeholder = "Örn. Drama - 12 Ekim")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ArcButton(text = "Vazgeç", onClick = { createSession = false },
                            variant = ArcButtonVariant.Ghost)
                        ArcButton(text = "Oluştur", enabled = title.isNotBlank(), onClick = {
                            viewModel.createGroupSession(title)
                            createSession = false
                        })
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupRosterPane(
    groups: List<GroupTaskEntity>,
    selected: GroupTaskEntity?,
    assessments: List<GroupAssessmentEntity>,
    onSelect: (GroupTaskEntity) -> Unit
) {
    Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Gruplar", fontWeight = FontWeight.Bold, color = ArcTheme.colors.foreground)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            items(groups, key = { it.id }) { group ->
                val evaluation = assessments.firstOrNull { it.groupId == group.id }
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable { onSelect(group) },
                    shape = ArcTheme.shapes.control,
                    color = if (selected?.id == group.id) ArcTheme.colors.surfaceRaised else ArcTheme.colors.surface,
                    border = BorderStroke(
                        if (selected?.id == group.id) 2.dp else 1.dp,
                        if (selected?.id == group.id) ArcTheme.colors.accent else ArcTheme.colors.borderSubtle
                    )
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(group.title, color = ArcTheme.colors.foreground, fontWeight = FontWeight.Bold)
                        Text(
                            when {
                                evaluation?.isCompleted == true -> "Tamamlandı • " + evaluation.definitiveTotal + " puan"
                                evaluation != null -> "Kısmi • " + evaluation.scoredCount + " ölçüt"
                                else -> "Başlanmadı"
                            },
                            fontSize = 12.sp, color = ArcTheme.colors.textSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupScoringPane(
    group: GroupTaskEntity?,
    assessment: GroupAssessmentEntity?,
    criterion: CriterionEntity?,
    criteria: List<CriterionEntity>,
    scores: Map<String, GroupCriterionScoreEntity>,
    students: List<StudentEntity>,
    memberResults: List<GroupMemberResultEntity>,
    quickGrading: com.knigdelioglu.puanla.ui.preferences.QuickGradingPreference,
    autoAdvance: Boolean,
    onAutoAdvance: (Boolean) -> Unit,
    onScore: (Int) -> Unit,
    onClear: () -> Unit,
    onNext: () -> Unit,
    onNextGroup: () -> Unit
) {
    if (group == null) {
        ArcEmptyState(title = "Grup seçilmedi", description = "Soldan değerlendireceğiniz grubu seçin.")
        return
    }
    val snapshot = assessment?.memberStudentIdsJson ?: group.memberStudentIdsJson
    val ids = remember(snapshot) {
        try {
            val array = JSONArray(snapshot)
            (0 until array.length()).map { array.getString(it) }
        } catch (_: Exception) { emptyList() }
    }
    val names = remember(students) { students.associateBy { it.id } }
    val total = assessment?.definitiveTotal
    Column(
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(group.title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = ArcTheme.colors.foreground)
        Text(
            if (assessment?.isCompleted == true) "Grup puanı: " + total + " / " + criteria.sumOf { it.maxPoints }
            else "Kısmi: " + (assessment?.scoredCount ?: 0) + " / " + criteria.size + " ölçüt",
            color = ArcTheme.colors.textSecondary
        )
        if (criterion != null) {
            ArcCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text(criterion.title, color = ArcTheme.colors.foreground, fontWeight = FontWeight.Bold)
                    ArcBadge(
                        text = (scores[criterion.id]?.points?.toString() ?: "Puanlanmadı") +
                            " / " + criterion.maxPoints,
                        variant = ArcBadgeVariant.Accent
                    )
                    QuickScoreControls(
                        maxPoints = criterion.maxPoints,
                        points = scores[criterion.id]?.points,
                        levels = emptyList(),
                        autoAdvance = autoAdvance,
                        onAutoAdvanceChange = onAutoAdvance,
                        hasNextCriterion = criteria.indexOfFirst { it.id == criterion.id } < criteria.lastIndex,
                        onNextCriterion = onNext,
                        onChooseScore = onScore,
                        quickGrading = quickGrading
                    )
                    ArcButton(
                        text = "Puanı kaldır", onClick = onClear,
                        enabled = scores[criterion.id]?.points != null,
                        variant = ArcButtonVariant.Ghost, size = ArcButtonSize.Sm
                    )
                }
            }
        }
        Text("Üyelere aktarılan puanlar", fontWeight = FontWeight.SemiBold,
            color = ArcTheme.colors.foreground, fontSize = 13.sp)
        ids.forEach { id ->
            val student = names[id]
            val grade = memberResults.firstOrNull { it.studentId == id }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(student?.fullName ?: "Öğrenci", fontSize = 12.sp, color = ArcTheme.colors.foreground)
                Text(
                    if (grade?.isCompleted == true) grade.definitiveTotal.toString()
                    else "Eksik",
                    color = ArcTheme.colors.textSecondary, fontSize = 12.sp
                )
            }
        }
        ArcButton(
            text = "Sonraki Grup", onClick = onNextGroup,
            variant = ArcButtonVariant.Outline, modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun GroupTaskCard(
    task: GroupTaskEntity,
    students: List<StudentEntity>,
    onToggleChecklist: (Int) -> Unit
) {
    val studentMap = remember(students) { students.associateBy { it.id } }

    val memberIds = remember(task.memberStudentIdsJson) {
        try {
            val arr = JSONArray(task.memberStudentIdsJson)
            (0 until arr.length()).map { arr.getString(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    val members = memberIds.mapNotNull { studentMap[it] }
    val memberInitials = members.map { it.initials }

    val checklist = remember(task.checklistJson) {
        try {
            val arr = JSONArray(task.checklistJson)
            (0 until arr.length()).map {
                val obj = arr.getJSONObject(it)
                obj.getString("title") to obj.getBoolean("checked")
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    val completedCount = checklist.count { it.second }
    val progressFraction = if (checklist.isNotEmpty()) completedCount.toFloat() / checklist.size else 0f
    val subtitle = if (checklist.isEmpty()) {
        "${members.size} Üye"
    } else {
        "${members.size} Üye • %${(progressFraction * 100).toInt()} Hazırlık Tamamlandı"
    }

    ArcExpandableCard(
        title = task.title,
        subtitle = subtitle,
        badge = {
            ArcAvatarGroup(membersInitials = memberInitials, avatarSize = 28.dp)
        },
        initiallyExpanded = true
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Member tags
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Grup Üyeleri: ",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ArcTheme.colors.textSecondary
                )
                Spacer(Modifier.width(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    members.forEach { m ->
                        ArcBadge(
                            text = "${m.studentNumber} ${m.fullName}",
                            variant = ArcBadgeVariant.Default
                        )
                    }
                }
            }

            if (checklist.isNotEmpty()) {
                // Progress bar
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Hazırlık Kontrol Listesi ($completedCount / ${checklist.size})",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = ArcTheme.colors.foreground
                        )
                        Text(
                            text = "%${(progressFraction * 100).toInt()}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = ArcTheme.colors.accent
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    ArcProgress(progress = progressFraction)
                }

                Divider(color = ArcTheme.colors.borderSubtle.copy(alpha = 0.5f))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    checklist.forEachIndexed { idx, (itemTitle, isChecked) ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggleChecklist(idx) },
                            shape = ArcTheme.shapes.control,
                            color = if (isChecked) ArcTheme.colors.surfaceMuted else ArcTheme.colors.surface,
                            border = BorderStroke(1.dp, ArcTheme.colors.borderSubtle)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ArcCheckbox(
                                    checked = isChecked,
                                    onCheckedChange = { onToggleChecklist(idx) }
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = itemTitle,
                                    fontSize = 14.sp,
                                    color = if (isChecked) ArcTheme.colors.textSecondary else ArcTheme.colors.foreground,
                                    fontWeight = if (isChecked) FontWeight.Normal else FontWeight.Medium
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
private fun CreateGroupDialog(
    students: List<StudentEntity>,
    onDismiss: () -> Unit,
    onConfirm: (String, List<String>) -> Unit
) {
    var groupTitle by remember { mutableStateOf("") }
    var selectedStudentIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = ArcTheme.shapes.panel,
            color = ArcTheme.colors.surfaceRaised,
            border = BorderStroke(1.dp, ArcTheme.colors.border),
            shadowElevation = 8.dp,
            modifier = Modifier.width(460.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "Yeni Grup Oluştur",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = ArcTheme.colors.foreground
                )
                Spacer(Modifier.height(14.dp))

                ArcInput(
                    value = groupTitle,
                    onValueChange = { groupTitle = it },
                    label = "Grup Adı",
                    placeholder = "Örn: 1. Münazara Grubu / Canlandırma Ekibi"
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "Grup Üyelerini Seçin (${selectedStudentIds.size} Seçildi):",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ArcTheme.colors.textSecondary
                )
                Spacer(Modifier.height(8.dp))

                // One controlled Arc selection component, no nested row/checkbox
                // click handlers that could toggle the same pupil twice.
                ArcMultiSelect(
                    selectedItems = selectedStudentIds,
                    items = students.map { it.id },
                    onSelectionChanged = { selectedStudentIds = it },
                    labelProvider = { id ->
                        students.firstOrNull { it.id == id }?.let {
                            "${it.studentNumber} - ${it.fullName}"
                        } ?: id
                    },
                    modifier = Modifier
                        .heightIn(max = 200.dp)
                        .verticalScroll(rememberScrollState())
                )

                Spacer(Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    ArcButton(
                        text = "Vazgeç",
                        onClick = onDismiss,
                        variant = ArcButtonVariant.Ghost
                    )
                    Spacer(Modifier.width(8.dp))
                    ArcButton(
                        text = "Grubu Kur",
                        onClick = {
                            if (groupTitle.isNotBlank() && selectedStudentIds.isNotEmpty()) {
                                onConfirm(groupTitle.trim(), selectedStudentIds.toList())
                            }
                        },
                        variant = ArcButtonVariant.Primary,
                        enabled = groupTitle.isNotBlank() && selectedStudentIds.isNotEmpty()
                    )
                }
            }
        }
    }
}
