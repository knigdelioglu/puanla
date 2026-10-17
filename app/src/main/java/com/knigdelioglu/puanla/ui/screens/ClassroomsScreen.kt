@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.knigdelioglu.puanla.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import com.knigdelioglu.arc.compose.controls.ArcButton
import com.knigdelioglu.arc.compose.controls.ArcButtonSize
import com.knigdelioglu.arc.compose.controls.ArcButtonVariant
import com.knigdelioglu.arc.compose.controls.ArcExpandingSearch
import com.knigdelioglu.arc.compose.controls.ArcInput
import com.knigdelioglu.arc.compose.controls.ArcInlineEdit
import com.knigdelioglu.arc.compose.controls.ArcRadioGroup
import com.knigdelioglu.arc.compose.datavis.ArcBarChart
import com.knigdelioglu.arc.compose.datavis.ArcBarData
import com.knigdelioglu.arc.compose.datavis.ArcSortableDataTable
import com.knigdelioglu.arc.compose.datavis.ArcTableColumn
import com.knigdelioglu.arc.compose.display.ArcAlert
import com.knigdelioglu.arc.compose.display.ArcAlertType
import com.knigdelioglu.arc.compose.display.ArcDialog
import com.knigdelioglu.arc.compose.display.ArcEmptyState
import com.knigdelioglu.arc.compose.display.ArcPopover
import com.knigdelioglu.arc.compose.foundation.ArcTheme
import com.knigdelioglu.puanla.data.local.ClassroomEntity
import com.knigdelioglu.puanla.data.local.StudentAssessmentSummary
import com.knigdelioglu.puanla.data.local.StudentEntity
import com.knigdelioglu.puanla.data.local.StudentRunScore
import com.knigdelioglu.puanla.data.local.fullName
import com.knigdelioglu.puanla.domain.roster.RosterStatusFilter
import com.knigdelioglu.puanla.domain.student.StudentNameRules
import com.knigdelioglu.puanla.ui.viewmodel.AppDestination
import com.knigdelioglu.puanla.ui.viewmodel.PuanlaViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private enum class ClassroomImportPanel { NONE, PHOTO, PASTE }

@Composable
fun ClassroomsScreen(viewModel: PuanlaViewModel) {
    val classrooms by viewModel.classrooms.collectAsState()
    val rosterSummaries by viewModel.classroomRosterSummaries.collectAsState()
    val selectedClassroom by viewModel.selectedClassroom.collectAsState()
    val students by viewModel.students.collectAsState()
    val classAssessments by viewModel.classAssessments.collectAsState()
    val studentSummaries by viewModel.studentAssessmentSummaries.collectAsState()
    val ocrStep by viewModel.ocrStep.collectAsState()
    val ocrError by viewModel.ocrError.collectAsState()

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var assessmentStatus by rememberSaveable { mutableStateOf("Tümü") }
    var showStatusFilters by remember { mutableStateOf(false) }
    var importPanel by rememberSaveable { mutableStateOf(ClassroomImportPanel.NONE) }
    var pastedRoster by rememberSaveable { mutableStateOf("") }
    var isAddClassDialogOpen by remember { mutableStateOf(false) }
    var classroomBeingEdited by remember { mutableStateOf<ClassroomEntity?>(null) }
    var classroomPendingDeletion by remember { mutableStateOf<ClassroomEntity?>(null) }
    var studentPendingDeletion by remember { mutableStateOf<StudentEntity?>(null) }
    var progressStudent by remember { mutableStateOf<StudentEntity?>(null) }
    var progressHistory by remember { mutableStateOf<List<StudentRunScore>?>(null) }

    val assessmentMap = remember(classAssessments) { classAssessments.associateBy { it.studentId } }
    val studentSummaryMap = remember(studentSummaries) { studentSummaries.associateBy { it.studentId } }
    val visibleStudents = remember(students, searchQuery, assessmentStatus, assessmentMap) {
        students.filter { student ->
            val matchesText = searchQuery.isBlank() ||
                student.fullName.contains(searchQuery, ignoreCase = true) ||
                student.studentNumber.contains(searchQuery)
            val assessment = assessmentMap[student.id]
            matchesText && RosterStatusFilter.matches(
                status = assessmentStatus,
                isCompleted = assessment?.isCompleted == true,
                scoredCount = assessment?.scoredCount ?: 0
            )
        }
    }
    val rosterHeight = (LocalConfiguration.current.screenHeightDp.dp * 0.62f).coerceAtLeast(390.dp)
    val rosterCounts = remember(rosterSummaries) { rosterSummaries.associate { it.classroomId to it.studentCount } }

    fun openImportPanel(panel: ClassroomImportPanel) {
        if (panel != importPanel) {
            viewModel.ocrStep.value = 0
            viewModel.ocrCandidates.value = emptyList()
            viewModel.ocrError.value = null
            viewModel.ocrSelectedImageUri.value = null
            viewModel.ocrIgnoredLineCount.value = 0
        }
        importPanel = panel
        if (panel == ClassroomImportPanel.PASTE) pastedRoster = ""
    }

    fun closeImportPanel() {
        importPanel = ClassroomImportPanel.NONE
        viewModel.ocrStep.value = 0
        viewModel.ocrCandidates.value = emptyList()
        viewModel.ocrError.value = null
        viewModel.ocrSelectedImageUri.value = null
        viewModel.ocrIgnoredLineCount.value = 0
    }

    val hero: @Composable () -> Unit = {
        ClassroomHero(
            classrooms = classrooms,
            selectedClassroom = selectedClassroom,
            rosterCounts = rosterCounts,
            onSelectClassroom = viewModel::selectClassroom,
            onAddClassroom = { isAddClassDialogOpen = true },
            onOpenPhoto = { openImportPanel(ClassroomImportPanel.PHOTO) },
            onOpenPaste = { openImportPanel(ClassroomImportPanel.PASTE) },
            onEditClassroom = { selectedClassroom?.let { classroomBeingEdited = it } },
            onDeleteClassroom = { selectedClassroom?.let { classroomPendingDeletion = it } }
        )
    }

    val isHandlingTextRoster = importPanel == ClassroomImportPanel.PASTE && (ocrStep == 2 || ocrStep == 3)
    if (importPanel == ClassroomImportPanel.PHOTO || isHandlingTextRoster) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            hero()
            OcrImportScreen(
                viewModel = viewModel,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                embedded = true,
                embeddedTitle = if (isHandlingTextRoster) "Metin / Liste Doğrulaması" else "Fotoğraftan OCR ile Sınıf Listesi Aktarımı",
                onClose = ::closeImportPanel
            )
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .testTag("classroomManagementScroll")
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        hero()

        if (importPanel == ClassroomImportPanel.PASTE) {
            PastedRosterPanel(
                text = pastedRoster,
                onTextChange = { pastedRoster = it },
                error = ocrError,
                onClose = ::closeImportPanel,
                onParse = {
                    viewModel.prepareOcrFromText(pastedRoster)
                }
            )
        }

        if (selectedClassroom == null) {
            ArcEmptyState(
                title = if (classrooms.isEmpty()) "Henüz sınıf yok" else "Sınıf seçilmedi",
                description = if (classrooms.isEmpty())
                    "Öğrenci eklemek veya liste aktarmak için önce bir sınıf oluşturun."
                else "Öğrenci listesini görüntülemek için yukarıdan bir sınıf seçin.",
                action = {
                    ArcButton(
                        text = "Yeni Sınıf Ekle",
                        onClick = { isAddClassDialogOpen = true },
                        variant = ArcButtonVariant.Primary
                    )
                }
            )
        } else {
            val activeClassroom = selectedClassroom ?: return@Column
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val entryPanel: @Composable (Modifier) -> Unit = { panelModifier ->
                    StudentEntryPanel(
                        modifier = panelModifier,
                        classroomName = activeClassroom.name,
                        enabled = true,
                        onAdd = { number, firstName, lastName, onResult ->
                            viewModel.addStudent(number, firstName, lastName) { success, message ->
                                viewModel.showToast(message)
                                onResult(success, message)
                            }
                        }
                    )
                }
                val rosterPanel: @Composable (Modifier) -> Unit = { panelModifier ->
                    StudentRosterPanel(
                        modifier = panelModifier,
                        classroom = activeClassroom,
                        students = visibleStudents,
                        totalStudentCount = students.size,
                        summaries = studentSummaryMap,
                        assessmentMap = assessmentMap,
                        rosterHeight = rosterHeight,
                        searchQuery = searchQuery,
                        onSearchChange = { searchQuery = it },
                        assessmentStatus = assessmentStatus,
                        onAssessmentStatusChange = { assessmentStatus = it },
                        statusMenuOpen = showStatusFilters,
                        onStatusMenuToggle = { showStatusFilters = it },
                        onClearFilters = { searchQuery = ""; assessmentStatus = "Tümü" },
                        onEditName = { student, fullName ->
                            runCatching { StudentNameRules.splitFullName(fullName) }
                                .onSuccess { (firstName, lastName) ->
                                    viewModel.updateStudentName(student.id, firstName, lastName)
                                }
                                .onFailure { viewModel.showToast(it.message ?: "Ad soyad güncellenemedi.") }
                        },
                        onDelete = { studentPendingDeletion = it },
                        onShowProgress = { student ->
                            progressStudent = student
                            progressHistory = null
                            viewModel.loadStudentProgress(student.id) { progressHistory = it }
                        },
                        onGradeStudent = { student ->
                            viewModel.selectStudent(student)
                            viewModel.currentDestination.value = AppDestination.WORKSPACE
                        }
                    )
                }
                if (maxWidth >= 900.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        entryPanel(Modifier.weight(0.32f))
                        rosterPanel(Modifier.weight(0.68f))
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        entryPanel(Modifier.fillMaxWidth())
                        rosterPanel(Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }

    classroomBeingEdited?.let { classroom ->
        EditClassroomDialog(
            classroom = classroom,
            onDismiss = { classroomBeingEdited = null },
            onConfirm = { section, year ->
                viewModel.updateClassroomDetails(classroom, section, year) { success, message ->
                    viewModel.showToast(message)
                    if (success) classroomBeingEdited = null
                }
            }
        )
    }
    classroomPendingDeletion?.let { classroom ->
        ArcDialog(
            isOpen = true,
            onDismissRequest = { classroomPendingDeletion = null },
            title = "Sınıfı Sil",
            description = "${classroom.name} yalnızca öğrenci, değerlendirme veya grup kaydı yoksa silinebilir. Bağlı kayıtlar varsa veri kaybını önlemek için işlem reddedilir.",
            confirmText = "Sınıfı Sil",
            cancelText = "Vazgeç",
            onConfirm = {
                viewModel.deleteClassroom(classroom) { success, message ->
                    viewModel.showToast(message)
                    if (success) classroomPendingDeletion = null
                }
            }
        )
    }
    studentPendingDeletion?.let { student ->
        ArcDialog(
            isOpen = true,
            onDismissRequest = { studentPendingDeletion = null },
            title = "Öğrenciyi Sil",
            description = "${student.fullName} (${student.studentNumber}) kaydı silinecektir. Bu işlem geri alınamaz.",
            confirmText = "Sil",
            cancelText = "Vazgeç",
            onConfirm = {
                viewModel.deleteStudent(student)
                studentPendingDeletion = null
            }
        )
    }
    if (isAddClassDialogOpen) {
        AddClassroomDialog(
            onDismiss = { isAddClassDialogOpen = false },
            onConfirm = { grade, section ->
                viewModel.addClassroom(grade, section) { success, message ->
                    if (success) isAddClassDialogOpen = false
                    viewModel.showToast(message)
                }
            }
        )
    }
    progressStudent?.let { student ->
        StudentProgressDialog(
            student = student,
            history = progressHistory,
            summary = studentSummaryMap[student.id],
            onDismiss = { progressStudent = null; progressHistory = null }
        )
    }
}

@Composable
private fun ClassroomHero(
    classrooms: List<ClassroomEntity>,
    selectedClassroom: ClassroomEntity?,
    rosterCounts: Map<String, Int>,
    onSelectClassroom: (ClassroomEntity) -> Unit,
    onAddClassroom: () -> Unit,
    onOpenPhoto: () -> Unit,
    onOpenPaste: () -> Unit,
    onEditClassroom: () -> Unit,
    onDeleteClassroom: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = ArcTheme.colors.surface,
        shape = ArcTheme.shapes.panel,
        border = BorderStroke(1.dp, ArcTheme.colors.borderSubtle)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val actions: @Composable () -> Unit = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ArcButton(
                            text = "Fotoğraftan OCR ile Liste Aktar",
                            onClick = onOpenPhoto,
                            enabled = selectedClassroom != null,
                            variant = ArcButtonVariant.Primary,
                            size = ArcButtonSize.Md,
                            leadingIcon = { Icon(Icons.Default.CameraAlt, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ArcButton(
                                text = "Metin / Liste Yapıştır",
                                onClick = onOpenPaste,
                                enabled = selectedClassroom != null,
                                variant = ArcButtonVariant.Outline,
                                size = ArcButtonSize.Md,
                                leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) }
                            )
                            ArcButton(
                                text = "Yeni Sınıf Ekle",
                                onClick = onAddClassroom,
                                variant = ArcButtonVariant.Outline,
                                size = ArcButtonSize.Md,
                                leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) }
                            )
                        }
                    }
                }
                if (maxWidth >= 900.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(26.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "Sınıf ve Öğrenci Yönetimi",
                                fontSize = 28.sp,
                                lineHeight = 34.sp,
                                fontWeight = FontWeight.Bold,
                                color = ArcTheme.colors.foreground
                            )
                            Text(
                                "Öğrenci listelerini elle ekleyin, toplu yapıştırın veya sınıf listesi fotoğrafından aktarın.",
                                fontSize = 16.sp,
                                lineHeight = 23.sp,
                                color = ArcTheme.colors.textSecondary
                            )
                        }
                        Box(modifier = Modifier.widthIn(min = 340.dp, max = 520.dp).fillMaxWidth()) { actions() }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            "Sınıf ve Öğrenci Yönetimi",
                            fontSize = 24.sp,
                            lineHeight = 30.sp,
                            fontWeight = FontWeight.Bold,
                            color = ArcTheme.colors.foreground
                        )
                        Text(
                            "Öğrenci listelerini elle ekleyin, toplu yapıştırın veya sınıf listesi fotoğrafından aktarın.",
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = ArcTheme.colors.textSecondary
                        )
                        actions()
                    }
                }
            }

            if (classrooms.isNotEmpty()) {
                HorizontalDivider(color = ArcTheme.colors.borderSubtle)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LazyRow(
                        modifier = Modifier.weight(1f).testTag("classroomSelector"),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 2.dp)
                    ) {
                        items(classrooms, key = { it.id }) { classroom ->
                            val isSelected = classroom.id == selectedClassroom?.id
                            Surface(
                                modifier = Modifier
                                    .semantics { selected = isSelected }
                                    .clickable(role = Role.Tab) { onSelectClassroom(classroom) },
                                color = if (isSelected) ArcTheme.colors.surfaceRaised else ArcTheme.colors.surfaceMuted,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(
                                    if (isSelected) 1.5.dp else 1.dp,
                                    if (isSelected) ArcTheme.colors.accent else ArcTheme.colors.borderSubtle
                                )
                            ) {
                                Text(
                                    text = "${classroom.name} · ${classroom.targetLabel()} (${rosterCounts[classroom.id] ?: 0})",
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                    color = if (isSelected) ArcTheme.colors.foreground else ArcTheme.colors.textSecondary,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                    if (selectedClassroom != null) {
                        IconButton(onClick = onEditClassroom, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "Sınıfı Düzenle", tint = ArcTheme.colors.textSecondary)
                        }
                        IconButton(onClick = onDeleteClassroom, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Sınıfı Sil", tint = ArcTheme.colors.textMuted)
                        }
                    }
                }
            }
        }
    }
}

private fun ClassroomEntity.targetLabel(): String = "${grade}. Sınıf"

@Composable
private fun PastedRosterPanel(
    text: String,
    onTextChange: (String) -> Unit,
    error: String?,
    onClose: () -> Unit,
    onParse: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().testTag("pastedRosterPanel"),
        color = ArcTheme.colors.surface,
        shape = ArcTheme.shapes.panel,
        border = BorderStroke(1.dp, ArcTheme.colors.borderSubtle)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Toplu Öğrenci Listesi Yapıştır${if (text.isBlank()) "" else " (${text.lineSequence().count { it.isNotBlank() }} satır)"}",
                    modifier = Modifier.weight(1f),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = ArcTheme.colors.foreground
                )
                ArcButton(text = "Kapat", onClick = onClose, variant = ArcButtonVariant.Ghost, size = ArcButtonSize.Sm)
            }
            Text(
                "Her satıra okul numarası ve ad soyadı yazın (ör. 109 - Ada Örnek). Okul numarası açıkça bulunmayan satırlar ayrıştırılmaz.",
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = ArcTheme.colors.textSecondary
            )
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp).testTag("pastedRosterInput"),
                placeholder = { Text("109 - Ada Örnek\n110 - Eren Deneme\n111 - Duru Taslak") },
                minLines = 5,
                maxLines = 12
            )
            if (error != null) {
                ArcAlert(title = "Liste ayrıştırılamadı", message = error, type = ArcAlertType.Warning)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ArcButton(text = "İptal", onClick = onClose, variant = ArcButtonVariant.Ghost)
                Spacer(Modifier.width(8.dp))
                ArcButton(
                    text = "Listeyi Ayrıştır ve İncele",
                    onClick = onParse,
                    enabled = text.isNotBlank(),
                    variant = ArcButtonVariant.Primary,
                    size = ArcButtonSize.Md
                )
            }
        }
    }
}

@Composable
private fun StudentEntryPanel(
    modifier: Modifier,
    classroomName: String,
    enabled: Boolean,
    onAdd: (String, String, String, (Boolean, String) -> Unit) -> Unit
) {
    var number by rememberSaveable(classroomName) { mutableStateOf("") }
    var fullName by rememberSaveable(classroomName) { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    Surface(
        modifier = modifier,
        color = ArcTheme.colors.surface,
        shape = ArcTheme.shapes.panel,
        border = BorderStroke(1.dp, ArcTheme.colors.borderSubtle)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Icon(Icons.Default.PersonAdd, contentDescription = null, tint = ArcTheme.colors.accent)
                Text(
                    "Elle Öğrenci Ekle ($classroomName)",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = ArcTheme.colors.foreground
                )
            }
            ArcInput(
                value = number,
                onValueChange = { number = it.filter(Char::isDigit).take(6); error = null },
                label = "Okul Numarası",
                placeholder = "Örn: 115",
                enabled = enabled && !saving,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth().testTag("manualStudentNumber")
            )
            ArcInput(
                value = fullName,
                onValueChange = { fullName = it; error = null },
                label = "Öğrenci Adı ve Soyadı",
                placeholder = "Örn: Ada Su Örnek",
                enabled = enabled && !saving,
                modifier = Modifier.fillMaxWidth().testTag("manualStudentFullName")
            )
            Text(
                "Son sözcük soyadı olarak kaydedilir.",
                fontSize = 12.sp,
                color = ArcTheme.colors.textSecondary
            )
            if (error != null) Text(error.orEmpty(), fontSize = 13.sp, color = ArcTheme.colors.danger)
            ArcButton(
                text = if (saving) "Öğrenci Ekleniyor…" else "Öğrenciyi Sınıfa Ekle",
                onClick = {
                    if (number.isBlank()) {
                        error = "Okul numarasını girin."
                        return@ArcButton
                    }
                    val parts = runCatching { StudentNameRules.splitFullName(fullName) }
                    if (parts.isFailure) {
                        error = parts.exceptionOrNull()?.message ?: "Ad ve soyadı kontrol edin."
                        return@ArcButton
                    }
                    saving = true
                    val (firstName, lastName) = parts.getOrThrow()
                    onAdd(number.trim(), firstName, lastName) { success, message ->
                        saving = false
                        if (success) {
                            number = ""
                            fullName = ""
                            error = null
                        } else error = message
                    }
                },
                enabled = enabled && !saving && number.isNotBlank() && fullName.isNotBlank(),
                variant = ArcButtonVariant.Primary,
                size = ArcButtonSize.Md,
                modifier = Modifier.fillMaxWidth().testTag("addStudentAction")
            )
        }
    }
}

@Composable
private fun StudentRosterPanel(
    modifier: Modifier,
    classroom: ClassroomEntity,
    students: List<StudentEntity>,
    totalStudentCount: Int,
    summaries: Map<String, StudentAssessmentSummary>,
    assessmentMap: Map<String, com.knigdelioglu.puanla.data.local.AssessmentEntity>,
    rosterHeight: androidx.compose.ui.unit.Dp,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    assessmentStatus: String,
    onAssessmentStatusChange: (String) -> Unit,
    statusMenuOpen: Boolean,
    onStatusMenuToggle: (Boolean) -> Unit,
    onClearFilters: () -> Unit,
    onEditName: (StudentEntity, String) -> Unit,
    onDelete: (StudentEntity) -> Unit,
    onShowProgress: (StudentEntity) -> Unit,
    onGradeStudent: (StudentEntity) -> Unit
) {
    Surface(
        modifier = modifier,
        color = ArcTheme.colors.surface,
        shape = ArcTheme.shapes.panel,
        border = BorderStroke(1.dp, ArcTheme.colors.borderSubtle)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    "${classroom.name} Öğrenci Listesi ($totalStudentCount Öğrenci)",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = ArcTheme.colors.foreground
                )
                Text(
                    "Sınıf: ${classroom.grade}. Sınıf · Akademik Yıl: ${classroom.academicYear}",
                    fontSize = 13.sp,
                    color = ArcTheme.colors.textSecondary
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().testTag("studentRosterControls"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ArcExpandingSearch(
                    query = searchQuery,
                    onQueryChange = onSearchChange,
                    placeholder = "Öğrenci adı veya numarası"
                )
                Box {
                    ArcButton(
                        text = "Durum: $assessmentStatus",
                        onClick = { onStatusMenuToggle(!statusMenuOpen) },
                        variant = ArcButtonVariant.Outline,
                        size = ArcButtonSize.Sm
                    )
                    ArcPopover(isOpen = statusMenuOpen, onDismiss = { onStatusMenuToggle(false) }) {
                        ArcRadioGroup(
                            options = listOf("Tümü", "Tamamlanan", "Kısmi", "Puanlanmadı"),
                            selectedOption = assessmentStatus,
                            onOptionSelected = {
                                onAssessmentStatusChange(it)
                                onStatusMenuToggle(false)
                            },
                            modifier = Modifier.width(185.dp)
                        )
                    }
                }
                if (searchQuery.isNotBlank() || assessmentStatus != "Tümü") {
                    ArcButton(text = "Temizle", onClick = onClearFilters, variant = ArcButtonVariant.Ghost, size = ArcButtonSize.Sm)
                }
            }
            if (students.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().heightIn(min = 230.dp), contentAlignment = Alignment.Center) {
                    ArcEmptyState(
                        title = if (searchQuery.isBlank() && assessmentStatus == "Tümü") "Henüz öğrenci yok" else "Öğrenci bulunamadı",
                        description = if (searchQuery.isBlank() && assessmentStatus == "Tümü")
                            "Elle ekleme alanını veya liste aktarma seçeneklerini kullanın."
                        else "Arama veya durum filtresini değiştirin."
                    )
                }
            } else {
                val columns = listOf(
                    ArcTableColumn<StudentEntity>(
                        title = "Okul No",
                        weight = 0.8f,
                        comparator = compareBy { it.studentNumber.toIntOrNull() ?: 0 }
                    ) { student ->
                        Text(student.studentNumber, fontWeight = FontWeight.SemiBold, color = ArcTheme.colors.foreground)
                    },
                    ArcTableColumn<StudentEntity>(
                        title = "Ad Soyad",
                        weight = 1.7f,
                        comparator = compareBy { it.fullName.lowercase(Locale.forLanguageTag("tr-TR")) }
                    ) { student ->
                        ArcInlineEdit(
                            value = student.fullName,
                            onCommit = { onEditName(student, it) }
                        )
                    },
                    ArcTableColumn<StudentEntity>(title = "Değerlendirme Sayısı", weight = 1.05f) { student ->
                        val count = summaries[student.id]?.assessmentCount ?: 0
                        Text(if (count == 0) "—" else "$count oturum", color = ArcTheme.colors.textSecondary)
                    },
                    ArcTableColumn<StudentEntity>(title = "Genel Ortalama", weight = 0.95f) { student ->
                        val average = summaries[student.id]?.averageCompletedTotal?.roundToInt()
                        Text(
                            text = average?.let { "$it / 100" } ?: "—",
                            fontWeight = FontWeight.SemiBold,
                            color = when {
                                average == null -> ArcTheme.colors.textMuted
                                average >= 80 -> ArcTheme.colors.success
                                average >= 60 -> ArcTheme.colors.accent
                                else -> ArcTheme.colors.warning
                            }
                        )
                    },
                    ArcTableColumn<StudentEntity>(title = "İşlemler", weight = 1.35f) { student ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            ArcButton(
                                text = "Gelişim Grafiği",
                                onClick = { onShowProgress(student) },
                                variant = ArcButtonVariant.Ghost,
                                size = ArcButtonSize.Sm
                            )
                            IconButton(onClick = { onGradeStudent(student) }, modifier = Modifier.size(40.dp)) {
                                Icon(Icons.Default.Edit, contentDescription = "${student.fullName} öğrencisini puanla", tint = ArcTheme.colors.accent)
                            }
                            IconButton(onClick = { onDelete(student) }, modifier = Modifier.size(40.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = "${student.fullName} öğrencisini sil", tint = ArcTheme.colors.textMuted)
                            }
                        }
                    }
                )
                ArcSortableDataTable(
                    items = students,
                    columns = columns,
                    modifier = Modifier.fillMaxWidth().height(rosterHeight).testTag("studentRosterTable"),
                    pageSize = 25,
                    rowKey = { it.id }
                )
            }
        }
    }
}

@Composable
private fun StudentProgressDialog(
    student: StudentEntity,
    history: List<StudentRunScore>?,
    summary: StudentAssessmentSummary?,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.94f).widthIn(max = 900.dp).fillMaxHeight(0.88f),
            color = ArcTheme.colors.surfaceRaised,
            shape = ArcTheme.shapes.panel,
            border = BorderStroke(1.dp, ArcTheme.colors.border)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${student.fullName} · Gelişim Grafiği", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = ArcTheme.colors.foreground)
                        Text(
                            "${student.studentNumber} numara · ${summary?.assessmentCount ?: 0} puanlanmış oturum",
                            fontSize = 13.sp,
                            color = ArcTheme.colors.textSecondary
                        )
                    }
                    ArcButton(text = "Kapat", onClick = onDismiss, variant = ArcButtonVariant.Ghost)
                }
                HorizontalDivider(color = ArcTheme.colors.borderSubtle)
                when {
                    history == null -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Text("Değerlendirme geçmişi yükleniyor…", color = ArcTheme.colors.textSecondary)
                    }
                    history.isEmpty() -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        ArcEmptyState(
                            title = "Tamamlanmış oturum yok",
                            description = "Gelişim grafiği yalnızca tamamlanmış değerlendirme oturumlarının gerçek puanlarından oluşturulur."
                        )
                    }
                    else -> {
                        ArcBarChart(
                            data = history.map { score -> ArcBarData(score.runTitle, score.total.toFloat(), ArcTheme.colors.accent) },
                            modifier = Modifier.fillMaxWidth().height(300.dp).testTag("studentProgressChart")
                        )
                        Column(
                            modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            history.asReversed().forEach { score ->
                                Text(
                                    "${score.runTitle} · ${formatAssessmentDate(score.assessedAt)} · ${score.total}/100",
                                    fontSize = 14.sp,
                                    color = ArcTheme.colors.textSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatAssessmentDate(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("tr-TR")))

@Composable
private fun EditClassroomDialog(
    classroom: ClassroomEntity,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var section by remember(classroom.id) { mutableStateOf(classroom.section) }
    var academicYear by remember(classroom.id) { mutableStateOf(classroom.academicYear) }
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = ArcTheme.shapes.panel,
            color = ArcTheme.colors.surfaceRaised,
            border = BorderStroke(1.dp, ArcTheme.colors.border),
            modifier = Modifier.fillMaxWidth().widthIn(max = 460.dp)
        ) {
            Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("${classroom.grade}. sınıf bilgilerini düzenle", fontWeight = FontWeight.Bold, color = ArcTheme.colors.foreground, fontSize = 18.sp)
                Text("Sınıf düzeyi ve kayıt kimliği sabit kalır; geçmiş değerlendirmelere olan bağlantılar korunur.", color = ArcTheme.colors.textSecondary, fontSize = 13.sp)
                ArcInput(value = section, onValueChange = { section = it.uppercase() }, label = "Şube", placeholder = "A")
                ArcInput(value = academicYear, onValueChange = { academicYear = it }, label = "Öğretim yılı", placeholder = "2026-2027")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    ArcButton(text = "Vazgeç", onClick = onDismiss, variant = ArcButtonVariant.Ghost)
                    Spacer(Modifier.width(8.dp))
                    ArcButton(text = "Kaydet", onClick = { onConfirm(section, academicYear) }, enabled = section.isNotBlank() && academicYear.isNotBlank())
                }
            }
        }
    }
}

@Composable
private fun AddClassroomDialog(onDismiss: () -> Unit, onConfirm: (Int, String) -> Unit) {
    var selectedGradeIndex by remember { mutableIntStateOf(2) }
    var section by remember { mutableStateOf("A") }
    val grades = listOf("9", "10", "11", "12")
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = ArcTheme.shapes.panel,
            color = ArcTheme.colors.surfaceRaised,
            border = BorderStroke(1.dp, ArcTheme.colors.border),
            modifier = Modifier.fillMaxWidth().widthIn(max = 520.dp)
        ) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Yeni Sınıf / Şube Ekle", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = ArcTheme.colors.foreground)
                Text("Sınıf düzeyini ve şube harfini seçin.", color = ArcTheme.colors.textSecondary)
                com.knigdelioglu.arc.compose.controls.ArcSegmentedControl(
                    items = grades.map { "$it. Sınıf" },
                    selectedIndex = selectedGradeIndex,
                    onSelectIndex = { selectedGradeIndex = it }
                )
                ArcInput(value = section, onValueChange = { section = it.uppercase() }, label = "Şube Harfi", placeholder = "A")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    ArcButton(text = "Vazgeç", onClick = onDismiss, variant = ArcButtonVariant.Ghost)
                    Spacer(Modifier.width(8.dp))
                    ArcButton(
                        text = "Sınıfı Oluştur",
                        onClick = { if (section.isNotBlank()) onConfirm(selectedGradeIndex + 9, section.trim()) },
                        enabled = section.isNotBlank(),
                        variant = ArcButtonVariant.Primary
                    )
                }
            }
        }
    }
}
