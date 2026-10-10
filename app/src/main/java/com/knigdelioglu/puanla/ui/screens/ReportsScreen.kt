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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.knigdelioglu.arc.compose.datavis.ArcBarChart
import com.knigdelioglu.arc.compose.datavis.ArcBarData
import com.knigdelioglu.arc.compose.datavis.ArcDonutChart
import com.knigdelioglu.arc.compose.datavis.ArcDonutSegment
import com.knigdelioglu.arc.compose.datavis.ArcGauge
import com.knigdelioglu.arc.compose.datavis.ArcMetricCard
import com.knigdelioglu.arc.compose.datavis.ArcSparkline
import com.knigdelioglu.arc.compose.datavis.ArcStatItem
import com.knigdelioglu.arc.compose.datavis.ArcStatsBand
import com.knigdelioglu.arc.compose.display.ArcAlert
import com.knigdelioglu.arc.compose.display.ArcAlertType
import com.knigdelioglu.arc.compose.display.ArcBadge
import com.knigdelioglu.arc.compose.display.ArcBadgeVariant
import com.knigdelioglu.arc.compose.display.ArcCopyButton
import com.knigdelioglu.arc.compose.display.ArcEmptyState
import com.knigdelioglu.arc.compose.foundation.ArcTheme
import com.knigdelioglu.puanla.ui.viewmodel.PuanlaViewModel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun ReportsScreen(viewModel: PuanlaViewModel) {
    val selectedClassroom by viewModel.selectedClassroom.collectAsState()
    val selectedRubric by viewModel.selectedRubric.collectAsState()
    val students by viewModel.students.collectAsState()
    val classAssessments by viewModel.classAssessments.collectAsState()

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var isCsvPreviewDialogOpen by remember { mutableStateOf(false) }
    var generatedCsvContent by remember { mutableStateOf("") }
    var csvGenerating by remember { mutableStateOf(false) }
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

    val scrollState = rememberScrollState()

    // Calculations
    val totalStudents = students.size
    val assessmentMap = remember(classAssessments) { classAssessments.associateBy { it.studentId } }

    val completedCount = students.count { assessmentMap[it.id]?.isCompleted == true }
    val partialCount = students.count {
        val asm = assessmentMap[it.id]
        asm?.isCompleted == false && (asm.scoredCount ?: 0) > 0
    }
    val unscoredCount = (totalStudents - completedCount - partialCount).coerceAtLeast(0)

    val validScores = students.mapNotNull {
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
                    text = "${selectedClassroom?.name ?: "Sınıf"} • ${selectedRubric?.title ?: "Rubrik"}",
                    fontSize = 14.sp,
                    color = colors.textSecondary
                )
            }

            ArcButton(
                text = "Excel / CSV Dışa Aktar",
                onClick = {
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
                variant = ArcButtonVariant.Primary,
                size = ArcButtonSize.Md,
                enabled = selectedClassroom != null && selectedRubric != null && !csvGenerating
            )
        }

        if (selectedClassroom == null || selectedRubric == null) {
            Box(modifier = Modifier.fillMaxWidth().height(260.dp), contentAlignment = Alignment.Center) {
                ArcEmptyState(
                    title = "Sınıf veya Rubrik Seçilmedi",
                    description = "Raporları incelemek için önce bir sınıf ve rubrik seçiniz."
                )
            }
            return@Column
        }

        // Stats Band
        ArcStatsBand(
            stats = listOf(
                ArcStatItem(label = "Toplam Öğrenci", value = "$totalStudents", detail = "Kayıtlı"),
                ArcStatItem(label = "Tamamlanan", value = "$completedCount", detail = "Kesinleşti"),
                ArcStatItem(label = "Kısmi Puanlanan", value = "$partialCount", detail = "Eksik ölçüt"),
                ArcStatItem(label = "Sınıf Ortalaması", value = if (validScores.isNotEmpty()) "${averageScore.roundToInt()}" else "-", detail = "100 üzerinden")
            )
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

        // Sparkline progression across students
        if (validScores.size >= 2) {
            Surface(
                shape = ArcTheme.shapes.panel,
                color = colors.surface,
                border = BorderStroke(1.dp, colors.borderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Liste Sırasına Göre Puan Eğilimi",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.foreground
                        )
                        Text(
                            text = "Öğrencilerin sınıf listesindeki başarı dalgalanması.",
                            fontSize = 13.sp,
                            color = colors.textSecondary
                        )
                    }
                    ArcSparkline(
                        points = validScores,
                        lineColor = colors.accent,
                        modifier = Modifier.width(160.dp).height(44.dp)
                    )
                }
            }
        }

        // CSV BOM Info Banner
        ArcAlert(
            title = "Microsoft Excel ve Türkçe Karakter Uyumu",
            message = "Dışa aktarılan CSV dosyaları UTF-8 BOM (\uFEFF) ile kodlanır. Excel, Numbers ve Google E-Tablolar'da İ, Ş, Ğ, Ç gibi Türkçe harfler bozulmadan açılır.",
            type = ArcAlertType.Info
        )
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
