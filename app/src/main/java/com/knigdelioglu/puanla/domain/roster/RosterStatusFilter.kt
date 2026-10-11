package com.knigdelioglu.puanla.domain.roster

/** Assessment-filter decisions are pure and never turn an unscored pupil into a zero grade. */
object RosterStatusFilter {
    fun matches(status: String, isCompleted: Boolean, scoredCount: Int): Boolean =
        when (status) {
            "Tamamlanan" -> isCompleted
            "Kısmi" -> !isCompleted && scoredCount > 0
            "Puanlanmadı" -> !isCompleted && scoredCount <= 0
            else -> true
        }
}
