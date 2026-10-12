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
    val steps = levelCount - 1
    return labels.mapIndexed { index, label ->
        val rounded = (maxPoints.toLong() * index * 2L + steps) / (steps * 2L)
        val icon = when (appearance) {
            QuickGradeAppearance.Text -> ""
            QuickGradeAppearance.Stars -> "★".repeat(index + 1)
            QuickGradeAppearance.Faces -> faces[index]
        }
        QuickGradeOption(label = label, icon = icon, points = rounded.toInt())
    }
}
