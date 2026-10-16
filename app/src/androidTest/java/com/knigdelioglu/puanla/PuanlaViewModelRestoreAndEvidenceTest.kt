package com.knigdelioglu.puanla

import android.app.Application
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.knigdelioglu.puanla.data.local.*
import com.knigdelioglu.puanla.data.repository.PuanlaRepository
import com.knigdelioglu.puanla.ui.viewmodel.EvidenceNoteSaveState
import com.knigdelioglu.puanla.ui.viewmodel.PuanlaViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PuanlaViewModelRestoreAndEvidenceTest {
    companion object {
        // Room query executors can finish cancellation after a test method returns. Keep the
        // in-memory databases alive until instrumentation exits instead of closing under a Flow.
        private val openDatabases = mutableListOf<PuanlaDatabase>()
    }
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun newDatabase() = Room.inMemoryDatabaseBuilder(context, PuanlaDatabase::class.java)
        .allowMainThreadQueries().build().also { openDatabases.add(it) }
    private fun repository(db: PuanlaDatabase) = PuanlaRepository(db) {
        context.assets.open("rubrics.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
    }
    private suspend fun awaitState(label: String, state: suspend () -> String, condition: suspend () -> Boolean) {
        val reached = withTimeoutOrNull(8_000) {
            while (!condition()) delay(20)
            true
        } ?: false
        check(reached) { "$label timed out; ${state()}" }
    }
    private suspend fun selectRubric(vm: PuanlaViewModel, rubricId: String) {
        withTimeout(8_000) {
            while (vm.selectedClassroom.value == null || vm.rubrics.value.none { it.id == rubricId }) delay(20)
        }
        vm.selectRubric(vm.rubrics.value.first { it.id == rubricId })
        withTimeout(8_000) { while (vm.selectedRubric.value?.id != rubricId) delay(20) }
    }
    private fun viewModel(db: PuanlaDatabase, repo: PuanlaRepository): Pair<PuanlaViewModel, ViewModelStore> {
        val vm = PuanlaViewModel(context.applicationContext as Application, db, repo)
        return vm to ViewModelStore().also { it.put("test", vm) }
    }

    @Test fun evidenceEditsAreDebouncedButLifecycleFlushPersistsLatestTarget() = runBlocking {
        val db = newDatabase()
        val repo = repository(db)
        var vmStore: ViewModelStore? = null
        lateinit var vm: PuanlaViewModel
        try {
            db.classroomDao().insertClassroom(ClassroomEntity("c1", 11, "A", "11-A", "2026-2027"))
            db.studentDao().insertStudent(StudentEntity("s1", "c1", "42", "Öykü", "Şen"))
            db.studentDao().insertStudent(StudentEntity("s2", "c1", "43", "Ece", "Kaya"))
            db.rubricDao().insertRubric(RubricEntity("r1", "R1", "", 11, "R1"))
            db.rubricDao().insertCriteria(listOf(CriterionEntity("k1", "r1", "K1", "", 100, 0)))
            val run = repo.createAssessmentRun("r1", "Deneme", listOf("c1"))
            repo.saveCriterionScore("c1", "s1", "r1", "k1", 70, runId = run.id)
            val pair = viewModel(db, repo); vm = pair.first; vmStore = pair.second
            withTimeout(8_000) { while (vm.selectedStudent.value?.id != "s1") delay(20) }
            selectRubric(vm, "r1")
            withTimeout(8_000) { while (vm.selectedRun.value?.id != run.id) delay(20) }
            vm.setEvidenceNote("k1", "ara")
            delay(120)
            vm.setEvidenceNote("k1", "son not")
            assertEquals(EvidenceNoteSaveState.Status.SAVING, vm.evidenceNoteSaveState.value?.status)
            assertEquals(null, repo.getScoresForAssessment(vm.currentAssessment.value!!.id).single().evidenceNote)
            vm.selectStudent(db.studentDao().getStudentById("s2"))
            vm.flushPendingEvidenceNotes()
            val stored = repo.getRunAssessmentForStudent("s1", run.id)!!
            assertEquals("son not", repo.getScoresForAssessment(stored.id).single().evidenceNote)
            assertEquals(EvidenceNoteSaveState.Status.SAVED, vm.evidenceNoteSaveState.value?.status)
            assertEquals(70, repo.getScoresForAssessment(stored.id).single().points)
        } finally {
            vmStore?.clear()
            // Keep the in-memory DB alive through the end of the instrumentation process: Room
            // may still be unwinding a just-cancelled Flow query on its query executor.
        }
    }

    @Test fun restoreClearsOldViewModelStateAndRebuildsFromRestoredClassroom() = runBlocking {
        val db = newDatabase()
        val repo = repository(db)
        var store: ViewModelStore? = null
        try {
            db.classroomDao().insertClassroom(ClassroomEntity("oldClass", 11, "A", "11-A", "2026-2027"))
            db.studentDao().insertStudent(StudentEntity("oldStudent", "oldClass", "1", "Eski", "Öğrenci"))
            db.rubricDao().insertRubric(RubricEntity("oldRubric", "Eski", "", 11, "Eski"))
            db.rubricDao().insertCriteria(listOf(CriterionEntity("oldCriterion", "oldRubric", "Eski ölçüt", "", 100, 0)))
            val oldRun = repo.createAssessmentRun("oldRubric", "Eski oturum", listOf("oldClass"))
            repo.saveCriterionScore("oldClass", "oldStudent", "oldRubric", "oldCriterion", 66, runId = oldRun.id)
            val oldGroup = GroupTaskEntity("oldGroup", "oldClass", "", "Eski grup", "[\"oldStudent\"]", "[]")
            repo.saveGroupTask(oldGroup)
            val oldSession = repo.createGroupSession("oldClass", "oldRubric", "Eski grup oturumu")
            repo.scoreGroup(oldSession.id, oldGroup.id, "oldCriterion", 88)
            val source = newDatabase()
            try {
                val sourceRepo = repository(source)
                source.classroomDao().insertClassroom(ClassroomEntity("newClass", 11, "B", "11-B", "2026-2027"))
                source.studentDao().insertStudent(StudentEntity("newStudent", "newClass", "2", "Yeni", "Öğrenci"))
                source.rubricDao().insertRubric(RubricEntity("newRubric", "Yeni", "", 11, "Yeni"))
                source.rubricDao().insertCriteria(listOf(CriterionEntity("newCriterion", "newRubric", "Yeni ölçüt", "", 100, 0)))
                val newRun = sourceRepo.createAssessmentRun("newRubric", "Yeni oturum", listOf("newClass"))
                sourceRepo.saveCriterionScore("newClass", "newStudent", "newRubric", "newCriterion", 91, runId = newRun.id)
                val newGroup = GroupTaskEntity("newGroup", "newClass", "", "Yeni grup", "[\"newStudent\"]", "[]")
                sourceRepo.saveGroupTask(newGroup)
                val newSession = sourceRepo.createGroupSession("newClass", "newRubric", "Yeni grup oturumu")
                sourceRepo.scoreGroup(newSession.id, newGroup.id, "newCriterion", 77)
                val backup = sourceRepo.generateFullBackupJson()
                val pair = viewModel(db, repo); val vm = pair.first; store = pair.second
                awaitState("Old student", { "student=${vm.selectedStudent.value?.id}, classroom=${vm.selectedClassroom.value?.id}" }) {
                    vm.selectedStudent.value?.id == "oldStudent"
                }
                selectRubric(vm, "oldRubric")
                awaitState("Old run and group", {
                    "run=${vm.selectedRun.value?.id}, group=${vm.selectedGroup.value?.id}, session=${vm.selectedGroupSession.value?.id}"
                }) {
                    vm.selectedRun.value?.id == oldRun.id && vm.selectedGroup.value?.id == "oldGroup" &&
                        vm.selectedGroupSession.value?.id == oldSession.id
                }
                vm.setScore("oldCriterion", 67)
                vm.setGroupScore("oldCriterion", 89)
                awaitState("Old writes", { "score=${repo.getRunCriterionScoresForStudent("oldStudent", oldRun.id)["oldCriterion"]}" }) {
                    repo.getRunCriterionScoresForStudent("oldStudent", oldRun.id)["oldCriterion"] == 67 &&
                        repo.getGroupAssessment(oldSession.id, oldGroup.id)?.let {
                            repo.getGroupScores(it.id).firstOrNull()?.points
                        } == 89
                }
                vm.setEvidenceNote("oldCriterion", "bekleyen eski not")
                // A failed restore must preserve the current selection and database.
                assertTrue(vm.restoreBackup("not-json").isFailure)
                assertEquals("oldStudent", vm.selectedStudent.value?.id)
                awaitState("Old note saved", { "saveState=${vm.evidenceNoteSaveState.value}" }) {
                    vm.evidenceNoteSaveState.value?.status == EvidenceNoteSaveState.Status.SAVED
                }
                val oldAssessment = repo.getRunAssessmentForStudent("oldStudent", oldRun.id)!!
                assertEquals("bekleyen eski not", repo.getScoresForAssessment(oldAssessment.id).single().evidenceNote)
                assertTrue(vm.restoreBackup(backup).isSuccess)
                awaitState("Restored selections", {
                    "class=${vm.selectedClassroom.value?.id}, rubric=${vm.selectedRubric.value?.id}, run=${vm.selectedRun.value?.id}, " +
                        "student=${vm.selectedStudent.value?.id}, group=${vm.selectedGroup.value?.id}, " +
                        "session=${vm.selectedGroupSession.value?.id}, members=${vm.currentGroupMembers.value.map { it.studentId }}"
                }) {
                    vm.selectedStudent.value?.id == "newStudent" && vm.selectedGroup.value?.id == "newGroup" &&
                        vm.selectedGroupSession.value?.id == newSession.id && vm.currentGroupMembers.value.isNotEmpty()
                }
                assertEquals("newClass", vm.selectedClassroom.value?.id)
                assertEquals("newRubric", vm.selectedRubric.value?.id)
                assertEquals(newRun.id, vm.selectedRun.value?.id)
                // Selection flows and score flows are observed independently.
                // The correct session may be selected before its Room score
                // collection emits the restored rows; wait for both emissions.
                awaitState("Restored scoring rows", {
                    "individual=${vm.currentScores.value["newCriterion"]?.points}, " +
                        "group=${vm.currentGroupScores.value["newCriterion"]?.points}, " +
                        "session=${vm.currentGroupAssessment.value?.sessionId}"
                }) {
                    vm.currentScores.value["newCriterion"]?.points == 91 &&
                        vm.currentGroupAssessment.value?.sessionId == newSession.id &&
                        vm.currentGroupScores.value["newCriterion"]?.points == 77
                }
                assertEquals(91, vm.currentScores.value["newCriterion"]?.points)
                assertEquals(newSession.id, vm.currentGroupAssessment.value?.sessionId)
                assertEquals(77, vm.currentGroupScores.value["newCriterion"]?.points)
                assertTrue(vm.currentGroupMembers.value.all { it.studentId == "newStudent" })
                assertTrue(vm.students.value.none { it.id == "oldStudent" })
                assertTrue(vm.groupTasks.value.none { it.id == "oldGroup" })
                vm.undoLastAction()
                vm.undoLastGroupAction()
                delay(200)
                assertEquals(91, repo.getRunCriterionScoresForStudent("newStudent", newRun.id)["newCriterion"])
                assertEquals(77, repo.getGroupScores(vm.currentGroupAssessment.value!!.id)
                    .first { it.criterionId == "newCriterion" }.points)
                assertNull(vm.evidenceNoteSaveState.value)
                // The old note was durable before restore and is absent from the replacement data.
                assertTrue(db.backupDao().classrooms().none { it.id == "oldClass" })
                val newAssessment = repo.getRunAssessmentForStudent("newStudent", newRun.id)!!
                assertNull(repo.getScoresForAssessment(newAssessment.id).single().evidenceNote)
            } finally { source.close() }
        } finally {
            store?.clear()
            // Keep the in-memory DB alive through the end of the instrumentation process: Room
            // may still be unwinding a just-cancelled Flow query on its query executor.
        }
    }

    @Test fun restoreToEmptySnapshotClearsSelectionsAndEveryGroupState() = runBlocking {
        val db = newDatabase()
        val repo = repository(db)
        var store: ViewModelStore? = null
        try {
            db.classroomDao().insertClassroom(ClassroomEntity("c1", 11, "A", "11-A", "2026-2027"))
            db.studentDao().insertStudent(StudentEntity("s1", "c1", "1", "Ada", "Yılmaz"))
            db.rubricDao().insertRubric(RubricEntity("r1", "Eski", "", 11, "Eski"))
            db.rubricDao().insertCriteria(listOf(CriterionEntity("k1", "r1", "Ölçüt", "", 100, 0)))
            val run = repo.createAssessmentRun("r1", "Oturum", listOf("c1"))
            repo.saveCriterionScore("c1", "s1", "r1", "k1", 55, runId = run.id)
            val pair = viewModel(db, repo); val vm = pair.first; store = pair.second
            withTimeout(8_000) { while (vm.selectedStudent.value?.id != "s1") delay(20) }
            selectRubric(vm, "r1")
            withTimeout(8_000) { while (vm.selectedRun.value?.id != run.id) delay(20) }
            val empty = BackupSnapshot(
                version = 3, classrooms = emptyList(), students = emptyList(), rubrics = emptyList(),
                criteria = emptyList(), levels = emptyList(), assessments = emptyList(), scores = emptyList(),
                groups = emptyList(), auditLogs = emptyList()
            )
            val json = Json { encodeDefaults = true; explicitNulls = true }.encodeToString(empty)
            assertTrue(vm.restoreBackup(json).isSuccess)
            withTimeout(8_000) {
                while (vm.selectedClassroom.value != null || vm.selectedStudent.value != null ||
                    vm.currentAssessment.value != null || vm.currentScores.value.isNotEmpty() ||
                    vm.groupTasks.value.isNotEmpty() || vm.groupSessions.value.isNotEmpty() ||
                    vm.groupAssessments.value.isNotEmpty() || vm.currentGroupScores.value.isNotEmpty() ||
                    vm.currentGroupMembers.value.isNotEmpty()) delay(20)
            }
            assertTrue(vm.assessmentRuns.value.isEmpty())
            assertTrue(vm.students.value.isEmpty())
            assertNull(vm.selectedRun.value)
            assertNull(vm.selectedGroup.value)
            assertNull(vm.selectedGroupSession.value)
        } finally {
            store?.clear()
            // Keep the in-memory DB alive through the end of the instrumentation process: Room
            // may still be unwinding a just-cancelled Flow query on its query executor.
        }
    }
    @Test fun groupEvidenceFlushKeepsOriginalSessionAfterSelectionChanges() = runBlocking {
        val db = newDatabase()
        val repo = repository(db)
        var store: ViewModelStore? = null
        try {
            db.classroomDao().insertClassroom(ClassroomEntity("cgroup", 11, "D", "11-D", "2026-2027"))
            db.studentDao().insertStudent(StudentEntity("sgroup", "cgroup", "1", "Ece", "Kaya"))
            db.rubricDao().insertRubric(RubricEntity("rgroup", "Drama", "", 11, "Drama"))
            db.rubricDao().insertCriteria(listOf(CriterionEntity("kgroup", "rgroup", "İçerik", "", 100, 0)))
            repo.saveGroupTask(GroupTaskEntity("ggroup", "cgroup", "", "Grup A", "[\"sgroup\"]", "[]"))
            val first = repo.createGroupSession("cgroup", "rgroup", "1. oturum")
            val second = repo.createGroupSession("cgroup", "rgroup", "2. oturum")
            val pair = viewModel(db, repo); val vm = pair.first; store = pair.second
            awaitState("group classroom loaded", { "class=${vm.selectedClassroom.value?.id}" }) {
                vm.selectedClassroom.value?.id == "cgroup"
            }
            selectRubric(vm, "rgroup")
            awaitState("group session loaded", {
                "group=${vm.selectedGroup.value?.id}; session=${vm.selectedGroupSession.value?.id}"
            }) {
                vm.selectedGroup.value?.id == "ggroup" &&
                    vm.groupCriteria.value.any { it.id == "kgroup" } &&
                    vm.groupSessions.value.any { it.id == first.id } &&
                    vm.groupSessions.value.any { it.id == second.id }
            }
            vm.selectGroupSession(first)
            vm.setGroupEvidenceNote("kgroup", "başlangıç")
            delay(130)
            vm.setGroupEvidenceNote("kgroup", "son kanıt")
            assertEquals(EvidenceNoteSaveState.Status.SAVING, vm.groupEvidenceNoteSaveState.value?.status)
            vm.selectGroupSession(second)
            vm.flushPendingEvidenceNotes()
            val assessment = requireNotNull(repo.getGroupAssessment(first.id, "ggroup"))
            val note = repo.getGroupScores(assessment.id).single()
            assertEquals("son kanıt", note.evidenceNote)
            assertNull(note.points)
            assertEquals(0, assessment.scoredCount)
            assertNull(repo.getGroupAssessment(second.id, "ggroup"))
            assertEquals(EvidenceNoteSaveState.Status.SAVED, vm.groupEvidenceNoteSaveState.value?.status)

            vm.setGroupScore("kgroup", 0)
            awaitState("second group score saved", {
                "score=${repo.getGroupAssessment(second.id, "ggroup")?.scoredCount}"
            }) {
                repo.getGroupAssessment(second.id, "ggroup")?.scoredCount == 1
            }
            assertEquals("son kanıt", repo.getGroupScores(assessment.id).single().evidenceNote)
            assertNull(repo.getGroupScores(assessment.id).single().points)
        } finally {
            store?.clear()
        }
    }

}
