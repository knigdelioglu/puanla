package com.knigdelioglu.puanla.domain.group

import kotlin.math.roundToInt

/** Balanced random assignment; no student is inserted twice. */
object GroupPartitioner {
    fun distribute(
        studentIds: List<String>,
        averageSize: Int,
        shuffle: (List<String>) -> List<String> = { it.shuffled() }
    ): List<List<String>> {
        require(averageSize > 0) { "Ortalama grup büyüklüğü pozitif olmalı." }
        require(studentIds.size == studentIds.distinct().size) { "Yinelenen öğrenci var." }
        if (studentIds.isEmpty()) return emptyList()
        val shuffled = shuffle(studentIds)
        require(shuffled.size == studentIds.size && shuffled.toSet() == studentIds.toSet()) {
            "Dağıtım öğrenci listesini değiştiremez."
        }
        val count = (studentIds.size.toDouble() / averageSize).roundToInt()
            .coerceIn(1, studentIds.size)
        val baseSize = studentIds.size / count
        val extra = studentIds.size % count
        var cursor = 0
        return List(count) { index ->
            val size = baseSize + if (index < extra) 1 else 0
            shuffled.subList(cursor, cursor + size).toList().also { cursor += size }
        }
    }
}
