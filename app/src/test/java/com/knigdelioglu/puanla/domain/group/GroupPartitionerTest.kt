package com.knigdelioglu.puanla.domain.group

import org.junit.Assert.*
import org.junit.Test

class GroupPartitionerTest {
    @Test fun makesEvenGroupsAndKeepsEveryStudentExactlyOnce() {
        val ids = (1..32).map { it.toString() }
        val groups = GroupPartitioner.distribute(ids, 5) { it }
        assertEquals(6, groups.size)
        assertEquals(ids, groups.flatten())
        assertEquals(1, groups.maxOf { it.size } - groups.minOf { it.size })
    }

    @Test fun oneGroupWhenFewerStudentsThanTarget() {
        assertEquals(listOf(listOf("a", "b")), GroupPartitioner.distribute(listOf("a", "b"), 5) { it })
        assertTrue(GroupPartitioner.distribute(emptyList(), 4).isEmpty())
    }

    @Test fun rejectsInvalidSizeAndDuplicatedStudents() {
        assertThrows(IllegalArgumentException::class.java) {
            GroupPartitioner.distribute(listOf("a"), 0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            GroupPartitioner.distribute(listOf("a", "a"), 2)
        }
    }
}
