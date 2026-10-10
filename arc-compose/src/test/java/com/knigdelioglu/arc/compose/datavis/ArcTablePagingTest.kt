package com.knigdelioglu.arc.compose.datavis

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArcTablePagingTest {
    @Test fun sortEntireRosterBeforePaging() {
        val sortedStudents = (51 downTo 1).sorted()
        val first = pageSortedRows(sortedStudents, 25, 1)
        val second = pageSortedRows(sortedStudents, 25, 2)
        val third = pageSortedRows(sortedStudents, 25, 3)
        assertEquals((1..25).toList(), first.rows)
        assertEquals((26..50).toList(), second.rows)
        assertEquals(listOf(51), third.rows)
        assertEquals(3, third.pageCount)
    }

    @Test fun outOfBoundsPageClampsToAvailableRange() {
        val data = (1..26).toList()
        assertEquals(listOf(26), pageSortedRows(data, 25, 100).rows)
        assertEquals((1..25).toList(), pageSortedRows(data, 25, -2).rows)
    }

    @Test fun noRowsStillHasFirstPage() {
        val result = pageSortedRows(emptyList<Int>(), 25, 99)
        assertEquals(1, result.page)
        assertEquals(1, result.pageCount)
        assertTrue(result.rows.isEmpty())
    }

    @Test fun nullPageSizePreservesAllRows() {
        val all = (1..100).toList()
        assertEquals(all, pageSortedRows(all, null, 9).rows)
    }
}
