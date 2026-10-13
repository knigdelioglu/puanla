package com.knigdelioglu.puanla.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ClassroomEntity::class,
        StudentEntity::class,
        RubricEntity::class,
        CriterionEntity::class,
        CriterionLevelEntity::class,
        AssessmentEntity::class,
        AssessmentRunEntity::class,
        RunClassroomEntity::class,
        CriterionScoreEntity::class,
        GroupTaskEntity::class,
        GroupAssessmentSessionEntity::class,
        GroupAssessmentEntity::class,
        GroupCriterionScoreEntity::class,
        GroupMemberResultEntity::class,
        AuditLogEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class PuanlaDatabase : RoomDatabase() {
    abstract fun classroomDao(): ClassroomDao
    abstract fun studentDao(): StudentDao
    abstract fun rubricDao(): RubricDao
    abstract fun assessmentDao(): AssessmentDao
    abstract fun assessmentRunDao(): AssessmentRunDao
    abstract fun groupTaskDao(): GroupTaskDao
    abstract fun groupAssessmentDao(): GroupAssessmentDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun backupDao(): BackupDao

    companion object {
        /** Only creates new tables: legacy group tasks, individual scores and rosters survive. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS group_assessment_sessions (id TEXT NOT NULL PRIMARY KEY, classroomId TEXT NOT NULL, rubricId TEXT NOT NULL, title TEXT NOT NULL, createdAt INTEGER NOT NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_group_assessment_sessions_classroomId_rubricId ON group_assessment_sessions(classroomId, rubricId)")
                db.execSQL("CREATE TABLE IF NOT EXISTS group_assessments (id TEXT NOT NULL PRIMARY KEY, sessionId TEXT NOT NULL, groupId TEXT NOT NULL, groupTitleSnapshot TEXT NOT NULL, memberStudentIdsJson TEXT NOT NULL, isCompleted INTEGER NOT NULL, definitiveTotal INTEGER, scoredCount INTEGER NOT NULL, lastModifiedAt INTEGER NOT NULL)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_group_assessments_sessionId_groupId ON group_assessments(sessionId, groupId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_group_assessments_sessionId ON group_assessments(sessionId)")
                db.execSQL("CREATE TABLE IF NOT EXISTS group_criterion_scores (id TEXT NOT NULL PRIMARY KEY, groupAssessmentId TEXT NOT NULL, criterionId TEXT NOT NULL, points INTEGER, evidenceNote TEXT, scoredAt INTEGER NOT NULL)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_group_criterion_scores_groupAssessmentId_criterionId ON group_criterion_scores(groupAssessmentId, criterionId)")
                db.execSQL("CREATE TABLE IF NOT EXISTS group_member_results (id TEXT NOT NULL PRIMARY KEY, groupAssessmentId TEXT NOT NULL, studentId TEXT NOT NULL, definitiveTotal INTEGER, isCompleted INTEGER NOT NULL)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_group_member_results_groupAssessmentId_studentId ON group_member_results(groupAssessmentId, studentId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_group_member_results_studentId ON group_member_results(studentId)")
            }
        }
        /**
         * Preserve all existing marks. Historical assessment dates are not
         * fabricated: legacy runs have createdAt=0 and a descriptive title.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS assessment_runs (id TEXT NOT NULL PRIMARY KEY, rubricId TEXT NOT NULL, title TEXT NOT NULL, criteriaSnapshotJson TEXT NOT NULL, status TEXT NOT NULL, createdAt INTEGER NOT NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_assessment_runs_rubricId ON assessment_runs(rubricId)")
                db.execSQL("CREATE TABLE IF NOT EXISTS run_classrooms (id TEXT NOT NULL PRIMARY KEY, runId TEXT NOT NULL, classroomId TEXT NOT NULL, FOREIGN KEY(runId) REFERENCES assessment_runs(id) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(classroomId) REFERENCES classrooms(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_run_classrooms_runId_classroomId ON run_classrooms(runId, classroomId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_run_classrooms_classroomId ON run_classrooms(classroomId)")
                db.execSQL("INSERT INTO assessment_runs (id,rubricId,title,criteriaSnapshotJson,status,createdAt) SELECT DISTINCT 'legacy_' || classroomId || '_' || rubricId, rubricId, 'Önceki değerlendirme', '', 'ACTIVE', 0 FROM assessments")
                db.execSQL("INSERT INTO run_classrooms (id,runId,classroomId) SELECT DISTINCT 'assign_legacy_' || classroomId || '_' || rubricId, 'legacy_' || classroomId || '_' || rubricId, classroomId FROM assessments")
                db.execSQL("ALTER TABLE assessments ADD COLUMN runId TEXT NOT NULL DEFAULT ''")
                db.execSQL("UPDATE assessments SET runId = 'legacy_' || classroomId || '_' || rubricId")
                db.execSQL("DROP INDEX IF EXISTS index_assessments_studentId_rubricId")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_assessments_studentId_runId ON assessments(studentId,runId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_assessments_runId ON assessments(runId)")
            }
        }
        @Volatile
        private var INSTANCE: PuanlaDatabase? = null

        fun getInstance(context: Context): PuanlaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PuanlaDatabase::class.java,
                    "puanla.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
