package com.knigdelioglu.puanla.data.local

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

@Serializable
data class RubricCatalog(
    val schemaVersion: Int,
    val sourceFile: String,
    val rubrics: List<RubricDefinition>
)

@Serializable
data class RubricDefinition(
    val id: String,
    val title: String,
    val description: String,
    val grade: Int,
    val targetTask: String,
    val criteria: List<CriterionDefinition>,
    val groupPreparationChecklist: GroupPreparationChecklist? = null
)

@Serializable
data class CriterionDefinition(
    val id: String,
    val title: String,
    val description: String,
    val maxPoints: Int,
    val orderIndex: Int
)

@Serializable
data class GroupPreparationChecklist(
    val description: String,
    val items: List<String>
)

object RubricCatalogParser {
    private val json = Json { ignoreUnknownKeys = false }

    fun parse(source: String): RubricCatalog {
        val catalog = json.decodeFromString<RubricCatalog>(source)
        require(catalog.schemaVersion == 1) { "Desteklenmeyen rubrik kataloğu sürümü." }
        require(catalog.sourceFile.isNotBlank()) { "Rubrik kaynağı belirtilmelidir." }
        require(catalog.rubrics.isNotEmpty()) { "Rubrik kataloğu boş." }
        require(catalog.rubrics.map { it.id }.distinct().size == catalog.rubrics.size) {
            "Rubrik kimlikleri yineleniyor."
        }
        require(catalog.rubrics.all { it.grade == 11 }) { "Bu kaynak yalnızca 11. sınıf rubriklerini içermelidir." }
        val criterionIds = catalog.rubrics.flatMap { rubric -> rubric.criteria.map { it.id } }
        require(criterionIds.distinct().size == criterionIds.size) { "Ölçüt kimlikleri katalog genelinde yineleniyor." }

        catalog.rubrics.forEach { rubric ->
            require(rubric.id.isNotBlank() && rubric.title.isNotBlank() && rubric.targetTask.isNotBlank()) {
                "Rubrik kimliği, başlığı ve görev adı boş olamaz."
            }
            require(rubric.criteria.isNotEmpty()) { "Rubrik ölçütsüz olamaz: ${rubric.id}" }
            require(rubric.criteria.map { it.orderIndex }.sorted() == rubric.criteria.indices.toList()) {
                "Ölçüt sırası geçersiz: ${rubric.id}"
            }
            require(rubric.criteria.all { it.id.isNotBlank() && it.title.isNotBlank() && it.maxPoints > 0 }) {
                "Ölçüt kimliği, başlığı veya puanı geçersiz: ${rubric.id}"
            }
            require(rubric.criteria.sumOf { it.maxPoints } == 100) {
                "Rubrik ölçütleri 100 puana toplamlanmıyor: ${rubric.id}"
            }
            rubric.groupPreparationChecklist?.let { checklist ->
                require(checklist.items.isNotEmpty() && checklist.items.all(String::isNotBlank)) {
                    "Grup hazırlık listesi geçersiz: ${rubric.id}"
                }
            }
        }

        return catalog
    }
}

fun RubricDefinition.toEntity(): RubricEntity = RubricEntity(
    id = id,
    title = title,
    description = description,
    grade = grade,
    targetTask = targetTask,
    isBuiltIn = true
)

fun RubricDefinition.toCriterionEntities(): List<CriterionEntity> = criteria.map { criterion ->
    CriterionEntity(
        id = criterion.id,
        rubricId = id,
        title = criterion.title,
        description = criterion.description,
        maxPoints = criterion.maxPoints,
        orderIndex = criterion.orderIndex
    )
}
