package com.knigdelioglu.puanla.ui.viewmodel

import android.app.Application
import android.net.Uri
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
    val undoBlock: suspend () -> Unit
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
    private val undoStack = mutableListOf<UndoAction>()

    // OCR Import State
    val ocrStep = MutableStateFlow(0) // 0: Select, 1: Scan, 2: Verify, 3: Done
    val ocrCandidates = MutableStateFlow<List<OcrStudentRow>>(emptyList())
    val ocrIsScanning = MutableStateFlow(false)
    val ocrError = MutableStateFlow<String?>(null)
    val ocrSelectedImageUri = MutableStateFlow<Uri?>(null)

    // Quick Search filter
    val studentSearchQuery = MutableStateFlow("")

    init {
        viewModelScope.launch {
            repository.initialize()
            seedSampleClassroomIfEmpty()

            classrooms.collect { list ->
                if (selectedClassroom.value == null && list.isNotEmpty()) {
                    selectedClassroom.value = list.first()
                }
            }
        }

        viewModelScope.launch {
            rubrics.collect { list ->
                if (selectedRubric.value == null && list.isNotEmpty()) {
                    selectedRubric.value = list.first()
                }
            }
        }

        // React to selectedClassroom change
        viewModelScope.launch {
            selectedClassroom.collect { classroom ->
                if (classroom != null) {
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

    private suspend fun seedSampleClassroomIfEmpty() {
        val existing = db.classroomDao().getAllClassrooms()
        if (existing.isEmpty()) {
            val sampleClass = ClassroomEntity(
                id = "class_11_A",
                grade = 11,
                section = "A",
                name = "11-A",
                academicYear = "2026-2027"
            )
            db.classroomDao().insertClassroom(sampleClass)
            val sampleStudents = listOf(
                StudentEntity("std_1", sampleClass.id, "101", "Ahmet", "YILMAZ"),
                StudentEntity("std_2", sampleClass.id, "102", "Ayşe", "KAYA"),
                StudentEntity("std_3", sampleClass.id, "103", "Mehmet", "DEMİR"),
                StudentEntity("std_4", sampleClass.id, "104", "Zeynep", "ÇELİK"),
                StudentEntity("std_5", sampleClass.id, "105", "Can", "ÖZKAN"),
                StudentEntity("std_6", sampleClass.id, "106", "Elif", "YILDIZ"),
                StudentEntity("std_7", sampleClass.id, "107", "Burak", "ŞAHİN"),
                StudentEntity("std_8", sampleClass.id, "108", "Deniz", "ARSLAN")
            )
            db.studentDao().insertStudents(sampleStudents)
        }
    }

    fun selectClassroom(classroom: ClassroomEntity) {
        selectedClassroom.value = classroom
    }

    fun selectRubric(rubric: RubricEntity) {
        selectedRubric.value = rubric
    }

    fun selectStudent(student: StudentEntity?) {
        selectedStudent.value = student
    }

    private fun loadStudentsForClassroom(classroomId: String) {
        viewModelScope.launch {
            repository.getStudentsForClassroomFlow(classroomId).collect { list ->
                students.value = list
                if (selectedStudent.value == null || list.none { it.id == selectedStudent.value?.id }) {
                    selectedStudent.value = list.firstOrNull()
                }
            }
        }
    }

    private fun loadClassAssessments() {
        val c = selectedClassroom.value ?: return
        val r = selectedRubric.value ?: return
        viewModelScope.launch {
            repository.getAssessmentsFlow(c.id, r.id).collect { list ->
                classAssessments.value = list
            }
        }
    }

    fun loadCurrentAssessment() {
        val s = selectedStudent.value ?: return
        val r = selectedRubric.value ?: return
        val c = selectedClassroom.value ?: return

        viewModelScope.launch {
            var asm = repository.getAssessmentForStudent(s.id, r.id)
            if (asm == null) {
                asm = AssessmentEntity(
                    id = "assess_${s.id}_${r.id}",
                    classroomId = c.id,
                    studentId = s.id,
                    rubricId = r.id,
                    isCompleted = false,
                    definitiveTotal = null,
                    scoredCount = 0
                )
                db.assessmentDao().insertOrUpdateAssessment(asm)
            }
            currentAssessment.value = asm
            val scoresList = repository.getScoresForAssessment(asm.id)
            currentScores.value = scoresList.associateBy { it.criterionId }
        }
    }

    fun setScore(criterionId: String, points: Int?, note: String? = null) {
        val s = selectedStudent.value ?: return
        val r = selectedRubric.value ?: return
        val c = selectedClassroom.value ?: return
        val currentCriteriaList = criteria.value
        val oldScore = currentScores.value[criterionId]

        viewModelScope.launch {
            repository.saveCriterionScore(
                classroomId = c.id,
                studentId = s.id,
                rubricId = r.id,
                criterionId = criterionId,
                scorePoints = points
            )

            // Reload assessment & scores
            val asm = repository.getAssessmentForStudent(s.id, r.id)
            currentAssessment.value = asm
            val scoresList = repository.getScoresForAssessment(asm?.id ?: "")
            currentScores.value = scoresList.associateBy { it.criterionId }

            // Register undo
            val previousPoints = oldScore?.points
            val studentName = s.fullName
            val critTitle = currentCriteriaList.find { it.id == criterionId }?.title ?: "Ölçüt"

            val undoAction = UndoAction(
                title = "$studentName - $critTitle puanlandı: ${points ?: "İptal"}"
            ) {
                setScore(criterionId, previousPoints, null)
            }
            undoStack.add(undoAction)
            showToast(undoAction.title) {
                viewModelScope.launch {
                    undoLastAction()
                }
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
        if (undoStack.isNotEmpty()) {
            val action = undoStack.removeAt(undoStack.size - 1)
            action.undoBlock()
            showToast("Geri alındı: ${action.title}")
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
            repository.updateStudent(student)
            showToast("${student.fullName} güncellendi.")
        }
    }

    fun deleteStudent(student: StudentEntity) {
        viewModelScope.launch {
            repository.deleteStudent(student)
            showToast("${student.fullName} silindi.")
        }
    }

    // OCR operations
    fun startOcrFromLines(lines: List<String>) {
        ocrIsScanning.value = true
        ocrError.value = null
        viewModelScope.launch {
            try {
                val parsed = OcrRosterParser.parseVisionText(lines.joinToString("\n"))
                ocrCandidates.value = parsed
                ocrStep.value = 2 // Move to verification
            } catch (e: Exception) {
                ocrError.value = "OCR ayrıştırma hatası: ${e.localizedMessage}"
            } finally {
                ocrIsScanning.value = false
            }
        }
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
            val studentsToInsert = ocrCandidates.value
                .filter { it.isApproved }
                .map { cand ->
                    StudentEntity(
                        id = UUID.randomUUID().toString(),
                        classroomId = classroom.id,
                        studentNumber = cand.studentNumber,
                        firstName = cand.firstName,
                        lastName = cand.lastName,
                        gender = cand.gender,
                        boardingStatus = cand.boardingStatus
                    )
                }
            repository.addStudents(studentsToInsert)
            ocrStep.value = 3
            onComplete(studentsToInsert.size)
            showToast("${studentsToInsert.size} öğrenci ${classroom.name} sınıfına aktarıldı.")
        }
    }

    // Group work
    private fun loadGroupTasks() {
        val c = selectedClassroom.value ?: return
        val r = selectedRubric.value ?: return
        viewModelScope.launch {
            repository.getGroupTasksFlow(c.id, r.id).collect { list ->
                groupTasks.value = list
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

    suspend fun restoreBackup(json: String): Result<Int> {
        return repository.restoreBackupFromJson(json)
    }
}
