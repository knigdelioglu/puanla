package com.knigdelioglu.puanla.domain

import com.knigdelioglu.puanla.data.local.BackupSnapshot
import com.knigdelioglu.puanla.data.local.ClassroomEntity
import com.knigdelioglu.puanla.data.local.StudentEntity
import com.knigdelioglu.puanla.domain.backup.BackupImport
import java.io.ByteArrayInputStream
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class BackupImportTest {
    private fun json(): String {
        val classroom = ClassroomEntity("c", 11, "A", "11-A", "2026-2027")
        val student = StudentEntity("s", "c", "01", "Ayşe", "Kaya")
        return Json { encodeDefaults = true; explicitNulls = true }.encodeToString(
            BackupSnapshot(
                exportedAt = 123456L,
                classrooms = listOf(classroom), students = listOf(student),
                rubrics = emptyList(), criteria = emptyList(), levels = emptyList(),
                assessments = emptyList(), scores = emptyList(), groups = emptyList(), auditLogs = emptyList()
            )
        )
    }

    @Test fun previewIsStrictAndNeverWrites() {
        val preview = BackupImport.inspect(json())
        assertEquals(1, preview.classrooms)
        assertEquals(1, preview.students)
        assertEquals(0, preview.scores)
        assertEquals(123456L, preview.exportedAt)
        assertThrows(Exception::class.java) {
            BackupImport.inspect("""{"version":1,"students":[]}""")
        }
    }

    @Test fun fileSizeLimitAppliesToStreamNotStringLength() {
        val input = ByteArrayInputStream("12345678".toByteArray())
        assertEquals("12345678", BackupImport.readLimited(input, 8))
        assertThrows(IllegalArgumentException::class.java) {
            BackupImport.readLimited(ByteArrayInputStream("123456789".toByteArray()), 8)
        }
    }

    @Test fun invalidReferencesNeverProduceApprovalPreview() {
        val damaged = json().replace(""""classroomId":"c"""", """"classroomId":"absent"""")
        assertThrows(IllegalArgumentException::class.java) { BackupImport.inspect(damaged) }
    }
}
