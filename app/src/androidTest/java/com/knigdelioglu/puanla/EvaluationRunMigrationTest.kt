package com.knigdelioglu.puanla

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.knigdelioglu.puanla.data.local.PuanlaDatabase
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EvaluationRunMigrationTest {
    @Test fun v2ToV3KeepsExistingAssessmentIdentitiesAndScores() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "migration-v2-v3-${System.nanoTime()}.db"
        val factory = FrameworkSQLiteOpenHelperFactory()
        val helper = factory.create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(name)
                .callback(object : SupportSQLiteOpenHelper.Callback(2) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL("CREATE TABLE classrooms (id TEXT NOT NULL PRIMARY KEY)")
                        db.execSQL("CREATE TABLE assessments (id TEXT NOT NULL PRIMARY KEY, classroomId TEXT NOT NULL, studentId TEXT NOT NULL, rubricId TEXT NOT NULL)")
                        db.execSQL("CREATE UNIQUE INDEX index_assessments_studentId_rubricId ON assessments(studentId,rubricId)")
                        db.execSQL("CREATE TABLE criterion_scores (id TEXT NOT NULL PRIMARY KEY, assessmentId TEXT NOT NULL, criterionId TEXT NOT NULL, points INTEGER)")
                        db.execSQL("INSERT INTO classrooms(id) VALUES ('c1')")
                        db.execSQL("INSERT INTO assessments(id,classroomId,studentId,rubricId) VALUES ('a1','c1','s1','r1')")
                        db.execSQL("INSERT INTO criterion_scores(id,assessmentId,criterionId,points) VALUES ('score1','a1','criterion',70)")
                    }
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                }).build()
        )
        try {
            val db = helper.writableDatabase
            PuanlaDatabase.MIGRATION_2_3.migrate(db)
            db.query("SELECT runId FROM assessments WHERE id = 'a1'").use {
                assertTrue(it.moveToFirst())
                assertEquals("legacy_c1_r1", it.getString(0))
            }
            db.query("SELECT points FROM criterion_scores WHERE id = 'score1'").use {
                assertTrue(it.moveToFirst())
                assertEquals(70, it.getInt(0))
            }
            db.query("SELECT COUNT(*) FROM run_classrooms WHERE runId = 'legacy_c1_r1'").use {
                assertTrue(it.moveToFirst())
                assertEquals(1, it.getInt(0))
            }
        } finally {
            helper.close()
            context.deleteDatabase(name)
        }
    }
}
