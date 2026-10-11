package com.knigdelioglu.puanla.domain.report

import com.knigdelioglu.puanla.data.local.CriterionEntity

/** Rows for two students scored against the same rubric, preserving unscored vs zero. */
fun studentComparisonRows(
    criteria: List<CriterionEntity>,
    firstScores: Map<String, Int?>,
    secondScores: Map<String, Int?>
): List<List<String>> = criteria.sortedBy { it.orderIndex }.map { criterion ->
    listOf(
        criterion.title,
        firstScores[criterion.id]?.let { "$it / ${criterion.maxPoints}" } ?: "Puanlanmadı",
        secondScores[criterion.id]?.let { "$it / ${criterion.maxPoints}" } ?: "Puanlanmadı"
    )
}
