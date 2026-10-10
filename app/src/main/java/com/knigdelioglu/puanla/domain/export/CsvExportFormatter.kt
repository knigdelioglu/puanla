package com.knigdelioglu.puanla.domain.export

/** Immutable input of the production CSV writer; used by the repository and the tests. */
data class CsvCriterion(val id: String, val title: String, val maxPoints: Int)
data class CsvStudent(
    val number: String,
    val firstName: String,
    val lastName: String,
    val scores: Map<String, Int?> = emptyMap(),
    val definitiveTotal: Int? = null,
    val isCompleted: Boolean = false,
    val scoredCount: Int = 0
)

/**
 * Excel-compatible, semicolon-delimited UTF-8 CSV. Quotes, separators, line breaks
 * and leading spreadsheet formulas are escaped. NULL never becomes numeric zero.
 */
object CsvExportFormatter {
    private const val BOM = '\uFEFF'
    private const val SEPARATOR = ";"
    private const val NEWLINE = "\r\n"

    fun generate(criteria: List<CsvCriterion>, students: List<CsvStudent>): String = buildString {
        require(criteria.distinctBy { it.id }.size == criteria.size) { "Duplicate rubric criterion" }
        require(criteria.all { it.maxPoints >= 0 }) { "Invalid maximum points" }
        append(BOM)
        val maxPoints = criteria.sumOf { it.maxPoints }
        val headers = listOf("Okul No", "Adı", "Soyadı") +
            criteria.map { "${it.title} (Azami ${it.maxPoints})" } +
            listOf("Toplam Puan ($maxPoints)", "Değerlendirme Durumu")
        appendRow(headers)

        for (student in students) {
            // Defensive: even a corrupt stored total may never appear as a final grade
            // unless every criterion actually has an assigned, in-range score.
            val allScored = criteria.isNotEmpty() && criteria.all { criterion ->
                student.scores[criterion.id]?.let { it in 0..criterion.maxPoints } == true
            }
            val completed = student.isCompleted && allScored && student.definitiveTotal != null &&
                student.definitiveTotal == criteria.sumOf { student.scores[it.id] ?: 0 }
            val state = when {
                completed -> "Tamamlandı"
                student.scoredCount > 0 || student.scores.values.any { it != null } -> "Kısmi"
                else -> "Başlanmadı"
            }
            appendRow(
                listOf(student.number, student.firstName, student.lastName) +
                    criteria.map { student.scores[it.id]?.toString() ?: "Puanlanmadı" } +
                    listOf(if (completed) student.definitiveTotal.toString() else "Eksik", state)
            )
        }
    }

    private fun StringBuilder.appendRow(values: List<String>) {
        append(values.joinToString(SEPARATOR, transform = ::escapeField))
        append(NEWLINE)
    }

    /** Prevent cells starting with =, +, -, @ or tab from executing as formulas. */
    fun escapeField(value: String): String {
        val formula = value.trimStart().firstOrNull() in setOf('=', '+', '-', '@') ||
            value.startsWith("\t") || value.startsWith("\r") || value.startsWith("\n")
        val protected = if (formula) "'$value" else value
        return if (protected.any { it == ';' || it == '"' || it == '\r' || it == '\n' }) {
            "\"" + protected.replace("\"", "\"\"") + "\""
        } else protected
    }
}
