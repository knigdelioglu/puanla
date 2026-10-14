package com.knigdelioglu.puanla.ui.preferences

/** Presentation choices are not rubric criteria and never alter source JSON. */
enum class QuickGradeAppearance(val label: String) {
    Text("Yazılı düzeyler"),
    Stars("Yıldızlar ★"),
    Faces("Yüz ifadeleri 🙂");

    companion object {
        fun fromStored(value: String?): QuickGradeAppearance =
            entries.firstOrNull { it.name == value } ?: Text
    }
}

data class QuickGradingPreference(
    val qualitativeEnabled: Boolean = false,
    val levelCount: Int = 4,
    val appearance: QuickGradeAppearance = QuickGradeAppearance.Text
) {
    init { require(levelCount == 3 || levelCount == 4) }
}

data class QuickGradeOption(
    val label: String,
    val icon: String,
    val points: Int
) {
    fun title(appearance: QuickGradeAppearance): String =
        if (appearance == QuickGradeAppearance.Text) label else "$icon  $label"
}

/**
 * App-defined shortcuts, not officially sourced rubric level definitions.
 * Each tap records a REAL integer score, never a synthetic level identifier.
 * Lowest level = 0, highest = criterion maximum, middle choices evenly spaced.
 */
fun qualitativeQuickOptions(
    maxPoints: Int,
    levelCount: Int,
    appearance: QuickGradeAppearance
): List<QuickGradeOption> {
    require(maxPoints >= 0)
    require(levelCount == 3 || levelCount == 4)

    val labels = if (levelCount == 3) {
        listOf("Geliştirilmeli", "Yeterli", "İyi")
    } else {
        listOf("Yetersiz", "Geliştirilmeli", "Yeterli", "İyi")
    }
    val faces = if (levelCount == 3) {
        listOf("🙁", "🙂", "😄")
    } else {
        listOf("😞", "🙁", "🙂", "😄")
    }
    // A persisted score is only an integer. Offering two cards with the same
    // integer makes the selected card ambiguous, so show at most one card per
    // possible score and retain the endpoints when fewer choices fit.
    val choiceCount = (maxPoints.toLong() + 1L).coerceAtMost(levelCount.toLong()).toInt()
    if (choiceCount == 1) {
        return listOf(QuickGradeOption("Tek düzey", if (appearance == QuickGradeAppearance.Text) "" else "★", 0))
    }
    val steps = choiceCount - 1
    return (0 until choiceCount).map { index ->
        val sourceIndex = (index * (levelCount - 1) + steps / 2) / steps
        val label = labels[sourceIndex]
        val rounded = (maxPoints.toLong() * index * 2L + steps) / (steps * 2L)
        val icon = when (appearance) {
            QuickGradeAppearance.Text -> ""
            QuickGradeAppearance.Stars -> "★".repeat(sourceIndex + 1)
            QuickGradeAppearance.Faces -> faces[sourceIndex]
        }
        QuickGradeOption(label = label, icon = icon, points = rounded.toInt())
    }
}
