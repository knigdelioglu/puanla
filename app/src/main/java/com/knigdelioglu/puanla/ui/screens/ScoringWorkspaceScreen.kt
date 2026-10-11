package com.knigdelioglu.puanla.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Grading
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.knigdelioglu.arc.compose.controls.ArcButton
import com.knigdelioglu.arc.compose.controls.ArcButtonSize
import com.knigdelioglu.arc.compose.controls.ArcButtonVariant
import com.knigdelioglu.arc.compose.controls.ArcElasticSlider
import com.knigdelioglu.arc.compose.controls.ArcFloatingButtonGroup
import com.knigdelioglu.arc.compose.controls.ArcNumberField
import com.knigdelioglu.arc.compose.controls.ArcSearchField
import com.knigdelioglu.arc.compose.controls.ArcSegmentedControl
import com.knigdelioglu.arc.compose.controls.ArcTextarea
import com.knigdelioglu.arc.compose.datavis.ArcAnimatedCounter
import com.knigdelioglu.arc.compose.display.ArcAlert
import com.knigdelioglu.arc.compose.display.ArcAlertType
import com.knigdelioglu.arc.compose.display.ArcAvatar
import com.knigdelioglu.arc.compose.display.ArcBadge
import com.knigdelioglu.arc.compose.display.ArcBadgeVariant
import com.knigdelioglu.arc.compose.display.ArcAccordion
import com.knigdelioglu.arc.compose.display.ArcCard
import com.knigdelioglu.arc.compose.display.ArcEmptyState
import com.knigdelioglu.arc.compose.display.ArcExpandableCard
import com.knigdelioglu.arc.compose.foundation.ArcTheme
import com.knigdelioglu.arc.compose.navigation.ArcBreadcrumb
import com.knigdelioglu.arc.compose.navigation.ArcPageHeader
import com.knigdelioglu.arc.compose.navigation.ArcResizablePanels
import com.knigdelioglu.puanla.data.local.CriterionEntity
import com.knigdelioglu.puanla.data.local.StudentEntity
import com.knigdelioglu.puanla.data.local.fullName
import com.knigdelioglu.puanla.data.local.initials
import com.knigdelioglu.puanla.ui.viewmodel.PuanlaViewModel
import kotlin.math.roundToInt

@Composable
fun ScoringWorkspaceScreen(viewModel: PuanlaViewModel) {
    val selectedClassroom by viewModel.selectedClassroom.collectAsState()
    val selectedRubric by viewModel.selectedRubric.collectAsState()
    val students by viewModel.students.collectAsState()
    val selectedStudent by viewModel.selectedStudent.collectAsState()
    val criteria by viewModel.criteria.collectAsState()
    val levels by viewModel.levels.collectAsState()
    val currentAssessment by viewModel.currentAssessment.collectAsState()
    val currentScores by viewModel.currentScores.collectAsState()
    val classAssessments by viewModel.classAssessments.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var activeCriterionIndex by remember { mutableIntStateOf(0) }

    val filteredStudents = remember(students, searchQuery) {
        if (searchQuery.isBlank()) students
        else students.filter {
            it.fullName.contains(searchQuery, ignoreCase = true) || it.studentNumber.contains(searchQuery)
        }
    }

    val activeCriterion = criteria.getOrNull(activeCriterionIndex) ?: criteria.firstOrNull()

    // Map student ID to assessment completion status
    val assessmentMap = remember(classAssessments) {
        classAssessments.associateBy { it.studentId }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWide = maxWidth >= 900.dp

        if (selectedClassroom == null || selectedRubric == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                ArcEmptyState(
                    title = "Sınıf veya Rubrik Seçilmedi",
                    description = "Değerlendirmeye başlamak için üst menüden bir sınıf ve rubrik seçin."
                )
            }
            return@BoxWithConstraints
        }

        if (isWide) {
            // Tablet 3-Pane Resizable Layout
            ArcResizablePanels(
                modifier = Modifier.fillMaxSize(),
                initialWeights = listOf(0.28f, 0.38f, 0.34f),
                minWeights = listOf(0.20f, 0.25f, 0.25f),
                firstPane = {
                    StudentRosterPane(
                        students = filteredStudents,
                        selectedStudent = selectedStudent,
                        assessmentMap = assessmentMap,
                        searchQuery = searchQuery,
                        onSearchChange = { searchQuery = it },
                        onSelectStudent = { viewModel.selectStudent(it) }
                    )
                },
                secondPane = {
                    CriteriaExplorerPane(
                        criteria = criteria,
                        activeCriterion = activeCriterion,
                        currentScores = currentScores,
                        levels = levels,
                        onSelectCriterion = { crit ->
                            activeCriterionIndex = criteria.indexOfFirst { it.id == crit.id }.coerceAtLeast(0)
                        }
                    )
                },
                thirdPane = {
                    ScoringControlPane(
                        student = selectedStudent,
                        criterion = activeCriterion,
                        criteria = criteria,
                        currentAssessment = currentAssessment,
                        currentScores = currentScores,
                        levels = levels.filter { it.criterionId == activeCriterion?.id },
                        onScore = { points, note ->
                            activeCriterion?.let { crit ->
                                viewModel.setScore(crit.id, points, note)
                            }
                        },
                        onClearScore = {
                            activeCriterion?.let { crit ->
                                viewModel.setScore(crit.id, null, null)
                            }
                        },
                        onNoteChange = { note ->
                            activeCriterion?.let { crit -> viewModel.setEvidenceNote(crit.id, note) }
                        },
                        onPrevStudent = { viewModel.previousStudent() },
                        onNextStudent = { viewModel.nextStudent() }
                    )
                }
            )
        } else {
            // Adaptive compact layout for portrait/smaller tablets
            CompactScoringLayout(
                students = filteredStudents,
                selectedStudent = selectedStudent,
                assessmentMap = assessmentMap,
                criteria = criteria,
                activeCriterion = activeCriterion,
                currentAssessment = currentAssessment,
                currentScores = currentScores,
                levels = levels,
                searchQuery = searchQuery,
                onSearchChange = { searchQuery = it },
                onSelectStudent = { viewModel.selectStudent(it) },
                onSelectCriterion = { crit ->
                    activeCriterionIndex = criteria.indexOfFirst { it.id == crit.id }.coerceAtLeast(0)
                },
                onScore = { points, note ->
                    activeCriterion?.let { crit ->
                        viewModel.setScore(crit.id, points, note)
                    }
                },
                onClearScore = {
                    activeCriterion?.let { crit ->
                        viewModel.setScore(crit.id, null, null)
                    }
                },
                onNoteChange = { note ->
                    activeCriterion?.let { crit -> viewModel.setEvidenceNote(crit.id, note) }
                },
                onPrevStudent = { viewModel.previousStudent() },
                onNextStudent = { viewModel.nextStudent() }
            )
        }
    }
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

            ArcSearchField(
                query = searchQuery,
                onQueryChange = onSearchChange,
                placeholder = "Numara veya isim..."
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
private fun CriteriaExplorerPane(
    criteria: List<CriterionEntity>,
    activeCriterion: CriterionEntity?,
    currentScores: Map<String, com.knigdelioglu.puanla.data.local.CriterionScoreEntity>,
    levels: List<com.knigdelioglu.puanla.data.local.CriterionLevelEntity>,
    onSelectCriterion: (CriterionEntity) -> Unit
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
                    text = "Değerlendirme Ölçütleri",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = ArcTheme.colors.foreground
                )
                ArcBadge(
                    text = "${criteria.size} Ölçüt",
                    variant = ArcBadgeVariant.Accent
                )
            }

            Spacer(Modifier.height(10.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(criteria) { criterion ->
                    val isActive = criterion.id == activeCriterion?.id
                    val score = currentScores[criterion.id]
                    val criterionLevels = levels.filter { it.criterionId == criterion.id }

                    ArcExpandableCard(
                        title = "${criterion.orderIndex + 1}. ${criterion.title}",
                        subtitle = criterion.description,
                        initiallyExpanded = isActive,
                        badge = {
                            val isScored = score?.points != null
                            ArcBadge(
                                text = if (isScored) "${score?.points} / ${criterion.maxPoints}" else "Max ${criterion.maxPoints} P",
                                variant = if (isScored) ArcBadgeVariant.Success else ArcBadgeVariant.Default
                            )
                        }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectCriterion(criterion) }
                        ) {
                            Text(
                                text = "Niteliksel Düzeyler:",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ArcTheme.colors.textSecondary
                            )
                            Spacer(Modifier.height(8.dp))
                            criterionLevels.forEach { lvl ->
                                val isSelectedLevel = score?.points == lvl.points
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp),
                                    shape = ArcTheme.shapes.control,
                                    color = if (isSelectedLevel) ArcTheme.colors.surfaceMuted else ArcTheme.colors.surface,
                                    border = BorderStroke(
                                        if (isSelectedLevel) 1.5.dp else 1.dp,
                                        if (isSelectedLevel) ArcTheme.colors.accent else ArcTheme.colors.borderSubtle
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = ArcTheme.shapes.pill,
                                            color = if (isSelectedLevel) ArcTheme.colors.accent else ArcTheme.colors.surfaceMuted
                                        ) {
                                            Text(
                                                text = "${lvl.points}P",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelectedLevel) ArcTheme.colors.accentForeground else ArcTheme.colors.textSecondary,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                        Spacer(Modifier.width(10.dp))
                                        Text(
                                            text = lvl.description,
                                            fontSize = 13.sp,
                                            color = ArcTheme.colors.foreground,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScoringControlPane(
    student: StudentEntity?,
    criterion: CriterionEntity?,
    criteria: List<CriterionEntity>,
    currentAssessment: com.knigdelioglu.puanla.data.local.AssessmentEntity?,
    currentScores: Map<String, com.knigdelioglu.puanla.data.local.CriterionScoreEntity>,
    levels: List<com.knigdelioglu.puanla.data.local.CriterionLevelEntity>,
    onScore: (Int?, String?) -> Unit,
    onClearScore: () -> Unit,
    onNoteChange: (String) -> Unit,
    onPrevStudent: () -> Unit,
    onNextStudent: () -> Unit
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
                        ArcBadge(text = "Eksik Değerlendirme", variant = ArcBadgeVariant.Warning)
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

                    ArcCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${criterion.orderIndex + 1}. ${criterion.title}",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.foreground
                                )
                                ArcBadge(
                                    text = if (points != null) "$points Puan" else "Puanlanmadı",
                                    variant = if (points != null) ArcBadgeVariant.Accent else ArcBadgeVariant.Default
                                )
                            }
                            Spacer(Modifier.height(6.dp))
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
                            } else {
                                Text(
                                    text = "Bu ölçüt için açıklama eklenmemiş.",
                                    fontSize = 13.sp,
                                    color = colors.textSecondary
                                )
                            }

                            Spacer(Modifier.height(18.dp))

                            // Preview is local; persist exactly once on release (never per pointer pixel).
                            var sliderPreview by remember(student.id, criterion.id) {
                                mutableFloatStateOf((points ?: 0).toFloat())
                            }
                            LaunchedEffect(student.id, criterion.id, points) {
                                sliderPreview = (points ?: 0).toFloat()
                            }
                            ArcElasticSlider(
                                value = sliderPreview,
                                onValueChange = { sliderPreview = it },
                                onValueCommit = { released ->
                                    onScore(released.roundToInt(), currentScore?.evidenceNote)
                                },
                                step = 1f,
                                valueRange = 0f..maxPoints.toFloat(),
                                label = "Ölçüt Puanı",
                                formatValue = { "${it.roundToInt()} / $maxPoints" }
                            )

                            Spacer(Modifier.height(16.dp))

                            // Qualitative Levels Segmented Control (if levels exist)
                            if (levels.isNotEmpty()) {
                                Text(
                                    text = "Hızlı Nitelik Seçimi:",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textSecondary
                                )
                                Spacer(Modifier.height(6.dp))
                                val levelLabels = levels.map { "${it.points}P" }
                                val selectedLevelIndex = levels.indexOfFirst { it.points == points }
                                ArcSegmentedControl(
                                    items = levelLabels,
                                    selectedIndex = selectedLevelIndex,
                                    onSelectIndex = { idx ->
                                        val chosenLvl = levels.getOrNull(idx)
                                        if (chosenLvl != null) {
                                            onScore(chosenLvl.points, currentScore?.evidenceNote)
                                        }
                                    }
                                )
                            }

                            Spacer(Modifier.height(16.dp))

                            // Precision Number Field & Reset Action
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                key(student.id, criterion.id) {
                                    ArcNumberField(
                                        value = points,
                                        onValueChange = { onScore(it, currentScore?.evidenceNote) },
                                        min = 0,
                                        max = maxPoints,
                                        label = "Hassas Tam Sayı"
                                    )
                                }
                                ArcButton(
                                    text = "Puanı Kaldır",
                                    onClick = onClearScore,
                                    variant = ArcButtonVariant.Ghost,
                                    size = ArcButtonSize.Sm
                                )
                            }
                        }
                    }

                    // Evidence / Observation Note
                    var noteText by remember(student.id, criterion.id) {
                        mutableStateOf(currentScore?.evidenceNote ?: "")
                    }
                    var noteTouched by remember(student.id, criterion.id) { mutableStateOf(false) }
                    LaunchedEffect(student.id, criterion.id, currentScore?.evidenceNote) {
                        if (!noteTouched) noteText = currentScore?.evidenceNote ?: ""
                    }
                    ArcTextarea(
                        value = noteText,
                        onValueChange = {
                            noteText = it
                            noteTouched = true
                            onNoteChange(it)
                        },
                        placeholder = "Ölçüte özel gözlem, kanıt veya öğretmen değerlendirme notu...",
                        label = "Gözlem ve Kanıt Notu",
                        minLines = 3
                    )
                }

                // Safety Rule Banner
                ArcAlert(
                    title = "Değerlendirme Güvenlik Kuralı",
                    message = "Puanlanmadı != 0. Bütün ölçütler değerlendirilmeden 100 üzerinden kesin not üretilmez.",
                    type = ArcAlertType.Info
                )
            }

            Spacer(Modifier.height(12.dp))

            // Navigation Bar for Quick Student Switching
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ArcButton(
                    text = "Önceki Öğrenci",
                    onClick = onPrevStudent,
                    variant = ArcButtonVariant.Outline,
                    size = ArcButtonSize.Md
                )
                ArcButton(
                    text = "Sonraki Öğrenci",
                    onClick = onNextStudent,
                    variant = ArcButtonVariant.Primary,
                    size = ArcButtonSize.Md
                )
            }
        }
    }
}

@Composable
private fun CompactScoringLayout(
    students: List<StudentEntity>,
    selectedStudent: StudentEntity?,
    assessmentMap: Map<String, com.knigdelioglu.puanla.data.local.AssessmentEntity>,
    criteria: List<CriterionEntity>,
    activeCriterion: CriterionEntity?,
    currentAssessment: com.knigdelioglu.puanla.data.local.AssessmentEntity?,
    currentScores: Map<String, com.knigdelioglu.puanla.data.local.CriterionScoreEntity>,
    levels: List<com.knigdelioglu.puanla.data.local.CriterionLevelEntity>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onSelectStudent: (StudentEntity) -> Unit,
    onSelectCriterion: (CriterionEntity) -> Unit,
    onScore: (Int?, String?) -> Unit,
    onClearScore: () -> Unit,
    onNoteChange: (String) -> Unit,
    onPrevStudent: () -> Unit,
    onNextStudent: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Öğrenciler, 1: Ölçütler, 2: Puanlama

    Column(modifier = Modifier.fillMaxSize()) {
        ArcSegmentedControl(
            items = listOf("Öğrenci Listesi", "Ölçütler", "Puanlama"),
            selectedIndex = selectedTab,
            onSelectIndex = { selectedTab = it },
            modifier = Modifier.padding(12.dp)
        )

        when (selectedTab) {
            0 -> StudentRosterPane(
                students = students,
                selectedStudent = selectedStudent,
                assessmentMap = assessmentMap,
                searchQuery = searchQuery,
                onSearchChange = onSearchChange,
                onSelectStudent = {
                    onSelectStudent(it)
                    selectedTab = 2 // jump to scoring
                }
            )
            1 -> CriteriaExplorerPane(
                criteria = criteria,
                activeCriterion = activeCriterion,
                currentScores = currentScores,
                levels = levels,
                onSelectCriterion = {
                    onSelectCriterion(it)
                    selectedTab = 2 // jump to scoring
                }
            )
            2 -> ScoringControlPane(
                student = selectedStudent,
                criterion = activeCriterion,
                criteria = criteria,
                currentAssessment = currentAssessment,
                currentScores = currentScores,
                levels = levels.filter { it.criterionId == activeCriterion?.id },
                onScore = onScore,
                onClearScore = onClearScore,
                onNoteChange = onNoteChange,
                onPrevStudent = onPrevStudent,
                onNextStudent = onNextStudent
            )
        }
    }
}
