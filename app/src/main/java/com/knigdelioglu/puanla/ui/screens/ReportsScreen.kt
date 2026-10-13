package com.knigdelioglu.puanla.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.OutlinedTextField
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
import com.knigdelioglu.arc.compose.controls.ArcButtonSize
import com.knigdelioglu.arc.compose.controls.ArcButtonVariant
import com.knigdelioglu.arc.compose.controls.ArcDateRangePicker
import com.knigdelioglu.arc.compose.controls.ArcCalendar
import com.knigdelioglu.arc.compose.controls.ArcSelect
import com.knigdelioglu.arc.compose.controls.ArcSplitButton
import com.knigdelioglu.arc.compose.controls.ArcButtonGroup
import com.knigdelioglu.arc.compose.controls.ArcDropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import com.knigdelioglu.puanla.domain.report.AssessmentDateFilter
import com.knigdelioglu.puanla.domain.export.PdfReportSnapshot
import com.knigdelioglu.puanla.domain.export.PdfReportWriter
import com.knigdelioglu.arc.compose.datavis.ArcBarChart
import com.knigdelioglu.arc.compose.datavis.ArcBarData
import com.knigdelioglu.arc.compose.datavis.ArcDonutChart
import com.knigdelioglu.arc.compose.datavis.ArcDonutSegment
import com.knigdelioglu.arc.compose.datavis.ArcGauge
import com.knigdelioglu.arc.compose.datavis.ArcMetricCard
import com.knigdelioglu.arc.compose.datavis.ArcStatItem
import com.knigdelioglu.arc.compose.datavis.ArcStatsBand
import com.knigdelioglu.arc.compose.datavis.ArcUsageMeter
import com.knigdelioglu.arc.compose.display.ArcAlert
import com.knigdelioglu.arc.compose.display.ArcAlertType
import com.knigdelioglu.arc.compose.display.ArcBadge
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
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ReportsScreen(viewModel: PuanlaViewModel) {
    val selectedClassroom by viewModel.selectedClassroom.collectAsState()
    val selectedRubric by viewModel.selectedRubric.collectAsState()
    val selectedRun by viewModel.selectedRun.collectAsState()
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
    var pdfGenerating by remember { mutableStateOf(false) }
    var pendingPdfSnapshots by remember { mutableStateOf<List<PdfReportSnapshot>>(emptyList()) }
    var pdfAllClasses by remember { mutableStateOf(false) }
    var pdfSummaryOnly by remember { mutableStateOf(false) }
    var pendingSummaryOnly by remember { mutableStateOf(false) }
    var pendingPdfCourse by remember { mutableStateOf("") }
    var pendingPdfTeacher by remember { mutableStateOf("") }
    var csvActionsOpen by remember { mutableStateOf(false) }
    var isCsvPreviewDialogOpen by remember { mutableStateOf(false) }
    var generatedCsvContent by remember { mutableStateOf("") }
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
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Değerlendirme Raporları & Analiz",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.foreground
                )
                Text(
                    text = "${selectedClassroom?.name ?: "Sınıf"} • ${selectedRun?.title ?: "Değerlendirme seçilmedi"}",
                    fontSize = 14.sp,
                    color = colors.textSecondary
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                ArcButton(
                    text = "PDF Raporu",
                    onClick = { showPdfDialog = true },
                    enabled = selectedClassroom != null && selectedRun != null && !pdfGenerating,
                    variant = ArcButtonVariant.Outline,
                    size = ArcButtonSize.Md
                )
                Box {
                ArcSplitButton(
                    onPrimaryClick = {
                    if (!csvGenerating) scope.launch {
                        csvGenerating = true
                        try {
                            val csv = viewModel.generateCsv()
                            if (csv.isBlank()) error("Dışa aktarılacak veri yok.")
                            generatedCsvContent = csv
                            isCsvPreviewDialogOpen = true
                        } catch (e: Exception) {
                            viewModel.showToast("CSV oluşturulamadı: ${e.message}")
                        } finally {
                            csvGenerating = false
                        }
                    }
                },
                    onMenuClick = { csvActionsOpen = true },
                    enabled = selectedClassroom != null && selectedRun != null && !csvGenerating,
                    variant = ArcButtonVariant.Primary,
                    size = ArcButtonSize.Md,
                    primaryContent = { Text("CSV Dışa Aktar") }
                )
                ArcDropdownMenu(
                    expanded = csvActionsOpen,
                    onDismissRequest = { csvActionsOpen = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("CSV'yi panoya kopyala") },
                        onClick = {
                            csvActionsOpen = false
                            if (!csvGenerating) scope.launch {
                                csvGenerating = true
                                try {
                                    val csv = viewModel.generateCsv()
                                    require(csv.isNotBlank()) { "Dışa aktarılacak veri yok." }
                                    clipboard.setText(AnnotatedString(csv))
                                    viewModel.showToast("Güncel sınıf CSV verisi panoya kopyalandı.")
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ArcMetricCard(
                title = "En Yüksek Puan",
                value = if (validScores.isNotEmpty()) "$maxScore" else "-",
                subtitle = "Sınıf birincisi",
                badgeText = "Maksimum",
                badgeVariant = ArcBadgeVariant.Success,
                modifier = Modifier.weight(1f)
            )
            ArcMetricCard(
                title = "En Düşük Puan",
                value = if (validScores.isNotEmpty()) "$minScore" else "-",
                subtitle = "Geliştirilmeli",
                badgeText = "Minimum",
                badgeVariant = ArcBadgeVariant.Warning,
                modifier = Modifier.weight(1f)
            )
            ArcMetricCard(
                title = "Tamamlanma Oranı",
                value = "%${(completionRate * 100).toInt()}",
                subtitle = "$completedCount / $totalStudents öğrenci",
                badgeText = if (completionRate == 1f) "Eksiksiz" else "Sürüyor",
                badgeVariant = if (completionRate == 1f) ArcBadgeVariant.Success else ArcBadgeVariant.Accent,
                modifier = Modifier.weight(1f)
            )
        }

        // Charts Row: Bar Chart, Donut Chart, and Gauge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ArcBarChart(
                data = barData,
                modifier = Modifier.weight(1.4f)
            )
            ArcDonutChart(
                segments = donutSegments,
                centerTitle = "Öğrenci",
                centerSubtitle = "$totalStudents",
                modifier = Modifier.weight(1.2f)
            )
            ArcGauge(
                percentage = completionRate,
                label = "Rubrik Doluluk Oranı",
                modifier = Modifier.weight(1f)
            )
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
                    Text("Ölçüt Bazlı PDF Raporu", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colors.foreground)
                    Text(
                        "Seçili sınıfın güncel bireysel puanları, ölçüt açıklamaları, sınıf özeti ve imza alanı yer alır. " +
                            "Tarih filtresinden bağımsız olarak seçili değerlendirme ve sınıfın puanları kullanılır. Grup notları karıştırılmaz.",
                        fontSize = 12.sp,
                        color = colors.textSecondary
                    )
                    OutlinedTextField(
                        value = pdfCourse,
                        onValueChange = { pdfCourse = it },
                        label = { Text("Ders adı") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = pdfTeacher,
                        onValueChange = { pdfTeacher = it },
                        label = { Text("Öğretmenin adı soyadı") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.material3.Checkbox(
                            checked = pdfAllClasses,
                            onCheckedChange = { pdfAllClasses = it }
                        )
                        Text(
                            "Bu değerlendirmedeki bütün sınıflar",
                            color = colors.foreground
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.material3.Checkbox(
                            checked = pdfSummaryOnly,
                            onCheckedChange = { pdfSummaryOnly = it }
                        )
                        Text(
                            "Yalnızca özet ve grafikler (ölçüt tablosu olmadan)",
                            color = colors.foreground
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
                            text = if (pdfGenerating) "Hazırlanıyor…" else "PDF Kaydet",
                            onClick = {
                                if (!pdfGenerating) scope.launch {
                                    pdfGenerating = true
                                    try {
                                        val snapshots = withContext(Dispatchers.IO) {
                                            viewModel.generatePdfSnapshots(pdfAllClasses)
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
                                        savePdf.launch("puanla-${if (pdfAllClasses) "tum-siniflar" else snapshots.first().classroomName}-${selectedRun?.title?.replace(Regex("[^a-zA-Z0-9_-]"), "-")?.take(30) ?: "rapor"}-${LocalDate.now()}.pdf")
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
                            onClick = { saveCsv.launch("puanla-degerlendirme.csv") },
                            variant = ArcButtonVariant.Primary
                        )
                    }
                }
            }
        }
    }
}
