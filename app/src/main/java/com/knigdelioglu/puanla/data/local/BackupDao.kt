package com.knigdelioglu.puanla.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/** Only used for complete, transactionally consistent JSON snapshots. */
@Dao
interface BackupDao {
    @Query("SELECT * FROM classrooms") suspend fun classrooms(): List<ClassroomEntity>
    @Query("SELECT * FROM students") suspend fun students(): List<StudentEntity>
    @Query("SELECT * FROM rubrics") suspend fun rubrics(): List<RubricEntity>
    @Query("SELECT * FROM criteria") suspend fun criteria(): List<CriterionEntity>
    @Query("SELECT * FROM criterion_levels") suspend fun levels(): List<CriterionLevelEntity>
    @Query("SELECT * FROM assessments") suspend fun assessments(): List<AssessmentEntity>
    @Query("SELECT * FROM criterion_scores") suspend fun scores(): List<CriterionScoreEntity>
    @Query("SELECT * FROM group_tasks") suspend fun groups(): List<GroupTaskEntity>
    @Query("SELECT * FROM audit_logs") suspend fun auditLogs(): List<AuditLogEntity>

    @Query("DELETE FROM criterion_scores") suspend fun clearScores()
    @Query("DELETE FROM assessments") suspend fun clearAssessments()
    @Query("DELETE FROM group_tasks") suspend fun clearGroups()
    @Query("DELETE FROM criterion_levels") suspend fun clearLevels()
    @Query("DELETE FROM criteria") suspend fun clearCriteria()
    @Query("DELETE FROM students") suspend fun clearStudents()
    @Query("DELETE FROM rubrics") suspend fun clearRubrics()
    @Query("DELETE FROM classrooms") suspend fun clearClassrooms()
    @Query("DELETE FROM audit_logs") suspend fun clearLogs()

    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun putClassrooms(rows: List<ClassroomEntity>)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun putRubrics(rows: List<RubricEntity>)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun putStudents(rows: List<StudentEntity>)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun putCriteria(rows: List<CriterionEntity>)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun putLevels(rows: List<CriterionLevelEntity>)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun putAssessments(rows: List<AssessmentEntity>)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun putScores(rows: List<CriterionScoreEntity>)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun putGroups(rows: List<GroupTaskEntity>)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun putLogs(rows: List<AuditLogEntity>)
}
