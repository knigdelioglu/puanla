
package com.knigdelioglu.puanla

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.knigdelioglu.puanla.data.local.*
import com.knigdelioglu.puanla.data.repository.PuanlaRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GroupModeScoringTest {
    @Test
    fun classroomDeletionPreservesStudentsAndAllowsOnlyEmptyClassrooms() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val db = Room.inMemoryDatabaseBuilder(context, PuanlaDatabase::class.java).build()
        try {
            val classroom = ClassroomEntity("classroom", 11, "A", "11-A", "2026-2027")
            db.classroomDao().insertClassroom(classroom)
            db.studentDao().insertStudent(StudentEntity("student", classroom.id, "1", "Ada", "Kaya"))
            val repo = PuanlaRepository(db) { "{}" }
            try {
                repo.deleteClassroom(classroom)
                fail("A classroom with students must not cascade-delete them")
            } catch (_: IllegalArgumentException) { }
            assertEquals("student", db.studentDao().getStudentById("student")?.id)
            repo.deleteStudent(db.studentDao().getStudentById("student")!!)
            repo.deleteClassroom(classroom)
            assertNull(db.classroomDao().getClassroomById(classroom.id))
        } finally {
            db.close()
        }
    }

    @Test
    fun groupScorePropagatesAtomicallyAndSessionsDoNotOverwriteOneAnother() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val db = Room.inMemoryDatabaseBuilder(context, PuanlaDatabase::class.java).build()
        try {
            val classroom = ClassroomEntity("class", 11, "A", "11-A", "2026-2027")
            val s1 = StudentEntity("s1", "class", "101", "Ayşe", "Yılmaz")
            val s2 = StudentEntity("s2", "class", "102", "Mert", "Demir")
            db.classroomDao().insertClassroom(classroom)
            db.studentDao().insertStudents(listOf(s1, s2))
            db.rubricDao().insertRubric(RubricEntity("rubric1", "Drama", "", 11, "Drama", false))
            db.rubricDao().insertCriteria(listOf(
                CriterionEntity("c1", "rubric1", "Anlatım", "", 10, 0),
                CriterionEntity("c2", "rubric1", "Canlandırma", "", 90, 1)
            ))
            val repo = PuanlaRepository(db) { "{}" }
            val group = GroupTaskEntity(
                id = "g1", classroomId = "class", rubricId = "",
                title = "Grup 1", memberStudentIdsJson = "[\"s1\",\"s2\"]", checklistJson = "[]"
            )
            repo.saveGroupTask(group)
            assertEquals(listOf(group), repo.getGroupTasksFlow("class").first())

            val first = repo.createGroupSession("class", "rubric1", "12 Ekim Draması")
            val partial = repo.scoreGroup(first.id, group.id, "c1", 10)
            assertFalse(partial.isCompleted)
            assertNull(partial.definitiveTotal)
            assertTrue(repo.getGroupMemberResults(partial.id).all { !it.isCompleted && it.definitiveTotal == null })

            val full = repo.scoreGroup(first.id, group.id, "c2", 70)
            assertEquals(80, full.definitiveTotal)
            assertTrue(repo.getGroupMemberResults(full.id).all { it.isCompleted && it.definitiveTotal == 80 })
            assertEquals(listOf(80), repo.getStudentGroupGradesFlow(s1.id, "rubric1")
                .first().map { it.definitiveTotal })
            assertEquals("12 Ekim Draması", repo.getStudentGroupGradesFlow(s1.id, "rubric1")
                .first().single().sessionTitle)
            try {
                repo.deleteStudent(s1)
                fail("Group graded student deletion should be blocked")
            } catch (_: IllegalArgumentException) { }
            try {
                repo.deleteGroupTask(group)
                fail("Graded group deletion should be blocked")
            } catch (_: IllegalArgumentException) { }

            val corrected = repo.scoreGroup(first.id, group.id, "c1", 8)
            assertEquals(78, corrected.definitiveTotal)
            assertTrue(repo.getGroupMemberResults(corrected.id).all { it.definitiveTotal == 78 })
            assertTrue(repo.undoGroupScoreIfCurrent(first.id, group.id, "c1", 8, 10))
            assertEquals(80, repo.getGroupAssessment(first.id, group.id)?.definitiveTotal)
            assertFalse(repo.undoGroupScoreIfCurrent(first.id, group.id, "c1", 8, null))

            val second = repo.createGroupSession("class", "rubric1", "19 Ekim Draması")
            val different = repo.scoreGroup(second.id, group.id, "c1", 5)
            assertNull(different.definitiveTotal)
            assertFalse(repo.undoGroupScoreIfCurrent(first.id, group.id, "c1", 5, null))
            assertEquals(80, repo.getGroupMemberResults(corrected.id).first().definitiveTotal)
            assertEquals(5, repo.getGroupScores(different.id).single().points)
            assertEquals(1, repo.getGroupAssessmentsFlow(first.id).first().size)
            assertEquals(1, repo.getGroupAssessmentsFlow(second.id).first().size)

            val emptySession = repo.createGroupSession("class", "rubric1", "Geri alınan oturum")
            val firstScore = repo.scoreGroup(emptySession.id, group.id, "c1", 4)
            assertTrue(repo.undoGroupScoreIfCurrent(
                emptySession.id, group.id, "c1", 4, null, removeAssessmentIfEmpty = true
            ))
            assertNull(repo.getGroupAssessment(emptySession.id, group.id))
            assertTrue(repo.getGroupMemberResults(firstScore.id).isEmpty())

            assertNull(repo.getAssessmentForStudent(s1.id, "rubric1"))
        } finally {
            db.close()
        }
    }

    @Test
    fun distinctGroupsReceiveIndependentGradesAndCanRepairStaleMembership() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val db = Room.inMemoryDatabaseBuilder(context, PuanlaDatabase::class.java).build()
        try {
            db.classroomDao().insertClassroom(ClassroomEntity("c", 11, "A", "11-A", "2026-2027"))
            val members = listOf(
                StudentEntity("s1", "c", "1", "Ayşe", "Yılmaz"),
                StudentEntity("s2", "c", "2", "Mert", "Demir"),
                StudentEntity("s3", "c", "3", "Ece", "Kaya")
            )
            db.studentDao().insertStudents(members)
            db.rubricDao().insertRubric(RubricEntity("r", "Drama", "", 11, "Drama", false))
            db.rubricDao().insertCriteria(listOf(
                CriterionEntity("crit", "r", "Sunum", "", 100, 0)
            ))
            val repo = PuanlaRepository(db) { "{}" }
            val group1 = GroupTaskEntity("g1", "c", "", "Grup A", "[\"s1\",\"s2\"]", "[]")
            val group2 = GroupTaskEntity("g2", "c", "", "Grup B", "[\"s3\"]", "[]")
            repo.saveGroupTask(group1)
            repo.saveGroupTask(group2)
            val session = repo.createGroupSession("c", "r", "Drama Oturumu")
            val first = repo.scoreGroup(session.id, "g1", "crit", 80)
            val second = repo.scoreGroup(session.id, "g2", "crit", 90)
            assertEquals(2, repo.getGroupAssessmentsFlow(session.id).first().size)
            assertEquals(listOf(80, 80), repo.getGroupMemberResults(first.id).map { it.definitiveTotal })
            assertEquals(listOf(90), repo.getGroupMemberResults(second.id).map { it.definitiveTotal })
            val initial = repo.generateGroupPdfReportSnapshot(session.id)
            assertEquals(listOf(80, 80, 90), initial.students.map { it.definitiveTotal })
            assertEquals(90, repo.getStudentGroupGradesFlow("s3", "r").first().single().definitiveTotal)
            assertNull(repo.getAssessmentForStudent("s3", "r"))

            // Simulate a legitimate roster correction during this same session.
            repo.saveGroupTask(group1.copy(memberStudentIdsJson = "[\"s1\"]"))
            repo.saveGroupTask(group2.copy(memberStudentIdsJson = "[\"s2\",\"s3\"]"))
            assertEquals(2, repo.syncGroupSessionMembership(session.id))
            assertEquals(0, repo.syncGroupSessionMembership(session.id))
            assertEquals(listOf("s1"), repo.getGroupMemberResults(first.id).map { it.studentId })
            assertEquals(setOf("s2", "s3"), repo.getGroupMemberResults(second.id).map { it.studentId }.toSet())
            assertEquals(90, repo.getStudentGroupGradesFlow("s2", "r").first().single().definitiveTotal)
            assertEquals(listOf(80, 90, 90), repo.generateGroupPdfReportSnapshot(session.id).students.map { it.definitiveTotal })
            assertTrue(repo.generateGroupCsvExport(session.id).contains("Tamamlandı"))
        } finally {
            db.close()
        }
    }

    @Test
    fun rejectsStudentInTwoScoredGroupsInTheSameSession() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val db = Room.inMemoryDatabaseBuilder(context, PuanlaDatabase::class.java).build()
        try {
            db.classroomDao().insertClassroom(ClassroomEntity("c", 11, "A", "11-A", "2026-2027"))
            db.studentDao().insertStudent(StudentEntity("s", "c", "1", "Ali", "Kaya"))
            db.rubricDao().insertRubric(RubricEntity("r", "Rubrik", "", 11, "Drama", false))
            db.rubricDao().insertCriteria(listOf(CriterionEntity("crit", "r", "Ölçüt", "", 100, 0)))
            val repo = PuanlaRepository(db) { "{}" }
            repo.saveGroupTask(GroupTaskEntity("g1", "c", "", "G1", "[\"s\"]", "[]"))
            try {
                repo.saveGroupTask(GroupTaskEntity("g2", "c", "", "G2", "[\"s\"]", "[]"))
                fail("New groups cannot contain a previously assigned student")
            } catch (_: IllegalArgumentException) { }
            // Legacy/backup records may still have overlapping membership; scoring
            // must not silently award a second group grade for the same session.
            db.groupTaskDao().insertOrUpdateGroupTask(
                GroupTaskEntity("g2", "c", "", "G2", "[\"s\"]", "[]")
            )
            val session = repo.createGroupSession("c", "r", "Drama")
            repo.scoreGroup(session.id, "g1", "crit", 90)
            try {
                repo.scoreGroup(session.id, "g2", "crit", 80)
                fail("A student must not be graded twice within one session")
            } catch (_: IllegalArgumentException) {
                // Expected overlap guard
            }
        } finally {
            db.close()
        }
    }
    @Test
    fun groupZeroScoreAndIndividualRunRemainIndependent() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val db = Room.inMemoryDatabaseBuilder(context, PuanlaDatabase::class.java).build()
        try {
            db.classroomDao().insertClassroom(ClassroomEntity("c", 11, "A", "11-A", "2026-2027"))
            db.studentDao().insertStudent(StudentEntity("student", "c", "101", "Ayşe", "Yılmaz"))
            db.rubricDao().insertRubric(RubricEntity("rubric", "Sunum", "", 11, "Sunum", false))
            db.rubricDao().insertCriteria(listOf(
                CriterionEntity("content", "rubric", "İçerik", "", 20, 0),
                CriterionEntity("speech", "rubric", "Anlatım", "", 80, 1)
            ))

            val repo = PuanlaRepository(db) { "{}" }
            val individual = repo.createAssessmentRun("rubric", "Bireysel Ekim", listOf("c"))
            repo.saveCriterionScore("c", "student", "rubric", "content", 15, runId = individual.id)
            repo.saveCriterionScore("c", "student", "rubric", "speech", 70, runId = individual.id)

            val group = GroupTaskEntity(
                id = "group", classroomId = "c", rubricId = "",
                title = "Grup 1", memberStudentIdsJson = "[\\"student\\"]", checklistJson = "[]"
            )
            repo.saveGroupTask(group)
            val groupSession = repo.createGroupSession("c", "rubric", "Grup Ekim")
            val partial = repo.scoreGroup(groupSession.id, group.id, "content", 0)

            assertFalse(partial.isCompleted)
            assertEquals(1, partial.scoredCount)
            assertNull(partial.definitiveTotal)
            assertTrue(repo.getGroupMemberResults(partial.id).all {
                !it.isCompleted && it.definitiveTotal == null
            })

            val complete = repo.scoreGroup(groupSession.id, group.id, "speech", 60)
            assertTrue(complete.isCompleted)
            assertEquals(60, complete.definitiveTotal)
            assertTrue(repo.getGroupMemberResults(complete.id).all {
                it.isCompleted && it.definitiveTotal == 60
            })
            val individualScores = repo.getRunCriterionScoresForStudent("student", individual.id)
            assertEquals(15, individualScores["content"])
            assertEquals(70, individualScores["speech"])
        } finally {
            db.close()
        }
    }

}
