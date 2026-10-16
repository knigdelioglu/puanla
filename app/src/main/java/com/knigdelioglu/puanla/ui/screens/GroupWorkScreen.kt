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
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import com.knigdelioglu.arc.compose.controls.ArcElasticSlider
import com.knigdelioglu.arc.compose.controls.ArcNumberField
import com.knigdelioglu.arc.compose.editor.ArcRichTextEditor
import kotlin.math.roundToInt
import androidx.compose.runtime.saveable.rememberSaveable
import com.knigdelioglu.arc.compose.controls.ArcSelect
import com.knigdelioglu.arc.compose.navigation.ArcResizablePanels
import com.knigdelioglu.puanla.data.local.GroupAssessmentEntity
import com.knigdelioglu.puanla.data.local.GroupAssessmentSessionEntity
import com.knigdelioglu.puanla.data.local.GroupMemberResultEntity
import com.knigdelioglu.puanla.data.local.GroupCriterionScoreEntity
import com.knigdelioglu.puanla.data.local.CriterionEntity
import com.knigdelioglu.puanla.data.local.CriterionLevelEntity
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
import kotlinx.coroutines.launch


/**
 * The existing Puanlama > Grup mode shares rubric and score controls with individual
 * grading, but owns separate session/group state and never rewrites individual grades.
 */
@Composable
fun GroupWorkScreen(viewModel: PuanlaViewModel) {
    val scope = rememberCoroutineScope()
    val classroom by viewModel.selectedClassroom.collectAsState()
    val rubric by viewModel.selectedRubric.collectAsState()
    val students by viewModel.students.collectAsState()
    val groups by viewModel.groupTasks.collectAsState()
    val selectedGroup by viewModel.selectedGroup.collectAsState()
    val session by viewModel.selectedGroupSession.collectAsState()
    val groupAssessments by viewModel.groupAssessments.collectAsState()
    val assessment by viewModel.currentGroupAssessment.collectAsState()
    val scores by viewModel.currentGroupScores.collectAsState()
    val memberResults by viewModel.currentGroupMembers.collectAsState()
    val criteria by viewModel.groupCriteria.collectAsState()
    val levels by viewModel.levels.collectAsState()
    val quickGrading by viewModel.quickGradingPreference.collectAsState()
    val canUndoGroupAction by viewModel.canUndoGroupAction.collectAsState()
    val groupNoteSaveState by viewModel.groupEvidenceNoteSaveState.collectAsState()
    val scoreSaveState by viewModel.scoreSaveState.collectAsState()

    var createGroup by remember { mutableStateOf(false) }
    var editingGroup by remember { mutableStateOf<GroupTaskEntity?>(null) }
    var deletingGroup by remember { mutableStateOf<GroupTaskEntity?>(null) }
    var createRandomGroups by remember { mutableStateOf(false) }
    var confirmMembershipSync by remember { mutableStateOf(false) }
    val assignedStudentIds = remember(groups) {
        groups.flatMap { group ->
            try {
                val array = JSONArray(group.memberStudentIdsJson)
                (0 until array.length()).map { array.getString(it) }
            } catch (_: Exception) { emptyList() }
        }.toSet()
    }
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
                text = "Rastgele Grup Oluştur", onClick = { createRandomGroups = true },
                size = ArcButtonSize.Sm,
                enabled = classroom != null && students.any { it.id !in assignedStudentIds },
                variant = ArcButtonVariant.Outline
            )
            ArcButton(
                text = "Yeni Grup", onClick = { createGroup = true },
                size = ArcButtonSize.Sm, enabled = classroom != null
            )
        }

        if (classroom == null) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                ArcEmptyState(
                    title = "Sınıf seçilmedi",
                    description = "Grup oluşturmak için üst çubuktan sınıf seçin."
                )
            }
        } else if (showPreparation || rubric == null) {
            if (rubric == null) {
                Text("Grup oluşturabilirsiniz. Puanlama için üst çubuktan rubrik seçin.",
                    color = ArcTheme.colors.textSecondary, fontSize = 13.sp)
            }
            if (groups.isEmpty()) {
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    ArcEmptyState(title = "Henüz grup yok", description = "Yeni Grup ile öğrencileri gruplandırın.")
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(groups, key = { it.id }) { group ->
                        GroupTaskCard(
                            task = group, students = students,
                            onToggleChecklist = { idx -> viewModel.toggleChecklistItem(group, idx) },
                            onEdit = { editingGroup = group },
                            onDelete = { deletingGroup = group }
                        )
                    }
                }
            }
        } else {
            // Session selection/creation now lives in the same top-bar
            // Değerlendirmeler sheet as individual scoring.
            if (session != null) {
                Text(
                    "Değerlendirme: ${session?.title}",
                    color = ArcTheme.colors.textSecondary,
                    fontSize = 13.sp
                )
            }

            if (groups.isEmpty() || session == null) {
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    ArcEmptyState(
                        title = if (groups.isEmpty()) "Grup oluşturun" else "Oturum oluşturun",
                        description = if (groups.isEmpty())
                            "Yeni Grup veya Rastgele Grup Oluştur ile grupları hazırlayın."
                        else "Üstteki Değerlendirmeler menüsünden değerlendirme açın veya oluşturun."
                    )
                }
            } else {
                // Shared presentation shell: the group session, its member results
                // and its undo stack remain independent from individual scoring.
                UnifiedScoringLayout(
                    mode = ScoringTargetMode.GROUP,
                    contextKey = "${classroom?.id}/${rubric?.id}/${session?.id}",
                    modifier = Modifier.weight(1f),
                    targetPane = { onSelected ->
                        GroupRosterPane(groups, selectedGroup, groupAssessments) {
                            viewModel.selectGroup(it)
                            onSelected()
                        }
                    },
                    criteriaPane = { onSelected ->
                        CriteriaExplorerPane(
                            criteria = criteria,
                            activeCriterion = activeCriterion,
                            scoreSummary = criteria.associate { criterion ->
                                criterion.id to (scores[criterion.id]?.points?.let { "$it / ${criterion.maxPoints}" }
                                    ?: "Puanlanmadı / ${criterion.maxPoints}")
                            },
                            onSelectCriterion = { chosen ->
                                activeCriterionIndex = criteria.indexOfFirst { it.id == chosen.id }.coerceAtLeast(0)
                                onSelected()
                            }
                        )
                    },
                    scoringPane = {
                        GroupScoringPane(
                            sessionId = session?.id.orEmpty(),
                            group = selectedGroup,
                            assessment = assessment,
                            modifier = Modifier.fillMaxSize(),
                            criterion = activeCriterion,
                            criteria = criteria,
                            scores = scores,
                            students = students,
                            memberResults = memberResults,
                            onUndo = { scope.launch { viewModel.undoLastGroupAction() } },
                            canUndo = canUndoGroupAction,
                            quickGrading = quickGrading,
                            levels = levels.filter { it.criterionId == activeCriterion?.id },
                            autoAdvance = autoAdvance,
                            onAutoAdvance = { autoAdvance = it },
                            onScore = { points ->
                                activeCriterion?.let { criterion ->
                                    viewModel.setGroupScore(criterion.id, points)
                                }
                            },
                            onClear = { activeCriterion?.let { viewModel.setGroupScore(it.id, null) } },
                            onNoteChange = { note ->
                                activeCriterion?.let { viewModel.setGroupEvidenceNote(it.id, note) }
                            },
                            noteSaveMessage = groupNoteSaveState?.takeIf {
                                it.key == "group/${session?.id}/${selectedGroup?.id}/${activeCriterion?.id}"
                            }?.status?.label,
                            scoreSaveMessage = scoreSaveState?.takeIf {
                                it.key == "group/${session?.id}/${selectedGroup?.id}/${activeCriterion?.id}"
                            }?.status?.label,
                            onNext = {
                                if (activeCriterionIndex < criteria.lastIndex) activeCriterionIndex++
                            },
                            onSyncMembership = { confirmMembershipSync = true },
                            onPreviousGroup = {
                                val i = groups.indexOfFirst { it.id == selectedGroup?.id }
                                groups.getOrNull(i - 1)?.let { viewModel.selectGroup(it) }
                            },
                            canPreviousGroup = groups.indexOfFirst { it.id == selectedGroup?.id } > 0,
                            canNextGroup = groups.indexOfFirst { it.id == selectedGroup?.id } in 0 until groups.lastIndex,
                            onNextGroup = {
                                val i = groups.indexOfFirst { it.id == selectedGroup?.id }
                                groups.getOrNull(i + 1)?.let { viewModel.selectGroup(it) }
                            }
                        )
                    }
                )
            }
        }
    }

    if (confirmMembershipSync) {
        Dialog(onDismissRequest = { confirmMembershipSync = false }) {
            Surface(
                shape = ArcTheme.shapes.panel,
                color = ArcTheme.colors.surfaceRaised,
                modifier = Modifier.width(440.dp)
            ) {
                Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Grup Üyeliklerini Eşitle", fontWeight = FontWeight.Bold,
                        fontSize = 18.sp, color = ArcTheme.colors.foreground)
                    Text(
                        "Bu oturumdaki puanlanmış grupların üye listeleri güncel sınıf gruplarıyla eşleştirilir. " +
                            "Gruptan çıkarılan öğrenci o grubun notunu kaybeder; yeni üye grup notunu alır. " +
                            "Başka oturumlar ve bireysel notlar etkilenmez.",
                        fontSize = 13.sp, color = ArcTheme.colors.textSecondary
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        ArcButton(text = "Vazgeç",
                            onClick = { confirmMembershipSync = false },
                            variant = ArcButtonVariant.Ghost)
                        Spacer(Modifier.width(8.dp))
                        ArcButton(text = "Üyelikleri Eşitle",
                            onClick = {
                                viewModel.syncGroupMemberships()
                                confirmMembershipSync = false
                            }, variant = ArcButtonVariant.Primary)
                    }
                }
            }
        }
    }
    if (createGroup && editingGroup == null) {
        CreateGroupDialog(
            initialTask = null,
            students = students,
            assignedStudentIds = assignedStudentIds,
            onDismiss = { createGroup = false },
            onConfirm = { title, memberIds ->
                viewModel.addGroupTask(title, memberIds)
                createGroup = false
            }
        )
    }
    editingGroup?.let { group ->
        val groupMemberIds = remember(group.id, group.memberStudentIdsJson) {
            try {
                val array = JSONArray(group.memberStudentIdsJson)
                (0 until array.length()).map { array.getString(it) }.toSet()
            } catch (_: Exception) { emptySet() }
        }
        CreateGroupDialog(
            initialTask = group,
            students = students,
            assignedStudentIds = assignedStudentIds - groupMemberIds,
            onDismiss = { editingGroup = null },
            onConfirm = { title, memberIds ->
                viewModel.updateGroupTask(group, title, memberIds)
                editingGroup = null
            }
        )
    }
    deletingGroup?.let { group ->
        Dialog(onDismissRequest = { deletingGroup = null }) {
            Surface(
                shape = ArcTheme.shapes.panel,
                color = ArcTheme.colors.surfaceRaised,
                modifier = Modifier.fillMaxWidth().widthIn(max = 440.dp)
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Grubu sil?", fontWeight = FontWeight.Bold, color = ArcTheme.colors.foreground)
                    Text(
                        "‘${group.title}’ grubunu silme isteği gönderilecek. Geçmiş değerlendirmesi varsa puanları ve üye kayıtlarını korumak için silme engellenir.",
                        color = ArcTheme.colors.textSecondary
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        ArcButton(text = "Vazgeç", onClick = { deletingGroup = null }, variant = ArcButtonVariant.Ghost)
                        ArcButton(text = "Grubu Sil", onClick = {
                            viewModel.deleteGroupTask(group)
                            deletingGroup = null
                        }, variant = ArcButtonVariant.Outline)
                    }
                }
            }
        }
    }
    if (createRandomGroups) {
        RandomGroupsDialog(
            availableCount = students.count { it.id !in assignedStudentIds },
            onDismiss = { createRandomGroups = false },
            onConfirm = { average ->
                viewModel.createRandomGroups(average)
                createRandomGroups = false
            }
        )
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
    sessionId: String,
    group: GroupTaskEntity?,
    assessment: GroupAssessmentEntity?,
    modifier: Modifier = Modifier,
    criterion: CriterionEntity?,
    criteria: List<CriterionEntity>,
    scores: Map<String, GroupCriterionScoreEntity>,
    students: List<StudentEntity>,
    memberResults: List<GroupMemberResultEntity>,
    onUndo: () -> Unit,
    canUndo: Boolean,
    quickGrading: com.knigdelioglu.puanla.ui.preferences.QuickGradingPreference,
    levels: List<CriterionLevelEntity>,
    autoAdvance: Boolean,
    onAutoAdvance: (Boolean) -> Unit,
    onScore: (Int) -> Unit,
    onClear: () -> Unit,
    onNoteChange: (String) -> Unit,
    noteSaveMessage: String?,
    scoreSaveMessage: String?,
    onNext: () -> Unit,
    onSyncMembership: () -> Unit,
    onPreviousGroup: () -> Unit,
    canPreviousGroup: Boolean,
    canNextGroup: Boolean,
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
    Column(modifier = modifier.fillMaxWidth().padding(12.dp)) {
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
        Text(group.title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = ArcTheme.colors.foreground)
        Text(
            if (assessment?.isCompleted == true) "Grup puanı: " + total + " / " + criteria.sumOf { it.maxPoints }
            else "Kısmi: " + (assessment?.scoredCount ?: 0) + " / " + criteria.size + " ölçüt",
            color = ArcTheme.colors.textSecondary
        )
        ArcButton(
            text = "Üyelikleri Eşitle",
            onClick = onSyncMembership,
            variant = ArcButtonVariant.Outline,
            size = ArcButtonSize.Sm,
            modifier = Modifier.fillMaxWidth()
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
                    var showNoteEditor by remember(sessionId, group.id, criterion.id) {
                        mutableStateOf(false)
                    }
                    CriterionGradingEditor(
                        contextKey = "group/$sessionId/${group.id}/${criterion.id}",
                        criterion = criterion,
                        points = scores[criterion.id]?.points,
                        levels = levels,
                        quickGrading = quickGrading,
                        autoAdvance = autoAdvance,
                        onAutoAdvanceChange = onAutoAdvance,
                        hasNextCriterion = criteria.indexOfFirst { it.id == criterion.id } < criteria.lastIndex,
                        onNextCriterion = onNext,
                        noteEditorOpen = showNoteEditor,
                        onScore = onScore,
                        onClearScore = onClear
                    )
                    if (scoreSaveMessage != null) {
                        Text(
                            "Puan: $scoreSaveMessage",
                            fontSize = 12.sp,
                            color = if (scoreSaveMessage == "Kaydedilemedi")
                                ArcTheme.colors.danger else ArcTheme.colors.textSecondary
                        )
                    }
                    var noteText by remember(sessionId, group.id, criterion.id) {
                        mutableStateOf(scores[criterion.id]?.evidenceNote ?: "")
                    }
                    var noteTouched by remember(sessionId, group.id, criterion.id) {
                        mutableStateOf(false)
                    }
                    LaunchedEffect(sessionId, group.id, criterion.id, scores[criterion.id]?.evidenceNote) {
                        if (!noteTouched) noteText = scores[criterion.id]?.evidenceNote ?: ""
                    }
                    ArcButton(
                        text = if (showNoteEditor) "Gözlem notunu gizle"
                            else if (noteText.isBlank()) "Gözlem ve kanıt notu ekle"
                            else "Gözlem notunu düzenle (mevcut not)",
                        onClick = { showNoteEditor = !showNoteEditor },
                        variant = ArcButtonVariant.Outline,
                        modifier = Modifier.fillMaxWidth(),
                        size = ArcButtonSize.Sm
                    )
                    if (showNoteEditor) {
                        ArcRichTextEditor(
                            value = noteText,
                            onValueChange = {
                                noteText = it
                                noteTouched = true
                                onNoteChange(it)
                            },
                            placeholder = "Grup ölçütüne özel gözlem veya kanıt...",
                            characterCountLabel = { "$it karakter" },
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (noteSaveMessage != null) {
                            Text(
                                noteSaveMessage,
                                fontSize = 12.sp,
                                color = if (noteSaveMessage == "Kaydedilemedi")
                                    ArcTheme.colors.danger else ArcTheme.colors.textSecondary
                            )
                        }
                    } else if (noteText.isNotBlank()) {
                        Text(
                            noteText.take(140) + if (noteText.length > 140) "…" else "",
                            fontSize = 12.sp, color = ArcTheme.colors.textSecondary
                        )
                    }
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
        }
        ScoringTargetToolbar(
            mode = ScoringTargetMode.GROUP,
            onPrevious = onPreviousGroup,
            onUndo = onUndo,
            onNext = onNextGroup,
            canPrevious = canPreviousGroup,
            canUndo = canUndo,
            canNext = canNextGroup
        )
    }
}

@Composable
private fun GroupTaskCard(
    task: GroupTaskEntity,
    students: List<StudentEntity>,
    onToggleChecklist: (Int) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
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
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                ArcButton(text = "Düzenle", onClick = onEdit,
                    variant = ArcButtonVariant.Outline, size = ArcButtonSize.Sm)
                ArcButton(text = "Sil", onClick = onDelete,
                    variant = ArcButtonVariant.Ghost, size = ArcButtonSize.Sm)
            }
            // Member tags
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Grup Üyeleri: ",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ArcTheme.colors.textSecondary
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
                                .fillMaxWidth(),
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
                                    onCheckedChange = { onToggleChecklist(idx) },
                                    label = itemTitle
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
    initialTask: GroupTaskEntity?,
    students: List<StudentEntity>,
    assignedStudentIds: Set<String>,
    onDismiss: () -> Unit,
    onConfirm: (String, List<String>) -> Unit
) {
    val initialMembers = remember(initialTask?.id, initialTask?.memberStudentIdsJson) {
        try {
            val array = JSONArray(initialTask?.memberStudentIdsJson ?: "[]")
            (0 until array.length()).map { array.getString(it) }.toSet()
        } catch (_: Exception) { emptySet() }
    }
    var groupTitle by remember(initialTask?.id) { mutableStateOf(initialTask?.title.orEmpty()) }
    var selectedStudentIds by remember(initialTask?.id) { mutableStateOf(initialMembers) }
    var showAllStudents by remember { mutableStateOf(false) }
    val visibleStudents = remember(students, assignedStudentIds, showAllStudents) {
        if (showAllStudents) students else students.filterNot { it.id in assignedStudentIds }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = ArcTheme.shapes.panel,
            color = ArcTheme.colors.surfaceRaised,
            border = BorderStroke(1.dp, ArcTheme.colors.border),
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth().widthIn(max = 460.dp).heightIn(max = 700.dp)
        ) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(24.dp)) {
                Text(
                    text = if (initialTask == null) "Yeni Grup Oluştur" else "Grubu Düzenle",
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "${visibleStudents.size} öğrenci gösteriliyor · ${assignedStudentIds.size} öğrenci gruplarda",
                        fontSize = 12.sp, color = ArcTheme.colors.textSecondary,
                        modifier = Modifier.weight(1f)
                    )
                    ArcButton(
                        text = if (showAllStudents) "Boştakileri göster" else "Tüm Sınıfı Göster",
                        size = ArcButtonSize.Sm,
                        variant = ArcButtonVariant.Outline,
                        onClick = {
                            if (showAllStudents) {
                                selectedStudentIds = selectedStudentIds - assignedStudentIds
                            }
                            showAllStudents = !showAllStudents
                        }
                    )
                }
                Spacer(Modifier.height(8.dp))

                // One controlled Arc selection component, no nested row/checkbox
                // click handlers that could toggle the same pupil twice.
                ArcMultiSelect(
                    selectedItems = selectedStudentIds,
                    items = visibleStudents.map { it.id },
                    onSelectionChanged = { selectedStudentIds = it - assignedStudentIds },
                    labelProvider = { id ->
                        students.firstOrNull { it.id == id }?.let {
                            "${it.studentNumber} - ${it.fullName}" +
                                if (id in assignedStudentIds) " (başka grupta)" else ""
                        } ?: id
                    },
                    modifier = Modifier
                        .heightIn(max = 200.dp)
                        .verticalScroll(rememberScrollState())
                )

                if (showAllStudents) {
                    Text("Başka gruptaki öğrenciler yalnızca görüntülenir; aynı anda ikinci bir gruba eklenemez.",
                        fontSize = 12.sp, color = ArcTheme.colors.textSecondary)
                }
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
                        text = if (initialTask == null) "Grubu Kur" else "Değişiklikleri Kaydet",
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


@Composable
private fun RandomGroupsDialog(
    availableCount: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var averageText by remember { mutableStateOf("4") }
    val average = averageText.toIntOrNull()?.takeIf { it > 0 }
    val groupSizes = remember(availableCount, average) {
        if (average == null) emptyList() else
            com.knigdelioglu.puanla.domain.group.GroupPartitioner
                .distribute((0 until availableCount).map { it.toString() }, average) { it }
                .map { it.size }
    }
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = ArcTheme.shapes.panel,
            color = ArcTheme.colors.surfaceRaised,
            border = BorderStroke(1.dp, ArcTheme.colors.border),
            modifier = Modifier.fillMaxWidth().widthIn(max = 440.dp).heightIn(max = 700.dp)
        ) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Rastgele Grup Oluştur", fontSize = 18.sp,
                    fontWeight = FontWeight.Bold, color = ArcTheme.colors.foreground)
                Text(
                    "Yalnızca başka bir gruba alınmamış ${availableCount} öğrenci rastgele ve dengeli dağıtılacak.",
                    fontSize = 13.sp, color = ArcTheme.colors.textSecondary
                )
                ArcInput(
                    value = averageText,
                    onValueChange = { input -> averageText = input.filter { it.isDigit() }.take(3) },
                    label = "Ortalama öğrenci sayısı",
                    placeholder = "Örn. 4"
                )
                Text(
                    if (groupSizes.isEmpty()) "Pozitif bir öğrenci sayısı girin."
                    else "${groupSizes.size} grup oluşturulacak · Üye sayıları: ${groupSizes.joinToString(", ")}",
                    fontSize = 13.sp, color = ArcTheme.colors.textSecondary
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    ArcButton(text = "Vazgeç", onClick = onDismiss, variant = ArcButtonVariant.Ghost)
                    Spacer(Modifier.width(8.dp))
                    ArcButton(
                        text = "Grupları Oluştur",
                        onClick = { average?.let(onConfirm) },
                        enabled = average != null && availableCount > 0,
                        variant = ArcButtonVariant.Primary
                    )
                }
            }
        }
    }
}
