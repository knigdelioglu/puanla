package com.knigdelioglu.puanla.data.local

/**
 * The earlier prototype shipped eight fabricated rubrics and incorrectly called them
 * official sources. They must never be seeded into new installations or offered for grading.
 *
 * IDs are retained exclusively to quarantine older on-device records without deleting
 * students' existing scores. Import approved source JSON in a separate, validated workflow.
 */
internal object RubricSeeder {
    val unverifiedLegacyIds: Set<String> = setOf(
        "rubric_oral_presentation",
        "rubric_prepared_speaking",
        "rubric_interactive_speaking",
        "rubric_group_work",
        "rubric_reading_skill",
        "rubric_writing_skill",
        "rubric_argumentative_writing",
        "rubric_project_assessment"
    )
}
