package com.knigdelioglu.puanla.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

data class RubricWithCriteria(
    val rubric: RubricEntity,
    val criteria: List<CriterionEntity>,
    val levels: List<CriterionLevelEntity>
)

data class AssessmentWithScores(
    val assessment: AssessmentEntity,
    val student: StudentEntity,
    val scores: List<CriterionScoreEntity>
)

/** Read-only group-originated grades, separate from individual rubric scores. */
data class StudentGroupGrade(
    val studentId: String,
    val sessionId: String,
    val sessionTitle: String,
    val groupTitle: String,
    val definitiveTotal: Int?,
    val isCompleted: Boolean,
    val createdAt: Long
)

@Dao
interface ClassroomDao {
    @Query("SELECT * FROM classrooms ORDER BY grade ASC, section ASC")
    fun getAllClassroomsFlow(): Flow<List<ClassroomEntity>>

    @Query("SELECT * FROM classrooms ORDER BY grade ASC, section ASC")
    suspend fun getAllClassrooms(): List<ClassroomEntity>

    @Query("SELECT * FROM classrooms WHERE id = :id LIMIT 1")
    suspend fun getClassroomById(id: String): ClassroomEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertClassroom(classroom: ClassroomEntity)

    @Update
    suspend fun updateClassroom(classroom: ClassroomEntity)

    @Delete
    suspend fun deleteClassroom(classroom: ClassroomEntity)
}

@Dao
interface StudentDao {
    @Query("SELECT COUNT(*) FROM students WHERE classroomId = :classroomId")
    suspend fun countForClassroom(classroomId: String): Int
    @Query("SELECT * FROM students WHERE classroomId = :classroomId ORDER BY CAST(studentNumber AS INTEGER) ASC, lastName ASC")
    fun getStudentsForClassroomFlow(classroomId: String): Flow<List<StudentEntity>>

    @Query("SELECT * FROM students WHERE classroomId = :classroomId ORDER BY CAST(studentNumber AS INTEGER) ASC, lastName ASC")
    suspend fun getStudentsForClassroom(classroomId: String): List<StudentEntity>

    @Query("SELECT * FROM students WHERE id = :id LIMIT 1")
    suspend fun getStudentById(id: String): StudentEntity?

    @Query("SELECT * FROM students WHERE classroomId = :classroomId AND studentNumber = :number LIMIT 1")
    suspend fun getStudentByNumber(classroomId: String, number: String): StudentEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertStudent(student: StudentEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertStudents(students: List<StudentEntity>)

    @Update
    suspend fun updateStudent(student: StudentEntity)

    @Delete
    suspend fun deleteStudent(student: StudentEntity)
}

@Dao
interface RubricDao {
    @Query("SELECT * FROM rubrics ORDER BY title ASC")
    fun getAllRubricsFlow(): Flow<List<RubricEntity>>

    @Query("SELECT * FROM rubrics ORDER BY title ASC")
    suspend fun getAllRubrics(): List<RubricEntity>

    @Query("SELECT * FROM rubrics WHERE id = :id LIMIT 1")
    suspend fun getRubricById(id: String): RubricEntity?

    @Query("SELECT * FROM criteria WHERE id = :criterionId LIMIT 1")
    suspend fun getCriterionById(criterionId: String): CriterionEntity?

    @Query("SELECT * FROM criteria WHERE rubricId = :rubricId ORDER BY orderIndex ASC")
    suspend fun getCriteriaForRubric(rubricId: String): List<CriterionEntity>

    @Query("SELECT * FROM criteria WHERE rubricId = :rubricId ORDER BY orderIndex ASC")
    fun getCriteriaForRubricFlow(rubricId: String): Flow<List<CriterionEntity>>

    @Query("SELECT * FROM criterion_levels WHERE criterionId IN (SELECT id FROM criteria WHERE rubricId = :rubricId) ORDER BY orderIndex ASC")
    suspend fun getLevelsForRubric(rubricId: String): List<CriterionLevelEntity>

    // REPLACE deletes the old row and can cascade-delete all past marks.
    // UPDATE-on-conflict keeps FK relationships and assessment history.
    @Upsert
    suspend fun insertRubric(rubric: RubricEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRubricIfAbsent(rubric: RubricEntity)

    @Upsert
    suspend fun insertCriteria(criteria: List<CriterionEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCriteriaFromApprovedSource(criteria: List<CriterionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLevels(levels: List<CriterionLevelEntity>)
}

data class RunClassroomProgress(
    val classroomId: String,
    val classroomName: String,
    val total: Int,
    val completed: Int,
    val partial: Int
)

@Dao
interface AssessmentRunDao {
    @Query("SELECT * FROM assessment_runs WHERE criteriaSnapshotJson = ''")
    suspend fun runsMissingCriterionSnapshots(): List<AssessmentRunEntity>

    @Query("SELECT * FROM assessment_runs ORDER BY createdAt DESC, id DESC")
    fun observeAll(): Flow<List<AssessmentRunEntity>>

    @Query("SELECT * FROM assessment_runs WHERE id = :id LIMIT 1")
    suspend fun get(id: String): AssessmentRunEntity?

    @Query("SELECT * FROM assessment_runs WHERE id = :id LIMIT 1")
    fun observe(id: String): Flow<AssessmentRunEntity?>

    @Query("SELECT * FROM assessment_runs WHERE rubricId = :rubricId AND id IN (SELECT runId FROM run_classrooms WHERE classroomId = :classroomId) ORDER BY createdAt DESC, id DESC")
    suspend fun getForClassroom(classroomId: String, rubricId: String): List<AssessmentRunEntity>

    @Query("SELECT * FROM assessment_runs WHERE rubricId = :rubricId AND id IN (SELECT runId FROM run_classrooms WHERE classroomId = :classroomId) ORDER BY createdAt DESC, id DESC")
    fun observeForClassroom(classroomId: String, rubricId: String): Flow<List<AssessmentRunEntity>>

    @Query("""
        SELECT c.id AS classroomId, c.name AS classroomName,
            (SELECT COUNT(*) FROM students s WHERE s.classroomId = c.id) AS total,
            (SELECT COUNT(*) FROM assessments a
                WHERE a.runId = :runId AND a.classroomId = c.id AND a.isCompleted = 1) AS completed,
            (SELECT COUNT(*) FROM assessments a
                WHERE a.runId = :runId AND a.classroomId = c.id
                    AND a.isCompleted = 0 AND a.scoredCount > 0) AS partial
        FROM run_classrooms rc
        JOIN classrooms c ON c.id = rc.classroomId
        WHERE rc.runId = :runId
        ORDER BY c.grade ASC, c.section ASC
    """)
    fun observeProgress(runId: String): Flow<List<RunClassroomProgress>>

    @Query("SELECT COUNT(*) FROM run_classrooms WHERE classroomId = :classroomId")
    suspend fun countAssignmentsForClassroom(classroomId: String): Int

    @Query("""
        SELECT rc.* FROM run_classrooms rc
        JOIN classrooms c ON c.id = rc.classroomId
        WHERE rc.runId = :runId
        ORDER BY c.grade ASC, c.section ASC
    """)
    suspend fun getAssignments(runId: String): List<RunClassroomEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM run_classrooms WHERE runId = :runId AND classroomId = :classroomId)")
    suspend fun containsClassroom(runId: String, classroomId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(run: AssessmentRunEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(run: AssessmentRunEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun assign(assignments: List<RunClassroomEntity>)

    @Update
    suspend fun update(run: AssessmentRunEntity)
}

@Dao
interface AssessmentDao {
    @Query("SELECT * FROM assessments WHERE classroomId = :classroomId AND runId = :runId")
    fun observeForRun(classroomId: String, runId: String): Flow<List<AssessmentEntity>>

    @Query("SELECT * FROM assessments WHERE classroomId = :classroomId AND runId = :runId")
    suspend fun getForRun(classroomId: String, runId: String): List<AssessmentEntity>

    @Query("SELECT * FROM assessments WHERE id = :id LIMIT 1")
    suspend fun getAssessmentById(id: String): AssessmentEntity?

    @Query("SELECT * FROM assessments WHERE studentId = :studentId AND runId = :runId LIMIT 1")
    suspend fun getStudentInRun(studentId: String, runId: String): AssessmentEntity?

    @Query("SELECT * FROM assessments WHERE studentId = :studentId AND runId = :runId LIMIT 1")
    fun observeStudentInRun(studentId: String, runId: String): Flow<AssessmentEntity?>

    @Query("SELECT * FROM assessments WHERE classroomId = :classroomId AND rubricId = :rubricId")
    fun getAssessmentsFlow(classroomId: String, rubricId: String): Flow<List<AssessmentEntity>>

    @Query("SELECT * FROM assessments WHERE classroomId = :classroomId AND rubricId = :rubricId")
    suspend fun getAssessments(classroomId: String, rubricId: String): List<AssessmentEntity>

    @Query("SELECT * FROM assessments WHERE studentId = :studentId AND rubricId = :rubricId LIMIT 1")
    suspend fun getAssessmentForStudent(studentId: String, rubricId: String): AssessmentEntity?

    @Query("SELECT * FROM assessments WHERE studentId = :studentId AND rubricId = :rubricId LIMIT 1")
    fun getAssessmentForStudentFlow(studentId: String, rubricId: String): Flow<AssessmentEntity?>

    @Query("SELECT * FROM criterion_scores WHERE assessmentId = :assessmentId")
    suspend fun getScoresForAssessment(assessmentId: String): List<CriterionScoreEntity>

    @Query("SELECT * FROM criterion_scores WHERE assessmentId = :assessmentId")
    fun getScoresForAssessmentFlow(assessmentId: String): Flow<List<CriterionScoreEntity>>

    @Upsert
    suspend fun insertOrUpdateAssessment(assessment: AssessmentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateScore(score: CriterionScoreEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateScores(scores: List<CriterionScoreEntity>)

    @Query("DELETE FROM criterion_scores WHERE assessmentId = :assessmentId")
    suspend fun clearScoresForAssessment(assessmentId: String)
}

@Dao
interface AuditLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: AuditLogEntity)

    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentLogsFlow(limit: Int = 50): Flow<List<AuditLogEntity>>

    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentLogs(limit: Int = 50): List<AuditLogEntity>

    @Query("SELECT * FROM audit_logs WHERE action = 'CRITERION_SCORE_CHANGED' AND timestamp >= :sinceInclusive ORDER BY timestamp ASC")
    fun getAssessmentActivityFlow(sinceInclusive: Long): Flow<List<AuditLogEntity>>
}

@Dao
interface GroupTaskDao {
    @Query("SELECT * FROM group_tasks WHERE id = :id LIMIT 1")
    suspend fun getGroupTaskById(id: String): GroupTaskEntity?

    @Query("SELECT * FROM group_tasks WHERE classroomId = :classroomId ORDER BY createdAt, title")
    fun getGroupTasksFlow(classroomId: String): Flow<List<GroupTaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateGroupTask(task: GroupTaskEntity)

    @Delete
    suspend fun deleteGroupTask(task: GroupTaskEntity)
}


@Dao
interface GroupAssessmentDao {
    @Query("SELECT COUNT(*) FROM group_member_results WHERE studentId = :studentId")
    suspend fun countResultsForStudent(studentId: String): Int
    @Query("SELECT COUNT(*) FROM group_assessments WHERE groupId = :groupId")
    suspend fun countAssessmentsForGroup(groupId: String): Int
    @Query("""
        SELECT COUNT(*) FROM group_assessments ga
        JOIN group_assessment_sessions s ON s.id = ga.sessionId
        WHERE s.classroomId = :classroomId
    """)
    suspend fun countAssessmentsForClassroom(classroomId: String): Int
    @Query("SELECT COUNT(*) FROM group_assessment_sessions WHERE classroomId = :classroomId")
    suspend fun countSessionsForClassroom(classroomId: String): Int

    @Query("""
        SELECT m.studentId AS studentId, s.id AS sessionId, s.title AS sessionTitle,
               a.groupTitleSnapshot AS groupTitle, m.definitiveTotal AS definitiveTotal,
               m.isCompleted AS isCompleted, s.createdAt AS createdAt
          FROM group_member_results m
          JOIN group_assessments a ON a.id = m.groupAssessmentId
          JOIN group_assessment_sessions s ON s.id = a.sessionId
         WHERE m.studentId = :studentId AND s.rubricId = :rubricId
         ORDER BY s.createdAt DESC, s.id DESC
    """)
    fun getStudentGroupGradesFlow(studentId: String, rubricId: String): Flow<List<StudentGroupGrade>>

    @Query("SELECT * FROM group_assessment_sessions WHERE classroomId = :classroomId AND rubricId = :rubricId ORDER BY createdAt DESC")
    fun getSessionsFlow(classroomId: String, rubricId: String): Flow<List<GroupAssessmentSessionEntity>>
    @Query("SELECT * FROM group_assessment_sessions WHERE id = :id LIMIT 1")
    suspend fun getSession(id: String): GroupAssessmentSessionEntity?
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSession(row: GroupAssessmentSessionEntity)
    @Query("SELECT * FROM group_assessments WHERE sessionId = :sessionId")
    fun getAssessmentsFlow(sessionId: String): Flow<List<GroupAssessmentEntity>>
    @Query("SELECT * FROM group_assessments WHERE sessionId = :sessionId")
    suspend fun getAssessments(sessionId: String): List<GroupAssessmentEntity>
    @Query("SELECT * FROM group_assessments WHERE sessionId = :sessionId AND groupId = :groupId LIMIT 1")
    suspend fun getAssessment(sessionId: String, groupId: String): GroupAssessmentEntity?
    @Upsert suspend fun upsertAssessment(row: GroupAssessmentEntity)
    @Query("SELECT * FROM group_criterion_scores WHERE groupAssessmentId = :assessmentId")
    suspend fun getScores(assessmentId: String): List<GroupCriterionScoreEntity>
    @Upsert suspend fun upsertScore(row: GroupCriterionScoreEntity)
    @Query("DELETE FROM group_criterion_scores WHERE groupAssessmentId = :assessmentId")
    suspend fun deleteScores(assessmentId: String)
    @Query("DELETE FROM group_assessments WHERE id = :assessmentId")
    suspend fun deleteAssessment(assessmentId: String)
    @Query("SELECT * FROM group_member_results WHERE groupAssessmentId = :assessmentId")
    suspend fun getMemberResults(assessmentId: String): List<GroupMemberResultEntity>
    @Query("DELETE FROM group_member_results WHERE groupAssessmentId = :assessmentId")
    suspend fun deleteMemberResults(assessmentId: String)
    @Upsert suspend fun upsertMemberResults(rows: List<GroupMemberResultEntity>)
}
