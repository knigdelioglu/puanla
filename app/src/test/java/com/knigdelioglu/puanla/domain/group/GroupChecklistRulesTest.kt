package com.knigdelioglu.puanla.domain.group

import org.junit.Assert.*
import org.junit.Test

class GroupChecklistRulesTest {
    private val source = """[{"title":"Hazırlık","checked":false},{"title":"Prova","checked":false}]"""

    @Test fun consecutiveTogglesReadTheLatestJson() {
        val afterFirst = GroupChecklistRules.toggle(source, 0)
        val afterSecond = GroupChecklistRules.toggle(afterFirst, 1)
        assertTrue(afterSecond.contains("\"title\":\"Hazırlık\",\"checked\":true"))
        assertTrue(afterSecond.contains("\"title\":\"Prova\",\"checked\":true"))
        assertEquals(source, GroupChecklistRules.toggle(afterFirst, 0))
    }

    @Test fun invalidIndexAndMalformedFlagRejected() {
        assertThrows(IllegalArgumentException::class.java) { GroupChecklistRules.toggle(source, 2) }
        assertThrows(IllegalArgumentException::class.java) {
            GroupChecklistRules.toggle("""[{"checked":"yes"}]""", 0)
        }
    }
}
