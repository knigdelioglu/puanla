package com.knigdelioglu.puanla.domain.student

import org.junit.Assert.*
import org.junit.Test

class StudentNameRulesTest {
    @Test fun invalidEditsNeverProduceBlankOrMalformedNames() {
        assertThrows(IllegalArgumentException::class.java) { StudentNameRules.firstName("   ") }
        assertThrows(IllegalArgumentException::class.java) { StudentNameRules.lastName("") }
        assertThrows(IllegalArgumentException::class.java) { StudentNameRules.firstName("Ayşe\nKaya") }
        assertThrows(IllegalArgumentException::class.java) { StudentNameRules.lastName("Çelik 2") }
        assertThrows(IllegalArgumentException::class.java) { StudentNameRules.firstName("a".repeat(101)) }
    }
    @Test fun normalizesMultinamesAndTurkishSurname() {
        assertEquals("Ayşe Nur", StudentNameRules.firstName("  Ayşe  Nur  "))
        assertEquals("IŞIK", StudentNameRules.lastName("ışık"))
        assertEquals("İNCİ", StudentNameRules.lastName("inci"))
    }
}
