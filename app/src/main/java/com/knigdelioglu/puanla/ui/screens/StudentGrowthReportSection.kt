package com.knigdelioglu.puanla.ui.screens

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.max
import com.knigdelioglu.arc.compose.controls.ArcButton
import com.knigdelioglu.arc.compose.controls.ArcButtonSize
import com.knigdelioglu.arc.compose.controls.ArcButtonVariant
import com.knigdelioglu.arc.compose.controls.ArcSelect
import com.knigdelioglu.arc.compose.display.ArcEmptyState
import com.knigdelioglu.arc.compose.display.ArcProgress
import com.knigdelioglu.arc.compose.foundation.ArcTheme
import com.knigdelioglu.puanla.data.local.ClassroomEntity
import com.knigdelioglu.puanla.data.local.StudentCriterionAverage
import com.knigdelioglu.puanla.data.local.StudentEntity
import com.knigdelioglu.puanla.data.local.StudentRunScore
import com.knigdelioglu.puanla.data.local.fullName
import com.knigdelioglu.puanla.ui.viewmodel.PuanlaViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun StudentGrowthReportSection(viewModel: PuanlaViewModel) {
    val classrooms by viewModel.classrooms.collectAsState()
    val selectedClassroom by viewModel.selectedClassroom.collectAsState()
    val selectedRun by viewModel.selectedRun.collectAsState()
    val students by viewModel.students.collectAsState()
    var selectedStudentId by remember(selectedClassroom?.id) { mutableStateOf<String?>(null) }
    val selectedStudent = students.firstOrNull { it.id == selectedStudentId } ?: students.firstOrNull()

    var history by remember { mutableStateOf<List<StudentRunScore>?>(null) }
    var criterionAverages by remember { mutableStateOf<List<StudentCriterionAverage>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf(false) }

    LaunchedEffect(selectedClassroom?.id, selectedStudent?.id, selectedRun?.id) {
        val classroomId = selectedClassroom?.id
        val studentId = selectedStudent?.id
        history = null
        criterionAverages = emptyList()
        loadError = false
        if (classroomId == null || studentId == null) {
            loading = false
            return@LaunchedEffect
        }
        loading = true
        try {
            val result = withContext(Dispatchers.IO) {
                viewModel.repository.getCompletedRunScoresForStudent(classroomId, studentId) to
                    viewModel.repository.getStudentCriterionAverages(classroomId, studentId)
            }
            history = result.first
            criterionAverages = result.second
        } catch (cancel: kotlinx.coroutines.CancellationException) {
            throw cancel
        } catch (_: Exception) {
            loadError = true
            history = emptyList()
        } finally {
            loading = false
        }
    }

    val colors = ArcTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = colors.surface,
            shape = ArcTheme.shapes.panel,
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    "Öğrenci Gelişim Grafikleri ve Raporlama Merkezi",
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.foreground
                )
                Text(
                    "Öğrencilerin oturumlar arası gelişim eğrilerini inceleyin, değerlendirme sonuçlarını PDF ve CSV olarak dışa aktarın.",
                    fontSize = 15.sp,
                    color = colors.textSecondary
                )
                if (classrooms.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        classrooms.forEach { classroom ->
                            ArcButton(
                                text = classroom.name,
                                onClick = { if (classroom.id != selectedClassroom?.id) viewModel.selectClassroom(classroom) },
                                variant = if (classroom.id == selectedClassroom?.id) ArcButtonVariant.Primary else ArcButtonVariant.Outline,
                                size = ArcButtonSize.Sm
                            )
                        }
                    }
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = colors.surface,
            shape = ArcTheme.shapes.panel,
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("Bireysel Öğrenci Gelişim Süreci Takibi", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = colors.foreground)
                        Text("Farklı değerlendirme oturumlarındaki puan değişimi ve ölçüt bazlı yetkinlik düzeyi", fontSize = 13.sp, color = colors.textSecondary)
                    }
                    if (students.isNotEmpty()) {
                        Column(modifier = Modifier.width(330.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text("Öğrenci Seçin", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.textSecondary)
                            ArcSelect(
                                selectedItem = selectedStudent,
                                items = students,
                                onItemSelected = { selectedStudentId = it.id },
                                labelProvider = { "#${it.studentNumber} — ${it.fullName}" },
                                placeholder = "Öğrenci seçin"
                            )
                        }
                    }
                }
                HorizontalDivider(color = colors.borderSubtle)
                when {
                    selectedClassroom == null -> ArcEmptyState(
                        title = "Sınıf seçilmedi",
                        description = "Öğrenci gelişimini görmek için yukarıdan bir sınıf seçin."
                    )
                    students.isEmpty() -> ArcEmptyState(
                        title = "Sınıfta öğrenci yok",
                        description = "Gelişim grafiği için önce Sınıflar & OCR ekranından öğrenci ekleyin."
                    )
                    loading || history == null -> Text("Öğrencinin tamamlanmış oturumları yükleniyor…", color = colors.textSecondary)
                    loadError -> ArcEmptyState(
                        title = "Gelişim verisi yüklenemedi",
                        description = "Bağlantı gerekmiyor. Ekranı yeniden açarak yerel kayıtları tekrar yükleyin."
                    )
                    else -> StudentGrowthDetails(
                        student = requireNotNull(selectedStudent),
                        history = requireNotNull(history),
                        criterionAverages = criterionAverages
                    )
                }
            }
        }
    }
}

@Composable
private fun StudentGrowthDetails(
    student: StudentEntity,
    history: List<StudentRunScore>,
    criterionAverages: List<StudentCriterionAverage>
) {
    val colors = ArcTheme.colors
    val first = history.firstOrNull()
    val last = history.lastOrNull()
    val delta = if (history.size >= 2) requireNotNull(last).total - requireNotNull(first).total else null
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val compact = maxWidth < 850.dp
        val left: @Composable () -> Unit = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("Öğrenci", fontSize = 12.sp, color = colors.textMuted)
                        Text("${student.fullName} (#${student.studentNumber})", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = colors.foreground)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Son Puan", fontSize = 12.sp, color = colors.textSecondary)
                            Text(last?.let { "${it.total}/100" } ?: "—", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = colors.foreground)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Gelişim Farkı", fontSize = 12.sp, color = colors.textSecondary)
                            Text(
                                delta?.let { (if (it >= 0) "+" else "") + "$it Puan" } ?: if (history.isEmpty()) "—" else "Tek oturum",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = when { delta == null -> colors.textSecondary; delta >= 0 -> colors.success; else -> colors.warning }
                            )
                        }
                    }
                }
                StudentGrowthChart(history)
                StudentSessionHistory(history)
            }
        }
        val right: @Composable (Modifier) -> Unit = { modifier ->
            CriterionAveragesCard(criterionAverages, modifier)
        }
        if (compact) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                left()
                right(Modifier.fillMaxWidth())
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(22.dp), verticalAlignment = Alignment.Top) {
                Box(Modifier.weight(1.55f)) { left() }
                right(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StudentGrowthChart(history: List<StudentRunScore>) {
    val colors = ArcTheme.colors
    Surface(
        modifier = Modifier.fillMaxWidth().testTag("studentGrowthChart"),
        color = colors.surfaceRaised,
        shape = ArcTheme.shapes.panel,
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Oturum Puan Gelişimi", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = colors.textSecondary)
            if (history.isEmpty()) {
                Box(Modifier.fillMaxWidth().height(210.dp), contentAlignment = Alignment.Center) {
                    Text("Gelişim eğrisi için tamamlanmış değerlendirme oturumu yok.", color = colors.textSecondary)
                }
            } else {
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val graphWidth = max(maxWidth, (history.size * 138).dp)
                    Row(Modifier.horizontalScroll(rememberScrollState())) {
                        Canvas(
                            modifier = Modifier.width(graphWidth).height(235.dp)
                                .semantics { contentDescription = history.mapIndexed { index, score -> "${index + 1}. oturum: ${score.total} puan, ${shortReportDate(score.assessedAt)}" }.joinToString("; ") }
                        ) {
                            val left = 38.dp.toPx()
                            val right = 18.dp.toPx()
                            val top = 22.dp.toPx()
                            val bottom = 54.dp.toPx()
                            val plotWidth = (size.width - left - right).coerceAtLeast(1f)
                            val plotHeight = (size.height - top - bottom).coerceAtLeast(1f)
                            val gridColor = colors.borderSubtle
                            val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                color = colors.textMuted.toArgb()
                                textSize = 11.sp.toPx()
                                typeface = Typeface.create("sans-serif", Typeface.NORMAL)
                                textAlign = Paint.Align.RIGHT
                            }
                            val pointLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                color = colors.foreground.toArgb()
                                textSize = 12.sp.toPx()
                                typeface = Typeface.create("sans-serif", Typeface.BOLD)
                                textAlign = Paint.Align.CENTER
                            }
                            val sessionLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                color = colors.textSecondary.toArgb()
                                textSize = 10.sp.toPx()
                                typeface = Typeface.create("sans-serif", Typeface.NORMAL)
                                textAlign = Paint.Align.CENTER
                            }
                            val xFor: (Int) -> Float = { index ->
                                if (history.size == 1) left + plotWidth / 2f
                                else left + plotWidth * index / (history.size - 1)
                            }
                            val yFor: (Int) -> Float = { score ->
                                top + plotHeight * (100 - score.coerceIn(0, 100)) / 100f
                            }

                            listOf(0, 25, 50, 75, 100).forEach { tick ->
                                val y = yFor(tick)
                                drawLine(gridColor, androidx.compose.ui.geometry.Offset(left, y), androidx.compose.ui.geometry.Offset(size.width - right, y), 1.dp.toPx())
                                drawContext.canvas.nativeCanvas.drawText(tick.toString(), left - 7.dp.toPx(), y + 4.dp.toPx(), axisPaint)
                            }

                            val line = Path()
                            val area = Path()
                            history.forEachIndexed { index, score ->
                                val x = xFor(index)
                                val y = yFor(score.total)
                                if (index == 0) {
                                    line.moveTo(x, y)
                                    area.moveTo(x, y)
                                } else {
                                    line.lineTo(x, y)
                                    area.lineTo(x, y)
                                }
                            }
                            area.lineTo(xFor(history.lastIndex), top + plotHeight)
                            area.lineTo(xFor(0), top + plotHeight)
                            area.close()
                            drawPath(area, colors.accent.copy(alpha = 0.12f), style = Fill)
                            if (history.size > 1) drawPath(line, colors.accent, style = Stroke(width = 2.6.dp.toPx()))
                            history.forEachIndexed { index, score ->
                                val x = xFor(index)
                                val y = yFor(score.total)
                                drawCircle(Color.White, 6.dp.toPx(), androidx.compose.ui.geometry.Offset(x, y))
                                drawCircle(colors.accent, 6.dp.toPx(), androidx.compose.ui.geometry.Offset(x, y), style = Stroke(width = 2.5.dp.toPx()))
                                drawContext.canvas.nativeCanvas.drawText(score.total.toString(), x, y - 11.dp.toPx(), pointLabelPaint)
                                drawContext.canvas.nativeCanvas.drawText("${index + 1}. Oturum", x, size.height - 25.dp.toPx(), sessionLabelPaint)
                                drawContext.canvas.nativeCanvas.drawText(shortReportDate(score.assessedAt), x, size.height - 9.dp.toPx(), sessionLabelPaint)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StudentSessionHistory(history: List<StudentRunScore>) {
    val colors = ArcTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Oturum Geçmişi ve Öğretmen Gözlem Notları", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.foreground)
        if (history.isEmpty()) {
            Text("Tamamlanan oturum ve gözlem notu bulunmuyor.", fontSize = 13.sp, color = colors.textSecondary)
        } else {
            history.asReversed().forEachIndexed { index, item ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = colors.surface,
                    shape = ArcTheme.shapes.control,
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${history.size - index}. ${item.runTitle}", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = colors.foreground)
                            Text("${item.total}/100", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.accent)
                        }
                        Text("Tarih: ${fullReportDate(item.assessedAt)}", fontSize = 12.sp, color = colors.textMuted)
                        item.observationNote?.takeIf { it.isNotBlank() }?.let {
                            Surface(color = colors.surfaceMuted, shape = RoundedCornerShape(8.dp)) {
                                Text("Gözlem: $it", modifier = Modifier.fillMaxWidth().padding(9.dp), fontSize = 13.sp, color = colors.textSecondary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CriterionAveragesCard(averages: List<StudentCriterionAverage>, modifier: Modifier = Modifier) {
    val colors = ArcTheme.colors
    Surface(
        modifier = modifier,
        color = colors.surfaceMuted,
        shape = ArcTheme.shapes.panel,
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Ölçüt Bazlı Ortalama Yetkinlik Düzeyi", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = colors.foreground)
            Text("Tamamlanmış bireysel oturumlarda ölçüt başına alınan puanların, ilgili rubrik üst puanına göre ortalaması.", fontSize = 12.sp, color = colors.textSecondary)
            if (averages.isEmpty()) {
                Text("Ölçüt ortalamaları tamamlanmış ve puanlanmış oturumlar oluşunca görünür.", fontSize = 13.sp, color = colors.textSecondary)
            } else {
                averages.forEach { average ->
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(average.title, modifier = Modifier.weight(1f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.foreground)
                            Spacer(Modifier.width(12.dp))
                            Text("%${average.averagePercent}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.foreground)
                        }
                        ArcProgress(
                            progress = average.averagePercent / 100f,
                            height = 9.dp,
                            progressColor = if (average.averagePercent >= 90) colors.success else colors.accent
                        )
                    }
                }
            }
        }
    }
}

private fun shortReportDate(epochMillis: Long): String = if (epochMillis <= 0L) "Tarih yok" else
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("MM-dd", Locale.forLanguageTag("tr-TR")))

private fun fullReportDate(epochMillis: Long): String = if (epochMillis <= 0L) "belirtilmedi" else
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.forLanguageTag("tr-TR")))
