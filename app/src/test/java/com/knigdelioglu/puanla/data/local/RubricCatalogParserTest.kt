package com.knigdelioglu.puanla.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class RubricCatalogParserTest {
    @Test
    fun acceptsACompleteGradeElevenRubricAndMapsItsCriteriaWithoutLevels() {
        val catalog = RubricCatalogParser.parse(
            """{"schemaVersion":1,"sourceFile":"source.xlsx","rubrics":[{"id":"r11","title":"Görev","description":"","grade":11,"targetTask":"Görev","criteria":[{"id":"c1","title":"Ölçüt","description":"","maxPoints":100,"orderIndex":0}]}]}"""
        )

        val rubric = catalog.rubrics.single()
        assertEquals(11, rubric.toEntity().grade)
        assertEquals(100, rubric.toCriterionEntities().single().maxPoints)
        assertEquals("c1", rubric.toCriterionEntities().single().id)
        assertEquals(null, rubric.groupPreparationChecklist)
    }

    @Test
    fun rejectsRubricsOutsideGradeEleven() {
        val json = """{"schemaVersion":1,"sourceFile":"source.xlsx","rubrics":[{"id":"r","title":"Görev","description":"","grade":10,"targetTask":"Görev","criteria":[{"id":"c","title":"Ölçüt","description":"","maxPoints":100,"orderIndex":0}]}]}"""
        assertThrows(IllegalArgumentException::class.java) { RubricCatalogParser.parse(json) }
    }

    @Test
    fun rejectsCriteriaThatDoNotAddUpToOneHundred() {
        val json = """{"schemaVersion":1,"sourceFile":"source.xlsx","rubrics":[{"id":"r","title":"Görev","description":"","grade":11,"targetTask":"Görev","criteria":[{"id":"c","title":"Ölçüt","description":"","maxPoints":99,"orderIndex":0}]}]}"""
        assertThrows(IllegalArgumentException::class.java) { RubricCatalogParser.parse(json) }
    }
}
