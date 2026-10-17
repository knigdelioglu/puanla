package com.knigdelioglu.puanla.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.knigdelioglu.arc.compose.controls.ArcButton
import com.knigdelioglu.arc.compose.controls.ArcButtonSize
import com.knigdelioglu.arc.compose.controls.ArcButtonVariant
import com.knigdelioglu.arc.compose.controls.ArcCheckbox
import com.knigdelioglu.arc.compose.controls.ArcInput
import com.knigdelioglu.arc.compose.controls.ArcMultiSelect
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import com.knigdelioglu.arc.compose.editor.ArcRichTextEditor
import androidx.compose.runtime.saveable.rememberSaveable
import com.knigdelioglu.puanla.data.local.GroupAssessmentEntity
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
    var compactPane by rememberSaveable { mutableStateOf(0) }
    var activeCriterionIndex by remember(selectedGroup?.id, session?.id, rubric?.id) { mutableIntStateOf(0) }
    var autoAdvance by rememberSaveable { mutableStateOf(true) }
    val activeCriterion = criteria.getOrNull(activeCriterionIndex) ?: criteria.firstOrNull()

    val showGroupAssessment = classroom != null && rubric != null && session != null && groups.isNotEmpty()
    val selectedGroupIndex = groups.indexOfFirst { it.id == selectedGroup?.id }
    val frozenMemberIds = remember(assessment?.memberStudentIdsJson, selectedGroup?.memberStudentIdsJson) {
        parseStudentIds(assessment?.memberStudentIdsJson ?: selectedGroup?.memberStudentIdsJson.orEmpty())
    }
    val frozenMembers = remember(students, frozenMemberIds) {
        frozenMemberIds.mapNotNull { id -> students.firstOrNull { it.id == id } }
    }
    val rubricTotal = criteria.sumOf { it.maxPoints }

    BoxWithConstraints(Modifier.fillMaxSize().padding(12.dp)) {
        val wideLayout = maxWidth >= 900.dp
        val managementPane: @Composable (Modifier) -> Unit = { paneModifier ->
            GroupManagementPane(
                modifier = paneModifier,
                groups = groups,
                students = students,
                assignedStudentIds = assignedStudentIds,
                assessments = groupAssessments,
                scoreMaximum = rubricTotal,
                selectedGroup = selectedGroup,
                createGroup = createGroup,
                onCreateGroupToggle = { createGroup = !createGroup },
                onCreateGroup = { title, memberIds ->
                    viewModel.addGroupTask(title, memberIds)
                    createGroup = false
                },
                onRandomGroups = { createRandomGroups = true },
                canRandomGroups = students.any { it.id !in assignedStudentIds },
                onSelectGroup = viewModel::selectGroup,
                onEditGroup = { editingGroup = it },
                onDeleteGroup = { deletingGroup = it },
                onToggleChecklist = { group, index -> viewModel.toggleChecklistItem(group, index) },
                onSyncMemberships = { confirmMembershipSync = true },
                canSyncMemberships = session != null && groupAssessments.isNotEmpty()
            )
        }
        val scoringPane: @Composable (Modifier) -> Unit = { paneModifier ->
            when {
                classroom == null -> Box(paneModifier, contentAlignment = Alignment.Center) {
                    ArcEmptyState("Sınıf seçilmedi", "Grup değerlendirmesi için üst çubuktan sınıf seçin.")
                }
                groups.isEmpty() -> Box(paneModifier, contentAlignment = Alignment.Center) {
                    ArcEmptyState("Henüz grup yok", "Soldaki panelden grup oluşturup öğrencileri seçin.")
                }
                rubric == null -> Box(paneModifier, contentAlignment = Alignment.Center) {
                    ArcEmptyState("Rubrik seçilmedi", "Üst çubuktan bu sınıfa uygun bir rubrik seçin.")
                }
                session == null -> Box(paneModifier, contentAlignment = Alignment.Center) {
                    ArcEmptyState("Oturum seçilmedi", "Üst çubuktan yeni grup değerlendirme oturumu başlatın.")
                }
                selectedGroup == null -> Box(paneModifier, contentAlignment = Alignment.Center) {
                    ArcEmptyState("Grup seçilmedi", "Değerlendirmek istediğiniz grubu soldan seçin.")
                }
                else -> GroupEvaluationBoard(
                    modifier = paneModifier,
                    group = selectedGroup!!,
                    members = frozenMembers,
                    assessment = assessment,
                    criteria = criteria,
                    levels = levels,
                    scores = scores,
                    sessionId = session!!.id,
                    evidenceNote = activeCriterion?.let { scores[it.id]?.evidenceNote }.orEmpty(),
                    noteSaveMessage = groupNoteSaveState?.takeIf {
                        it.key == "group/${session?.id}/${selectedGroup?.id}/${activeCriterion?.id}"
                    }?.status?.label,
                    scoreSaveMessage = scoreSaveState?.takeIf {
                        it.key == "group/${session?.id}/${selectedGroup?.id}/${activeCriterion?.id}"
                    }?.status?.label,
                    canUndo = canUndoGroupAction,
                    canPreviousGroup = selectedGroupIndex > 0,
                    canNextGroup = selectedGroupIndex in 0 until groups.lastIndex,
                    activeCriterionId = activeCriterion?.id,
                    onSelectCriterion = { criterion ->
                        activeCriterionIndex = criteria.indexOfFirst { it.id == criterion.id }.coerceAtLeast(0)
                    },
                    onScore = { criterion, points ->
                        activeCriterionIndex = criteria.indexOfFirst { it.id == criterion.id }.coerceAtLeast(0)
                        viewModel.setGroupScore(criterion.id, points)
                    },
                    onClearScore = { criterion -> viewModel.setGroupScore(criterion.id, null) },
                    onEvidenceNoteChange = { note -> activeCriterion?.let { viewModel.setGroupEvidenceNote(it.id, note) } },
                    onPublish = viewModel::publishCurrentGroupResultToMembers,
                    onUndo = { scope.launch { viewModel.undoLastGroupAction() } },
                    onPreviousGroup = { groups.getOrNull(selectedGroupIndex - 1)?.let(viewModel::selectGroup) },
                    onNextGroup = { groups.getOrNull(selectedGroupIndex + 1)?.let(viewModel::selectGroup) }
                )
            }
        }

        if (wideLayout) {
            Row(Modifier.fillMaxSize().testTag("groupEvaluationWideLayout"), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                managementPane(Modifier.width(300.dp).fillMaxHeight())
                scoringPane(Modifier.weight(1f).fillMaxHeight())
            }
        } else {
            Column(Modifier.fillMaxSize().testTag("groupEvaluationCompactLayout"), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ArcButton("Çalışma Grupları", { compactPane = 0 },
                        variant = if (compactPane == 0) ArcButtonVariant.Primary else ArcButtonVariant.Outline)
                    ArcButton("Grup Puanlama", { compactPane = 1 },
                        variant = if (compactPane == 1) ArcButtonVariant.Primary else ArcButtonVariant.Outline)
                }
                if (compactPane == 0) managementPane(Modifier.weight(1f).fillMaxWidth())
                else scoringPane(Modifier.weight(1f).fillMaxWidth())
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
private fun GroupManagementPane(
    modifier: Modifier = Modifier,
    groups: List<GroupTaskEntity>,
    students: List<StudentEntity>,
    assignedStudentIds: Set<String>,
    assessments: List<GroupAssessmentEntity>,
    scoreMaximum: Int,
    selectedGroup: GroupTaskEntity?,
    createGroup: Boolean,
    onCreateGroupToggle: () -> Unit,
    onCreateGroup: (String, List<String>) -> Unit,
    onRandomGroups: () -> Unit,
    canRandomGroups: Boolean,
    onSelectGroup: (GroupTaskEntity) -> Unit,
    onEditGroup: (GroupTaskEntity) -> Unit,
    onDeleteGroup: (GroupTaskEntity) -> Unit,
    onToggleChecklist: (GroupTaskEntity, Int) -> Unit,
    onSyncMemberships: () -> Unit,
    canSyncMemberships: Boolean
) {
    Surface(
        modifier = modifier.testTag("groupManagementPane"),
        shape = ArcTheme.shapes.panel,
        color = ArcTheme.colors.surfaceRaised,
        border = BorderStroke(1.dp, ArcTheme.colors.borderSubtle)
    ) {
        Column(
            Modifier.fillMaxSize().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Çalışma Grupları (${groups.size})",
                    modifier = Modifier.weight(1f),
                    color = ArcTheme.colors.foreground,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                ArcButton(
                    text = if (createGroup) "Vazgeç" else "+ Yeni Grup Oluştur",
                    onClick = onCreateGroupToggle,
                    size = ArcButtonSize.Sm,
                    variant = if (createGroup) ArcButtonVariant.Ghost else ArcButtonVariant.Outline
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ArcButton(
                    text = "Rastgele Grup Oluştur",
                    onClick = onRandomGroups,
                    enabled = canRandomGroups,
                    size = ArcButtonSize.Sm,
                    variant = ArcButtonVariant.Outline
                )
                if (canSyncMemberships) {
                    ArcButton(
                        text = "Oturum Üyeliklerini Eşitle",
                        onClick = onSyncMemberships,
                        size = ArcButtonSize.Sm,
                        variant = ArcButtonVariant.Ghost
                    )
                }
            }
            if (canSyncMemberships) {
                Text(
                    "Eşitleme yalnızca seçili oturumun geçmiş üye kayıtlarını değiştirir.",
                    color = ArcTheme.colors.textSecondary,
                    fontSize = 11.sp
                )
            }
            if (createGroup) {
                InlineCreateGroupForm(
                    students = students,
                    assignedStudentIds = assignedStudentIds,
                    onCancel = onCreateGroupToggle,
                    onSave = onCreateGroup
                )
            }
            if (groups.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    ArcEmptyState(
                        title = "Henüz grup yok",
                        description = "Yeni grup oluşturup sınıftaki öğrencileri ekleyin."
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(groups, key = { it.id }) { group ->
                        GroupListItem(
                            group = group,
                            assessment = assessments.firstOrNull { it.groupId == group.id },
                            scoreMaximum = scoreMaximum,
                            students = students,
                            selected = selectedGroup?.id == group.id,
                            onSelect = { onSelectGroup(group) },
                            onEdit = { onEditGroup(group) },
                            onDelete = { onDeleteGroup(group) },
                            onToggleChecklist = { index -> onToggleChecklist(group, index) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InlineCreateGroupForm(
    students: List<StudentEntity>,
    assignedStudentIds: Set<String>,
    onCancel: () -> Unit,
    onSave: (String, List<String>) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedIds by remember { mutableStateOf(emptySet<String>()) }
    val available = remember(students, assignedStudentIds) {
        students.filterNot { it.id in assignedStudentIds }
    }
    Surface(
        shape = ArcTheme.shapes.control,
        color = ArcTheme.colors.surfaceMuted,
        border = BorderStroke(1.dp, ArcTheme.colors.borderSubtle)
    ) {
        Column(
            Modifier.fillMaxWidth().padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ArcInput(
                value = title,
                onValueChange = { title = it },
                label = "Grup Adı",
                placeholder = "Örn: Mikroskop Ekibi"
            )
            Text(
                "Grup Üyelerini Seçin (${selectedIds.size} seçili)",
                color = ArcTheme.colors.foreground,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
            if (available.isEmpty()) {
                Text("Gruplara eklenebilecek öğrenci kalmadı.", color = ArcTheme.colors.textSecondary, fontSize = 12.sp)
            } else {
                Column(
                    Modifier.fillMaxWidth().heightIn(max = 220.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    available.forEach { student ->
                        ArcCheckbox(
                            checked = student.id in selectedIds,
                            onCheckedChange = { checked ->
                                selectedIds = if (checked) selectedIds + student.id else selectedIds - student.id
                            },
                            label = "${student.studentNumber}  ${student.fullName}"
                        )
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                ArcButton("İptal", onCancel, variant = ArcButtonVariant.Ghost, size = ArcButtonSize.Sm)
                Spacer(Modifier.width(6.dp))
                ArcButton(
                    "Grubu Kaydet",
                    onClick = { onSave(title.trim(), selectedIds.toList()) },
                    enabled = title.isNotBlank() && selectedIds.isNotEmpty(),
                    size = ArcButtonSize.Sm
                )
            }
        }
    }
}

@Composable
private fun GroupListItem(
    group: GroupTaskEntity,
    assessment: GroupAssessmentEntity?,
    scoreMaximum: Int,
    students: List<StudentEntity>,
    selected: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleChecklist: (Int) -> Unit
) {
    var showChecklist by remember(group.id) { mutableStateOf(false) }
    val memberNames = remember(group.memberStudentIdsJson, students) {
        parseStudentIds(group.memberStudentIdsJson)
            .mapNotNull { id -> students.firstOrNull { it.id == id }?.fullName }
    }
    val checklist = remember(group.checklistJson) {
        runCatching {
            val array = JSONArray(group.checklistJson)
            (0 until array.length()).map { index ->
                val item = array.getJSONObject(index)
                item.getString("title") to item.getBoolean("checked")
            }
        }.getOrDefault(emptyList())
    }
    val total = assessment?.definitiveTotal
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onSelect),
        shape = ArcTheme.shapes.control,
        color = if (selected) ArcTheme.colors.accent.copy(alpha = 0.06f) else ArcTheme.colors.surface,
        border = BorderStroke(
            if (selected) 1.5.dp else 1.dp,
            if (selected) ArcTheme.colors.accent else ArcTheme.colors.borderSubtle
        )
    ) {
        Column(Modifier.padding(11.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    group.title,
                    modifier = Modifier.weight(1f),
                    color = ArcTheme.colors.foreground,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 2
                )
                Text(
                    total?.let { "$it/$scoreMaximum" } ?: "—",
                    color = if (assessment?.isCompleted == true) ArcTheme.colors.accent else ArcTheme.colors.textSecondary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
            Text(
                "Üyeler (${memberNames.size}): ${memberNames.joinToString(", ").ifBlank { "Öğrenci seçilmedi" }}",
                color = ArcTheme.colors.textSecondary,
                fontSize = 12.sp,
                maxLines = 2
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    when {
                        assessment?.isCompleted == true -> "✓ Üyelere aktarıldı"
                        assessment != null -> "${assessment.scoredCount} ölçüt puanlandı"
                        else -> "Değerlendirilmedi"
                    },
                    modifier = Modifier.weight(1f),
                    color = if (assessment?.isCompleted == true) ArcTheme.colors.success else ArcTheme.colors.textSecondary,
                    fontSize = 11.sp
                )
                ArcButton("Düzenle", onEdit, size = ArcButtonSize.Sm, variant = ArcButtonVariant.Ghost)
                ArcButton("Sil", onDelete, size = ArcButtonSize.Sm, variant = ArcButtonVariant.Ghost)
            }
            if (checklist.isNotEmpty()) {
                ArcButton(
                    text = if (showChecklist) "Hazırlık listesini gizle" else "Hazırlık (${checklist.count { it.second }}/${checklist.size})",
                    onClick = { showChecklist = !showChecklist },
                    size = ArcButtonSize.Sm,
                    variant = ArcButtonVariant.Outline
                )
                if (showChecklist) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        checklist.forEachIndexed { index, (label, checked) ->
                            ArcCheckbox(checked, { onToggleChecklist(index) }, label = label)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupEvaluationBoard(
    modifier: Modifier = Modifier,
    group: GroupTaskEntity,
    members: List<StudentEntity>,
    assessment: GroupAssessmentEntity?,
    criteria: List<CriterionEntity>,
    levels: List<CriterionLevelEntity>,
    scores: Map<String, GroupCriterionScoreEntity>,
    sessionId: String,
    evidenceNote: String,
    noteSaveMessage: String?,
    scoreSaveMessage: String?,
    canUndo: Boolean,
    canPreviousGroup: Boolean,
    canNextGroup: Boolean,
    activeCriterionId: String?,
    onSelectCriterion: (CriterionEntity) -> Unit,
    onScore: (CriterionEntity, Int) -> Unit,
    onClearScore: (CriterionEntity) -> Unit,
    onEvidenceNoteChange: (String) -> Unit,
    onPublish: () -> Unit,
    onUndo: () -> Unit,
    onPreviousGroup: () -> Unit,
    onNextGroup: () -> Unit
) {
    val totalPoints = criteria.sumOf { it.maxPoints }
    val missingCount = criteria.count { scores[it.id]?.points == null }
    val groupTotal = assessment?.definitiveTotal
    Surface(
        modifier = modifier.testTag("groupEvaluationBoard"),
        shape = ArcTheme.shapes.panel,
        color = ArcTheme.colors.surfaceRaised,
        border = BorderStroke(1.dp, ArcTheme.colors.borderSubtle)
    ) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val compactHeader = maxWidth < 720.dp
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "Grup Değerlendirmesi · ${members.size} Öğrenci",
                                    color = ArcTheme.colors.textSecondary,
                                    fontSize = 12.sp
                                )
                                Text(group.title, color = ArcTheme.colors.foreground,
                                    fontWeight = FontWeight.Bold, fontSize = 22.sp)
                            }
                            if (!compactHeader) GroupScoreSummary(groupTotal, totalPoints)
                        }
                        Text(
                            "Üyeler: " + members.joinToString(" · ") { "${it.fullName} (#${it.studentNumber})" }
                                .ifBlank { "Üye bulunamadı" },
                            color = ArcTheme.colors.textSecondary,
                            fontSize = 13.sp,
                            maxLines = 2
                        )
                        if (compactHeader) GroupScoreSummary(groupTotal, totalPoints)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (assessment?.isCompleted != true) {
                                Text(
                                    if (criteria.isEmpty()) "Ölçüt bulunamadı"
                                    else "$missingCount ölçüt puanlanmadı",
                                    modifier = Modifier.weight(1f),
                                    color = ArcTheme.colors.textSecondary,
                                    fontSize = 12.sp
                                )
                            } else {
                                Spacer(Modifier.weight(1f))
                            }
                            ArcButton(
                                text = if (assessment?.isCompleted == true) "Grup Puanlarını Üyelere Aktar"
                                    else "Önce Tüm Ölçütleri Puanlayın",
                                onClick = onPublish,
                                enabled = assessment?.isCompleted == true,
                                size = ArcButtonSize.Md
                            )
                        }
                    }
                }
                Divider(color = ArcTheme.colors.borderSubtle)
                if (criteria.isEmpty()) {
                    ArcEmptyState("Ölçüt bulunamadı", "Seçili rubrikte puanlanacak ölçüt yok.")
                } else {
                    BoxWithConstraints(Modifier.fillMaxWidth()) {
                        val twoColumns = maxWidth >= 760.dp
                        if (twoColumns) {
                            criteria.chunked(2).forEach { pair ->
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    pair.forEach { criterion ->
                                        GroupCriterionCard(
                                            modifier = Modifier.weight(1f),
                                            criterion = criterion,
                                            allMaxPoints = totalPoints,
                                            levels = levels.filter { it.criterionId == criterion.id },
                                            points = scores[criterion.id]?.points,
                                            active = criterion.id == activeCriterionId,
                                            saveMessage = scoreSaveMessage.takeIf { criterion.id == activeCriterionId },
                                            onSelect = { onSelectCriterion(criterion) },
                                            onScore = { points -> onScore(criterion, points) },
                                            onClear = { onClearScore(criterion) }
                                        )
                                    }
                                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                                }
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                criteria.forEach { criterion ->
                                    GroupCriterionCard(
                                        criterion = criterion,
                                        allMaxPoints = totalPoints,
                                        levels = levels.filter { it.criterionId == criterion.id },
                                        points = scores[criterion.id]?.points,
                                        active = criterion.id == activeCriterionId,
                                        saveMessage = scoreSaveMessage.takeIf { criterion.id == activeCriterionId },
                                        onSelect = { onSelectCriterion(criterion) },
                                        onScore = { points -> onScore(criterion, points) },
                                        onClear = { onClearScore(criterion) }
                                    )
                                }
                            }
                        }
                    }
                }
                Divider(color = ArcTheme.colors.borderSubtle)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "Grup Gözlem Notu" + activeCriterionId?.let { id ->
                            criteria.firstOrNull { it.id == id }?.let { " · ${it.title}" }.orEmpty()
                        }.orEmpty(),
                        color = ArcTheme.colors.foreground,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Not, seçili ölçütün grup değerlendirmesine kaydedilir.",
                        color = ArcTheme.colors.textSecondary,
                        fontSize = 12.sp
                    )
                    if (activeCriterionId != null) {
                        GroupEvidenceNoteEditor(
                            key = "$sessionId/${group.id}/$activeCriterionId",
                            value = evidenceNote,
                            onValueChange = onEvidenceNoteChange
                        )
                        noteSaveMessage?.let { message ->
                            Text(
                                message,
                                color = if (message == "Kaydedilemedi") ArcTheme.colors.danger
                                    else ArcTheme.colors.textSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
                if (assessment?.isCompleted == true) {
                    Text(
                        "Grup değerlendirmesi tamamlandı. Bu işlem grup üyesi sonuçlarını yeniler; bireysel notları değiştirmez.",
                        color = ArcTheme.colors.success,
                        fontSize = 12.sp
                    )
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
    }
}

@Composable
private fun GroupScoreSummary(score: Int?, total: Int) {
    Column(horizontalAlignment = Alignment.End) {
        Text("GRUP PUANI", color = ArcTheme.colors.textSecondary, fontSize = 11.sp)
        Text(
            score?.let { "$it / $total" } ?: "— / $total",
            color = ArcTheme.colors.foreground,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp
        )
    }
}

@Composable
private fun GroupCriterionCard(
    modifier: Modifier = Modifier,
    criterion: CriterionEntity,
    allMaxPoints: Int,
    levels: List<CriterionLevelEntity>,
    points: Int?,
    active: Boolean,
    saveMessage: String?,
    onSelect: () -> Unit,
    onScore: (Int) -> Unit,
    onClear: () -> Unit
) {
    Surface(
        modifier = modifier.clickable(onClick = onSelect),
        shape = ArcTheme.shapes.panel,
        color = if (active) ArcTheme.colors.accent.copy(alpha = 0.035f) else ArcTheme.colors.surface,
        border = BorderStroke(
            if (active) 1.5.dp else 1.dp,
            if (active) ArcTheme.colors.accent.copy(alpha = 0.7f) else ArcTheme.colors.borderSubtle
        )
    ) {
        Column(Modifier.padding(11.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    "${(criterion.orderIndex + 1).toString().padStart(2, '0')}. ${criterion.title}",
                    modifier = Modifier.weight(1f),
                    color = ArcTheme.colors.foreground,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 2
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Ağırlık: %${if (allMaxPoints > 0) (criterion.maxPoints * 100 / allMaxPoints) else 0}",
                    color = ArcTheme.colors.textSecondary,
                    fontSize = 12.sp
                )
            }
            if (levels.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    levels.sortedBy { it.orderIndex }.forEachIndexed { index, level ->
                        val isSelected = points == level.points
                        Surface(
                            onClick = { onScore(level.points) },
                            modifier = Modifier.weight(1f).heightIn(min = 156.dp),
                            shape = ArcTheme.shapes.control,
                            color = if (isSelected) ArcTheme.colors.accent else ArcTheme.colors.surfaceRaised,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) ArcTheme.colors.accent else ArcTheme.colors.borderSubtle
                            )
                        ) {
                            Column(
                                Modifier.fillMaxWidth().padding(horizontal = 7.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    groupLevelTitle(index),
                                    color = if (isSelected) ArcTheme.colors.accentForeground else ArcTheme.colors.foreground,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp,
                                    maxLines = 2
                                )
                                Text(
                                    "${level.points} Puan",
                                    color = if (isSelected) ArcTheme.colors.accentForeground else ArcTheme.colors.accent,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                Spacer(Modifier.weight(1f))
                                Text(
                                    level.description,
                                    color = if (isSelected) ArcTheme.colors.accentForeground.copy(alpha = 0.92f)
                                        else ArcTheme.colors.textSecondary,
                                    fontSize = 11.sp,
                                    maxLines = 5
                                )
                            }
                        }
                    }
                }
            } else {
                Text(
                    "Bu ölçütte hazır başarı düzeyi yok. 0–${criterion.maxPoints} arasında tam puan girin.",
                    color = ArcTheme.colors.textSecondary,
                    fontSize = 12.sp
                )
                ArcInput(
                    value = points?.toString().orEmpty(),
                    onValueChange = { value ->
                        if (value.isBlank()) onClear()
                        else value.toIntOrNull()?.takeIf { it in 0..criterion.maxPoints }?.let(onScore)
                    },
                    label = "Kriter puanı (0–${criterion.maxPoints})",
                    placeholder = "Puanlanmadı"
                )
            }
            if (points != null) {
                ArcButton(
                    "Puanı Kaldır",
                    onClick = onClear,
                    variant = ArcButtonVariant.Ghost,
                    size = ArcButtonSize.Sm
                )
            }
            saveMessage?.let { message ->
                Text(
                    "Puan: $message",
                    color = if (message == "Kaydedilemedi") ArcTheme.colors.danger
                        else ArcTheme.colors.textSecondary,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun GroupEvidenceNoteEditor(
    key: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    var text by remember(key) { mutableStateOf(value) }
    var touched by remember(key) { mutableStateOf(false) }
    LaunchedEffect(key, value) {
        if (!touched) text = value
    }
    ArcRichTextEditor(
        value = text,
        onValueChange = {
            text = it
            touched = true
            onValueChange(it)
        },
        placeholder = "Gruptaki çalışma ve kanıta ilişkin gözlem...",
        characterCountLabel = { "$it karakter" },
        modifier = Modifier.fillMaxWidth()
    )
}

private fun groupLevelTitle(index: Int): String = when (index) {
    0 -> "Destek Gerekli"
    1 -> "Gelişmekte"
    2 -> "Yetkin"
    else -> "Örnek Düzey"
}

private fun parseStudentIds(json: String): List<String> = runCatching {
    val array = JSONArray(json)
    (0 until array.length()).map { array.getString(it) }
}.getOrDefault(emptyList())

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
