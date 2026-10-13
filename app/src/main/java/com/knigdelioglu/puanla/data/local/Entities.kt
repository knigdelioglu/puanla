package com.knigdelioglu.puanla.data.local

import kotlinx.serialization.Serializable
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "classrooms",
    indices = [Index(value = ["grade", "section"], unique = true)]
)
@Serializable
data class ClassroomEntity(
    @PrimaryKey val id: String,
    val grade: Int, // 9..12
    val section: String, // "A", "B", "C"...
    val name: String, // "11-A"
    val academicYear: String, // "2026-2027"
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "students",
    foreignKeys = [
        ForeignKey(
            entity = ClassroomEntity::class,
            parentColumns = ["id"],
            childColumns = ["classroomId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["classroomId"]),
        Index(value = ["classroomId", "studentNumber"], unique = true)
    ]
)
@Serializable
data class StudentEntity(
    @PrimaryKey val id: String,
    val classroomId: String,
    val studentNumber: String,
    val firstName: String,
    val lastName: String,
    val gender: String? = null,
    val boardingStatus: String? = null,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

val StudentEntity.fullName: String
    get() = "$firstName $lastName"

val StudentEntity.initials: String
    get() = "${firstName.firstOrNull() ?: ""}${lastName.firstOrNull() ?: ""}".uppercase()



@Entity(tableName = "rubrics")
@Serializable
data class RubricEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val grade: Int, // 9..12
    val targetTask: String,
    val isBuiltIn: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "criteria",
    foreignKeys = [
        ForeignKey(
            entity = RubricEntity::class,
            parentColumns = ["id"],
            childColumns = ["rubricId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["rubricId"])]
)
@Serializable
data class CriterionEntity(
    @PrimaryKey val id: String,
    val rubricId: String,
    val title: String,
    val description: String,
    val maxPoints: Int,
    val orderIndex: Int
)

@Entity(
    tableName = "criterion_levels",
    foreignKeys = [
        ForeignKey(
            entity = CriterionEntity::class,
            parentColumns = ["id"],
            childColumns = ["criterionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["criterionId"])]
)
@Serializable
data class CriterionLevelEntity(
    @PrimaryKey val id: String,
    val criterionId: String,
    val points: Int,
    val description: String,
    val orderIndex: Int
)

/** One evaluation may be assigned to multiple classrooms. */
@Entity(tableName = "assessment_runs", indices = [Index(value = ["rubricId"])])
@Serializable
data class AssessmentRunEntity(
    @PrimaryKey val id: String,
    val rubricId: String,
    val title: String,
    val criteriaSnapshotJson: String = "",
    val status: String = "ACTIVE",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "run_classrooms",
    indices = [
        Index(value = ["runId", "classroomId"], unique = true),
        Index(value = ["classroomId"])
    ],
    foreignKeys = [
        ForeignKey(entity = AssessmentRunEntity::class, parentColumns = ["id"], childColumns = ["runId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = ClassroomEntity::class, parentColumns = ["id"], childColumns = ["classroomId"], onDelete = ForeignKey.CASCADE)
    ]
)
@Serializable
data class RunClassroomEntity(
    @PrimaryKey val id: String,
    val runId: String,
    val classroomId: String
)

@Entity(
    tableName = "assessments",
    foreignKeys = [
        ForeignKey(
            entity = ClassroomEntity::class,
            parentColumns = ["id"],
            childColumns = ["classroomId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = StudentEntity::class,
            parentColumns = ["id"],
            childColumns = ["studentId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = RubricEntity::class,
            parentColumns = ["id"],
            childColumns = ["rubricId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["classroomId", "rubricId"]),
        Index(value = ["studentId", "runId"], unique = true),
        Index(value = ["runId"]),
        Index(value = ["rubricId"])
    ]

)
@Serializable
data class AssessmentEntity(
    @PrimaryKey val id: String,
    val classroomId: String,
    val studentId: String,
    val rubricId: String,
    val runId: String = "",
    val isCompleted: Boolean = false,
    val definitiveTotal: Int? = null,
    val scoredCount: Int = 0,
    val observationNote: String? = null,
    val lastModifiedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "criterion_scores",
    foreignKeys = [
        ForeignKey(
            entity = AssessmentEntity::class,
            parentColumns = ["id"],
            childColumns = ["assessmentId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CriterionEntity::class,
            parentColumns = ["id"],
            childColumns = ["criterionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["assessmentId", "criterionId"], unique = true),
        Index(value = ["criterionId"])
    ]
)
@Serializable
data class CriterionScoreEntity(
    @PrimaryKey val id: String,
    val assessmentId: String,
    val criterionId: String,
    val points: Int?, // null = unscored, 0..maxPoints
    val evidenceNote: String? = null,
    val scoredAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "group_tasks")
@Serializable
data class GroupTaskEntity(
    @PrimaryKey val id: String,
    val classroomId: String,
    val rubricId: String,
    val title: String,
    val memberStudentIdsJson: String, // JSON array of student IDs
    val checklistJson: String, // JSON array of preparation items {"title": "...", "checked": true}
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "audit_logs")
@Serializable
data class AuditLogEntity(
    @PrimaryKey val id: String,
    val timestamp: Long = System.currentTimeMillis(),
    val action: String,
    val details: String
)


/** Rubric-independent groups stay in legacy group_tasks; assessment snapshots live here. */
@Entity(tableName = "group_assessment_sessions", indices = [Index(value = ["classroomId", "rubricId"])])
@Serializable
data class GroupAssessmentSessionEntity(
    @PrimaryKey val id: String, val classroomId: String, val rubricId: String,
    val title: String, val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "group_assessments", indices = [
    Index(value = ["sessionId", "groupId"], unique = true), Index(value = ["sessionId"])
])
@Serializable
data class GroupAssessmentEntity(
    @PrimaryKey val id: String, val sessionId: String, val groupId: String,
    val groupTitleSnapshot: String, val memberStudentIdsJson: String,
    val isCompleted: Boolean = false, val definitiveTotal: Int? = null,
    val scoredCount: Int = 0, val lastModifiedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "group_criterion_scores", indices = [
    Index(value = ["groupAssessmentId", "criterionId"], unique = true)
])
@Serializable
data class GroupCriterionScoreEntity(
    @PrimaryKey val id: String, val groupAssessmentId: String, val criterionId: String,
    val points: Int?, val evidenceNote: String? = null, val scoredAt: Long = System.currentTimeMillis()
)

/** Materialized grade for a student; group_assessments remains authoritative. */
@Entity(tableName = "group_member_results", indices = [
    Index(value = ["groupAssessmentId", "studentId"], unique = true), Index(value = ["studentId"])
])
@Serializable
data class GroupMemberResultEntity(
    @PrimaryKey val id: String, val groupAssessmentId: String, val studentId: String,
    val definitiveTotal: Int? = null, val isCompleted: Boolean = false
)
