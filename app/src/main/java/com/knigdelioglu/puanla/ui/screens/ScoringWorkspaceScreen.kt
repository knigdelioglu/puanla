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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import kotlinx.coroutines.launch
import androidx.compose.runtime.key
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.knigdelioglu.arc.compose.controls.ArcButton
import com.knigdelioglu.arc.compose.controls.ArcButtonSize
import com.knigdelioglu.arc.compose.controls.ArcButtonVariant
import com.knigdelioglu.arc.compose.controls.ArcCombobox
import com.knigdelioglu.arc.compose.controls.ArcSearchField
import com.knigdelioglu.arc.compose.editor.ArcRichTextEditor
import com.knigdelioglu.arc.compose.datavis.ArcAnimatedCounter
import com.knigdelioglu.arc.compose.display.ArcAvatar
import com.knigdelioglu.arc.compose.display.ArcBadge
import com.knigdelioglu.arc.compose.display.ArcBadgeVariant
import com.knigdelioglu.arc.compose.display.ArcAccordion
import com.knigdelioglu.arc.compose.display.ArcCard
import com.knigdelioglu.arc.compose.display.ArcEmptyState
import com.knigdelioglu.arc.compose.foundation.ArcTheme
import com.knigdelioglu.puanla.data.local.CriterionEntity
import com.knigdelioglu.puanla.data.local.StudentGroupGrade
import com.knigdelioglu.puanla.data.local.StudentEntity
import com.knigdelioglu.puanla.data.local.CriterionLevelEntity
import com.knigdelioglu.puanla.ui.preferences.QuickGradingPreference
import com.knigdelioglu.puanla.data.local.fullName
import com.knigdelioglu.puanla.data.local.initials
import com.knigdelioglu.puanla.ui.viewmodel.PuanlaViewModel

/**
 * The criterion selection belongs to a student/rubric pair, not the screen.
 * Changing either identity restarts grading at criterion one immediately.
 */
@Composable
internal fun rememberStudentCriterionIndex(studentId: String?, rubricId: String?, runId: String? = null): MutableIntState =
    remember(studentId, rubricId, runId) { mutableIntStateOf(0) }

@Composable
fun ScoringWorkspaceScreen(viewModel: PuanlaViewModel) {
    val selectedClassroom by viewModel.selectedClassroom.collectAsState()
    val selectedRubric by viewModel.selectedRubric.collectAsState()
    val selectedRun by viewModel.selectedRun.collectAsState()
    val students by viewModel.students.collectAsState()
    val selectedStudent by viewModel.selectedStudent.collectAsState()
    val criteria by viewModel.criteria.collectAsState()
    val levels by viewModel.levels.collectAsState()
    val quickGrading by viewModel.quickGradingPreference.collectAsState()
    val currentAssessment by viewModel.currentAssessment.collectAsState()
    val currentScores by viewModel.currentScores.collectAsState()
    val noteSaveState by viewModel.evidenceNoteSaveState.collectAsState()
    val scoreSaveState by viewModel.scoreSaveState.collectAsState()
    val studentGroupGrades by viewModel.currentStudentGroupGrades.collectAsState()
    val classAssessments by viewModel.classAssessments.collectAsState()
    val scope = rememberCoroutineScope()
    val undoSelected: () -> Unit = { scope.launch { viewModel.undoLastAction() } }

    var searchQuery by remember { mutableStateOf("") }
    // Reset to the first criterion whenever the selected student or rubric changes.
    // Keyed state prevents the previous student's last criterion flashing on screen.
    var activeCriterionIndex by rememberStudentCriterionIndex(selectedStudent?.id, selectedRubric?.id, selectedRun?.id)
    var autoAdvanceQuickScore by rememberSaveable { mutableStateOf(true) }
    var lastCriterionContext by remember { mutableStateOf<Pair<String, String>?>(null) }
    var readyCriterionContext by remember { mutableStateOf<Pair<String, String>?>(null) }

    // Changing student in the same session always starts at measure one.
    // Returning to a session (including after app restart) resumes its saved
    // measure, without carrying over another session's selection.
    LaunchedEffect(selectedStudent?.id, selectedRun?.id, criteria.size) {
        val studentId = selectedStudent?.id ?: return@LaunchedEffect
        val runId = selectedRun?.id ?: return@LaunchedEffect
        if (criteria.isEmpty()) return@LaunchedEffect
        val context = runId to studentId
        if (lastCriterionContext != context) {
            activeCriterionIndex = if (lastCriterionContext?.first != runId) {
                viewModel.lastCriterionIndex(studentId, runId).coerceIn(0, criteria.lastIndex)
            } else 0
            lastCriterionContext = context
        }
        readyCriterionContext = context
    }

    LaunchedEffect(activeCriterionIndex, selectedStudent?.id, selectedRun?.id, readyCriterionContext) {
        val studentId = selectedStudent?.id ?: return@LaunchedEffect
        val runId = selectedRun?.id ?: return@LaunchedEffect
        if (readyCriterionContext == (runId to studentId) && criteria.isNotEmpty()) {
            viewModel.rememberCriterionIndex(studentId, runId, activeCriterionIndex)
        }
    }

    val filteredStudents = remember(students, searchQuery) {
        if (searchQuery.isBlank()) students
        else students.filter {
            it.fullName.contains(searchQuery, ignoreCase = true) || it.studentNumber.contains(searchQuery)
        }
    }

    val activeCriterion = criteria.getOrNull(activeCriterionIndex) ?: criteria.firstOrNull()
    val noteKey = listOfNotNull(selectedStudent?.id, selectedRun?.id, activeCriterion?.id).joinToString("/")
    val noteSaveMessage = noteSaveState?.takeIf { it.key == noteKey }?.status?.label
    val scoreSaveMessage = scoreSaveState?.takeIf {
        it.key == "individual/${selectedClassroom?.id}/${selectedRun?.id}/${selectedStudent?.id}/${activeCriterion?.id}"
    }?.status?.label

    // Map student ID to assessment completion status
    val assessmentMap = remember(classAssessments) {
        classAssessments.associateBy { it.studentId }
    }

    if (selectedClassroom == null || selectedRubric == null || selectedRun == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            ArcEmptyState(
                title = if (selectedClassroom == null || selectedRubric == null)
                    "Sınıf veya Rubrik Seçilmedi" else "Değerlendirme Seçilmedi",
                description = if (selectedClassroom == null || selectedRubric == null)
                    "Başlamak için sınıf ve rubrik seçin."
                else "Üstteki Değerlendirmeler düğmesinden yeni bir değerlendirme oluşturun ya da eskisini açın."
            )
        }
        return
    }

    if (selectedRun?.status == "ARCHIVED") {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            ArcEmptyState(
                title = "Arşivlenmiş Değerlendirme",
                description = "Sonuçlar Raporlar bölümünden incelenebilir. Puanları düzenlemek için Değerlendirmeler menüsünden arşivden çıkarın."
            )
        }
        return
    }

    // The same responsive three-pane workspace is now used in Group mode.
    // Individual scoring still owns its session-specific grading and undo actions.
    UnifiedScoringLayout(
        mode = ScoringTargetMode.INDIVIDUAL,
        contextKey = "${selectedClassroom?.id}/${selectedRubric?.id}/${selectedRun?.id}",
        modifier = Modifier.fillMaxSize(),
        targetPane = { onSelected ->
            StudentRosterPane(
                students = filteredStudents,
                selectedStudent = selectedStudent,
                assessmentMap = assessmentMap,
                searchQuery = searchQuery,
                onSearchChange = { searchQuery = it },
                onSelectStudent = {
                    viewModel.selectStudent(it)
                    onSelected()
                }
            )
        },
        criteriaPane = { onSelected ->
            CriteriaExplorerPane(
                criteria = criteria,
                activeCriterion = activeCriterion,
                scoreSummary = criteria.associate { criterion ->
                    criterion.id to (currentScores[criterion.id]?.points?.let { "$it / ${criterion.maxPoints}" }
                        ?: "Puanlanmadı / ${criterion.maxPoints}")
                },
                onSelectCriterion = { chosen ->
                    activeCriterionIndex = criteria.indexOfFirst { it.id == chosen.id }.coerceAtLeast(0)
                    onSelected()
                }
            )
        },
        scoringPane = {
            ScoringControlPane(
                runId = selectedRun?.id.orEmpty(),
                student = selectedStudent,
                criterion = activeCriterion,
                criteria = criteria,
                currentAssessment = currentAssessment,
                currentScores = currentScores,
                noteSaveMessage = noteSaveMessage,
                scoreSaveMessage = scoreSaveMessage,
                groupGrades = studentGroupGrades,
                levels = levels.filter { it.criterionId == activeCriterion?.id },
                autoAdvanceQuickScore = autoAdvanceQuickScore,
                quickGrading = quickGrading,
                onAutoAdvanceQuickScoreChange = { autoAdvanceQuickScore = it },
                onNextCriterion = {
                    if (activeCriterionIndex < criteria.lastIndex) activeCriterionIndex += 1
                },
                onScore = { points, note ->
                    activeCriterion?.let { crit -> viewModel.setScore(crit.id, points, note) }
                },
                onClearScore = {
                    activeCriterion?.let { crit -> viewModel.setScore(crit.id, null, null) }
                },
                onNoteChange = { note ->
                    activeCriterion?.let { crit -> viewModel.setEvidenceNote(crit.id, note) }
                },
                onPrevStudent = { viewModel.previousStudent() },
                onNextStudent = { viewModel.nextStudent() },
                onUndo = undoSelected
            )
        }
    )
}

@Composable
private fun StudentRosterPane(
    students: List<StudentEntity>,
    selectedStudent: StudentEntity?,
    assessmentMap: Map<String, com.knigdelioglu.puanla.data.local.AssessmentEntity>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onSelectStudent: (StudentEntity) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = ArcTheme.colors.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Öğrenci Listesi",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = ArcTheme.colors.foreground
                )
                ArcBadge(
                    text = "${students.size} Öğrenci",
                    variant = ArcBadgeVariant.Default
                )
            }

            Spacer(Modifier.height(10.dp))

            ArcCombobox(
                selectedItem = selectedStudent,
                items = students,
                onItemSelected = onSelectStudent,
                placeholder = "Öğrenciye hızlı geçiş",
                labelProvider = { "${it.studentNumber} - ${it.fullName}" },
                searchQueryFilter = { student, query ->
                    student.fullName.contains(query, ignoreCase = true) ||
                        student.studentNumber.contains(query)
                }
            )
            Spacer(Modifier.height(8.dp))
            ArcSearchField(
                query = searchQuery,
                onQueryChange = onSearchChange,
                placeholder = "Listeyi filtrele..."
            )

            Spacer(Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(students) { student ->
                    val isSelected = student.id == selectedStudent?.id
                    val asm = assessmentMap[student.id]

                    val (badgeText, badgeVariant) = when {
                        asm?.isCompleted == true -> "Tamamlandı (${asm.definitiveTotal})" to ArcBadgeVariant.Success
                        (asm?.scoredCount ?: 0) > 0 -> "${asm?.scoredCount} Puanlandı" to ArcBadgeVariant.Warning
                        else -> "Puanlanmadı" to ArcBadgeVariant.Default
                    }

                    ArcCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectStudent(student) },
                        color = if (isSelected) ArcTheme.colors.surfaceRaised else ArcTheme.colors.surface,
                        border = BorderStroke(
                            if (isSelected) 2.dp else 1.dp,
                            if (isSelected) ArcTheme.colors.accent else ArcTheme.colors.borderSubtle
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ArcAvatar(
                                initials = student.initials,
                                size = 38.dp
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = student.fullName,
                                    fontSize = 15.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                    color = ArcTheme.colors.foreground
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = "No: ${student.studentNumber}",
                                    fontSize = 13.sp,
                                    color = ArcTheme.colors.textSecondary
                                )
                            }
                            ArcBadge(
                                text = badgeText,
                                variant = badgeVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScoringControlPane(
    runId: String,
    student: StudentEntity?,
    criterion: CriterionEntity?,
    criteria: List<CriterionEntity>,
    currentAssessment: com.knigdelioglu.puanla.data.local.AssessmentEntity?,
    currentScores: Map<String, com.knigdelioglu.puanla.data.local.CriterionScoreEntity>,
    noteSaveMessage: String?,
    scoreSaveMessage: String?,
    groupGrades: List<StudentGroupGrade>,
    levels: List<CriterionLevelEntity>,
    autoAdvanceQuickScore: Boolean,
    quickGrading: QuickGradingPreference,
    onAutoAdvanceQuickScoreChange: (Boolean) -> Unit,
    onNextCriterion: () -> Unit,
    onScore: (Int?, String?) -> Unit,
    onClearScore: () -> Unit,
    onNoteChange: (String) -> Unit,
    onPrevStudent: () -> Unit,
    onNextStudent: () -> Unit,
    onUndo: () -> Unit
) {
    val scrollState = rememberScrollState()
    val colors = ArcTheme.colors

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = colors.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            if (student == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    ArcEmptyState(
                        title = "Öğrenci Seçilmedi",
                        description = "Puanlamak için sol listeden bir öğrenci seçin."
                    )
                }
                return@Column
            }

            // Student Overview Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ArcAvatar(initials = student.initials, size = 46.dp)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = student.fullName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.foreground
                    )
                    Text(
                        text = "Öğrenci No: ${student.studentNumber}",
                        fontSize = 13.sp,
                        color = colors.textSecondary
                    )
                }

                // Definitive Score or Incomplete Badge
                Column(horizontalAlignment = Alignment.End) {
                    if (currentAssessment?.isCompleted == true) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Kesin Not: ",
                                fontSize = 13.sp,
                                color = colors.textSecondary
                            )
                            ArcAnimatedCounter(
                                count = currentAssessment.definitiveTotal ?: 0,
                                fontSize = 22.sp,
                                color = colors.success
                            )
                            Text(" / 100", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = colors.foreground)
                        }
                        ArcBadge(text = "Tamamlandı", variant = ArcBadgeVariant.Success)
                    } else {
                        val scored = currentAssessment?.scoredCount ?: 0
                        Text(
                            text = "$scored / ${criteria.size} Ölçüt",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.warning
                        )
                        ArcBadge(text = "Eksik bireysel değerlendirme", variant = ArcBadgeVariant.Warning)
                    }
                }
            }

            if (groupGrades.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Surface(
                    shape = ArcTheme.shapes.control,
                    border = BorderStroke(1.dp, colors.borderSubtle),
                    color = colors.surfaceRaised
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text("Grup değerlendirmesinden aktarılan notlar",
                            fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.foreground)
                        // Keep scoring controls visible even after many group sessions.
                        groupGrades.take(2).forEach { entry ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(entry.sessionTitle, fontSize = 12.sp, color = colors.foreground)
                                    Text(entry.groupTitle, fontSize = 11.sp, color = colors.textSecondary)
                                }
                                Text(
                                    if (entry.isCompleted) entry.definitiveTotal?.toString() ?: "Eksik"
                                    else "Eksik",
                                    fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                                    color = if (entry.isCompleted) colors.success else colors.warning
                                )
                            }
                        }
                        if (groupGrades.size > 2) {
                            Text("Diğer ${groupGrades.size - 2} grup oturumu Grup modunda görülebilir.",
                                fontSize = 11.sp, color = colors.textSecondary)
                        }
                        Text("Bu notlar bireysel rubrik puanlarını değiştirmez.",
                            fontSize = 11.sp, color = colors.textSecondary)
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            Divider(color = colors.borderSubtle)
            Spacer(Modifier.height(14.dp))

            // Scrollable Scoring Area
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (criterion != null) {
                    val currentScore = currentScores[criterion.id]
                    val points = currentScore?.points
                    val maxPoints = criterion.maxPoints

                    val hasNextCriterion =
                        criteria.indexOfFirst { it.id == criterion.id } in 0 until criteria.lastIndex
                    var showNoteEditor by remember(runId, student.id, criterion.id) {
                        mutableStateOf(false)
                    }

                    ArcCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "${criterion.orderIndex + 1}. ${criterion.title}",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.foreground,
                                    modifier = Modifier.weight(1f)
                                )
                                ArcBadge(
                                    text = points?.let { "$it / $maxPoints" } ?: "Puanlanmadı",
                                    variant = if (points != null) ArcBadgeVariant.Success else ArcBadgeVariant.Default
                                )
                            }

                            Spacer(Modifier.height(14.dp))
                            CriterionGradingEditor(
                                contextKey = "individual/$runId/${student.id}/${criterion.id}",
                                criterion = criterion,
                                points = points,
                                levels = levels,
                                quickGrading = quickGrading,
                                autoAdvance = autoAdvanceQuickScore,
                                onAutoAdvanceChange = onAutoAdvanceQuickScoreChange,
                                hasNextCriterion = hasNextCriterion,
                                noteEditorOpen = showNoteEditor,
                                onNextCriterion = onNextCriterion,
                                onScore = { chosen ->
                                    onScore(chosen, currentScore?.evidenceNote)
                                },
                                onClearScore = onClearScore
                            )
                            if (scoreSaveMessage != null) {
                                Text(
                                    "Puan: $scoreSaveMessage",
                                    fontSize = 12.sp,
                                    color = if (scoreSaveMessage == "Kaydedilemedi")
                                        ArcTheme.colors.danger else ArcTheme.colors.textSecondary
                                )
                            }
                        }
                    }

                    val guidance = buildList {
                        if (criterion.description.isNotBlank()) {
                            add("Ölçüt yönergesi" to criterion.description)
                        }
                        if (levels.isNotEmpty()) {
                            add("Puan düzeylerinin açıklamaları" to levels.joinToString("\n") {
                                "${it.points} puan: ${it.description}"
                            })
                        }
                    }
                    if (guidance.isNotEmpty()) {
                        ArcAccordion(items = guidance, modifier = Modifier.fillMaxWidth())
                    }

                    // Evidence / Observation Note
                    var noteText by remember(runId, student.id, criterion.id) {
                        mutableStateOf(currentScore?.evidenceNote ?: "")
                    }
                    var noteTouched by remember(runId, student.id, criterion.id) { mutableStateOf(false) }
                    LaunchedEffect(runId, student.id, criterion.id, currentScore?.evidenceNote) {
                        if (!noteTouched) noteText = currentScore?.evidenceNote ?: ""
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
                            placeholder = "Ölçüte özel gözlem veya kanıt...",
                            characterCountLabel = { "$it karakter" },
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (noteSaveMessage != null) {
                            Text(noteSaveMessage, fontSize = 12.sp,
                                color = if (noteSaveMessage == "Kaydedilemedi") ArcTheme.colors.danger
                                    else ArcTheme.colors.textSecondary)
                        }
                    } else if (noteText.isNotBlank()) {
                        Text(noteText.take(140) + if (noteText.length > 140) "…" else "",
                            fontSize = 12.sp, color = ArcTheme.colors.textSecondary,
                            maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }

            }

            Spacer(Modifier.height(12.dp))

            // Footer stays visible while the criterion body scrolls.
            ScoringTargetToolbar(
                mode = ScoringTargetMode.INDIVIDUAL,
                onPrevious = onPrevStudent,
                onUndo = onUndo,
                onNext = onNextStudent
            )
        }
    }
}
