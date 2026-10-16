package com.knigdelioglu.puanla.ui.viewmodel

import android.app.Application
import android.net.Uri
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.knigdelioglu.puanla.data.local.AssessmentEntity
import com.knigdelioglu.puanla.data.local.AssessmentRunEntity
import com.knigdelioglu.puanla.data.local.RunClassroomProgress
import com.knigdelioglu.puanla.data.local.AuditLogEntity
import com.knigdelioglu.puanla.data.local.ClassroomEntity
import com.knigdelioglu.puanla.data.local.CriterionEntity
import com.knigdelioglu.puanla.data.local.CriterionLevelEntity
import com.knigdelioglu.puanla.data.local.CriterionScoreEntity
import com.knigdelioglu.puanla.data.local.GroupTaskEntity
import com.knigdelioglu.puanla.data.local.GroupAssessmentSessionEntity
import com.knigdelioglu.puanla.data.local.GroupAssessmentEntity
import com.knigdelioglu.puanla.data.local.GroupCriterionScoreEntity
import com.knigdelioglu.puanla.data.local.GroupMemberResultEntity
import com.knigdelioglu.puanla.data.local.PuanlaDatabase
import com.knigdelioglu.puanla.data.local.RubricEntity
import com.knigdelioglu.puanla.data.local.StudentEntity
import com.knigdelioglu.puanla.data.local.StudentGroupGrade
import com.knigdelioglu.puanla.data.local.fullName
import com.knigdelioglu.puanla.data.repository.PuanlaRepository
import com.knigdelioglu.arc.compose.foundation.ArcAccent
import com.knigdelioglu.puanla.domain.CriterionScore
import com.knigdelioglu.puanla.domain.ocr.OcrRosterParser
import com.knigdelioglu.puanla.domain.ocr.OcrStudentRow
import com.knigdelioglu.puanla.domain.ocr.OcrImportDuplicateDetector
import com.knigdelioglu.puanla.domain.ocr.OcrRosterIdentity
import com.knigdelioglu.puanla.domain.summarizeScores
import com.knigdelioglu.puanla.domain.undo.UndoHistory
import com.knigdelioglu.puanla.domain.scoring.resolveSessionSelection
import com.knigdelioglu.puanla.domain.report.assessmentActivityByDay
import com.knigdelioglu.puanla.domain.student.StudentNameRules
import com.knigdelioglu.puanla.domain.backup.MutationCoordinator
import com.knigdelioglu.puanla.ui.preferences.AppearancePreferenceRules
import com.knigdelioglu.puanla.ui.preferences.MotionPreference
import com.knigdelioglu.puanla.ui.preferences.QuickGradeAppearance
import com.knigdelioglu.puanla.ui.preferences.QuickGradingPreference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class AppDestination(val label: String, val iconName: String) {
    WORKSPACE("Puanlama", "Grading"),
    CLASSROOMS("Sınıflar", "School"),
    OCR_IMPORT("Fotoğraftan Aktarım", "DocumentScanner"),
    GROUP_WORK("Grup Çalışmaları", "Group"),
    REPORTS("Raporlar", "BarChart"),
    SETTINGS("Ayarlar", "Settings")
}

data class UndoAction(
    val title: String,
    val contextId: String? = null,
    // False means a later edit superseded the original score. Never undo it blindly.
    val undoBlock: suspend () -> Boolean
)

data class EvidenceNoteSaveState(val key: String, val status: Status) {
    enum class Status(val label: String) {
        SAVING("Kaydediliyor…"), SAVED("Kaydedildi"), ERROR("Kaydedilemedi")
    }
}

data class ScoreSaveState(val key: String, val status: EvidenceNoteSaveState.Status)

class PuanlaViewModel internal constructor(
    application: Application,
    db: PuanlaDatabase,
    repositoryOverride: PuanlaRepository?
) : AndroidViewModel(application) {

    constructor(application: Application) : this(application, PuanlaDatabase.getInstance(application), null)

    private val db = db
    val repository = repositoryOverride ?: PuanlaRepository(db) {
        getApplication<Application>().assets.open("rubrics.json")
            .bufferedReader(Charsets.UTF_8).use { it.readText() }
    }
    private val arcPrefs = application.getSharedPreferences("arc_ui", android.content.Context.MODE_PRIVATE)

    // Navigation & Global UI State
    val currentDestination = MutableStateFlow(AppDestination.WORKSPACE)
    val isCommandPaletteOpen = MutableStateFlow(false)
    val isNotificationCenterOpen = MutableStateFlow(false)
    val darkMode = MutableStateFlow<Boolean?>(
        if (arcPrefs.contains("dark_mode")) arcPrefs.getBoolean("dark_mode", false) else null
    ) // null = system
    val reduceMotion = MutableStateFlow(
        MotionPreference.fromStoredValue(
            value = arcPrefs.getString("motion_preference", null),
            legacyBoolean = if (arcPrefs.contains("reduce_motion")) arcPrefs.getBoolean("reduce_motion", false) else null
        )
    )
    val accent = MutableStateFlow(AppearancePreferenceRules.accentFromStoredValue(arcPrefs.getString("accent", null)))
    val commandPaletteShortcut = MutableStateFlow(arcPrefs.getString("command_palette_shortcut", "Ctrl+K") ?: "Ctrl+K")
    val commandPaletteShortcutIsCustom = MutableStateFlow(arcPrefs.contains("command_palette_shortcut"))
    val quickGradingPreference = MutableStateFlow(
        QuickGradingPreference(
            qualitativeEnabled = arcPrefs.getBoolean("qualitative_quick_grading", false),
            levelCount = arcPrefs.getInt("qualitative_grading_levels", 4)
                .takeIf { it == 3 || it == 4 } ?: 4,
            appearance = QuickGradeAppearance.fromStored(
                arcPrefs.getString("qualitative_grading_appearance", null)
            )
        )
    )

    fun setQualitativeQuickGrading(enabled: Boolean) {
        quickGradingPreference.value = quickGradingPreference.value.copy(qualitativeEnabled = enabled)
        arcPrefs.edit().putBoolean("qualitative_quick_grading", enabled).apply()
    }

    fun setQuickGradingLevelCount(count: Int) {
        require(count == 3 || count == 4)
        quickGradingPreference.value = quickGradingPreference.value.copy(levelCount = count)
        arcPrefs.edit().putInt("qualitative_grading_levels", count).apply()
    }

    fun setQuickGradingAppearance(appearance: QuickGradeAppearance) {
        quickGradingPreference.value = quickGradingPreference.value.copy(appearance = appearance)
        arcPrefs.edit().putString("qualitative_grading_appearance", appearance.name).apply()
    }

    fun setDarkMode(enabled: Boolean?) {
        darkMode.value = enabled
        arcPrefs.edit().apply {
            if (enabled == null) remove("dark_mode") else putBoolean("dark_mode", enabled)
        }.apply()
    }

    fun toggleDarkMode() {
        val systemDark = (getApplication<Application>().resources.configuration.uiMode and
            android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
            android.content.res.Configuration.UI_MODE_NIGHT_YES
        setDarkMode(!(darkMode.value ?: systemDark))
    }

    fun setReducedMotion(preference: MotionPreference) {
        reduceMotion.value = preference
        arcPrefs.edit()
            .putString("motion_preference", preference.storedValue)
            .remove("reduce_motion")
            .apply()
    }

    fun setAccent(value: ArcAccent) {
        accent.value = value
        arcPrefs.edit().putString("accent", value.name).apply()
    }

    fun setCommandPaletteShortcut(value: String) {
        commandPaletteShortcut.value = value
        commandPaletteShortcutIsCustom.value = true
        arcPrefs.edit().putString("command_palette_shortcut", value).apply()
    }

    // Active Selection State
    val classrooms: StateFlow<List<ClassroomEntity>> = repository.getAllClassroomsFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val selectedClassroom = MutableStateFlow<ClassroomEntity?>(null)

    val rubrics: StateFlow<List<RubricEntity>> = repository.getAllRubricsFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val selectedRubric = MutableStateFlow<RubricEntity?>(null)
    val assessmentRuns = MutableStateFlow<List<AssessmentRunEntity>>(emptyList())
    val runClassProgress = MutableStateFlow<List<RunClassroomProgress>>(emptyList())
    val selectedRun = MutableStateFlow<AssessmentRunEntity?>(null)

    val students = MutableStateFlow<List<StudentEntity>>(emptyList())
    val selectedStudent = MutableStateFlow<StudentEntity?>(null)

    val criteria = MutableStateFlow<List<CriterionEntity>>(emptyList())
    // Group mode always uses the rubric's criteria, not an individual run snapshot.
    val groupCriteria = MutableStateFlow<List<CriterionEntity>>(emptyList())
    val levels = MutableStateFlow<List<CriterionLevelEntity>>(emptyList())

    // Assessment for selected student & rubric
    val currentAssessment = MutableStateFlow<AssessmentEntity?>(null)
    val currentScores = MutableStateFlow<Map<String, CriterionScoreEntity>>(emptyMap())
    val currentStudentGroupGrades = MutableStateFlow<List<StudentGroupGrade>>(emptyList())

    // Assessments for class & rubric
    val classAssessments = MutableStateFlow<List<AssessmentEntity>>(emptyList())

    // Group tasks
    val groupTasks = MutableStateFlow<List<GroupTaskEntity>>(emptyList())
    val selectedGroup = MutableStateFlow<GroupTaskEntity?>(null)
    val groupSessions = MutableStateFlow<List<GroupAssessmentSessionEntity>>(emptyList())
    val selectedGroupSession = MutableStateFlow<GroupAssessmentSessionEntity?>(null)
    val groupAssessments = MutableStateFlow<List<GroupAssessmentEntity>>(emptyList())
    val currentGroupAssessment = MutableStateFlow<GroupAssessmentEntity?>(null)
    val currentGroupScores = MutableStateFlow<Map<String, GroupCriterionScoreEntity>>(emptyMap())
    val currentGroupMembers = MutableStateFlow<List<GroupMemberResultEntity>>(emptyList())

    // Audit logs
    val recentLogs: StateFlow<List<AuditLogEntity>> = repository.getRecentLogsFlow(50)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val activityZone = ZoneId.systemDefault()
    private val activityToday = LocalDate.now(activityZone)
    private val activityStart = activityToday.minusDays(27).atStartOfDay(activityZone).toInstant().toEpochMilli()
    val assessmentActivity: StateFlow<List<Int>> = repository.getAssessmentActivityFlow(activityStart)
        .map { assessmentActivityByDay(it, activityToday, activityZone) }
        // Stop expensive audit-log queries when the analytics screen is no longer observed.
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), List(28) { 0 })
    private val activityDateFormat = DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("tr-TR"))
    val assessmentActivityLabels: StateFlow<List<String>> = assessmentActivity
        .map { levels ->
            levels.mapIndexed { index, level ->
                "${activityToday.minusDays(27L - index).format(activityDateFormat)}: puan değişikliği etkinliği, seviye $level/4"
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Toast stack & Undo
    val toastMessages = MutableStateFlow<List<Pair<String, (() -> Unit)?>>>(emptyList())
    val canUndoGroupAction = MutableStateFlow(false)
    val evidenceNoteSaveState = MutableStateFlow<EvidenceNoteSaveState?>(null)
    val groupEvidenceNoteSaveState = MutableStateFlow<EvidenceNoteSaveState?>(null)
    val scoreSaveState = MutableStateFlow<ScoreSaveState?>(null)
    private val scoreWriteRevisions = mutableMapOf<String, Long>()
    private fun nextScoreRevision(key: String): Long =
        (scoreWriteRevisions[key] ?: 0L).plus(1L).also {
            scoreWriteRevisions[key] = it
            scoreSaveState.value = ScoreSaveState(key, EvidenceNoteSaveState.Status.SAVING)
        }
    private fun finishScoreWrite(key: String, revision: Long, success: Boolean) {
        if (scoreWriteRevisions[key] == revision) {
            scoreSaveState.value = ScoreSaveState(key,
                if (success) EvidenceNoteSaveState.Status.SAVED else EvidenceNoteSaveState.Status.ERROR)
            scoreWriteRevisions.remove(key)
        }
    }
    private val undoStack = UndoHistory<UndoAction>()
    private val groupUndoStack = UndoHistory<UndoAction>()
    private data class PendingEvidenceNote(
        val key: String,
        val classroomId: String,
        val studentId: String,
        val rubricId: String,
        val runId: String,
        val criterionId: String,
        var note: String,
        var revision: Long = 0,
        var flushRequested: Boolean = false,
        val edits: Channel<Unit> = Channel(Channel.CONFLATED),
        val flush: Channel<Unit> = Channel(Channel.CONFLATED),
        var job: Job? = null
    )
    private val pendingNoteWrites = mutableMapOf<String, PendingEvidenceNote>()
    private data class PendingGroupEvidenceNote(
        val key: String,
        val sessionId: String,
        val groupId: String,
        val criterionId: String,
        var note: String,
        var revision: Long = 0,
        var flushRequested: Boolean = false,
        val edits: Channel<Unit> = Channel(Channel.CONFLATED),
        val flush: Channel<Unit> = Channel(Channel.CONFLATED),
        var job: Job? = null
    )
    private val pendingGroupNoteWrites = mutableMapOf<String, PendingGroupEvidenceNote>()
    // A fixed set of locks avoids retaining a mutex for every pupil/criterion ever graded.
    // Hash collisions only serialize unrelated writes; same-key writes remain ordered.
    private val scoreLocks = Array(128) { Mutex() }
    private val mutations = MutationCoordinator()
    private fun launchMutation(
        onBlocked: () -> Unit = { showToast("Yedekleme veya geri yükleme sırasında veri değiştirilemez.") },
        action: suspend CoroutineScope.() -> Unit
    ): Job? {
        val job = mutations.launchWrite(viewModelScope, action)
        if (job == null) onBlocked()
        return job
    }
    private fun scoreMutex(studentId: String, rubricId: String, criterionId: String): Mutex {
        val key = "$studentId/$rubricId/$criterionId"
        return scoreLocks[(key.hashCode() and Int.MAX_VALUE) % scoreLocks.size]
    }
    private var lastStudentSelectionContext: String? = null
    private fun lastRunStudentKey(classroomId: String, runId: String) =
        "last_student_${classroomId}_${runId}"

    private var studentFlowJob: Job? = null
    private var assessmentFlowJob: Job? = null
    private var groupFlowJob: Job? = null
    private var groupSessionFlowJob: Job? = null
    private var groupAssessmentFlowJob: Job? = null
    private var currentGroupScoreJob: Job? = null
    private var currentAssessmentJob: Job? = null
    private var studentGroupGradesJob: Job? = null

    // OCR Import State
    val ocrStep = MutableStateFlow(0) // 0: Select, 1: Scan, 2: Verify, 3: Done
    val ocrCandidates = MutableStateFlow<List<OcrStudentRow>>(emptyList())
    val ocrIsScanning = MutableStateFlow(false)
    val ocrIsImporting = MutableStateFlow(false)
    val ocrError = MutableStateFlow<String?>(null)
    val ocrSelectedImageUri = MutableStateFlow<Uri?>(null)
    val ocrImportedCount = MutableStateFlow(0)

    init {
        viewModelScope.launch {
            repository.initialize()

            classrooms.collect { list ->
                if (selectedClassroom.value == null && list.isNotEmpty()) {
                    selectedClassroom.value = list.firstOrNull {
                        it.id == arcPrefs.getString("last_classroom", null)
                    } ?: list.first()
                }
            }
        }

        viewModelScope.launch {
            rubrics.collect { list ->
                val grade = selectedClassroom.value?.grade
                if (selectedRubric.value !in list || selectedRubric.value?.grade != grade) {
                    val classroom = selectedClassroom.value
                    val lastId = classroom?.let { arcPrefs.getString("last_rubric_${it.id}", null) }
                    selectedRubric.value = list.firstOrNull { it.id == lastId && it.grade == grade }
                        ?: list.firstOrNull { grade != null && it.grade == grade }
                }
            }
        }

        // React to selectedClassroom change
        viewModelScope.launch {
            selectedClassroom.collect { classroom ->
                if (classroom != null) {
                    val savedRubricId = arcPrefs.getString("last_rubric_${classroom.id}", null)
                    val targetRubric = rubrics.value.firstOrNull {
                        it.id == savedRubricId && it.grade == classroom.grade
                    } ?: selectedRubric.value?.takeIf { it.grade == classroom.grade }
                        ?: rubrics.value.firstOrNull { it.grade == classroom.grade }
                    if (selectedRubric.value?.id != targetRubric?.id) {
                        selectedRun.value = null
                        selectedRubric.value = targetRubric
                    }
                    loadStudentsForClassroom(classroom.id)
                    loadGroupTasks()
                    loadGroupSessions()
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
                    val rubricCriteria = repository.getCriteriaForRubric(rubric.id)
                    groupCriteria.value = rubricCriteria
                    criteria.value = rubricCriteria
                    levels.value = db.rubricDao().getLevelsForRubric(rubric.id)
                    loadCurrentAssessment()
                    loadStudentGroupGrades()
                    loadGroupSessions()
                    loadClassAssessments()
                } else {
                    criteria.value = emptyList()
                    groupCriteria.value = emptyList()
                    levels.value = emptyList()
                    currentAssessment.value = null
                    currentScores.value = emptyMap()
                }
            }
        }

        viewModelScope.launch {
            selectedGroupSession.collect {
                refreshGroupUndoAvailability()
                loadGroupAssessments()
                loadCurrentGroupAssessment()
            }
        }
        viewModelScope.launch {
            selectedGroup.collect { loadCurrentGroupAssessment() }
        }

        // Run selection belongs to the (classroom, rubric) pair.
        // collectLatest cancels old subscriptions when either selection changes.
        viewModelScope.launch {
            combine(selectedClassroom, selectedRubric) { classroom, rubric -> classroom to rubric }
                .collectLatest { (classroom, rubric) ->
                    if (classroom == null || rubric == null) {
                        assessmentRuns.value = emptyList()
                        selectedRun.value = null
                    } else {
                        repository.observeRunsForClassroom(classroom.id, rubric.id).collectLatest { list ->
                            assessmentRuns.value = list
                            val current = selectedRun.value
                            val preferred = arcPrefs.getString("last_run_${classroom.id}_${rubric.id}", null)
                            selectedRun.value = resolveSessionSelection(
                                items = list,
                                currentId = current?.id,
                                preferredId = preferred,
                                idOf = { it.id },
                                isActive = { it.status == "ACTIVE" }
                            )
                        }
                    }
                }
        }

        viewModelScope.launch {
            selectedRun.collectLatest { run ->
                val classroom = selectedClassroom.value
                if (run == null) {
                    lastStudentSelectionContext = null
                } else if (classroom != null && run.rubricId == selectedRubric.value?.id) {
                    val contextKey = "${classroom.id}/${run.id}"
                    if (lastStudentSelectionContext != contextKey) {
                        lastStudentSelectionContext = contextKey
                        val preferredId = arcPrefs.getString(lastRunStudentKey(classroom.id, run.id), null)
                        // Every session has its own resume position; a new
                        // session starts with the first student.
                        val target = students.value.firstOrNull { it.id == preferredId }
                            ?: students.value.firstOrNull()
                        if (target != null && target.id != selectedStudent.value?.id) selectStudent(target)
                    }
                }
                val rubric = selectedRubric.value
                if (run != null && rubric != null && run.rubricId == rubric.id) {
                    criteria.value = repository.getCriteriaForRun(run)
                } else if (rubric != null) {
                    criteria.value = repository.getCriteriaForRubric(rubric.id)
                }
                loadCurrentAssessment()
                loadClassAssessments()
            }
        }

        viewModelScope.launch {
            selectedRun.collectLatest { run ->
                if (run == null) {
                    runClassProgress.value = emptyList()
                } else {
                    repository.observeRunProgress(run.id).collectLatest { progress ->
                        if (selectedRun.value?.id == run.id) runClassProgress.value = progress
                    }
                }
            }
        }

        // React to selectedStudent change
        viewModelScope.launch {
            selectedStudent.collect {
                loadCurrentAssessment()
                loadStudentGroupGrades()
            }
        }
    }

    fun selectClassroom(classroom: ClassroomEntity) {
        if (selectedClassroom.value?.id != classroom.id) selectedRun.value = null
        arcPrefs.edit().putString("last_classroom", classroom.id).apply()
        selectedClassroom.value = classroom
        val lastId = arcPrefs.getString("last_rubric_${classroom.id}", null)
        val targetRubric = rubrics.value.firstOrNull {
            it.id == lastId && it.grade == classroom.grade
        } ?: selectedRubric.value?.takeIf { it.grade == classroom.grade }
            ?: rubrics.value.firstOrNull { it.grade == classroom.grade }
        if (selectedRubric.value?.id != targetRubric?.id) {
            selectedRun.value = null
            selectedRubric.value = targetRubric
        }
    }

    fun selectRubric(rubric: RubricEntity) {
        val classGrade = selectedClassroom.value?.grade
        if (classGrade == null || rubric.grade != classGrade || rubric !in rubrics.value) {
            showToast("Rubrik bu sınıf düzeyine uygun değil.")
            return
        }
        if (selectedRubric.value?.id != rubric.id) selectedRun.value = null
        selectedClassroom.value?.let { classroom ->
            arcPrefs.edit().putString("last_rubric_${classroom.id}", rubric.id).apply()
        }
        selectedRubric.value = rubric
    }

    fun selectRun(run: AssessmentRunEntity) {
        if (selectedRubric.value?.id != run.rubricId || run !in assessmentRuns.value) return
        selectedRun.value = run
        selectedClassroom.value?.let { classroom ->
            arcPrefs.edit().putString("last_run_${classroom.id}_${run.rubricId}", run.id).apply()
        }
    }

    fun openRunClassroom(run: AssessmentRunEntity, classroom: ClassroomEntity) {
        if (run.rubricId != selectedRubric.value?.id || classroom.grade != selectedRubric.value?.grade) return
        arcPrefs.edit().putString("last_run_${classroom.id}_${run.rubricId}", run.id).apply()
        selectClassroom(classroom)
    }

    fun createRun(title: String, classroomIds: List<String>) {
        val rubric = selectedRubric.value ?: return
        launchMutation {
            try {
                val created = repository.createAssessmentRun(rubric.id, title, classroomIds)
                if (selectedClassroom.value?.id in classroomIds) {
                    arcPrefs.edit().putString(
                        "last_run_${selectedClassroom.value?.id}_${rubric.id}", created.id
                    ).apply()
                    selectedRun.value = created
                }
                showToast("Değerlendirme oluşturuldu.")
            } catch (cancel: CancellationException) { throw cancel }
            catch (e: Exception) { showToast("Değerlendirme oluşturulamadı: ${e.message}") }
        }
    }

    fun addClassroomsToRun(run: AssessmentRunEntity, classroomIds: List<String>) {
        launchMutation {
            try {
                repository.addClassroomsToRun(run.id, classroomIds)
                showToast("Sınıflar değerlendirmeye eklendi.")
            } catch (cancel: CancellationException) { throw cancel }
            catch (e: Exception) { showToast("Sınıf eklenemedi: ${e.message}") }
        }
    }

    fun setRunArchived(run: AssessmentRunEntity, archived: Boolean) {
        launchMutation {
            try { repository.archiveRun(run.id, archived) }
            catch (cancel: CancellationException) { throw cancel }
            catch (e: Exception) { showToast("Değerlendirme güncellenemedi: ${e.message}") }
        }
    }

    fun lastCriterionIndex(studentId: String, runId: String): Int =
        arcPrefs.getInt("criterion_${runId}_${studentId}", 0).coerceAtLeast(0)

    fun rememberCriterionIndex(studentId: String, runId: String, index: Int) {
        if (selectedStudent.value?.id != studentId || selectedRun.value?.id != runId) return
        arcPrefs.edit().putInt("criterion_${runId}_${studentId}", index.coerceAtLeast(0)).apply()
    }

    fun selectStudent(student: StudentEntity?) {
        if (student != null && student.classroomId != selectedClassroom.value?.id) {
            showToast("Öğrenci farklı sınıfa ait.")
            return
        }
        selectedStudent.value = student
        if (student != null) {
            val editor = arcPrefs.edit().putString("last_student_${student.classroomId}", student.id)
            selectedRun.value?.takeIf { run -> run.rubricId == selectedRubric.value?.id }?.let { run ->
                editor.putString(lastRunStudentKey(student.classroomId, run.id), student.id)
            }
            editor.apply()
        }
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
                    val runId = selectedRun.value?.id
                    val preferredId = runId?.let { arcPrefs.getString(lastRunStudentKey(classroomId, it), null) }
                        ?: arcPrefs.getString("last_student_$classroomId", null)
                    val restored = list.firstOrNull { it.id == preferredId } ?: list.firstOrNull()
                    selectStudent(restored)
                }
            }
        }
    }

    private fun loadClassAssessments() {
        assessmentFlowJob?.cancel()
        classAssessments.value = emptyList()
        val c = selectedClassroom.value ?: return
        val r = selectedRubric.value ?: return
        val run = selectedRun.value ?: return
        if (run.rubricId != r.id) return
        assessmentFlowJob = viewModelScope.launch {
            repository.getRunAssessmentsFlow(c.id, run.id).collect { list ->
                if (selectedClassroom.value?.id == c.id && selectedRubric.value?.id == r.id &&
                    selectedRun.value?.id == run.id) {
                    classAssessments.value = list
                }
            }
        }
    }

    private fun loadStudentGroupGrades() {
        studentGroupGradesJob?.cancel()
        currentStudentGroupGrades.value = emptyList()
        val student = selectedStudent.value ?: return
        val rubric = selectedRubric.value ?: return
        studentGroupGradesJob = viewModelScope.launch {
            repository.getStudentGroupGradesFlow(student.id, rubric.id).collect { grades ->
                if (selectedStudent.value?.id == student.id && selectedRubric.value?.id == rubric.id) {
                    currentStudentGroupGrades.value = grades
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
        val run = selectedRun.value ?: return
        if (s.classroomId != c.id || r.grade != c.grade || run.rubricId != r.id) return
        currentAssessmentJob = viewModelScope.launch {
            val assessment = repository.getRunAssessmentForStudent(s.id, run.id)
            val scores = if (assessment != null) repository.getScoresForAssessment(assessment.id) else emptyList()
            if (selectedStudent.value?.id == s.id &&
                selectedRubric.value?.id == r.id &&
                selectedRun.value?.id == run.id &&
                selectedClassroom.value?.id == c.id) {
                currentAssessment.value = assessment
                currentScores.value = scores.associateBy { it.criterionId }
            }
        }
    }

    private fun refreshSelectionIfMatches(classId: String, studentId: String, rubricId: String, runId: String) {
        if (selectedClassroom.value?.id == classId &&
            selectedStudent.value?.id == studentId &&
            selectedRubric.value?.id == rubricId &&
            selectedRun.value?.id == runId) {
            loadCurrentAssessment()
        }
    }

    fun setScore(criterionId: String, points: Int?, note: String? = null) {
        if (mutations.isPaused()) {
            showToast("Yedekleme veya geri yükleme sırasında puan kaydedilemez.")
            return
        }
        val student = selectedStudent.value ?: return
        val rubric = selectedRubric.value ?: return
        val classroom = selectedClassroom.value ?: return
        val run = selectedRun.value ?: return
        if (run.status != "ACTIVE" || run.rubricId != rubric.id) {
            showToast("Arşivlenmiş değerlendirmede puan değiştirilemez.")
            return
        }
        val scoreKey = "individual/${classroom.id}/${run.id}/${student.id}/$criterionId"
        val scoreRevision = nextScoreRevision(scoreKey)
        val criterionTitle = criteria.value.find { it.id == criterionId }?.title ?: "Ölçüt"
        // Take the lock before launch so rapid taps for one criterion are applied
        // sequentially, even across asynchronous Room transactions.
        val lock = scoreMutex(student.id, run.id, criterionId)
        val submitted = launchMutation {
            try {
                lock.withLock {
                    // Preserve the persisted observation note when changing/clearing a
                    // score. Note edits are a separate operation.
                    val assessment = repository.getRunAssessmentForStudent(student.id, run.id)
                    val stored = assessment?.let { a ->
                        repository.getScoresForAssessment(a.id).firstOrNull { it.criterionId == criterionId }
                    }
                    val oldPoints = stored?.points
                    if (oldPoints == points && stored != null) {
                        finishScoreWrite(scoreKey, scoreRevision, success = true)
                        return@withLock
                    }
                    val original = repository.saveCriterionScore(
                        classroomId = classroom.id, studentId = student.id, rubricId = rubric.id,
                        criterionId = criterionId, scorePoints = points,
                        evidenceNote = stored?.evidenceNote ?: note, runId = run.id
                    )
                    refreshSelectionIfMatches(classroom.id, student.id, rubric.id, run.id)
                    val undoId = UUID.randomUUID().toString()
                    undoStack.push(undoId, UndoAction(
                        title = "${student.fullName} - $criterionTitle",
                        contextId = "${classroom.id}/${run.id}"
                    ) {
                        lock.withLock {
                            // Undo only the intended edit, never a later score or note.
                            val latestAssessment = repository.getRunAssessmentForStudent(student.id, run.id)
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
                                    evidenceNote = latest?.evidenceNote, runId = run.id
                                )
                                refreshSelectionIfMatches(classroom.id, student.id, rubric.id, run.id)
                                true
                            }
                        }
                    })
                    finishScoreWrite(scoreKey, scoreRevision, success = true)
                    // Undo remains available from the scoring toolbar.
                }
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (e: Exception) {
                finishScoreWrite(scoreKey, scoreRevision, success = false)
                showToast("Puan kaydedilemedi: ${e.message}")
            }
        }
        if (submitted == null) finishScoreWrite(scoreKey, scoreRevision, success = false)
    }

    /**
     * Observations are not written on every keystroke. The pending save owns the
     * original student/rubric/criterion IDs even if the teacher changes screens.
     * It reads the latest score before updating the note, avoiding stale grades.
     */
    fun setEvidenceNote(criterionId: String, note: String) {
        if (mutations.isPaused()) {
            showToast("Yedekleme veya geri yükleme sırasında gözlem notu kaydedilemez.")
            return
        }
        val student = selectedStudent.value ?: return
        val rubric = selectedRubric.value ?: return
        val classroom = selectedClassroom.value ?: return
        val run = selectedRun.value ?: return
        if (run.status != "ACTIVE" || run.rubricId != rubric.id) return
        val key = "${student.id}/${run.id}/$criterionId"
        val pending = pendingNoteWrites[key] ?: PendingEvidenceNote(
            key, classroom.id, student.id, rubric.id, run.id, criterionId, note
        ).also { pendingNoteWrites[key] = it }
        pending.note = note
        pending.revision++
        pending.flushRequested = false
        pending.edits.trySend(Unit)
        evidenceNoteSaveState.value = EvidenceNoteSaveState(key, EvidenceNoteSaveState.Status.SAVING)
        startEvidenceNoteWorker(pending)
    }

    private fun startEvidenceNoteWorker(pending: PendingEvidenceNote) {
        if (pending.job?.isActive == true) return
        val job = launchMutation(onBlocked = {
            if (pendingNoteWrites[pending.key] === pending) {
                evidenceNoteSaveState.value = EvidenceNoteSaveState(pending.key, EvidenceNoteSaveState.Status.ERROR)
                showToast("Yedekleme veya geri yükleme sırasında gözlem notu kaydedilemedi.")
            }
        }) {
            while (pendingNoteWrites[pending.key] === pending) {
                if (!pending.flushRequested) {
                    // Either a new edit restarts the quiet period or lifecycle flush bypasses it.
                    val interrupted = withTimeoutOrNull(500) {
                        select<Unit> {
                            pending.edits.onReceive { }
                            pending.flush.onReceive { }
                        }
                        true
                    } ?: false
                    if (interrupted && !pending.flushRequested) continue
                }
                pending.flushRequested = false
                val revision = pending.revision
                val note = pending.note
                try {
                    scoreMutex(pending.studentId, pending.runId, pending.criterionId).withLock {
                        if (pendingNoteWrites[pending.key] === pending && pending.revision == revision) {
                            val assessment = repository.getRunAssessmentForStudent(pending.studentId, pending.runId)
                            val previous = assessment?.let {
                                repository.getScoresForAssessment(it.id).firstOrNull { score -> score.criterionId == pending.criterionId }
                            }
                            if ((previous?.evidenceNote ?: "") != note) {
                                repository.saveCriterionScore(
                                    classroomId = pending.classroomId, studentId = pending.studentId,
                                    rubricId = pending.rubricId, criterionId = pending.criterionId,
                                    scorePoints = previous?.points, evidenceNote = note, runId = pending.runId
                                )
                            }
                            if (pending.revision == revision && pendingNoteWrites[pending.key] === pending) {
                                refreshSelectionIfMatches(pending.classroomId, pending.studentId, pending.rubricId, pending.runId)
                                evidenceNoteSaveState.value = EvidenceNoteSaveState(pending.key, EvidenceNoteSaveState.Status.SAVED)
                                pendingNoteWrites.remove(pending.key)
                                pending.edits.close()
                                pending.flush.close()
                                return@launchMutation
                            }
                        }
                    }
                } catch (cancel: CancellationException) {
                    throw cancel
                } catch (e: Exception) {
                    if (pendingNoteWrites[pending.key] === pending) {
                        evidenceNoteSaveState.value = EvidenceNoteSaveState(pending.key, EvidenceNoteSaveState.Status.ERROR)
                        showToast("Gözlem notu kaydedilemedi: ${e.message}")
                    }
                    return@launchMutation
                }
                // A newer edit arrived during the write; serialize it after its debounce.
            }
        }
        if (job == null) return
        pending.job = job
        job.invokeOnCompletion { if (pending.job === job) pending.job = null }
    }

    /** Lifecycle flush bypasses debounce and joins the coordinator-tracked writes. */
    suspend fun flushPendingEvidenceNotes() {
        val pending = pendingNoteWrites.values.toList()
        pending.forEach {
            it.flushRequested = true
            it.flush.trySend(Unit)
            startEvidenceNoteWorker(it)
        }
        flushPendingGroupEvidenceNotes()
        pending.mapNotNull { it.job }.joinAll()
    }

    fun nextStudent() {
        val list = students.value
        val current = selectedStudent.value ?: return
        val currentIndex = list.indexOfFirst { it.id == current.id }
        if (currentIndex != -1 && currentIndex < list.size - 1) {
            selectStudent(list[currentIndex + 1])
        }
    }

    fun previousStudent() {
        val list = students.value
        val current = selectedStudent.value ?: return
        val currentIndex = list.indexOfFirst { it.id == current.id }
        if (currentIndex > 0) {
            selectStudent(list[currentIndex - 1])
        }
    }

    suspend fun undoLastAction() {
        val action = undoStack.latest() ?: return
        val classroomId = selectedClassroom.value?.id ?: return
        val runId = selectedRun.value?.id ?: return
        if (action.contextId != "$classroomId/$runId") {
            showToast("Geri almak için son puanlanan sınıf ve değerlendirmeyi seçin.")
            return
        }
        val latestId = undoStack.latestId() ?: return
        undoAction(latestId)
    }

    suspend fun undoLastGroupAction() {
        val action = groupUndoStack.latest() ?: return
        if (action.contextId != selectedGroupSession.value?.id) {
            refreshGroupUndoAvailability()
            showToast("Geri almak için puanlanan grup oturumunu seçin.")
            return
        }
        val latestId = groupUndoStack.latestId() ?: return
        undoAction(latestId, groupUndoStack)
    }

    private fun refreshGroupUndoAvailability() {
        canUndoGroupAction.value = groupUndoStack.latest()?.contextId == selectedGroupSession.value?.id
    }

    private suspend fun undoAction(id: String, history: UndoHistory<UndoAction> = undoStack) {
        val undoJob = currentCoroutineContext()[Job]
        if (mutations.isPaused()) {
            showToast("Yedekleme veya geri yükleme sırasında puan geri alınamaz.")
            return
        }
        // A write can still be in flight when Undo is pressed.
        if (mutations.hasPendingWrites()) {
            showToast("Puan kaydı sürüyor; tamamlandıktan sonra geri alabilirsiniz.")
            return
        }
        val action = history.takeIfLatest(id)
        if (action == null) {
            showToast("Bu geri alma işlemi güncel değil; daha yeni bir puanlama yapıldı.")
            return
        }
        if (history === groupUndoStack) refreshGroupUndoAvailability()
        // Undo itself is a tracked mutation; restore cannot race it.
        if (undoJob == null || !mutations.track(undoJob)) {
            history.restore(id, action)
            if (history === groupUndoStack) refreshGroupUndoAvailability()
            showToast("Yedekleme veya geri yükleme sırasında geri alma yapılamaz.")
            return
        }
        try {
            if (action.undoBlock()) showToast("Geri alındı: ${action.title}")
            else showToast("Geri alınmadı: puan daha sonra değiştirilmiş.")
        } catch (cancel: CancellationException) {
            history.restore(id, action)
            if (history === groupUndoStack) refreshGroupUndoAvailability()
            throw cancel
        } catch (e: Exception) {
            history.restore(id, action)
            if (history === groupUndoStack) refreshGroupUndoAvailability()
            showToast("Geri alma başarısız: ${e.message}")
        } finally {
            mutations.untrack(undoJob)
        }
    }

    fun showToast(message: String, onUndo: (() -> Unit)? = null) {
        toastMessages.value = listOf(message to onUndo)
    }

    fun dismissToast() {
        toastMessages.value = emptyList()
    }

    // Classroom operations: all writes are registered BEFORE launch, so a
    // concurrently requested backup or restore cannot miss them.
    fun addClassroom(grade: Int, section: String, onResult: (Boolean, String) -> Unit) {
        launchMutation(onBlocked = { onResult(false, "Yedekleme/geri yükleme sırasında sınıf eklenemez.") }) {
            try {
                val name = "$grade-${section.uppercase()}"
                if (classrooms.value.any { it.grade == grade && it.section.equals(section, ignoreCase = true) }) {
                    onResult(false, "$name sınıfı zaten mevcut.")
                } else {
                    val created = repository.createClassroom(grade, section)
                    selectedClassroom.value = created
                    onResult(true, "$name sınıfı başarıyla oluşturuldu.")
                }
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (e: Exception) {
                onResult(false, e.message ?: "Sınıf oluşturulamadı.")
            }
        }
    }

    fun updateClassroomDetails(
        classroom: ClassroomEntity,
        section: String,
        academicYear: String,
        onResult: (Boolean, String) -> Unit
    ) {
        launchMutation(onBlocked = { onResult(false, "Yedekleme veya geri yükleme sırasında sınıf düzenlenemez.") }) {
            try {
                val updated = repository.updateClassroomDetails(classroom, section, academicYear)
                if (selectedClassroom.value?.id == classroom.id) selectedClassroom.value = updated
                onResult(true, "${updated.name} sınıfı güncellendi.")
            } catch (cancel: CancellationException) { throw cancel }
            catch (e: Exception) { onResult(false, e.message ?: "Sınıf güncellenemedi.") }
        }
    }

    fun deleteClassroom(classroom: ClassroomEntity, onResult: (Boolean, String) -> Unit) {
        launchMutation(onBlocked = { onResult(false, "Yedekleme veya geri yükleme sırasında sınıf silinemez.") }) {
            try {
                repository.deleteClassroom(classroom)
                if (selectedClassroom.value?.id == classroom.id) {
                    selectedClassroom.value = null
                    selectedStudent.value = null
                    currentAssessment.value = null
                    currentScores.value = emptyMap()
                }
                onResult(true, "${classroom.name} sınıfı silindi.")
            } catch (cancel: CancellationException) { throw cancel }
            catch (e: Exception) { onResult(false, e.message ?: "Sınıf silinemedi.") }
        }
    }

    fun addStudent(number: String, firstName: String, lastName: String, onResult: (Boolean, String) -> Unit) {
        val classroom = selectedClassroom.value
        if (classroom == null) {
            onResult(false, "Önce bir sınıf seçin.")
            return
        }
        launchMutation(onBlocked = { onResult(false, "Yedekleme/geri yükleme sırasında öğrenci eklenemez.") }) {
            try {
                val result = repository.addStudent(
                    classroomId = classroom.id,
                    studentNumber = number,
                    firstName = firstName,
                    lastName = lastName
                )
                result.onSuccess { student ->
                    if (selectedClassroom.value?.id == classroom.id) selectedStudent.value = student
                    onResult(true, "${student.fullName} sınıfa eklendi.")
                }.onFailure { err ->
                    onResult(false, err.message ?: "Öğrenci eklenemedi.")
                }
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (e: Exception) {
                onResult(false, e.message ?: "Öğrenci eklenemedi.")
            }
        }
    }

    fun updateStudent(student: StudentEntity) {
        launchMutation {
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

    fun updateStudentName(studentId: String, firstName: String? = null, lastName: String? = null) {
        launchMutation {
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
        launchMutation {
            try {
                repository.deleteStudent(student)
                showToast("${student.fullName} silindi.")
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (e: Exception) {
                showToast("Öğrenci silinemedi: ${e.message}")
            }
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
            val previous = list[index]
            val identityChanged = previous.studentNumber != candidate.studentNumber ||
                previous.firstName != candidate.firstName || previous.lastName != candidate.lastName
            list[index] = if (identityChanged) {
                candidate.copy(isConfirmedDistinctFromPossibleMatch = false)
            } else candidate
            if (identityChanged) {
                list.indices.filter { it != index && list[it].isConfirmedDistinctFromPossibleMatch }.forEach { otherIndex ->
                    list[otherIndex] = list[otherIndex].copy(isConfirmedDistinctFromPossibleMatch = false)
                }
            }
            ocrCandidates.value = list
        }
    }

    fun bulkSetOcrApproval(approveReliable: Boolean) {
        val current = ocrCandidates.value
        if (!approveReliable) {
            ocrCandidates.value = current.map { it.copy(isApproved = false) }
            showToast("OCR satırlarının onayları kaldırıldı.")
            return
        }
        val classroomId = selectedClassroom.value?.id ?: return
        val roster = students.value.map { OcrRosterIdentity(it.studentNumber, it.firstName, it.lastName) }
        val reliable = OcrImportDuplicateDetector.reliableCandidateIndices(current, roster)
        if (reliable.isEmpty()) {
            showToast("Toplu onaylanabilecek güvenilir satır bulunamadı.")
            return
        }
        val proposed = current.mapIndexed { index, candidate ->
            if (index in reliable) candidate.copy(isApproved = true) else candidate
        }
        val validation = OcrImportDuplicateDetector.validateSelectedCandidates(proposed, roster)
        if (!validation.canImport) {
            showToast("Toplu onay çakışmaları aşamaz. Belirsiz veya çakışmalı satırları tek tek düzeltin.")
            return
        }
        if (selectedClassroom.value?.id == classroomId) {
            ocrCandidates.value = proposed
            showToast("${reliable.size} güvenilir OCR satırı onaylandı.")
        }
    }

    fun confirmOcrImport(onComplete: (Int) -> Unit) {
        // Ignore repeated confirmations while the database write is in flight.
        if (ocrStep.value != 2 || ocrIsImporting.value) return
        val classroom = selectedClassroom.value ?: return
        ocrIsImporting.value = true
        ocrError.value = null
        launchMutation(onBlocked = {
            ocrIsImporting.value = false
            ocrError.value = "Yedekleme/geri yükleme sırasında öğrenci aktarılamaz."
            showToast(ocrError.value ?: "Aktarım ertelendi.")
        }) {
            try {
                val candidates = ocrCandidates.value
                val selected = candidates.filter { it.isApproved }
                require(selected.isNotEmpty()) { "Onaylanmış öğrenci yok." }
                val currentRoster = repository.getStudentsForClassroom(classroom.id).map {
                    OcrRosterIdentity(it.studentNumber, it.firstName, it.lastName)
                }
                val duplicateValidation = OcrImportDuplicateDetector.validateSelectedCandidates(candidates, currentRoster)
                require(duplicateValidation.sourceRowConflicts.isEmpty()) {
                    "Aynı fotoğraf satırından iki öğrenci aktarılamaz. Yinelenen okumayı aktarımdan çıkarın."
                }
                require(duplicateValidation.schoolNumberConflicts.isEmpty()) {
                    "Aynı okul numarasına sahip satır var. Okul numarasını düzeltin veya satırı aktarımdan çıkarın."
                }
                require(duplicateValidation.unconfirmedPossibleMatches.isEmpty()) {
                    "Olası mükerrer satır için ayrı bir öğrenci olduğunu ayrıca onaylayın veya satırı aktarımdan çıkarın."
                }
                require(selected.all {
                    it.studentNumber.isNotBlank() && it.studentNumber.all(Char::isDigit) &&
                        it.firstName.isNotBlank() && it.lastName.isNotBlank()
                }) { "Bazı onaylı öğrencilerde numara, ad veya soyad eksik." }
                val students = selected.map {
                    StudentEntity(
                        id = UUID.randomUUID().toString(),
                        classroomId = classroom.id,
                        studentNumber = it.studentNumber.trim(),
                        firstName = StudentNameRules.firstName(it.firstName),
                        lastName = StudentNameRules.lastName(it.lastName),
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
            } finally {
                ocrIsImporting.value = false
            }
        }
    }


    // Group roster is classroom-owned; its legacy rubricId is never used as a filter.
    private fun loadGroupTasks() {
        groupFlowJob?.cancel()
        groupTasks.value = emptyList()
        selectedGroup.value = null
        val classroom = selectedClassroom.value ?: return
        val preferredGroupId = arcPrefs.getString("last_group_${classroom.id}", null)
        groupFlowJob = viewModelScope.launch {
            repository.getGroupTasksFlow(classroom.id).collect { list ->
                if (selectedClassroom.value?.id == classroom.id) {
                    groupTasks.value = list
                    if (list.none { it.id == selectedGroup.value?.id }) {
                        selectedGroup.value = list.firstOrNull { it.id == preferredGroupId }
                            ?: list.firstOrNull()
                    }
                }
            }
        }
    }

    fun selectGroup(group: GroupTaskEntity) {
        if (group.classroomId == selectedClassroom.value?.id) {
            arcPrefs.edit().putString("last_group_${group.classroomId}", group.id).apply()
            selectedGroup.value = group
        }
    }

    private fun loadGroupSessions() {
        groupSessionFlowJob?.cancel()
        groupSessions.value = emptyList()
        selectedGroupSession.value = null
        val classroom = selectedClassroom.value ?: return
        val rubric = selectedRubric.value ?: return
        val preferredSessionId = arcPrefs.getString("last_group_session_${classroom.id}_${rubric.id}", null)
        groupSessionFlowJob = viewModelScope.launch {
            repository.getGroupSessionsFlow(classroom.id, rubric.id).collect { list ->
                if (selectedClassroom.value?.id == classroom.id && selectedRubric.value?.id == rubric.id) {
                    groupSessions.value = list
                    if (list.none { it.id == selectedGroupSession.value?.id }) {
                        selectedGroupSession.value = resolveSessionSelection(
                            items = list,
                            currentId = selectedGroupSession.value?.id,
                            preferredId = preferredSessionId,
                            idOf = { it.id }
                        )
                    }
                }
            }
        }
    }

    fun selectGroupSession(session: GroupAssessmentSessionEntity) {
        if (session.classroomId == selectedClassroom.value?.id &&
            session.rubricId == selectedRubric.value?.id && session in groupSessions.value) {
            arcPrefs.edit().putString("last_group_session_${session.classroomId}_${session.rubricId}", session.id).apply()
            selectedGroupSession.value = session
        }
    }

    fun createGroupSession(title: String) {
        val classroom = selectedClassroom.value ?: return
        val rubric = selectedRubric.value ?: return
        launchMutation {
            try {
                val created = repository.createGroupSession(classroom.id, rubric.id, title)
                if (selectedClassroom.value?.id == classroom.id && selectedRubric.value?.id == rubric.id) {
                    arcPrefs.edit().putString("last_group_session_${classroom.id}_${rubric.id}", created.id).apply()
                    selectedGroupSession.value = created
                }
            } catch (cancel: CancellationException) { throw cancel }
            catch (e: Exception) { showToast("Oturum oluşturulamadı: " + e.message) }
        }
    }

    private fun loadGroupAssessments() {
        groupAssessmentFlowJob?.cancel()
        groupAssessments.value = emptyList()
        val session = selectedGroupSession.value ?: return
        groupAssessmentFlowJob = viewModelScope.launch {
            repository.getGroupAssessmentsFlow(session.id).collect { assessments ->
                if (selectedGroupSession.value?.id == session.id) {
                    groupAssessments.value = assessments
                    loadCurrentGroupAssessment()
                }
            }
        }
    }

    private fun loadCurrentGroupAssessment() {
        currentGroupScoreJob?.cancel()
        currentGroupAssessment.value = null
        currentGroupScores.value = emptyMap()
        currentGroupMembers.value = emptyList()
        val session = selectedGroupSession.value ?: return
        val group = selectedGroup.value ?: return
        currentGroupScoreJob = viewModelScope.launch {
            val found = groupAssessments.value.firstOrNull { it.sessionId == session.id && it.groupId == group.id }
            val scores = found?.let { repository.getGroupScores(it.id) }.orEmpty()
            val members = found?.let { repository.getGroupMemberResults(it.id) }.orEmpty()
            if (selectedGroupSession.value?.id == session.id && selectedGroup.value?.id == group.id) {
                currentGroupAssessment.value = found
                currentGroupScores.value = scores.associateBy { it.criterionId }
                currentGroupMembers.value = members
            }
        }
    }

    fun addGroupTask(title: String, memberIds: List<String>) {
        val classroom = selectedClassroom.value ?: return
        launchMutation {
            try {
                require(memberIds.isNotEmpty() && memberIds.distinct().size == memberIds.size)
                val validStudentIds = repository.getStudentsForClassroom(classroom.id).map { it.id }.toSet()
                require(memberIds.all { it in validStudentIds })
                val task = GroupTaskEntity(
                    id = UUID.randomUUID().toString(), classroomId = classroom.id,
                    rubricId = "", title = title.trim(),
                    memberStudentIdsJson = JSONArray(memberIds).toString(), checklistJson = "[]"
                )
                require(task.title.isNotBlank())
                repository.saveGroupTask(task)
                if (selectedClassroom.value?.id == classroom.id) selectedGroup.value = task
            } catch (cancel: CancellationException) { throw cancel }
            catch (e: Exception) { showToast("Grup oluşturulamadı: " + e.message) }
        }
    }

    fun updateGroupTask(task: GroupTaskEntity, title: String, memberIds: List<String>) {
        val classroom = selectedClassroom.value ?: return
        if (task.classroomId != classroom.id) return
        launchMutation {
            try {
                require(title.isNotBlank()) { "Grup adı boş olamaz." }
                require(memberIds.isNotEmpty() && memberIds.distinct().size == memberIds.size)
                val updated = task.copy(
                    title = title.trim(),
                    memberStudentIdsJson = JSONArray(memberIds).toString()
                )
                repository.saveGroupTask(updated)
                showToast("Grup güncellendi. Geçmiş değerlendirmelerdeki üye kayıtları korunuyor.")
            } catch (cancel: CancellationException) { throw cancel }
            catch (e: Exception) { showToast("Grup güncellenemedi: ${e.message}") }
        }
    }

    fun deleteGroupTask(task: GroupTaskEntity) {
        launchMutation {
            try {
                repository.deleteGroupTask(task)
                showToast("Grup silindi.")
            } catch (cancel: CancellationException) { throw cancel }
            catch (e: Exception) { showToast("Grup silinemedi: ${e.message}") }
        }
    }

    fun syncGroupMemberships() {
        val session = selectedGroupSession.value ?: return
        launchMutation {
            try {
                val count = repository.syncGroupSessionMembership(session.id)
                if (selectedGroupSession.value?.id == session.id) {
                    loadCurrentGroupAssessment()
                }
                showToast(
                    if (count == 0) "Üyelikler zaten güncel."
                    else "${count} grubun oturumdaki üye kayıtları güncellendi."
                )
            } catch (cancel: CancellationException) { throw cancel }
            catch (e: Exception) { showToast("Üyelikler eşitlenemedi: ${e.message}") }
        }
    }

    fun createRandomGroups(averageSize: Int) {
        val classroom = selectedClassroom.value ?: return
        launchMutation {
            try {
                val created = repository.createRandomGroups(classroom.id, averageSize)
                if (selectedClassroom.value?.id == classroom.id) {
                    selectedGroup.value = created.firstOrNull()
                    showToast("${created.size} rastgele grup oluşturuldu.")
                }
            } catch (cancel: CancellationException) { throw cancel }
            catch (e: Exception) { showToast("Rastgele gruplar oluşturulamadı: " + e.message) }
        }
    }

    /**
     * Group evidence is bound to the original session, group and criterion,
     * even if the teacher switches modes while the debounce is pending.
     */
    fun setGroupEvidenceNote(criterionId: String, note: String) {
        if (mutations.isPaused()) {
            showToast("Yedekleme veya geri yükleme sırasında gözlem notu kaydedilemez.")
            return
        }
        val group = selectedGroup.value ?: return
        val session = selectedGroupSession.value ?: return
        val rubric = selectedRubric.value ?: return
        if (session.rubricId != rubric.id || group.classroomId != session.classroomId ||
            selectedClassroom.value?.id != session.classroomId ||
            groupCriteria.value.none { it.id == criterionId }) return
        val key = "group/${session.id}/${group.id}/$criterionId"
        val pending = pendingGroupNoteWrites[key] ?: PendingGroupEvidenceNote(
            key, session.id, group.id, criterionId, note
        ).also { pendingGroupNoteWrites[key] = it }
        pending.note = note
        pending.revision++
        pending.flushRequested = false
        pending.edits.trySend(Unit)
        groupEvidenceNoteSaveState.value =
            EvidenceNoteSaveState(key, EvidenceNoteSaveState.Status.SAVING)
        startGroupEvidenceNoteWorker(pending)
    }

    private fun startGroupEvidenceNoteWorker(pending: PendingGroupEvidenceNote) {
        if (pending.job?.isActive == true) return
        val job = launchMutation(onBlocked = {
            if (pendingGroupNoteWrites[pending.key] === pending) {
                groupEvidenceNoteSaveState.value =
                    EvidenceNoteSaveState(pending.key, EvidenceNoteSaveState.Status.ERROR)
            }
        }) {
            while (pendingGroupNoteWrites[pending.key] === pending) {
                if (!pending.flushRequested) {
                    val interrupted = withTimeoutOrNull(500) {
                        select<Unit> {
                            pending.edits.onReceive { }
                            pending.flush.onReceive { }
                        }
                        true
                    } ?: false
                    if (interrupted && !pending.flushRequested) continue
                }
                pending.flushRequested = false
                val revision = pending.revision
                val note = pending.note
                try {
                    scoreMutex(pending.groupId, pending.sessionId, pending.criterionId).withLock {
                        if (pendingGroupNoteWrites[pending.key] === pending &&
                            pending.revision == revision) {
                            repository.saveGroupEvidenceNote(
                                pending.sessionId, pending.groupId, pending.criterionId, note
                            )
                            if (pending.revision == revision && pendingGroupNoteWrites[pending.key] === pending) {
                                if (selectedGroup.value?.id == pending.groupId &&
                                    selectedGroupSession.value?.id == pending.sessionId) {
                                    loadCurrentGroupAssessment()
                                }
                                groupEvidenceNoteSaveState.value =
                                    EvidenceNoteSaveState(pending.key, EvidenceNoteSaveState.Status.SAVED)
                                pendingGroupNoteWrites.remove(pending.key)
                                pending.edits.close()
                                pending.flush.close()
                                return@launchMutation
                            }
                        }
                    }
                } catch (cancel: CancellationException) {
                    throw cancel
                } catch (e: Exception) {
                    if (pendingGroupNoteWrites[pending.key] === pending) {
                        groupEvidenceNoteSaveState.value =
                            EvidenceNoteSaveState(pending.key, EvidenceNoteSaveState.Status.ERROR)
                        showToast("Grup gözlem notu kaydedilemedi: ${e.message}")
                    }
                    return@launchMutation
                }
            }
        }
        if (job == null) return
        pending.job = job
        job.invokeOnCompletion { if (pending.job === job) pending.job = null }
    }

    suspend fun flushPendingGroupEvidenceNotes() {
        val pending = pendingGroupNoteWrites.values.toList()
        pending.forEach {
            it.flushRequested = true
            it.flush.trySend(Unit)
            startGroupEvidenceNoteWorker(it)
        }
        pending.mapNotNull { it.job }.joinAll()
    }

    fun setGroupScore(criterionId: String, points: Int?) {
        if (mutations.isPaused()) {
            showToast("Yedekleme veya geri yükleme sırasında puan kaydedilemez.")
            return
        }
        val group = selectedGroup.value ?: return
        val session = selectedGroupSession.value ?: return
        val rubric = selectedRubric.value ?: return
        if (session.rubricId != rubric.id) return
        if (session.classroomId != selectedClassroom.value?.id || group.classroomId != session.classroomId) return
        val scoreKey = "group/${session.id}/${group.id}/$criterionId"
        val scoreRevision = nextScoreRevision(scoreKey)
        val lock = scoreMutex(group.id, session.id, criterionId)
        val submitted = launchMutation {
            try {
                lock.withLock {
                    val before = repository.getGroupAssessment(session.id, group.id)
                    val oldScore = before?.let { repository.getGroupScores(it.id)
                        .firstOrNull { score -> score.criterionId == criterionId } }
                    if (oldScore?.points != points) {
                        repository.scoreGroup(session.id, group.id, criterionId, points)
                        val title = groupCriteria.value.firstOrNull { it.id == criterionId }?.title ?: "Ölçüt"
                        val undoId = UUID.randomUUID().toString()
                        groupUndoStack.push(undoId, UndoAction("${group.title} · $title", contextId = session.id) {
                            lock.withLock {
                                val undone = repository.undoGroupScoreIfCurrent(
                                    sessionId = session.id,
                                    groupId = group.id,
                                    criterionId = criterionId,
                                    expectedCurrentPoints = points,
                                    restorePoints = oldScore?.points,
                                    removeAssessmentIfEmpty = before == null
                                )
                                if (!undone) return@withLock false
                                if (selectedGroup.value?.id == group.id && selectedGroupSession.value?.id == session.id) {
                                    loadCurrentGroupAssessment()
                                }
                                true
                            }
                        })
                        refreshGroupUndoAvailability()
                    }
                    if (selectedGroup.value?.id == group.id && selectedGroupSession.value?.id == session.id) {
                        loadCurrentGroupAssessment()
                    }
                    finishScoreWrite(scoreKey, scoreRevision, success = true)
                }
            } catch (cancel: CancellationException) { throw cancel }
            catch (e: Exception) {
                finishScoreWrite(scoreKey, scoreRevision, success = false)
                showToast("Grup puanı kaydedilemedi: " + e.message)
            }
        }
        if (submitted == null) finishScoreWrite(scoreKey, scoreRevision, success = false)
    }

    fun toggleChecklistItem(task: GroupTaskEntity, itemIndex: Int) {
        launchMutation {
            try { repository.toggleGroupChecklistItem(task.id, itemIndex) }
            catch (cancel: CancellationException) { throw cancel }
            catch (e: Exception) { showToast("Grup görevi güncellenemedi: " + e.message) }
        }
    }

    // Export CSV
    suspend fun generateCsv(): String {
        val c = selectedClassroom.value ?: return ""
        val r = selectedRubric.value ?: return ""
        val run = selectedRun.value ?: return ""
        return repository.generateCsvExport(c.id, r.id, run.id)
    }

    /** Export waits for pending criterion edits before taking its database snapshot. */
    suspend fun generatePdfSnapshot(): com.knigdelioglu.puanla.domain.export.PdfReportSnapshot =
        mutations.snapshot {
            val classroom = requireNotNull(selectedClassroom.value) { "Sınıf seçilmedi." }
            val rubric = requireNotNull(selectedRubric.value) { "Rubrik seçilmedi." }
            val run = requireNotNull(selectedRun.value) { "Değerlendirme seçilmedi." }
            repository.generatePdfReportSnapshot(classroom.id, rubric.id, run.id)
        }

    suspend fun generatePdfSnapshots(allClassrooms: Boolean): List<com.knigdelioglu.puanla.domain.export.PdfReportSnapshot> =
        mutations.snapshot {
            val classroom = requireNotNull(selectedClassroom.value) { "Sınıf seçilmedi." }
            val rubric = requireNotNull(selectedRubric.value) { "Rubrik seçilmedi." }
            val run = requireNotNull(selectedRun.value) { "Değerlendirme seçilmedi." }
            require(run.rubricId == rubric.id) { "Seçili rubrik değişmiş." }
            val ids = if (allClassrooms) {
                repository.getRunClassrooms(run.id).map { it.classroomId }
            } else listOf(classroom.id)
            require(ids.isNotEmpty()) { "Değerlendirmeye bağlı sınıf yok." }
            ids.map { repository.generatePdfReportSnapshot(it, rubric.id, run.id) }
        }

    // Backup and restore are mutually exclusive with every tracked write,
    // not just criterion scores. Snapshots wait for pending work; restore
    // waits for it before performing the one-transaction replacement.
    suspend fun createBackup(): String = mutations.snapshot {
        check(withContext(Dispatchers.Main.immediate) {
            pendingNoteWrites.isEmpty() && pendingGroupNoteWrites.isEmpty()
        }) {
            "Bekleyen gözlem notu kaydedilemedi; yedekleme durduruldu."
        }
        repository.generateFullBackupJson()
    }

    suspend fun restoreBackup(json: String): Result<Int> {
        val validation = repository.validateBackupJson(json)
        if (validation.isFailure) return Result.failure(requireNotNull(validation.exceptionOrNull()))
        return mutations.restore {
            val hasUnsavedNote = withContext(Dispatchers.Main.immediate) {
                pendingNoteWrites.isNotEmpty() || pendingGroupNoteWrites.isNotEmpty()
            }
            if (hasUnsavedNote) {
                return@restore Result.failure(IllegalStateException(
                    "Bekleyen gözlem notu kaydedilemedi; geri yükleme durduruldu."
                ))
            }
            val result = repository.restoreBackupFromJson(json)
            if (result.isSuccess) {
                // Do not rely only on Room's next Flow emission to rebuild the selection:
                // it can arrive before we clear the old state, leaving the selection null.
                val restoredClassrooms = repository.getAllClassrooms()
                val restoredClassroom = restoredClassrooms.firstOrNull {
                    it.id == arcPrefs.getString("last_classroom", null)
                } ?: restoredClassrooms.firstOrNull()
                val restoredRubric = restoredClassroom?.let { classroom ->
                    val available = repository.getAllRubricsFlow().first()
                    available.firstOrNull {
                        it.grade == classroom.grade &&
                            it.id == arcPrefs.getString("last_rubric_${classroom.id}", null)
                    } ?: available.firstOrNull { it.grade == classroom.grade }
                }
                withContext(Dispatchers.Main.immediate) {
                    pendingNoteWrites.values.forEach { it.edits.close(); it.flush.close() }
                    pendingNoteWrites.clear()
                    pendingGroupNoteWrites.values.forEach { it.edits.close(); it.flush.close() }
                    pendingGroupNoteWrites.clear()
                    evidenceNoteSaveState.value = null
                    groupEvidenceNoteSaveState.value = null
                    scoreSaveState.value = null
                    scoreWriteRevisions.clear()
                    lastStudentSelectionContext = null
                    studentFlowJob?.cancel(); studentFlowJob = null
                    assessmentFlowJob?.cancel(); assessmentFlowJob = null
                    currentAssessmentJob?.cancel(); currentAssessmentJob = null
                    studentGroupGradesJob?.cancel(); studentGroupGradesJob = null
                    groupFlowJob?.cancel(); groupFlowJob = null
                    groupSessionFlowJob?.cancel(); groupSessionFlowJob = null
                    groupAssessmentFlowJob?.cancel(); groupAssessmentFlowJob = null
                    currentGroupScoreJob?.cancel(); currentGroupScoreJob = null
                    currentStudentGroupGrades.value = emptyList()
                    classAssessments.value = emptyList()
                    runClassProgress.value = emptyList()
                    undoStack.clear()
                    groupUndoStack.clear()
                    canUndoGroupAction.value = false
                    // scoreLocks is a fixed Array<Mutex>; no clearing is needed or supported.
                    selectedStudent.value = null
                    selectedClassroom.value = null
                    selectedRubric.value = null
                    selectedRun.value = null
                    assessmentRuns.value = emptyList()
                    students.value = emptyList()
                    criteria.value = emptyList()
                    levels.value = emptyList()
                    currentScores.value = emptyMap()
                    currentAssessment.value = null
                    groupTasks.value = emptyList()
                    selectedGroup.value = null
                    groupSessions.value = emptyList()
                    selectedGroupSession.value = null
                    groupAssessments.value = emptyList()
                    currentGroupAssessment.value = null
                    currentGroupScores.value = emptyMap()
                    currentGroupMembers.value = emptyList()
                    // Start the restored classroom's existing collectors deterministically;
                    // they repopulate students, runs, assessments and groups.
                    selectedClassroom.value = restoredClassroom
                    selectedRubric.value = restoredRubric
                    // Room flows can subsequently refresh these selections normally.
                }
            }
            result
        }
    }
}
