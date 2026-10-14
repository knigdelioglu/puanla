package com.knigdelioglu.puanla.domain.export

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import java.io.OutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Print-friendly landscape A4 PDF. Uses Android's built-in PDF engine, so no
 * network, storage permission, screenshots, or PDF dependency is necessary.
 */
object PdfReportWriter {
    private const val WIDTH = 842
    private const val HEIGHT = 595
    private const val MARGIN = 28f
    private const val RIGHT = WIDTH - MARGIN
    private const val TABLE_BOTTOM = 507f
    private const val ROW_HEIGHT = 20f
    private const val ROWS_PER_PAGE = 16
    private const val CRITERIA_PER_PAGE = 6

    private val accent = Color.rgb(21, 112, 112)
    private val navy = Color.rgb(28, 44, 63)
    private val muted = Color.rgb(102, 116, 129)
    private val line = Color.rgb(215, 223, 228)
    private val pale = Color.rgb(239, 246, 245)
    private val warning = Color.rgb(177, 110, 20)
    private val fill = Color.WHITE
    private val regular = Typeface.create("sans-serif", Typeface.NORMAL)
    private val bold = Typeface.create("sans-serif", Typeface.BOLD)

    fun write(
        snapshot: PdfReportSnapshot,
        courseName: String,
        teacherName: String,
        output: OutputStream,
        generatedDate: LocalDate = LocalDate.now()
    ) = writeAll(listOf(snapshot), courseName, teacherName, output, false, generatedDate)

    /** Multi-class export: each classroom occupies its own contiguous section. */
    fun writeAll(
        snapshots: List<PdfReportSnapshot>,
        courseName: String,
        teacherName: String,
        output: OutputStream,
        summaryOnly: Boolean = false,
        generatedDate: LocalDate = LocalDate.now()
    ) {
        require(snapshots.isNotEmpty()) { "Raporlanacak sınıf yok." }
        require(courseName.isNotBlank() && teacherName.isNotBlank()) { "Ders ve öğretmen adı zorunludur." }
        snapshots.forEach { snapshot ->
            require(snapshot.criteria.isNotEmpty() &&
                snapshot.criteria.distinctBy { it.id }.size == snapshot.criteria.size) {
                "Rubrik ölçütleri geçersiz."
            }
        }
        val date = generatedDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.forLanguageTag("tr-TR")))
        val pdf = PdfDocument()
        var pageNumber = 0

        fun newPage(snapshot: PdfReportSnapshot, title: String): Pair<PdfDocument.Page, Canvas> {
            pageNumber++
            val page = pdf.startPage(
                PdfDocument.PageInfo.Builder(WIDTH, HEIGHT, pageNumber).create()
            )
            val canvas = page.canvas
            canvas.drawColor(fill)
            frame(canvas, snapshot, title, date, pageNumber, courseName, teacherName)
            return page to canvas
        }

        try {
            snapshots.forEach { snapshot ->
            val summary = PdfReportStatistics.summary(snapshot)
            run {
                val (page, canvas) = newPage(snapshot, "Değerlendirme Raporu")
                drawSummary(canvas, snapshot, summary)
                pdf.finishPage(page)
            }
            if (!summaryOnly) {
            // Every criterion title is preserved in full here. Table columns
            // use Ö1, Ö2, ... to avoid tiny, unreadable text.
            run {
                var pair = newPage(snapshot, "Rubrik Ölçütleri")
                var y = 121f
                snapshot.criteria.forEachIndexed { index, criterion ->
                    val label = "Ö${index + 1}  ${criterion.title}  (Azami ${criterion.maxPoints} puan)"
                    val wrapped = wrap(label, 10f, RIGHT - MARGIN - 24f)
                    val required = (wrapped.size * 15 + 8).toFloat()
                    if (y + required > TABLE_BOTTOM) {
                        pdf.finishPage(pair.first)
                        pair = newPage(snapshot, "Rubrik Ölçütleri (devam)")
                        y = 121f
                    }
                    rounded(pair.second, MARGIN, y - 11f, RIGHT, y + required - 7f, pale)
                    wrapped.forEach { str ->
                        writeText(pair.second, str, MARGIN + 12f, y, 10f, navy)
                        y += 15f
                    }
                    y += 8f
                }
                pdf.finishPage(pair.first)
            }

            val criterionGroups = snapshot.criteria.withIndex().toList().chunked(CRITERIA_PER_PAGE)
            val studentGroups = if (snapshot.students.isEmpty()) {
                listOf(emptyList())
            } else snapshot.students.chunked(ROWS_PER_PAGE)

            criterionGroups.forEachIndexed { groupIndex, criteria ->
                studentGroups.forEachIndexed { studentPageIndex, students ->
                    val (page, canvas) = newPage(snapshot, "Öğrenci Ölçüt Puanları")
                    val first = studentPageIndex * ROWS_PER_PAGE + 1
                    val last = first + students.size - 1
                    writeText(
                        canvas,
                        "Ölçüt grubu ${groupIndex + 1}/${criterionGroups.size}    |    " +
                            if (students.isEmpty()) "Öğrenci kaydı yok" else "Öğrenciler $first-$last / ${snapshot.students.size}",
                        MARGIN, 111f, 10f, muted
                    )
                    drawTable(canvas, snapshot, criteria, students)
                    pdf.finishPage(page)
                }
            }
            }
            }
            pdf.writeTo(output)
        } finally {
            pdf.close()
        }
    }

    private fun frame(
        canvas: Canvas,
        snapshot: PdfReportSnapshot,
        pageTitle: String,
        date: String,
        page: Int,
        course: String,
        teacher: String
    ) {
        rounded(canvas, MARGIN, 20f, RIGHT, 83f, pale)
        writeText(canvas, pageTitle, MARGIN + 16, 45f, 18f, navy, bold, 580f)
        writeText(
            canvas, "${snapshot.classroomName}  •  ${snapshot.academicYear}  •  ${snapshot.rubricTitle}",
            MARGIN + 16, 67f, 10f, muted, regular, 710f
        )
        writeText(canvas, date, RIGHT - 105f, 45f, 10f, muted, regular, 100f)
        stroke(canvas, MARGIN, 517f, RIGHT, 517f)
        writeText(canvas, "Ders: $course", MARGIN, 536f, 10f, navy, bold, 455f)
        writeText(canvas, "Öğretmen: $teacher", MARGIN, 552f, 10f, navy, regular, 475f)
        writeText(canvas, "İmza", 639f, 534f, 10f, navy, bold)
        stroke(canvas, 672f, 551f, 797f, 551f)
        writeText(canvas, "Sayfa $page", RIGHT - 70f, 573f, 9f, muted)
        writeText(canvas, "Puanla • Yerel değerlendirme çıktısı", MARGIN, 573f, 9f, muted)
    }

    private fun drawSummary(canvas: Canvas, snapshot: PdfReportSnapshot, s: PdfReportSummary) {
        val metrics = listOf(
            "Kayıtlı Öğrenci" to "${s.registered}",
            "Tamamlanan" to "${s.completed}",
            "Sınıf Ortalaması" to (s.average?.let { String.format(Locale.forLanguageTag("tr-TR"), "%.1f", it) } ?: "—"),
            "Tamamlama" to (if (s.registered > 0) "${s.completed * 100 / s.registered}%" else "—")
        )
        val cardWidth = 187f
        metrics.forEachIndexed { i, (label, value) ->
            val x = MARGIN + i * 199f
            rounded(canvas, x, 106f, x + cardWidth, 173f, pale)
            writeText(canvas, label, x + 12, 126f, 10f, muted, regular, cardWidth - 24)
            writeText(canvas, value, x + 12, 158f, 23f, navy, bold, cardWidth - 24)
        }

        writeText(canvas, "Puan Dağılımı", MARGIN, 209f, 14f, navy, bold)
        writeText(canvas, "Yalnız kesinleşmiş puanlar", MARGIN, 226f, 9f, muted)
        val maxValue = s.gradeDistribution.maxOrNull()?.coerceAtLeast(1) ?: 1
        val bands = listOf("0–49", "50–69", "70–84", "85–100")
        s.gradeDistribution.forEachIndexed { i, count ->
            val y = 246f + i * 40f
            writeText(canvas, bands[i], MARGIN, y + 11f, 11f, navy)
            rounded(canvas, MARGIN + 78, y, MARGIN + 520, y + 15f, Color.rgb(236, 239, 242))
            if (count > 0) rounded(
                canvas, MARGIN + 78, y,
                MARGIN + 78 + 442f * count / maxValue, y + 15f, accent
            )
            writeText(canvas, "$count öğrenci", MARGIN + 535, y + 12f, 10f, navy)
        }

        // Completion ring mirrors the dashboard while remaining ink-friendly.
        val ringBounds = RectF(651f, 252f, 771f, 372f)
        val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 14f
            strokeCap = Paint.Cap.ROUND
            color = line
        }
        canvas.drawArc(ringBounds, -90f, 360f, false, ringPaint)
        val completedRate = if (s.registered == 0) 0f else s.completed.toFloat() / s.registered
        if (completedRate > 0f) {
            ringPaint.color = accent
            canvas.drawArc(ringBounds, -90f, completedRate * 360f, false, ringPaint)
        }
        writeText(canvas, "${(completedRate * 100).toInt()}%", 688f, 316f, 19f, navy, bold, 75f)
        writeText(canvas, "tamamlandı", 681f, 334f, 9f, muted, regular, 91f)
        writeText(canvas, "Kısmi: ${s.partial}  •  Boş: ${s.notStarted}", 645f, 402f, 10f, muted, regular, 163f)

        rounded(canvas, MARGIN, 422f, RIGHT, 498f, pale)
        writeText(canvas, "Değerlendirme Özeti", MARGIN + 12, 444f, 12f, navy, bold)
        writeText(
            canvas,
            "Eksik: ${s.partial}   •   Başlanmadı: ${s.notStarted}   •   " +
                "En yüksek: ${s.highest ?: "—"}   •   En düşük: ${s.lowest ?: "—"}",
            MARGIN + 12, 464f, 11f, navy, regular, 735f
        )
        writeText(
            canvas,
            "Ölçütler sonraki sayfalarda açıklanır. “—” puanlanmamış alanı gösterir; 0 gerçek bir puandır.",
            MARGIN + 12, 484f, 9f, muted, regular, 735f
        )
    }

    private fun drawTable(
        canvas: Canvas,
        snapshot: PdfReportSnapshot,
        group: List<IndexedValue<CsvCriterion>>,
        students: List<CsvStudent>
    ) {
        val includesGroup = snapshot.students.any { it.groupTitle != null }
        val nameWidth = if (includesGroup) 135f else 180f
        val groupColumn = if (includesGroup) listOf(110f) else emptyList()
        val fixed = 68f + nameWidth + groupColumn.sum() + 58f + 80f
        val widths = listOf(68f, nameWidth) + groupColumn +
            List(group.size) { (RIGHT - MARGIN - fixed) / group.size } +
            listOf(58f, 80f)
        val positions = mutableListOf(MARGIN)
        widths.forEach { positions.add(positions.last() + it) }
        rounded(canvas, MARGIN, 133f, RIGHT, 160f, pale)
        val headers = listOf("Okul No", "Adı Soyadı") +
            (if (includesGroup) listOf("Grup") else emptyList()) +
            group.map { "Ö${it.index + 1} (${it.value.maxPoints})" } +
            listOf("Toplam", "Durum")
        headers.forEachIndexed { i, title ->
            val x = positions[i]
            writeText(
                canvas, title, x + 4f, 151f, 9f, navy, bold, widths[i] - 8f
            )
        }
        students.forEachIndexed { rowIndex, student ->
            val y = 161f + rowIndex * ROW_HEIGHT
            if (rowIndex % 2 == 0) rounded(canvas, MARGIN, y, RIGHT, y + ROW_HEIGHT, Color.rgb(249, 250, 251))
            val total = PdfReportStatistics.definitiveScore(student, snapshot.criteria)
            val state = PdfReportStatistics.state(student, snapshot.criteria)
            val cells = listOf(student.number, "${student.firstName} ${student.lastName}") +
                (if (includesGroup) listOf(student.groupTitle ?: "—") else emptyList()) +
                group.map { (_, criterion) ->
                    student.scores[criterion.id]?.takeIf { it in 0..criterion.maxPoints }?.toString() ?: "—"
                } + listOf(
                    total?.toString() ?: "—",
                    when (state) {
                        PdfScoreState.COMPLETED -> "Tamam"
                        PdfScoreState.PARTIAL -> "Kısmi"
                        PdfScoreState.NOT_STARTED -> "Boş"
                    }
                )
            cells.forEachIndexed { i, value ->
                val color = if (i == cells.lastIndex && state != PdfScoreState.COMPLETED) warning else navy
                writeText(canvas, value, positions[i] + 4f, y + 14f, 9f, color, regular, widths[i] - 8f)
            }
            stroke(canvas, MARGIN, y + ROW_HEIGHT, RIGHT, y + ROW_HEIGHT)
        }
        if (students.isEmpty()) {
            writeText(canvas, "Bu sınıfta kayıtlı öğrenci bulunmuyor.", MARGIN + 8, 186f, 11f, muted)
        }
        writeText(
            canvas,
            "Toplam yalnızca tüm ölçütleri tamamlanmış ve doğrulanmış değerlendirmelerde gösterilir.",
            MARGIN, 501f, 9f, muted, regular, 730f
        )
    }

    private fun wrap(value: String, size: Float, maxWidth: Float): List<String> {
        val paint = textPaint(size, navy)
        val lines = mutableListOf<String>()
        var line = ""
        for (word in value.split(Regex("\\s+"))) {
            val next = if (line.isEmpty()) word else "$line $word"
            if (paint.measureText(next) > maxWidth && line.isNotEmpty()) {
                lines.add(line)
                line = word
            } else line = next
        }
        if (line.isNotEmpty()) lines.add(line)
        return lines.ifEmpty { listOf("") }
    }

    private fun writeText(
        canvas: Canvas,
        value: String,
        x: Float,
        y: Float,
        size: Float,
        color: Int,
        typeface: Typeface = regular,
        maxWidth: Float = RIGHT - x
    ) {
        val paint = textPaint(size, color, typeface)
        var shortened = value
        if (paint.measureText(shortened) > maxWidth) {
            while (shortened.isNotEmpty() && paint.measureText("$shortened…") > maxWidth) {
                shortened = shortened.dropLast(1)
            }
            shortened += "…"
        }
        canvas.drawText(shortened, x, y, paint)
    }

    private fun textPaint(size: Float, color: Int, font: Typeface = regular) =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            textSize = size
            typeface = font
        }

    private fun rounded(canvas: Canvas, left: Float, top: Float, right: Float, bottom: Float, color: Int) {
        canvas.drawRoundRect(
            left, top, right, bottom, 6f, 6f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
        )
    }

    private fun stroke(canvas: Canvas, left: Float, top: Float, right: Float, bottom: Float) {
        canvas.drawLine(
            left, top, right, bottom,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = line; strokeWidth = 0.8f }
        )
    }
}
