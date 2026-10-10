package com.knigdelioglu.puanla.domain

/** null = puanlanmadı; 0 = verilmiş gerçek sıfır puan. */
data class CriterionScore(val maxPoints: Int, val points: Int?) {
    init {
        require(maxPoints >= 0) { "Azami puan negatif olamaz." }
        require(points == null || points in 0..maxPoints) {
            "Puan ölçütün izin verilen aralığında olmalıdır."
        }
    }
}

data class ScoreSummary(val completed: Boolean, val definitiveTotal: Int?, val scoredCount: Int)

/** Tamamlanmamış değerlendirmeye kesin toplam vermez. */
fun summarizeScores(criteria: List<CriterionScore>): ScoreSummary {
    val complete = criteria.isNotEmpty() && criteria.all { it.points != null }
    return ScoreSummary(
        completed = complete,
        definitiveTotal = if (complete) criteria.sumOf { requireNotNull(it.points) } else null,
        scoredCount = criteria.count { it.points != null }
    )
}
