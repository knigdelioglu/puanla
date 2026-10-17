package com.knigdelioglu.puanla

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.knigdelioglu.puanla.data.local.AssessmentEntity
import com.knigdelioglu.puanla.data.local.AssessmentRunEntity
import com.knigdelioglu.puanla.data.local.ClassroomEntity
import com.knigdelioglu.puanla.data.local.PuanlaDatabase
import com.knigdelioglu.puanla.data.local.RubricEntity
import com.knigdelioglu.puanla.data.local.StudentEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ClassroomRosterSummaryDaoTest {
    @Test
    fun summaryCountsStartedSessionsAndAveragesOnlyCompletedMarks() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, PuanlaDatabase::class.java)
            .allowMainThreadQueries().build()
        try {
        val classroom = ClassroomEntity("summary-class", 11, "S", "11-S", "2026-2027")
        val student = StudentEntity("summary-student", classroom.id, "702", "Ada", "Örnek")
        database.classroomDao().insertClassroom(classroom)
        database.rubricDao().insertRubric(RubricEntity("summary-rubric", "Örnek Rubrik", "", 11, "Fen"))
        database.studentDao().insertStudent(student)
        database.assessmentRunDao().insert(AssessmentRunEntity("summary-run-1", "summary-rubric", "Oturum 1"))
        database.assessmentRunDao().insert(AssessmentRunEntity("summary-run-2", "summary-rubric", "Oturum 2"))
        database.assessmentRunDao().insert(AssessmentRunEntity("summary-run-empty", "summary-rubric", "Oturum 3"))
        database.assessmentDao().insertOrUpdateAssessment(
            AssessmentEntity("summary-assessment-1", classroom.id, student.id, "summary-rubric", true, 80, 4, runId = "summary-run-1")
        )
        database.assessmentDao().insertOrUpdateAssessment(
            AssessmentEntity("summary-assessment-2", classroom.id, student.id, "summary-rubric", false, null, 2, runId = "summary-run-2")
        )
        database.assessmentDao().insertOrUpdateAssessment(
            AssessmentEntity("summary-assessment-empty", classroom.id, student.id, "summary-rubric", false, null, 0, runId = "summary-run-empty")
        )

        val summary = database.assessmentDao().observeStudentAssessmentSummaries(classroom.id).first().single()
        val history = database.assessmentDao().getCompletedRunScoresForStudent(classroom.id, student.id)
        val rosterSummary = database.classroomDao().observeRosterSummaries().first().single()

        assertEquals(1, rosterSummary.studentCount)
        assertEquals(2, summary.assessmentCount)
        assertEquals(80.0, summary.averageCompletedTotal!!, 0.0)
        assertEquals(1, history.size)
        assertEquals(80, history.single().total)
        } finally {
            database.close()
        }
    }
}
