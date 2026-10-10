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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRubric(rubric: RubricEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCriteria(criteria: List<CriterionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLevels(levels: List<CriterionLevelEntity>)
}

@Dao
interface AssessmentDao {
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
}

@Dao
interface GroupTaskDao {
    @Query("SELECT * FROM group_tasks WHERE classroomId = :classroomId AND rubricId = :rubricId")
    fun getGroupTasksFlow(classroomId: String, rubricId: String): Flow<List<GroupTaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateGroupTask(task: GroupTaskEntity)

    @Delete
    suspend fun deleteGroupTask(task: GroupTaskEntity)
}
