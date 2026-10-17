package com.knigdelioglu.puanla.domain.rubric

import org.junit.Assert.assertThrows
import org.junit.Test

class RubricDraftRulesTest {
    @Test
    fun acceptsACompleteHundredPointRubricWithFourOrderedLevels() {
        RubricDraftRules.validate(validDraft())
    }

    @Test
    fun rejectsPointTotalsThatDoNotEqualOneHundred() {
        val invalid = validDraft().copy(
            criteria = validDraft().criteria.map { criterion ->
                criterion.copy(
                    maxPoints = 90,
                    levels = criterion.levels.mapIndexed { index, level ->
                        level.copy(points = (index + 1) * 22 + if (index == 3) 2 else 0)
                    }
                )
            }
        )
        assertThrows(IllegalArgumentException::class.java) { RubricDraftRules.validate(invalid) }
    }

    @Test
    fun rejectsMissingLevelDescriptionsAndScoresOutsideCriterionBounds() {
        val criterion = validDraft().criteria.single()
        assertThrows(IllegalArgumentException::class.java) {
            RubricDraftRules.validate(validDraft().copy(criteria = listOf(criterion.copy(levels = criterion.levels.dropLast(1)))))
        }
        assertThrows(IllegalArgumentException::class.java) {
            RubricDraftRules.validate(validDraft().copy(criteria = listOf(
                criterion.copy(levels = criterion.levels.mapIndexed { index, level ->
                    if (index == 3) level.copy(points = 101) else level
                })
            )))
        }
    }

    private fun validDraft() = RubricDraft(
        title = "Sunum Rubriği",
        subject = "Türkçe",
        description = "Sunum performansını değerlendirir.",
        grade = 11,
        criteria = listOf(
            RubricCriterionDraft(
                title = "Hazırlık",
                category = "Süreç",
                maxPoints = 100,
                levels = listOf(
                    RubricLevelDraft(25, "Başlangıç"),
                    RubricLevelDraft(50, "Gelişiyor"),
                    RubricLevelDraft(75, "Yetkin"),
                    RubricLevelDraft(100, "Örnek")
                )
            )
        )
    )
}
