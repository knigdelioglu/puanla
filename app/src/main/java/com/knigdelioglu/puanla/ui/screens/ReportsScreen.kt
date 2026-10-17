package com.knigdelioglu.puanla.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.knigdelioglu.arc.compose.controls.ArcButton
import com.knigdelioglu.arc.compose.controls.ArcInput
import com.knigdelioglu.arc.compose.controls.ArcCheckbox
import com.knigdelioglu.arc.compose.controls.ArcButtonSize
import com.knigdelioglu.arc.compose.controls.ArcButtonVariant
import com.knigdelioglu.arc.compose.controls.ArcDateRangePicker
import com.knigdelioglu.arc.compose.controls.ArcCalendar
import com.knigdelioglu.arc.compose.controls.ArcSelect
import com.knigdelioglu.arc.compose.controls.ArcSplitButton
import com.knigdelioglu.arc.compose.controls.ArcSegmentedControl
import com.knigdelioglu.arc.compose.controls.ArcButtonGroup
import com.knigdelioglu.arc.compose.controls.ArcDropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import com.knigdelioglu.puanla.domain.report.AssessmentDateFilter
import com.knigdelioglu.puanla.domain.report.canExportSelectedReport
import com.knigdelioglu.puanla.domain.export.PdfReportSnapshot
import com.knigdelioglu.puanla.domain.export.CsvStudent
import com.knigdelioglu.puanla.domain.export.PdfReportStatistics
import com.knigdelioglu.puanla.domain.export.PdfScoreState
import com.knigdelioglu.puanla.domain.export.PdfReportWriter
import com.knigdelioglu.arc.compose.datavis.ArcBarChart
import com.knigdelioglu.arc.compose.datavis.ArcBarData
import com.knigdelioglu.arc.compose.datavis.ArcSortableDataTable
import com.knigdelioglu.arc.compose.datavis.ArcTableColumn
import com.knigdelioglu.arc.compose.datavis.ArcDonutChart
import com.knigdelioglu.arc.compose.datavis.ArcDonutSegment
import com.knigdelioglu.arc.compose.datavis.ArcGauge
import com.knigdelioglu.arc.compose.datavis.ArcMetricCard
import com.knigdelioglu.arc.compose.datavis.ArcStatItem
import com.knigdelioglu.arc.compose.datavis.ArcStatsBand
import com.knigdelioglu.arc.compose.datavis.ArcUsageMeter
import com.knigdelioglu.arc.compose.display.ArcAlert
import com.knigdelioglu.arc.compose.display.ArcAlertType
import com.knigdelioglu.arc.compose.display.ArcBadgeVariant
import com.knigdelioglu.arc.compose.display.ArcCopyButton
import com.knigdelioglu.arc.compose.display.ArcComparisonTable
import com.knigdelioglu.arc.compose.display.ArcEmptyState
import com.knigdelioglu.arc.compose.foundation.ArcTheme
import com.knigdelioglu.puanla.ui.viewmodel.PuanlaViewModel
import com.knigdelioglu.puanla.domain.report.studentComparisonRows
import com.knigdelioglu.puanla.data.local.fullName
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ReportsScreen(viewModel: PuanlaViewModel) {
    val selectedClassroom by viewModel.selectedClassroom.collectAsState()
    val selectedRubric by viewModel.selectedRubric.collectAsState()
    val selectedRun by viewModel.selectedRun.collectAsState()
    val assessmentRuns by viewModel.assessmentRuns.collectAsState()
    val groupSessions by viewModel.groupSessions.collectAsState()
    val selectedGroupSession by viewModel.selectedGroupSession.collectAsState()
    val groupAssessments by viewModel.groupAssessments.collectAsState()
    val reportsGroupMode by viewModel.reportsGroupMode.collectAsState()
    val students by viewModel.students.collectAsState()
    val classAssessments by viewModel.classAssessments.collectAsState()

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val pdfPreferences = remember(context) {
        context.getSharedPreferences("report_pdf_details", android.content.Context.MODE_PRIVATE)
    }
    var pdfCourse by remember { mutableStateOf(pdfPreferences.getString("course", "") ?: "") }
    var pdfTeacher by remember { mutableStateOf(pdfPreferences.getString("teacher", "") ?: "") }
    var showPdfDialog by remember { mutableStateOf(false) }
    var showPdfPreviewDialog by remember { mutableStateOf(false) }
    var groupReportMode by remember { mutableStateOf(false) }
    var pdfRequestedGroupSessionId by remember { mutableStateOf<String?>(null) }
    var pdfRequestedGroupSessionTitle by remember { mutableStateOf<String?>(null) }
    var pdfRequestedClassroomId by remember { mutableStateOf<String?>(null) }
    var pdfRequestedRubricId by remember { mutableStateOf<String?>(null) }
    var pdfRequestedRunId by remember { mutableStateOf<String?>(null) }
    var pdfRequestedRunTitle by remember { mutableStateOf<String?>(null) }
    var pdfGenerating by remember { mutableStateOf(false) }
    var pendingPdfSnapshots by remember { mutableStateOf<List<PdfReportSnapshot>>(emptyList()) }
    var sessionReportSnapshot by remember { mutableStateOf<PdfReportSnapshot?>(null) }
    var sessionReportLoading by remember { mutableStateOf(false) }
    var sessionReportError by remember { mutableStateOf(false) }
    var pdfAllClasses by remember { mutableStateOf(false) }
    var pdfSummaryOnly by remember { mutableStateOf(false) }
    var pendingSummaryOnly by remember { mutableStateOf(false) }
    var pendingPdfCourse by remember { mutableStateOf("") }
    var pendingPdfTeacher by remember { mutableStateOf("") }
    var csvActionsOpen by remember { mutableStateOf(false) }
    var isCsvPreviewDialogOpen by remember { mutableStateOf(false) }
    var generatedCsvContent by remember { mutableStateOf("") }
    var csvSuggestedFilename by remember { mutableStateOf("puanla-degerlendirme-${LocalDate.now()}.csv") }
    var csvGenerating by remember { mutableStateOf(false) }
    var showDateFilter by remember { mutableStateOf(false) }
    var calendarTarget by remember { mutableStateOf<String?>(null) }
    var comparisonOpen by remember { mutableStateOf(false) }
    var firstComparisonStudentId by remember(selectedClassroom?.id, selectedRubric?.id, selectedRun?.id) { mutableStateOf<String?>(null) }
    var secondComparisonStudentId by remember(selectedClassroom?.id, selectedRubric?.id, selectedRun?.id) { mutableStateOf<String?>(null) }
    var comparisonRows by remember { mutableStateOf<List<List<String>>>(emptyList()) }
    var comparisonLoading by remember { mutableStateOf(false) }
    var comparisonError by remember { mutableStateOf(false) }
    var filterStart by remember(selectedClassroom?.id, selectedRubric?.id, selectedRun?.id) { mutableStateOf("") }
    var filterEnd by remember(selectedClassroom?.id, selectedRubric?.id, selectedRun?.id) { mutableStateOf("") }
    val filterActive = filterStart.isNotEmpty() || filterEnd.isNotEmpty()
    val filterValid = AssessmentDateFilter.isValidRange(filterStart, filterEnd)
    val saveCsv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val stream = requireNotNull(context.contentResolver.openOutputStream(uri)) { "CSV dosyası açılamadı." }
                    stream.bufferedWriter(Charsets.UTF_8).use { it.write(generatedCsvContent) }
                }
                viewModel.showToast("CSV dosyası başarıyla kaydedildi.")
                isCsvPreviewDialogOpen = false
            } catch (e: Exception) {
                viewModel.showToast("CSV kaydedilemedi: ${e.message}")
            }
        }
    }

    val savePdf = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        val snapshots = pendingPdfSnapshots
        if (uri != null && snapshots.isNotEmpty()) scope.launch {
            pdfGenerating = true
            try {
                withContext(Dispatchers.IO) {
                    val stream = requireNotNull(context.contentResolver.openOutputStream(uri)) {
                        "PDF dosyası açılamadı."
                    }
                    stream.use {
                        PdfReportWriter.writeAll(snapshots, pendingPdfCourse, pendingPdfTeacher, it, pendingSummaryOnly)
                    }
                }
                viewModel.showToast("Ölçüt bazlı PDF raporu kaydedildi.")
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (e: Exception) {
                viewModel.showToast("PDF kaydedilemedi: ${e.message}")
            } finally {
                pdfGenerating = false
                pendingPdfSnapshots = emptyList()
            }
        } else {
            pendingPdfSnapshots = emptyList()
        }
    }

    val scrollState = rememberScrollState()

    LaunchedEffect(selectedClassroom?.id, selectedRubric?.id, selectedRun?.id, reportsGroupMode) {
        val classroomId = selectedClassroom?.id
        val rubricId = selectedRubric?.id
        val runId = selectedRun?.id
        sessionReportSnapshot = null
        sessionReportError = false
        if (reportsGroupMode || classroomId == null || rubricId == null || runId == null) {
            sessionReportLoading = false
        } else {
            sessionReportLoading = true
            try {
                sessionReportSnapshot = withContext(Dispatchers.IO) {
                    viewModel.generatePdfSnapshotsForRun(classroomId, rubricId, runId, allClassrooms = false)
                        .singleOrNull()
                }
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (_: Exception) {
                sessionReportError = true
            } finally {
                sessionReportLoading = false
            }
        }
    }

    // Calculations
    val datedAssessments = remember(classAssessments, filterStart, filterEnd) {
        if (filterActive && filterValid) {
            AssessmentDateFilter.filter(classAssessments, filterStart, filterEnd)
        } else if (filterActive) emptyList() else classAssessments
    }
    val datedIds = remember(datedAssessments) { datedAssessments.map { it.studentId }.toSet() }
    // In date mode the denominator is only students with a last-updated
    // assessment in range, never the whole classroom.
    val effectiveStudents = remember(students, datedIds, filterActive) {
        if (filterActive) students.filter { it.id in datedIds } else students
    }
    val totalStudents = effectiveStudents.size
    val assessmentMap = remember(datedAssessments) { datedAssessments.associateBy { it.studentId } }

    val completedCount = effectiveStudents.count { assessmentMap[it.id]?.isCompleted == true }
    val partialCount = effectiveStudents.count {
        val asm = assessmentMap[it.id]
        asm?.isCompleted == false && (asm.scoredCount ?: 0) > 0
    }
    val unscoredCount = (totalStudents - completedCount - partialCount).coerceAtLeast(0)

    val validScores = effectiveStudents.mapNotNull {
        val asm = assessmentMap[it.id]
        if (asm?.isCompleted == true) asm.definitiveTotal?.toFloat() else null
    }

    val averageScore = if (validScores.isNotEmpty()) validScores.average().toFloat() else 0f
    val maxScore = validScores.maxOrNull()?.toInt() ?: 0
    val minScore = validScores.minOrNull()?.toInt() ?: 0

    // Grade intervals: 0-49, 50-69, 70-84, 85-100
    val count0to49 = validScores.count { it < 50 }
    val count50to69 = validScores.count { it in 50f..69.9f }
    val count70to84 = validScores.count { it in 70f..84.9f }
    val count85to100 = validScores.count { it >= 85 }

    val colors = ArcTheme.colors
    val firstComparisonStudent = students.firstOrNull { it.id == firstComparisonStudentId }
    val secondComparisonStudent = students.firstOrNull { it.id == secondComparisonStudentId }

    LaunchedEffect(comparisonOpen, firstComparisonStudentId, secondComparisonStudentId, selectedRubric?.id, selectedRun?.id) {
        val rubric = selectedRubric
        val firstId = firstComparisonStudentId
        val secondId = secondComparisonStudentId
        if (comparisonOpen && rubric != null && selectedRun != null && firstId != null && secondId != null && firstId != secondId) {
            comparisonLoading = true
            comparisonError = false
            comparisonRows = try {
                val criteria = viewModel.repository.getCriteriaForRun(requireNotNull(selectedRun))
                val firstScores = viewModel.repository.getRunCriterionScoresForStudent(firstId, requireNotNull(selectedRun).id)
                val secondScores = viewModel.repository.getRunCriterionScoresForStudent(secondId, requireNotNull(selectedRun).id)
                studentComparisonRows(criteria, firstScores, secondScores)
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (_: Exception) {
                comparisonError = true
                emptyList()
            } finally {
                comparisonLoading = false
            }
        } else {
            comparisonError = false
            comparisonRows = emptyList()
        }
    }

    val barData = listOf(
        ArcBarData("0 - 49", count0to49.toFloat(), colors.danger),
        ArcBarData("50 - 69", count50to69.toFloat(), colors.warning),
        ArcBarData("70 - 84", count70to84.toFloat(), colors.accent),
        ArcBarData("85 - 100", count85to100.toFloat(), colors.success)
    )

    val donutSegments = listOf(
        ArcDonutSegment("Tamamlandı", completedCount.toFloat(), colors.success),
        ArcDonutSegment("Kısmi", partialCount.toFloat(), colors.warning),
        ArcDonutSegment("Başlanmadı", unscoredCount.toFloat(), colors.surfaceMuted)
    )

    val completionRate = if (totalStudents > 0) completedCount.toFloat() / totalStudents else 0f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        StudentGrowthReportSection(viewModel)

        // Reports follow the most recently used scoring mode. Session data is
        // still separate; switching modes never copies or combines grades.
        ArcSegmentedControl(
            items = listOf("Bireysel değerlendirme", "Grup değerlendirmesi"),
            selectedIndex = if (reportsGroupMode) 1 else 0,
            onSelectIndex = { viewModel.reportsGroupMode.value = it == 1 },
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Değerlendirme Sonuç Raporu",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.foreground
                )
                Text(
                    text = "${selectedClassroom?.name ?: "Sınıf"} • " +
                        if (reportsGroupMode)
                            (selectedGroupSession?.title ?: "Grup değerlendirmesi seçilmedi")
                        else (selectedRun?.title ?: "Bireysel değerlendirme seçilmedi"),
                    fontSize = 14.sp,
                    color = colors.textSecondary
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                val hasReportSession = canExportSelectedReport(
                    groupMode = reportsGroupMode,
                    classroomId = selectedClassroom?.id,
                    rubricId = selectedRubric?.id,
                    individualRunId = selectedRun?.id,
                    groupSessionId = selectedGroupSession?.id
                )
                ArcButton(
                    text = "PDF Raporu Önizle",
                    onClick = {
                        groupReportMode = reportsGroupMode
                        pdfRequestedGroupSessionId = if (reportsGroupMode) selectedGroupSession?.id else null
                        pdfRequestedGroupSessionTitle = if (reportsGroupMode) selectedGroupSession?.title else null
                        pdfRequestedClassroomId = selectedClassroom?.id
                        pdfRequestedRubricId = selectedRubric?.id
                        pdfRequestedRunId = if (!reportsGroupMode) selectedRun?.id else null
                        pdfRequestedRunTitle = if (!reportsGroupMode) selectedRun?.title else null
                        if (reportsGroupMode) pdfAllClasses = false
                        showPdfDialog = true
                    },
                    enabled = hasReportSession && !pdfGenerating,
                    variant = ArcButtonVariant.Outline,
                    size = ArcButtonSize.Md
                )
                Box {
                    ArcSplitButton(
                        onPrimaryClick = {
                            val requestedGroupMode = reportsGroupMode
                            val requestedGroupSessionId = selectedGroupSession?.id
                            val requestedGroupTitle = selectedGroupSession?.title.orEmpty()
                            val requestedClassName = selectedClassroom?.name.orEmpty()
                            val requestedClassroomId = selectedClassroom?.id
                            val requestedRubricId = selectedRubric?.id
                            val requestedRunId = selectedRun?.id
                            if (!csvGenerating && hasReportSession) scope.launch {
                                csvGenerating = true
                                try {
                                    val csv = if (requestedGroupMode) {
                                        val sessionId = requireNotNull(requestedGroupSessionId) {
                                            "Grup oturumu seçilmedi."
                                        }
                                        viewModel.generateGroupCsvForSession(sessionId)
                                    } else {
                                        viewModel.generateCsvForRun(
                                            requireNotNull(requestedClassroomId),
                                            requireNotNull(requestedRubricId),
                                            requireNotNull(requestedRunId)
                                        )
                                    }
                                    require(csv.isNotBlank()) { "Dışa aktarılacak veri yok." }
                                    generatedCsvContent = csv
                                    val stamp = LocalDateTime.now().format(
                                        DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS")
                                    )
                                    csvSuggestedFilename = if (requestedGroupMode) {
                                        "puanla-grup-${filenamePart(requestedClassName)}-" +
                                            "${filenamePart(requestedGroupTitle)}-$stamp.csv"
                                    } else {
                                        "puanla-degerlendirme-${LocalDate.now()}.csv"
                                    }
                                    isCsvPreviewDialogOpen = true
                                } catch (e: Exception) {
                                    viewModel.showToast("CSV oluşturulamadı: ${e.message}")
                                } finally {
                                    csvGenerating = false
                                }
                            }
                        },
                        onMenuClick = { csvActionsOpen = true },
                        enabled = hasReportSession && !csvGenerating,
                        variant = ArcButtonVariant.Primary,
                        size = ArcButtonSize.Md,
                        primaryContent = { Text("CSV İndir (Excel / e-Okul)") }
                    )
                    ArcDropdownMenu(
                        expanded = csvActionsOpen,
                        onDismissRequest = { csvActionsOpen = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("CSV'yi panoya kopyala") },
                            onClick = {
                                csvActionsOpen = false
                                val requestedGroupMode = reportsGroupMode
                                val requestedGroupSessionId = selectedGroupSession?.id
                                val requestedClassroomId = selectedClassroom?.id
                                val requestedRubricId = selectedRubric?.id
                                val requestedRunId = selectedRun?.id
                                if (!csvGenerating && hasReportSession) scope.launch {
                                    csvGenerating = true
                                    try {
                                        val csv = if (requestedGroupMode) {
                                            val sessionId = requireNotNull(requestedGroupSessionId) {
                                                "Grup oturumu seçilmedi."
                                            }
                                            viewModel.generateGroupCsvForSession(sessionId)
                                        } else viewModel.generateCsvForRun(
                                            requireNotNull(requestedClassroomId),
                                            requireNotNull(requestedRubricId),
                                            requireNotNull(requestedRunId)
                                        )
                                        require(csv.isNotBlank()) { "Dışa aktarılacak veri yok." }
                                        clipboard.setText(AnnotatedString(csv))
                                        viewModel.showToast("Seçili değerlendirme CSV verisi panoya kopyalandı.")
                                    } catch (e: Exception) {
                                        viewModel.showToast("CSV kopyalanamadı: ${e.message}")
                                    } finally {
                                        csvGenerating = false
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }

        if (!reportsGroupMode && selectedClassroom != null && selectedRubric != null) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Raporlanacak Değerlendirme Oturumu", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.textSecondary)
                ArcSelect(
                    selectedItem = selectedRun,
                    items = assessmentRuns,
                    onItemSelected = { viewModel.selectRun(it) },
                    labelProvider = { run -> "${run.title} (${reportRunDate(run.createdAt)})" },
                    placeholder = "Değerlendirme oturumu seçin",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (reportsGroupMode) {
            // Individual statistics must never be shown as if they described
            // the currently selected group session.
            if (selectedClassroom == null || selectedRubric == null) {
                ArcEmptyState(
                    title = "Sınıf veya rubrik seçilmedi",
                    description = "Grup raporu için önce bir sınıf ve rubrik seçin."
                )
            } else {
                if (groupSessions.isEmpty()) {
                    ArcEmptyState(
                        title = "Grup değerlendirmesi bulunamadı",
                        description = "Puanlama > Grup alanından değerlendirme oluşturun."
                    )
                } else {
                    ArcSelect(
                        selectedItem = selectedGroupSession,
                        items = groupSessions,
                        onItemSelected = { viewModel.selectGroupSession(it) },
                        labelProvider = { it.title },
                        placeholder = "Grup değerlendirmesi seç",
                        modifier = Modifier.fillMaxWidth()
                    )
                    val completedGroups = groupAssessments.count { it.isCompleted }
                    val partialGroups = groupAssessments.count {
                        !it.isCompleted && it.scoredCount > 0
                    }
                    Text(
                        "${groupAssessments.size} değerlendirilen grup • " +
                            "$completedGroups tamamlanan • $partialGroups kısmi",
                        fontSize = 14.sp,
                        color = colors.textSecondary
                    )
                    Text(
                        "Grup raporu seçili oturumun güncel sonuçlarını içerir; " +
                            "bireysel notlar değişmez. Eksik ölçütler kesin not değildir.",
                        fontSize = 12.sp, color = colors.textSecondary
                    )
                }
            }
            return@Column
        }

        if (selectedClassroom == null || selectedRubric == null || selectedRun == null) {
            Box(modifier = Modifier.fillMaxWidth().height(260.dp), contentAlignment = Alignment.Center) {
                ArcEmptyState(
                    title = "Değerlendirme Seçilmedi",
                    description = "Sınıf ve rubriği seçin, ardından üstteki Değerlendirmeler menüsünden raporlamak istediğiniz oturumu açın."
                )
            }
            return@Column
        }

        ArcButtonGroup(modifier = Modifier.fillMaxWidth()) {
            ArcButton(
                text = if (showDateFilter) "Tarih Filtresini Gizle" else "Son Değişiklik Tarihine Göre Filtrele",
                onClick = { showDateFilter = !showDateFilter },
                variant = ArcButtonVariant.Outline,
                size = ArcButtonSize.Sm
            )
            if (filterActive) {
                ArcButton(
                    text = "Filtreyi Sıfırla",
                    onClick = { filterStart = ""; filterEnd = "" },
                    variant = ArcButtonVariant.Ghost,
                    size = ArcButtonSize.Sm
                )
            }
        }
        if (effectiveStudents.size >= 2) {
            ArcButton(
                text = "İki öğrenciyi ölçüt bazında karşılaştır",
                onClick = {
                    firstComparisonStudentId = effectiveStudents.getOrNull(0)?.id
                    secondComparisonStudentId = effectiveStudents.getOrNull(1)?.id
                    comparisonOpen = true
                },
                variant = ArcButtonVariant.Outline,
                size = ArcButtonSize.Sm
            )
        }
        if (showDateFilter) {
            ArcDateRangePicker(
                startDate = filterStart,
                endDate = filterEnd,
                onRangeSelected = { from, to ->
                    filterStart = from
                    filterEnd = to
                },
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ArcButton(
                    text = "Başlangıç için aylık takvim",
                    onClick = { calendarTarget = if (calendarTarget == "start") null else "start" },
                    variant = ArcButtonVariant.Ghost,
                    size = ArcButtonSize.Sm
                )
                ArcButton(
                    text = "Bitiş için aylık takvim",
                    onClick = { calendarTarget = if (calendarTarget == "end") null else "end" },
                    variant = ArcButtonVariant.Ghost,
                    size = ArcButtonSize.Sm
                )
            }
            if (calendarTarget != null) {
                // ArcCalendar chooses a real ISO date. This filters assessments
                // by their last-update date, never fabricates historical grades.
                val currentCalendarDate = runCatching {
                    LocalDate.parse(if (calendarTarget == "start") filterStart else filterEnd)
                }.getOrNull()
                val calendarMonth = currentCalendarDate?.format(
                    DateTimeFormatter.ofPattern("LLLL yyyy", Locale.forLanguageTag("tr-TR"))
                ).orEmpty()
                ArcCalendar(
                    selectedDay = currentCalendarDate?.dayOfMonth,
                    onSelectDay = {},
                    monthName = calendarMonth,
                    onSelectDate = { isoDate ->
                        if (calendarTarget == "start") filterStart = isoDate else filterEnd = isoDate
                        calendarTarget = null
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        if (filterActive) {
            ArcAlert(
                title = if (filterValid) "Tarihe göre filtrelendi" else "Geçersiz tarih aralığı",
                message = if (filterValid) {
                    "Yalnızca son değişikliği bu tarihlerde yapılan değerlendirmeler gösteriliyor. " +
                        "Bu geçmiş tarihteki notların anlık görüntüsü değildir. PDF ve CSV seçili oturumun güncel sonuçlarını içerir."
                } else "Bitiş tarihi başlangıçtan önce olamaz. Aralığı düzeltin veya filtreyi sıfırlayın.",
                type = if (filterValid) ArcAlertType.Info else ArcAlertType.Warning
            )
        }

        // Stats Band
        ArcStatsBand(
            stats = listOf(
                ArcStatItem(label = if (filterActive) "Aralıkta Güncellenen" else "Toplam Öğrenci", value = "$totalStudents", detail = if (filterActive) "Son değişiklik tarihi" else "Kayıtlı"),
                ArcStatItem(label = "Tamamlanan", value = "$completedCount", detail = "Kesinleşti"),
                ArcStatItem(label = "Kısmi Puanlanan", value = "$partialCount", detail = "Eksik ölçüt"),
                ArcStatItem(label = "Sınıf Ortalaması", value = if (validScores.isNotEmpty()) "${averageScore.roundToInt()}" else "-", detail = "100 üzerinden")
            )
        )

        // A started assessment is not necessarily completed; show both separately.
        ArcUsageMeter(
            used = completedCount + partialCount,
            total = totalStudents,
            label = if (filterActive) "Aralıkta güncellenenlerden puanlananlar" else "Değerlendirmesi başlatılan öğrenciler",
            modifier = Modifier.fillMaxWidth()
        )

        // Metric Cards Grid
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val compact = maxWidth < 700.dp
            val cardModifier = if (compact) Modifier.fillMaxWidth()
                else Modifier.width((maxWidth - 28.dp) / 3)
            val cards = @Composable {
            ArcMetricCard(
                title = "En Yüksek Puan",
                value = if (validScores.isNotEmpty()) "$maxScore" else "-",
                subtitle = "Sınıf birincisi",
                badgeText = "Maksimum",
                badgeVariant = ArcBadgeVariant.Success,
                modifier = cardModifier
            )
            ArcMetricCard(
                title = "En Düşük Puan",
                value = if (validScores.isNotEmpty()) "$minScore" else "-",
                subtitle = "Geliştirilmeli",
                badgeText = "Minimum",
                badgeVariant = ArcBadgeVariant.Warning,
                modifier = cardModifier
            )
            ArcMetricCard(
                title = "Tamamlanma Oranı",
                value = "%${(completionRate * 100).toInt()}",
                subtitle = "$completedCount / $totalStudents öğrenci",
                badgeText = if (completionRate == 1f) "Eksiksiz" else "Sürüyor",
                badgeVariant = if (completionRate == 1f) ArcBadgeVariant.Success else ArcBadgeVariant.Accent,
                modifier = cardModifier
            )
            }
            if (compact) Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { cards() }
            else Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) { cards() }
        }

        // Charts Row: Bar Chart, Donut Chart, and Gauge
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val compact = maxWidth < 760.dp
            val chartModifier = Modifier.fillMaxWidth().height(if (compact) 250.dp else 320.dp)
            val wideChartModifier = Modifier.width((maxWidth - 28.dp) / 3).height(320.dp)
            val charts = @Composable {
            ArcBarChart(
                data = barData,
                modifier = if (compact) chartModifier else wideChartModifier
            )
            ArcDonutChart(
                segments = donutSegments,
                centerTitle = "Öğrenci",
                centerSubtitle = "$totalStudents",
                modifier = if (compact) chartModifier else wideChartModifier
            )
            ArcGauge(
                percentage = completionRate,
                label = "Rubrik Doluluk Oranı",
                modifier = if (compact) chartModifier else wideChartModifier
            )
            }
            if (compact) Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { charts() }
            else Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) { charts() }
        }

        Text("Sınıf Değerlendirme Sonuçları", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colors.foreground)
        when {
            sessionReportLoading -> Text("Seçili oturumun sonuçları yükleniyor…", color = colors.textSecondary)
            sessionReportError -> ArcEmptyState(
                title = "Oturum raporu yüklenemedi",
                description = "Seçili değerlendirme verileri okunamadı. Başka bir oturum seçip yeniden deneyin."
            )
            sessionReportSnapshot == null -> Text("Sonuç tablosu için bir değerlendirme oturumu seçin.", color = colors.textSecondary)
            else -> IndividualReportResultTable(requireNotNull(sessionReportSnapshot))
        }

        // CSV BOM Info Banner
        ArcAlert(
            title = "Microsoft Excel ve Türkçe Karakter Uyumu",
            message = "Dışa aktarılan CSV dosyaları UTF-8 BOM (\uFEFF) ile kodlanır. Excel, Numbers ve Google E-Tablolar'da İ, Ş, Ğ, Ç gibi Türkçe harfler bozulmadan açılır.",
            type = ArcAlertType.Info
        )
    }

    if (comparisonOpen) {
        Dialog(onDismissRequest = { comparisonOpen = false }) {
            Surface(
                shape = ArcTheme.shapes.panel,
                color = colors.surfaceRaised,
                border = BorderStroke(1.dp, colors.border),
                modifier = Modifier.fillMaxWidth(0.95f)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp).fillMaxHeight(0.9f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Aynı rubrikte öğrenci karşılaştırması", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colors.foreground)
                    Text("Puanlar yalnız seçili sınıf ve rubriğin gerçek kayıtlarından okunur.", fontSize = 13.sp, color = colors.textSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ArcSelect(
                            selectedItem = firstComparisonStudent,
                            items = effectiveStudents,
                            onItemSelected = {
                                firstComparisonStudentId = it.id
                                if (secondComparisonStudentId == it.id) {
                                    secondComparisonStudentId = effectiveStudents.firstOrNull { student -> student.id != it.id }?.id
                                }
                            },
                            labelProvider = { it.fullName },
                            placeholder = "İlk öğrenci",
                            modifier = Modifier.weight(1f)
                        )
                        ArcSelect(
                            selectedItem = secondComparisonStudent,
                            items = effectiveStudents.filter { it.id != firstComparisonStudentId },
                            onItemSelected = { secondComparisonStudentId = it.id },
                            labelProvider = { it.fullName },
                            placeholder = "İkinci öğrenci",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (comparisonLoading) {
                        Text("Ölçüt puanları okunuyor…", color = colors.textSecondary)
                    } else if (firstComparisonStudentId == secondComparisonStudentId) {
                        Text("Karşılaştırmak için iki farklı öğrenci seçin.", color = colors.textSecondary)
                    } else if (comparisonError) {
                        Text("Ölçüt puanları yüklenemedi. Seçimi değiştirip yeniden deneyin.", color = colors.danger)
                    } else if (comparisonRows.isEmpty()) {
                        Text("Bu rubrikte gösterilecek ölçüt veya puan kaydı yok.", color = colors.textSecondary)
                    } else {
                        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                            ArcComparisonTable(
                                headers = listOf("Ölçüt", firstComparisonStudent?.fullName ?: "Öğrenci 1", secondComparisonStudent?.fullName ?: "Öğrenci 2"),
                                rows = comparisonRows
                            )
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        ArcButton(text = "Kapat", onClick = { comparisonOpen = false }, variant = ArcButtonVariant.Ghost)
                    }
                }
            }
        }
    }

    if (showPdfDialog) {
        Dialog(onDismissRequest = { if (!pdfGenerating) showPdfDialog = false }) {
            Surface(
                shape = ArcTheme.shapes.panel,
                color = colors.surfaceRaised,
                border = BorderStroke(1.dp, colors.border),
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(if (groupReportMode) "Grup Ölçüt Bazlı PDF Raporu" else "Ölçüt Bazlı PDF Raporu", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colors.foreground)
                    Text(
                        if (groupReportMode)
                            "Seçili grup oturumunun ölçüt puanları bütün grup üyelerine yansıtılır. Bireysel sınav puanları değiştirilmez."
                        else "Seçili sınıfın güncel bireysel puanları, ölçüt açıklamaları, sınıf özeti ve imza alanı yer alır. " +
                            "Tarih filtresinden bağımsız olarak seçili değerlendirme ve sınıfın puanları kullanılır. Grup notları karıştırılmaz.",
                        fontSize = 12.sp,
                        color = colors.textSecondary
                    )
                    ArcInput(
                        value = pdfCourse,
                        onValueChange = { pdfCourse = it },
                        label = "Ders adı"
                    )
                    ArcInput(
                        value = pdfTeacher,
                        onValueChange = { pdfTeacher = it },
                        label = "Öğretmenin adı soyadı"
                    )
                    if (!groupReportMode) Row(verticalAlignment = Alignment.CenterVertically) {
                        ArcCheckbox(
                            checked = pdfAllClasses,
                            onCheckedChange = { pdfAllClasses = it },
                            label = "Bu değerlendirmedeki bütün sınıflar"
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ArcCheckbox(
                            checked = pdfSummaryOnly,
                            onCheckedChange = { pdfSummaryOnly = it },
                            label = "Yalnızca özet ve grafikler (ölçüt tablosu olmadan)"
                        )
                    }
                    Text(
                        "Bu bilgiler sonraki PDF çıktıları için cihazda saklanır. " +
                            "İmza çizgisi çıktı üzerinde elle imzalanabilir.",
                        fontSize = 12.sp,
                        color = colors.textSecondary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ArcButton(
                            text = "Vazgeç",
                            onClick = { showPdfDialog = false },
                            variant = ArcButtonVariant.Ghost,
                            enabled = !pdfGenerating
                        )
                        ArcButton(
                            text = if (pdfGenerating) "Hazırlanıyor…" else "PDF Raporu Önizle",
                            onClick = {
                                val requestedGroupMode = groupReportMode
                                val requestedGroupSessionId = pdfRequestedGroupSessionId
                                val requestedGroupTitle = pdfRequestedGroupSessionTitle
                                val requestedClassroomId = pdfRequestedClassroomId
                                val requestedRubricId = pdfRequestedRubricId
                                val requestedRunId = pdfRequestedRunId
                                val requestedRunTitle = pdfRequestedRunTitle
                                val requestedAllClasses = pdfAllClasses
                                if (!pdfGenerating) scope.launch {
                                    pdfGenerating = true
                                    try {
                                        val snapshots = withContext(Dispatchers.IO) {
                                            if (requestedGroupMode) {
                                                val sessionId = requireNotNull(requestedGroupSessionId) {
                                                    "Grup oturumu seçilmedi."
                                                }
                                                listOf(viewModel.generateGroupPdfForSession(sessionId))
                                            } else {
                                                viewModel.generatePdfSnapshotsForRun(
                                                    classroomId = requireNotNull(requestedClassroomId),
                                                    rubricId = requireNotNull(requestedRubricId),
                                                    runId = requireNotNull(requestedRunId),
                                                    allClassrooms = requestedAllClasses
                                                )
                                            }
                                        }
                                        require(snapshots.isNotEmpty() && snapshots.all { it.criteria.isNotEmpty() }) {
                                            "Rubrikte ölçüt bulunamadı."
                                        }
                                        pendingPdfSnapshots = snapshots
                                        pendingSummaryOnly = pdfSummaryOnly
                                        pendingPdfCourse = pdfCourse.trim()
                                        pendingPdfTeacher = pdfTeacher.trim()
                                        pdfPreferences.edit()
                                            .putString("course", pendingPdfCourse)
                                            .putString("teacher", pendingPdfTeacher)
                                            .apply()
                                        showPdfDialog = false
                                        showPdfPreviewDialog = true
                                    } catch (cancel: CancellationException) {
                                        throw cancel
                                    } catch (e: Exception) {
                                        pendingPdfSnapshots = emptyList()
                                        viewModel.showToast("PDF hazırlanamadı: ${e.message}")
                                    } finally {
                                        pdfGenerating = false
                                    }
                                }
                            },
                            enabled = !pdfGenerating && pdfCourse.isNotBlank() && pdfTeacher.isNotBlank(),
                            variant = ArcButtonVariant.Primary
                        )
                    }
                }
            }
        }
    }

    if (showPdfPreviewDialog && pendingPdfSnapshots.isNotEmpty()) {
        Dialog(onDismissRequest = { showPdfPreviewDialog = false }) {
            Surface(
                shape = ArcTheme.shapes.panel,
                color = colors.surfaceRaised,
                border = BorderStroke(1.dp, colors.border),
                modifier = Modifier.fillMaxWidth(0.94f).fillMaxHeight(0.9f)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("PDF Raporu Önizleme", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = colors.foreground)
                    Text(
                        "Ders: $pendingPdfCourse  •  Öğretmen: $pendingPdfTeacher",
                        fontSize = 13.sp,
                        color = colors.textSecondary
                    )
                    Column(
                        modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        pendingPdfSnapshots.forEach { snapshot ->
                            val summary = PdfReportStatistics.summary(snapshot)
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = colors.surface,
                                shape = ArcTheme.shapes.control,
                                border = BorderStroke(1.dp, colors.borderSubtle)
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(
                                        "${snapshot.classroomName} — ${if (groupReportMode) pdfRequestedGroupSessionTitle else pdfRequestedRunTitle}",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.foreground
                                    )
                                    Text("Ders: ${snapshot.rubricTitle}  •  Akademik yıl: ${snapshot.academicYear}", fontSize = 13.sp, color = colors.textSecondary)
                                    Text(
                                        "${summary.registered} öğrenci  •  ${summary.completed} tamamlandı  •  " +
                                            "${summary.partial} kısmi  •  Ortalama: ${summary.average?.roundToInt() ?: "—"}/100",
                                        fontSize = 13.sp,
                                        color = colors.foreground
                                    )
                                    if (!pendingSummaryOnly) {
                                        Text("Öğrenci sonuç örneği", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = colors.foreground)
                                        snapshot.students.take(6).forEach { student ->
                                            val total = PdfReportStatistics.definitiveScore(student, snapshot.criteria)
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("${student.number}  ${student.firstName} ${student.lastName}", fontSize = 13.sp, color = colors.textSecondary)
                                                Text(total?.let { "$it/100" } ?: "Değerlendirilmedi", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.foreground)
                                            }
                                        }
                                    }
                                    Text(
                                        if (pendingSummaryOnly) "PDF özeti ve grafiklerini içerir."
                                        else "PDF sınıf özeti, puan dağılımı ve rubriğin ölçüt tablolarını içerir.",
                                        fontSize = 12.sp,
                                        color = colors.textMuted
                                    )
                                }
                            }
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                        ArcButton(text = "Kapat", onClick = { showPdfPreviewDialog = false }, variant = ArcButtonVariant.Ghost)
                        Spacer(Modifier.width(10.dp))
                        ArcButton(
                            text = "PDF Raporu Yazdır / Kaydet",
                            onClick = {
                                val requestedGroupMode = groupReportMode
                                val requestedAllClasses = pdfAllClasses
                                val title = if (requestedGroupMode) pdfRequestedGroupSessionTitle else pdfRequestedRunTitle
                                val classLabel = if (requestedAllClasses) "tum-siniflar" else pendingPdfSnapshots.first().classroomName
                                val filename = "puanla-${filenamePart(classLabel)}-${filenamePart(title ?: "rapor")}-${LocalDate.now()}.pdf"
                                showPdfPreviewDialog = false
                                savePdf.launch(filename)
                            },
                            enabled = !pdfGenerating,
                            variant = ArcButtonVariant.Primary
                        )
                    }
                }
            }
        }
    }

    // CSV Export Preview Dialog
    if (isCsvPreviewDialogOpen) {
        Dialog(onDismissRequest = { isCsvPreviewDialogOpen = false }) {
            Surface(
                shape = ArcTheme.shapes.panel,
                color = colors.surfaceRaised,
                border = BorderStroke(1.dp, colors.border),
                shadowElevation = 10.dp,
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "CSV Dışa Aktarma Önizleme",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.foreground
                        )
                        ArcCopyButton(textToCopy = generatedCsvContent)
                    }

                    Spacer(Modifier.height(14.dp))

                    Surface(
                        shape = ArcTheme.shapes.control,
                        color = colors.surfaceMuted,
                        border = BorderStroke(1.dp, colors.borderSubtle),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                    ) {
                        Box(modifier = Modifier.padding(12.dp).verticalScroll(rememberScrollState())) {
                            Text(
                                text = generatedCsvContent,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color = colors.foreground
                            )
                        }
                    }

                    Spacer(Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        ArcButton(
                            text = "Kapat",
                            onClick = { isCsvPreviewDialogOpen = false },
                            variant = ArcButtonVariant.Ghost
                        )
                        Spacer(Modifier.width(10.dp))
                        ArcButton(
                            text = "CSV Dosyası Kaydet",
                            onClick = { saveCsv.launch(csvSuggestedFilename) },
                            variant = ArcButtonVariant.Primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IndividualReportResultTable(snapshot: PdfReportSnapshot) {
    val colors = ArcTheme.colors
    if (snapshot.students.isEmpty()) {
        ArcEmptyState(
            title = "Bu oturumda öğrenci sonucu yok",
            description = "Seçili oturumda henüz öğrenci değerlendirmesi bulunmuyor."
        )
        return
    }
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val minimumWidth = (320 + snapshot.criteria.size * 138).dp
        val tableWidth = if (maxWidth > minimumWidth) maxWidth else minimumWidth
        Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
            ArcSortableDataTable(
                items = snapshot.students,
                columns = buildList {
                    add(ArcTableColumn<CsvStudent>("No", 0.55f) { row -> Text(row.number, color = colors.foreground) })
                    add(ArcTableColumn<CsvStudent>("Öğrenci Adı Soyadı", 1.2f) { row -> Text("${row.firstName} ${row.lastName}", color = colors.foreground) })
                    snapshot.criteria.forEach { criterion ->
                        add(ArcTableColumn<CsvStudent>("${criterion.title} (%${criterion.maxPoints})", 1f) { row ->
                            Text(row.scores[criterion.id]?.let { "$it/${criterion.maxPoints}" } ?: "—", color = colors.textSecondary)
                        })
                    }
                    add(ArcTableColumn<CsvStudent>("Toplam (100)", 0.7f) { row ->
                        Text(
                            PdfReportStatistics.definitiveScore(row, snapshot.criteria)?.toString() ?: "—",
                            fontWeight = FontWeight.Bold,
                            color = colors.foreground
                        )
                    })
                    add(ArcTableColumn<CsvStudent>("Düzey", 1f) { row ->
                        val state = PdfReportStatistics.state(row, snapshot.criteria)
                        val (label, color) = when (state) {
                            PdfScoreState.COMPLETED -> "Tamamlandı" to colors.success
                            PdfScoreState.PARTIAL -> "Kısmi" to colors.warning
                            PdfScoreState.NOT_STARTED -> "Değerlendirilmedi" to colors.textMuted
                        }
                        Text(label, color = color)
                    })
                },
                modifier = Modifier.width(tableWidth).height(510.dp),
                pageSize = 25,
                rowKey = { "${it.number}-${it.firstName}-${it.lastName}" }
            )
        }
    }
}

private fun reportRunDate(epochMillis: Long): String = if (epochMillis <= 0L) "Tarih belirtilmedi" else
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.forLanguageTag("tr-TR")))

private fun filenamePart(value: String): String = value.trim()
    .replace(Regex("[^\\p{L}\\p{N}_-]+"), "-")
    .trim('-')
    .take(40)
    .ifEmpty { "rapor" }
