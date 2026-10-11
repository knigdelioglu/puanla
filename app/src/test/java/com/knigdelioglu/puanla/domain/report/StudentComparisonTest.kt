package com.knigdelioglu.puanla.domain.report

import com.knigdelioglu.puanla.data.local.CriterionEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class StudentComparisonTest {
    @Test
    fun `same rubric criteria are ordered and unscored stays distinct from zero`() {
        val criteria = listOf(
            CriterionEntity("second", "rubric", "İkinci", "", 5, 1),
            CriterionEntity("first", "rubric", "İlk", "", 10, 0)
        )

        val rows = studentComparisonRows(
            criteria,
            firstScores = mapOf("first" to 0, "second" to 4),
            secondScores = mapOf("first" to null)
        )

        assertEquals(
            listOf(listOf("İlk", "0 / 10", "Puanlanmadı"), listOf("İkinci", "4 / 5", "Puanlanmadı")),
            rows
        )
    }
}
