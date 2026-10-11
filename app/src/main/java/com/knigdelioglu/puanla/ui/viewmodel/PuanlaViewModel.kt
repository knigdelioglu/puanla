package com.knigdelioglu.puanla.ui.viewmodel

import android.app.Application
import android.net.Uri
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.knigdelioglu.puanla.data.local.AssessmentEntity
import com.knigdelioglu.puanla.data.local.AuditLogEntity
import com.knigdelioglu.puanla.data.local.ClassroomEntity
import com.knigdelioglu.puanla.data.local.CriterionEntity
import com.knigdelioglu.puanla.data.local.CriterionLevelEntity
import com.knigdelioglu.puanla.data.local.CriterionScoreEntity
import com.knigdelioglu.puanla.data.local.GroupTaskEntity
import com.knigdelioglu.puanla.data.local.PuanlaDatabase
import com.knigdelioglu.puanla.data.local.RubricEntity
import com.knigdelioglu.puanla.data.local.StudentEntity
import com.knigdelioglu.puanla.data.local.fullName
import com.knigdelioglu.puanla.data.repository.PuanlaRepository
import com.knigdelioglu.puanla.domain.CriterionScore
import com.knigdelioglu.puanla.domain.ocr.OcrRosterParser
import com.knigdelioglu.puanla.domain.ocr.OcrStudentRow
import com.knigdelioglu.puanla.domain.summarizeScores
import com.knigdelioglu.puanla.domain.undo.UndoHistory
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class AppDestination(val label: String, val iconName: String) {
    WORKSPACE("Değerlendirme", "Grading"),
    CLASSROOMS("Sınıflar & Öğrenciler", "School"),
    OCR_IMPORT("Fotoğraftan Aktarım", "DocumentScanner"),
    GROUP_WORK("Grup Çalışmaları", "Group"),
    REPORTS("Raporlar & Analiz", "BarChart"),
    SETTINGS("Ayarlar & Güvenlik", "Settings"),
    CATALOG("Arc Bileşen Kataloğu", "Widgets")
}

data class UndoAction(
    val title: String,
    // False means a later edit superseded the original score. Never undo it blindly.
    val undoBlock: suspend () -> Boolean
)

class PuanlaViewModel(application: Application) : AndroidViewModel(application) {

    private val db = PuanlaDatabase.getInstance(application)
    val repository = PuanlaRepository(db)

    // Navigation & Global UI State
    val currentDestination = MutableStateFlow(AppDestination.WORKSPACE)
    val isCommandPaletteOpen = MutableStateFlow(false)
    val isNotificationCenterOpen = MutableStateFlow(false)
    val darkMode = MutableStateFlow<Boolean?>(null) // null = system, true = dark, false = light
    val reduceMotion = MutableStateFlow(false)

    // Active Selection State
    val classrooms: StateFlow<List<ClassroomEntity>> = repository.getAllClassroomsFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val selectedClassroom = MutableStateFlow<ClassroomEntity?>(null)

    val rubrics: StateFlow<List<RubricEntity>> = repository.getAllRubricsFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val selectedRubric = MutableStateFlow<RubricEntity?>(null)

    val students = MutableStateFlow<List<StudentEntity>>(emptyList())
    val selectedStudent = MutableStateFlow<StudentEntity?>(null)

    val criteria = MutableStateFlow<List<CriterionEntity>>(emptyList())
    val levels = MutableStateFlow<List<CriterionLevelEntity>>(emptyList())

    // Assessment for selected student & rubric
    val currentAssessment = MutableStateFlow<AssessmentEntity?>(null)
    val currentScores = MutableStateFlow<Map<String, CriterionScoreEntity>>(emptyMap())

    // Assessments for class & rubric
    val classAssessments = MutableStateFlow<List<AssessmentEntity>>(emptyList())

    // Group tasks
    val groupTasks = MutableStateFlow<List<GroupTaskEntity>>(emptyList())

    // Audit logs
    val recentLogs: StateFlow<List<AuditLogEntity>> = repository.getRecentLogsFlow(50)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Toast stack & Undo
    val toastMessages = MutableStateFlow<List<Pair<String, (() -> Unit)?>>>(emptyList())
    private val undoStack = UndoHistory<UndoAction>()
    private val pendingNoteWrites = mutableMapOf<String, Job>()
    private val scoreLocks = mutableMapOf<String, Mutex>()
    private val activeScoreJobs = mutableSetOf<Job>()
    private val restoreMutex = Mutex()
    private val isRestoring = MutableStateFlow(false)
    private fun scoreMutex(studentId: String, rubricId: String, criterionId: String): Mutex =
        scoreLocks.getOrPut("$studentId/$rubricId/$criterionId") { Mutex() }
    private var studentFlowJob: Job? = null
    private var assessmentFlowJob: Job? = null
    private var groupFlowJob: Job? = null
    private var currentAssessmentJob: Job? = null

    // OCR Import State
    val ocrStep = MutableStateFlow(0) // 0: Select, 1: Scan, 2: Verify, 3: Done
    val ocrCandidates = MutableStateFlow<List<OcrStudentRow>>(emptyList())
    val ocrIsScanning = MutableStateFlow(false)
    val ocrError = MutableStateFlow<String?>(null)
    val ocrSelectedImageUri = MutableStateFlow<Uri?>(null)
    val ocrImportedCount = MutableStateFlow(0)

    // Quick Search filter
    val studentSearchQuery = MutableStateFlow("")

    init {
        viewModelScope.launch {
            repository.initialize()

            classrooms.collect { list ->
                if (selectedClassroom.value == null && list.isNotEmpty()) {
                    selectedClassroom.value = list.first()
                }
            }
        }

        viewModelScope.launch {
            rubrics.collect { list ->
                val grade = selectedClassroom.value?.grade
                if (selectedRubric.value !in list || selectedRubric.value?.grade != grade) {
                    selectedRubric.value = list.firstOrNull { grade != null && it.grade == grade }
                }
            }
        }

        // React to selectedClassroom change
        viewModelScope.launch {
            selectedClassroom.collect { classroom ->
                if (classroom != null) {
                    if (selectedRubric.value?.grade != classroom.grade) {
                        selectedRubric.value = rubrics.value.firstOrNull { it.grade == classroom.grade }
                    }
                    loadStudentsForClassroom(classroom.id)
                    loadGroupTasks()
                    loadClassAssessments()
                } else {
                    students.value = emptyList()
                    selectedStudent.value = null
                }
            }
        }

        // React to selectedRubric change
        viewModelScope.launch {
            selectedRubric.collect { rubric ->
                if (rubric != null) {
                    criteria.value = repository.getCriteriaForRubric(rubric.id)
                    levels.value = db.rubricDao().getLevelsForRubric(rubric.id)
                    loadCurrentAssessment()
                    loadGroupTasks()
                    loadClassAssessments()
                } else {
                    criteria.value = emptyList()
                    levels.value = emptyList()
                    currentAssessment.value = null
                    currentScores.value = emptyMap()
                }
            }
        }

        // React to selectedStudent change
        viewModelScope.launch {
            selectedStudent.collect {
                loadCurrentAssessment()
            }
        }
    }

    fun selectClassroom(classroom: ClassroomEntity) {
        selectedClassroom.value = classroom
        if (selectedRubric.value?.grade != classroom.grade) {
            selectedRubric.value = rubrics.value.firstOrNull { it.grade == classroom.grade }
        }
    }

    fun selectRubric(rubric: RubricEntity) {
        val classGrade = selectedClassroom.value?.grade
        if (classGrade == null || rubric.grade != classGrade || rubric !in rubrics.value) {
            showToast("Rubrik bu sınıf düzeyine uygun değil.")
            return
        }
        selectedRubric.value = rubric
    }

    fun selectStudent(student: StudentEntity?) {
        if (student != null && student.classroomId != selectedClassroom.value?.id) {
            showToast("Öğrenci farklı sınıfa ait.")
            return
        }
        selectedStudent.value = student
    }

    private fun loadStudentsForClassroom(classroomId: String) {
        studentFlowJob?.cancel()
        students.value = emptyList()
        selectedStudent.value = null
        currentAssessment.value = null
        currentScores.value = emptyMap()
        studentFlowJob = viewModelScope.launch {
            repository.getStudentsForClassroomFlow(classroomId).collect { list ->
                if (selectedClassroom.value?.id != classroomId) return@collect
                students.value = list
                if (selectedStudent.value == null || list.none { it.id == selectedStudent.value?.id }) {
                    selectedStudent.value = list.firstOrNull()
                }
            }
        }
    }

    private fun loadClassAssessments() {
        assessmentFlowJob?.cancel()
        classAssessments.value = emptyList()
        val c = selectedClassroom.value ?: return
        val r = selectedRubric.value ?: return
        assessmentFlowJob = viewModelScope.launch {
            repository.getAssessmentsFlow(c.id, r.id).collect { list ->
                if (selectedClassroom.value?.id == c.id && selectedRubric.value?.id == r.id) {
                    classAssessments.value = list
                }
            }
        }
    }

    fun loadCurrentAssessment() {
        currentAssessmentJob?.cancel()
        currentAssessment.value = null
        currentScores.value = emptyMap()
        val s = selectedStudent.value ?: return
        val r = selectedRubric.value ?: return
        val c = selectedClassroom.value ?: return
        if (s.classroomId != c.id || r.grade != c.grade) return
        currentAssessmentJob = viewModelScope.launch {
            val assessment = repository.getAssessmentForStudent(s.id, r.id)
            val scores = if (assessment != null) repository.getScoresForAssessment(assessment.id) else emptyList()
            if (selectedStudent.value?.id == s.id &&
                selectedRubric.value?.id == r.id &&
                selectedClassroom.value?.id == c.id) {
                currentAssessment.value = assessment
                currentScores.value = scores.associateBy { it.criterionId }
            }
        }
    }

    private fun refreshSelectionIfMatches(classId: String, studentId: String, rubricId: String) {
        if (selectedClassroom.value?.id == classId &&
            selectedStudent.value?.id == studentId &&
            selectedRubric.value?.id == rubricId) {
            loadCurrentAssessment()
        }
    }

    fun setScore(criterionId: String, points: Int?, note: String? = null) {
        if (isRestoring.value) {
            showToast("Yedek geri yüklenirken puan kaydedilemez.")
            return
        }
        val student = selectedStudent.value ?: return
        val rubric = selectedRubric.value ?: return
        val classroom = selectedClassroom.value ?: return
        val criterionTitle = criteria.value.find { it.id == criterionId }?.title ?: "Ölçüt"
        // Take the lock before launch so rapid taps for one criterion are applied
        // sequentially, even across asynchronous Room transactions.
        val lock = scoreMutex(student.id, rubric.id, criterionId)
        val job = viewModelScope.launch {
            try {
                lock.withLock {
                    // Preserve the persisted observation note when changing/clearing a
                    // score. Note edits are a separate operation.
                    val assessment = repository.getAssessmentForStudent(student.id, rubric.id)
                    val stored = assessment?.let { a ->
                        repository.getScoresForAssessment(a.id).firstOrNull { it.criterionId == criterionId }
                    }
                    val oldPoints = stored?.points
                    if (oldPoints == points && stored != null) return@withLock
                    val original = repository.saveCriterionScore(
                        classroomId = classroom.id, studentId = student.id, rubricId = rubric.id,
                        criterionId = criterionId, scorePoints = points,
                        evidenceNote = stored?.evidenceNote ?: note
                    )
                    refreshSelectionIfMatches(classroom.id, student.id, rubric.id)
                    val undoId = UUID.randomUUID().toString()
                    undoStack.push(undoId, UndoAction("${student.fullName} - $criterionTitle") {
                        lock.withLock {
                            // Undo only the intended edit, never a later score or note.
                            val latestAssessment = repository.getAssessmentForStudent(student.id, rubric.id)
                            val latest = latestAssessment?.let { a ->
                                repository.getScoresForAssessment(a.id).firstOrNull { it.criterionId == criterionId }
                            }
                            if (latest?.points != points) {
                                false
                            } else {
                                repository.saveCriterionScore(
                                    classroomId = classroom.id, studentId = student.id,
                                    rubricId = rubric.id, criterionId = criterionId,
                                    scorePoints = original?.points,
                                    evidenceNote = latest?.evidenceNote
                                )
                                refreshSelectionIfMatches(classroom.id, student.id, rubric.id)
                                true
                            }
                        }
                    })
                    showToast("${student.fullName}: $criterionTitle — ${points ?: "Puan kaldırıldı"}") {
                        viewModelScope.launch { undoAction(undoId) }
                    }
                }
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (e: Exception) {
                showToast("Puan kaydedilemedi: ${e.message}")
            } finally {
                activeScoreJobs.remove(currentCoroutineContext()[Job])
            }
        }
        activeScoreJobs.add(job)
    }

    /**
     * Observations are not written on every keystroke. The pending save owns the
     * original student/rubric/criterion IDs even if the teacher changes screens.
     * It reads the latest score before updating the note, avoiding stale grades.
     */
    fun setEvidenceNote(criterionId: String, note: String) {
        if (isRestoring.value) {
            showToast("Yedek geri yüklenirken gözlem notu kaydedilemez.")
            return
        }
        val student = selectedStudent.value ?: return
        val rubric = selectedRubric.value ?: return
        val classroom = selectedClassroom.value ?: return
        val key = "${student.id}/${rubric.id}/$criterionId"
        pendingNoteWrites.remove(key)?.cancel()
        pendingNoteWrites[key] = viewModelScope.launch {
            try {
                delay(500)
                scoreMutex(student.id, rubric.id, criterionId).withLock {
                    val assessment = repository.getAssessmentForStudent(student.id, rubric.id)
                    val previous = assessment?.let {
                        repository.getScoresForAssessment(it.id).firstOrNull { score -> score.criterionId == criterionId }
                    }
                    if ((previous?.evidenceNote ?: "") != note) {
                        repository.saveCriterionScore(
                            classroomId = classroom.id, studentId = student.id,
                            rubricId = rubric.id, criterionId = criterionId,
                            scorePoints = previous?.points, evidenceNote = note
                        )
                        refreshSelectionIfMatches(classroom.id, student.id, rubric.id)
                    }
                }
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (e: Exception) {
                showToast("Gözlem notu kaydedilemedi: ${e.message}")
            } finally {
                if (pendingNoteWrites[key] == currentCoroutineContext()[Job]) pendingNoteWrites.remove(key)
            }
        }
    }

    fun nextStudent() {
        val list = students.value
        val current = selectedStudent.value ?: return
        val currentIndex = list.indexOfFirst { it.id == current.id }
        if (currentIndex != -1 && currentIndex < list.size - 1) {
            selectedStudent.value = list[currentIndex + 1]
        }
    }

    fun previousStudent() {
        val list = students.value
        val current = selectedStudent.value ?: return
        val currentIndex = list.indexOfFirst { it.id == current.id }
        if (currentIndex > 0) {
            selectedStudent.value = list[currentIndex - 1]
        }
    }

    suspend fun undoLastAction() {
        val latestId = undoStack.latestId() ?: return
        undoAction(latestId)
    }

    private suspend fun undoAction(id: String) {
        // A new write can still be in flight when the user presses Undo.
        // Never accidentally undo a different or not-yet-saved action.
        if (activeScoreJobs.any { it.isActive }) {
            showToast("Puan kaydı sürüyor; tamamlandıktan sonra geri alabilirsiniz.")
            return
        }
        val action = undoStack.takeIfLatest(id)
        if (action == null) {
            showToast("Bu geri alma işlemi güncel değil; daha yeni bir puanlama yapıldı.")
            return
        }
        try {
            if (action.undoBlock()) showToast("Geri alındı: ${action.title}")
            else showToast("Geri alınmadı: puan daha sonra değiştirilmiş.")
        } catch (cancel: CancellationException) {
            undoStack.restore(id, action)
            throw cancel
        } catch (e: Exception) {
            undoStack.restore(id, action)
            showToast("Geri alma başarısız: ${e.message}")
        }
    }

    fun showToast(message: String, onUndo: (() -> Unit)? = null) {
        toastMessages.value = listOf(message to onUndo)
    }

    fun dismissToast() {
        toastMessages.value = emptyList()
    }

    // Classroom operations
    fun addClassroom(grade: Int, section: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val name = "$grade-${section.uppercase()}"
            val exists = classrooms.value.any { it.grade == grade && it.section.equals(section, ignoreCase = true) }
            if (exists) {
                onResult(false, "$name sınıfı zaten mevcut.")
                return@launch
            }
            val newClass = repository.createClassroom(grade, section)
            selectedClassroom.value = newClass
            onResult(true, "$name sınıfı başarıyla oluşturuldu.")
        }
    }

    fun addStudent(number: String, firstName: String, lastName: String, onResult: (Boolean, String) -> Unit) {
        val classroom = selectedClassroom.value ?: return
        viewModelScope.launch {
            val result = repository.addStudent(
                classroomId = classroom.id,
                studentNumber = number,
                firstName = firstName,
                lastName = lastName
            )
            result.onSuccess { student ->
                selectedStudent.value = student
                onResult(true, "${student.fullName} sınıfa eklendi.")
            }.onFailure { err ->
                onResult(false, err.message ?: "Öğrenci eklenemedi.")
            }
        }
    }

    fun updateStudent(student: StudentEntity) {
        viewModelScope.launch {
            try {
                repository.updateStudent(student)
                showToast("${student.fullName} güncellendi.")
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (e: Exception) {
                showToast("Öğrenci güncellenemedi: ${e.message}")
            }
        }
    }

    /** Update the one field the teacher edited, not a stale whole-row copy. */
    fun updateStudentName(studentId: String, firstName: String? = null, lastName: String? = null) {
        viewModelScope.launch {
            try {
                repository.updateStudentName(studentId, firstName, lastName)
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (e: Exception) {
                showToast("Öğrenci adı/soyadı kaydedilemedi: ${e.message}")
            }
        }
    }

    fun deleteStudent(student: StudentEntity) {
        viewModelScope.launch {
            repository.deleteStudent(student)
            showToast("${student.fullName} silindi.")
        }
    }

    // OCR operations
    fun startOcrFromBitmap(bitmap: Bitmap) {
        ocrIsScanning.value = true
        ocrError.value = null
        ocrCandidates.value = emptyList()
        ocrImportedCount.value = 0
        ocrStep.value = 1
        viewModelScope.launch {
            try {
                val parsed = OcrRosterParser.processBitmap(bitmap)
                if (parsed.isEmpty()) {
                    ocrError.value = "Sütun başlıkları ve öğrenci satırları güvenilir biçimde tanınamadı. Başka fotoğraf deneyin veya öğrencileri elle ekleyin."
                    return@launch
                }
                ocrCandidates.value = parsed
                ocrStep.value = 2
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (e: Exception) {
                ocrError.value = "Fotoğraf OCR işlemi başarısız: ${e.message}"
            } finally {
                bitmap.recycle()
                ocrIsScanning.value = false
            }
        }
    }

    fun reportOcrImageError(message: String) {
        ocrError.value = message
        ocrStep.value = 1
        ocrIsScanning.value = false
    }

    fun updateOcrCandidate(index: Int, candidate: OcrStudentRow) {
        val list = ocrCandidates.value.toMutableList()
        if (index in list.indices) {
            list[index] = candidate
            ocrCandidates.value = list
        }
    }

    fun confirmOcrImport(onComplete: (Int) -> Unit) {
        val classroom = selectedClassroom.value ?: return
        viewModelScope.launch {
            try {
                val selected = ocrCandidates.value.filter { it.isApproved }
                require(selected.isNotEmpty()) { "Onaylanmış öğrenci yok." }
                require(selected.all {
                    it.studentNumber.isNotBlank() && it.studentNumber.all(Char::isDigit) &&
                        it.firstName.isNotBlank() && it.lastName.isNotBlank()
                }) { "Bazı onaylı öğrencilerde numara, ad veya soyad eksik." }
                val students = selected.map {
                    StudentEntity(
                        id = UUID.randomUUID().toString(),
                        classroomId = classroom.id,
                        studentNumber = it.studentNumber.trim(),
                        firstName = it.firstName.trim(),
                        lastName = it.lastName.trim(),
                        gender = it.gender,
                        boardingStatus = it.boardingStatus
                    )
                }
                repository.addStudents(students)
                ocrImportedCount.value = students.size
                ocrStep.value = 3
                onComplete(students.size)
                showToast("${students.size} onaylanmış öğrenci aktarıldı.")
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (e: Exception) {
                ocrError.value = "Aktarım reddedildi: ${e.message}"
                showToast(ocrError.value ?: "Aktarım başarısız.")
            }
        }
    }

    // Group work
    private fun loadGroupTasks() {
        groupFlowJob?.cancel()
        groupTasks.value = emptyList()
        val c = selectedClassroom.value ?: return
        val r = selectedRubric.value ?: return
        groupFlowJob = viewModelScope.launch {
            repository.getGroupTasksFlow(c.id, r.id).collect { list ->
                if (selectedClassroom.value?.id == c.id && selectedRubric.value?.id == r.id) {
                    groupTasks.value = list
                }
            }
        }
    }

    fun addGroupTask(title: String, memberIds: List<String>) {
        val c = selectedClassroom.value ?: return
        val r = selectedRubric.value ?: return
        viewModelScope.launch {
            val defaultChecklist = JSONArray().apply {
                put(JSONObject().put("title", "1. Konu ve Amaç Belirleme").put("checked", false))
                put(JSONObject().put("title", "2. Görev Dağılımı ve Roller").put("checked", false))
                put(JSONObject().put("title", "3. Kaynak ve Malzeme Hazırlığı").put("checked", false))
                put(JSONObject().put("title", "4. Araştırma ve İçerik Taslağı").put("checked", false))
                put(JSONObject().put("title", "5. Grup İçi Prova ve Eşgüdüm").put("checked", false))
                put(JSONObject().put("title", "6. Zaman Planı ve Takvim Uyumu").put("checked", false))
                put(JSONObject().put("title", "7. Final Sunum / Teslim Hazırlığı").put("checked", false))
            }.toString()

            val memberIdsJson = JSONArray(memberIds).toString()
            val task = GroupTaskEntity(
                id = UUID.randomUUID().toString(),
                classroomId = c.id,
                rubricId = r.id,
                title = title,
                memberStudentIdsJson = memberIdsJson,
                checklistJson = defaultChecklist
            )
            repository.saveGroupTask(task)
            showToast("Grup oluşturuldu: $title")
        }
    }

    fun toggleChecklistItem(task: GroupTaskEntity, itemIndex: Int) {
        viewModelScope.launch {
            val array = JSONArray(task.checklistJson)
            if (itemIndex in 0 until array.length()) {
                val obj = array.getJSONObject(itemIndex)
                obj.put("checked", !obj.getBoolean("checked"))
                val updated = task.copy(checklistJson = array.toString())
                repository.saveGroupTask(updated)
            }
        }
    }

    // Export CSV
    suspend fun generateCsv(): String {
        val c = selectedClassroom.value ?: return ""
        val r = selectedRubric.value ?: return ""
        return repository.generateCsvExport(c.id, r.id)
    }

    // Backup & Restore
    suspend fun createBackup(): String {
        return repository.generateFullBackupJson()
    }

    suspend fun restoreBackup(json: String): Result<Int> = restoreMutex.withLock {
        // The Settings SAF callback can run this on Dispatchers.IO. Snapshot and
        // cancel the UI-owned job collections on main, then WAIT for them to finish
        // before replacing Room tables. cancel() alone is not a completion barrier.
        val jobs = withContext(Dispatchers.Main.immediate) {
            isRestoring.value = true
            (activeScoreJobs.toList() + pendingNoteWrites.values.toList()).distinct().also { running ->
                running.forEach { it.cancel() }
                activeScoreJobs.clear()
                pendingNoteWrites.clear()
            }
        }
        try {
            jobs.joinAll()
            val result = repository.restoreBackupFromJson(json)
            if (result.isSuccess) {
                withContext(Dispatchers.Main.immediate) {
                    undoStack.clear()
                    selectedStudent.value = null
                    selectedClassroom.value = null
                    selectedRubric.value = null
                    students.value = emptyList()
                    criteria.value = emptyList()
                    currentScores.value = emptyMap()
                    currentAssessment.value = null
                    // Room flows repopulate selections after the atomic restore.
                }
            }
            result
        } finally {
            // Must reset the guard even if the restore caller gets cancelled.
            withContext(NonCancellable + Dispatchers.Main.immediate) {
                isRestoring.value = false
            }
        }
    }

}
