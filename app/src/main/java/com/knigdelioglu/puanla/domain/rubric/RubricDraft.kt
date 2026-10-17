package com.knigdelioglu.puanla.domain.rubric

data class RubricDraft(
    val title: String,
    val subject: String,
    val description: String,
    val grade: Int,
    val criteria: List<RubricCriterionDraft>
)

data class RubricCriterionDraft(
    val id: String? = null,
    val title: String,
    val category: String,
    val maxPoints: Int,
    val levels: List<RubricLevelDraft>
)

data class RubricLevelDraft(
    val points: Int,
    val description: String
)

object RubricDraftRules {
    const val TOTAL_POINTS = 100
    const val PERFORMANCE_LEVEL_COUNT = 4

    fun validate(draft: RubricDraft) {
        require(draft.title.isNotBlank()) { "Rubrik başlığı boş olamaz." }
        require(draft.subject.isNotBlank()) { "Ders / alan boş olamaz." }
        require(draft.grade in 9..12) { "Sınıf düzeyi 9 ile 12 arasında olmalıdır." }
        require(draft.criteria.isNotEmpty()) { "En az bir değerlendirme ölçütü ekleyin." }
        require(draft.criteria.mapNotNull { it.id }.distinct().size == draft.criteria.mapNotNull { it.id }.size) {
            "Ölçüt kimlikleri yinelenemez."
        }
        draft.criteria.forEachIndexed { index, criterion ->
            require(criterion.title.isNotBlank()) { "${index + 1}. ölçütün başlığı boş olamaz." }
            require(criterion.maxPoints > 0) { "${index + 1}. ölçüt için puan sıfırdan büyük olmalıdır." }
            require(criterion.levels.size == PERFORMANCE_LEVEL_COUNT) {
                "Her ölçüt için dört performans düzeyi tanımlayın."
            }
            require(criterion.levels.all { it.points in 0..criterion.maxPoints }) {
                "Performans düzeyi puanları ölçütün 0–${criterion.maxPoints} aralığında olmalıdır."
            }
            require(criterion.levels.map { it.points } == criterion.levels.map { it.points }.sorted()) {
                "Performans düzeyi puanları küçükten büyüğe sıralanmalıdır."
            }
            require(criterion.levels.all { it.description.isNotBlank() }) {
                "Dört performans düzeyinin açıklamasını da doldurun."
            }
        }
        require(draft.criteria.sumOf { it.maxPoints } == TOTAL_POINTS) {
            "Ölçüt puanları toplamı 100 olmalıdır."
        }
    }
}
