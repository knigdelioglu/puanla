package com.knigdelioglu.arc.compose.datavis

/** Pure paging logic shared by the production ArcSortableDataTable and unit tests. */
internal data class ArcTablePage<T>(
    val rows: List<T>,
    val page: Int,
    val pageCount: Int
)

/**
 * rows have ALREADY been sorted globally by the table before entering here.
 * A missing/invalid size preserves the old, unpaginated table behavior.
 */
internal fun <T> pageSortedRows(rows: List<T>, pageSize: Int?, requestedPage: Int): ArcTablePage<T> {
    if (pageSize == null || pageSize <= 0) return ArcTablePage(rows, 1, 1)
    val pageCount = (rows.size / pageSize + if (rows.size % pageSize > 0) 1 else 0).coerceAtLeast(1)
    val page = requestedPage.coerceIn(1, pageCount)
    val from = (page - 1) * pageSize
    return ArcTablePage(rows.drop(from).take(pageSize), page, pageCount)
}
