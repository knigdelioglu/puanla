package com.knigdelioglu.puanla.domain.student

import java.util.Locale

/** Ensures editing a roster cannot introduce names that invalidate full backups. */
object StudentNameRules {
    private val turkish = Locale.forLanguageTag("tr-TR")
    private val repeatedSpaces = Regex(" {2,}")
    fun firstName(raw: String): String = normalized(raw)
    fun lastName(raw: String): String = normalized(raw).uppercase(turkish)

    private fun normalized(raw: String): String {
        require(raw.none { Character.isISOControl(it) }) { "Ad veya soyad kontrol karakteri içeremez." }
        val clean = raw.trim().replace(repeatedSpaces, " ")
        require(clean.isNotEmpty() && clean.length <= 100) { "Ad veya soyad boş olamaz ve 100 karakteri aşamaz." }
        require(clean.none { it.isDigit() }) { "Ad veya soyad rakam içeremez." }
        return clean
    }
}
