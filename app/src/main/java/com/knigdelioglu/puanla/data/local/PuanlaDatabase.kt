package com.knigdelioglu.puanla.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ClassroomEntity::class,
        StudentEntity::class,
        RubricEntity::class,
        CriterionEntity::class,
        CriterionLevelEntity::class,
        AssessmentEntity::class,
        CriterionScoreEntity::class,
        GroupTaskEntity::class,
        AuditLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class PuanlaDatabase : RoomDatabase() {
    abstract fun classroomDao(): ClassroomDao
    abstract fun studentDao(): StudentDao
    abstract fun rubricDao(): RubricDao
    abstract fun assessmentDao(): AssessmentDao
    abstract fun groupTaskDao(): GroupTaskDao
    abstract fun auditLogDao(): AuditLogDao

    companion object {
        @Volatile
        private var INSTANCE: PuanlaDatabase? = null

        fun getInstance(context: Context): PuanlaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PuanlaDatabase::class.java,
                    "puanla.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
