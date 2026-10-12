package com.knigdelioglu.puanla.domain.scoring

/**
 * Prefer approved rubric levels if the rubric supplies a manageable number of choices.
 * Otherwise offer five explicit NUMERIC shortcuts, not invented qualitative levels.
 * The precise editor remains available for every intermediate integer.
 */
fun quickScorePresets(maxPoints: Int, approvedLevelPoints: List<Int> = emptyList()): List<Int> {
    require(maxPoints >= 0) { "Azami puan negatif olamaz." }

    val approved = approvedLevelPoints.filter { it in 0..maxPoints }.distinct().sorted()
    if (approved.isNotEmpty() && approved.size <= 5) return approved

    return (0..4)
        .map { step -> ((maxPoints.toLong() * step + 2L) / 4L).toInt() }
        .distinct()
}
