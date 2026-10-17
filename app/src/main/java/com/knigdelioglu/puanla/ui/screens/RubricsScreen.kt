@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.knigdelioglu.puanla.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.knigdelioglu.arc.compose.foundation.ArcTheme
import com.knigdelioglu.puanla.data.local.CriterionEntity
import com.knigdelioglu.puanla.data.local.CriterionLevelEntity
import com.knigdelioglu.puanla.data.local.RubricEntity
import com.knigdelioglu.puanla.data.service.RubricDetails
import com.knigdelioglu.puanla.domain.rubric.RubricCriterionDraft
import com.knigdelioglu.puanla.domain.rubric.RubricDraft
import com.knigdelioglu.puanla.domain.rubric.RubricDraftRules
import com.knigdelioglu.puanla.domain.rubric.RubricLevelDraft
import com.knigdelioglu.puanla.ui.viewmodel.PuanlaViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val performanceLevelLabels = listOf(
    "Destek Gerekli",
    "Gelişmekte",
    "Yetkin",
    "Örnek Düzey"
)

private val starterLevelDescriptions = listOf(
    "Temel adımlarda yoğun öğretmen rehberliğine ihtiyaç duyar.",
    "Beklenen beceriyi kısmen sergiler, küçük hatırlatmalarla ilerler.",
    "Hedef kazanımı bağımsız ve doğru biçimde yerine getirir.",
    "Beceriyi üst düzeyde, özgün ve arkadaşlarına rehberlik ederek uygular."
)

@Composable
fun RubricsScreen(viewModel: PuanlaViewModel) {
    val rubrics by viewModel.rubrics.collectAsState()
    val summaries by viewModel.rubricSummaries.collectAsState()
    val details by viewModel.managedRubricDetails.collectAsState()
    val selectedClassroom by viewModel.selectedClassroom.collectAsState()
    val scope = rememberCoroutineScope()

    var selectedRubricId by rememberSaveable { mutableStateOf<String?>(null) }
    var editorPayload by remember { mutableStateOf<RubricEditorPayload?>(null) }
    var deleteTarget by remember { mutableStateOf<RubricEntity?>(null) }
    var mutationError by remember { mutableStateOf<String?>(null) }
    var isMutating by remember { mutableStateOf(false) }

    LaunchedEffect(rubrics, selectedRubricId) {
        if (selectedRubricId == null || rubrics.none { it.id == selectedRubricId }) {
            selectedRubricId = rubrics.firstOrNull()?.id
        }
    }
    LaunchedEffect(selectedRubricId) { viewModel.observeManagedRubric(selectedRubricId) }
    DisposableEffect(viewModel) {
        onDispose { viewModel.observeManagedRubric(null) }
    }

    val colors = ArcTheme.colors
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        RubricHero(
            onCreate = {
                mutationError = null
                editorPayload = RubricEditorPayload(
                    existingRubricId = null,
                    draft = starterRubricDraft(selectedClassroom?.grade ?: 11)
                )
            },
            enabled = !isMutating
        )

        if (editorPayload != null) {
            val payload = requireNotNull(editorPayload)
            RubricEditor(
                modifier = Modifier.weight(1f),
                initialDraft = payload.draft,
                existingRubricId = payload.existingRubricId,
                saving = isMutating,
                error = mutationError,
                onCancel = {
                    if (!isMutating) {
                        editorPayload = null
                        mutationError = null
                    }
                },
                onSave = { draft ->
                    if (!isMutating) {
                        isMutating = true
                        mutationError = null
                        scope.launch {
                            try {
                                val saved = viewModel.saveRubricDraft(draft, payload.existingRubricId)
                                selectedRubricId = saved.id
                                editorPayload = null
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (error: Exception) {
                                mutationError = error.message ?: "Rubrik kaydedilemedi."
                            } finally {
                                isMutating = false
                            }
                        }
                    }
                }
            )
        } else {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth().weight(1f)) {
                if (maxWidth >= 850.dp) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        RubricListPane(
                            modifier = Modifier.weight(0.34f).fillMaxHeight(),
                            rubrics = rubrics,
                            summaries = summaries.associateBy { it.rubricId },
                            selectedRubricId = selectedRubricId,
                            onSelect = { selectedRubricId = it }
                        )
                        RubricDetailsPane(
                            modifier = Modifier.weight(0.66f).fillMaxHeight(),
                            empty = rubrics.isEmpty(),
                            details = details?.takeIf { it.rubric.id == selectedRubricId },
                            onEdit = { selected ->
                                val selectedDetails = details?.takeIf { it.rubric.id == selected.id }
                                if (selectedDetails != null) {
                                    mutationError = null
                                    editorPayload = RubricEditorPayload(selected.id, selectedDetails.toDraft())
                                }
                            },
                            onDuplicate = { selected ->
                                if (!isMutating) {
                                    isMutating = true
                                    mutationError = null
                                    scope.launch {
                                        try {
                                            selectedRubricId = viewModel.duplicateRubric(selected.id).id
                                        } catch (cancelled: CancellationException) {
                                            throw cancelled
                                        } catch (error: Exception) {
                                            mutationError = error.message ?: "Rubrik çoğaltılamadı."
                                        } finally {
                                            isMutating = false
                                        }
                                    }
                                }
                            },
                            onDelete = { deleteTarget = it },
                            operationError = mutationError,
                            busy = isMutating
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        RubricListPane(
                            modifier = Modifier.fillMaxWidth().heightIn(max = 250.dp),
                            rubrics = rubrics,
                            summaries = summaries.associateBy { it.rubricId },
                            selectedRubricId = selectedRubricId,
                            onSelect = { selectedRubricId = it }
                        )
                        RubricDetailsPane(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            empty = rubrics.isEmpty(),
                            details = details?.takeIf { it.rubric.id == selectedRubricId },
                            onEdit = { selected ->
                                val selectedDetails = details?.takeIf { it.rubric.id == selected.id }
                                if (selectedDetails != null) {
                                    mutationError = null
                                    editorPayload = RubricEditorPayload(selected.id, selectedDetails.toDraft())
                                }
                            },
                            onDuplicate = { selected ->
                                if (!isMutating) {
                                    isMutating = true
                                    mutationError = null
                                    scope.launch {
                                        try {
                                            selectedRubricId = viewModel.duplicateRubric(selected.id).id
                                        } catch (cancelled: CancellationException) {
                                            throw cancelled
                                        } catch (error: Exception) {
                                            mutationError = error.message ?: "Rubrik çoğaltılamadı."
                                        } finally {
                                            isMutating = false
                                        }
                                    }
                                }
                            },
                            onDelete = { deleteTarget = it },
                            operationError = mutationError,
                            busy = isMutating
                        )
                    }
                }
            }
        }
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Rubrik silinsin mi?") },
            text = { Text("“${target.title}” rubriği kalıcı olarak silinecek.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleteTarget = null
                        isMutating = true
                        mutationError = null
                        scope.launch {
                            try {
                                viewModel.deleteRubric(target.id)
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (error: Exception) {
                                mutationError = error.message ?: "Rubrik silinemedi."
                            } finally {
                                isMutating = false
                            }
                        }
                    }
                ) { Text("Sil") }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("İptal") } }
        )
    }
}

private data class RubricEditorPayload(
    val existingRubricId: String?,
    val draft: RubricDraft
)

@Composable
private fun RubricHero(onCreate: () -> Unit, enabled: Boolean) {
    val colors = ArcTheme.colors
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = colors.surface,
        border = BorderStroke(1.dp, colors.border)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "Dereceli Puanlama Anahtarı (Rubrik) Tasarımcısı",
                    color = colors.foreground,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Ölçütleri, puan ağırlıklarını ve dört performans düzeyini tanımlayın.",
                    color = colors.textSecondary,
                    fontSize = 16.sp
                )
            }
            Button(
                onClick = onCreate,
                enabled = enabled,
                modifier = Modifier.heightIn(min = 52.dp).testTag("newRubricButton")
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Yeni Rubrik Oluştur", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun RubricListPane(
    modifier: Modifier,
    rubrics: List<RubricEntity>,
    summaries: Map<String, com.knigdelioglu.puanla.data.local.RubricSummary>,
    selectedRubricId: String?,
    onSelect: (String) -> Unit
) {
    val colors = ArcTheme.colors
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = colors.surface,
        border = BorderStroke(1.dp, colors.border)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
            Text(
                "Kayıtlı Rubrikler (${rubrics.size})",
                color = colors.foreground,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
            )
            Spacer(Modifier.height(6.dp))
            if (rubrics.isEmpty()) {
                Text(
                    "Henüz rubrik yok. Yeni bir rubrik oluşturarak başlayın.",
                    color = colors.textSecondary,
                    modifier = Modifier.padding(8.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().testTag("rubricList"),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(rubrics, key = { it.id }) { rubric ->
                        val summary = summaries[rubric.id]
                        val selected = rubric.id == selectedRubricId
                        Surface(
                            modifier = Modifier.fillMaxWidth()
                                .semantics { this.selected = selected }
                                .clickable(role = Role.RadioButton, onClick = { onSelect(rubric.id) })
                                .testTag("rubricItem-${rubric.id}"),
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected) colors.accent.copy(alpha = 0.08f) else colors.surface,
                            border = BorderStroke(
                                if (selected) 1.5.dp else 1.dp,
                                if (selected) colors.accent else colors.borderSubtle
                            )
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                                Text(
                                    text = "${rubric.targetTask} · ${summary?.criterionCount ?: 0} ölçüt · ${summary?.totalPoints ?: 0} Puan",
                                    color = colors.textSecondary,
                                    fontSize = 13.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.height(5.dp))
                                Text(
                                    rubric.title,
                                    color = colors.foreground,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RubricDetailsPane(
    modifier: Modifier,
    empty: Boolean,
    details: RubricDetails?,
    onEdit: (RubricEntity) -> Unit,
    onDuplicate: (RubricEntity) -> Unit,
    onDelete: (RubricEntity) -> Unit,
    operationError: String?,
    busy: Boolean
) {
    val colors = ArcTheme.colors
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = colors.surface,
        border = BorderStroke(1.dp, colors.border)
    ) {
        if (details == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (empty) {
                    Text("Rubrik oluşturmak için üstteki Yeni Rubrik Oluştur düğmesini kullanın.", color = colors.textSecondary)
                } else {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp, vertical = 18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(
                            "${details.rubric.targetTask} · ${details.rubric.grade}. sınıf · ${formatRubricDate(details.rubric.createdAt)}",
                            color = colors.textSecondary,
                            fontSize = 13.sp
                        )
                        Text(
                            details.rubric.title,
                            color = colors.foreground,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (details.rubric.description.isNotBlank()) {
                            Text(details.rubric.description, color = colors.textSecondary, fontSize = 15.sp)
                        }
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        TextButton(
                            onClick = { onEdit(details.rubric) },
                            enabled = !busy && details.usageCount == 0,
                            modifier = Modifier.testTag("editRubricButton")
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Rubriği Düzenle")
                        }
                        OutlinedButton(
                            onClick = { onDuplicate(details.rubric) },
                            enabled = !busy,
                            modifier = Modifier.testTag("duplicateRubricButton")
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Çoğalt")
                        }
                        if (!details.rubric.isBuiltIn) {
                            IconButton(
                                onClick = { onDelete(details.rubric) },
                                enabled = !busy,
                                modifier = Modifier.testTag("deleteRubricButton")
                            ) { Icon(Icons.Default.Delete, contentDescription = "Rubriği sil") }
                        }
                    }
                }
                if (details.usageCount > 0) {
                    Text(
                        "Bu rubrik ${details.usageCount} değerlendirme kaydında kullanılıyor. Geçmiş ölçüt yapısı korunuyor; yeni bir sürüm için rubriği çoğaltın.",
                        color = colors.textSecondary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                } else if (details.rubric.isBuiltIn) {
                    Text(
                        "Onaylı kaynak rubriği korunur. Değişiklik yapmak için önce çoğaltabilirsiniz.",
                        color = colors.textSecondary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                if (!operationError.isNullOrBlank()) {
                    Text(
                        operationError,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = colors.borderSubtle)
                Spacer(Modifier.height(16.dp))
                RubricCriteriaTable(details.criteria, details.levels)
            }
        }
    }
}

@Composable
private fun RubricCriteriaTable(criteria: List<CriterionEntity>, levels: List<CriterionLevelEntity>) {
    val colors = ArcTheme.colors
    val levelsByCriterion = levels.groupBy(CriterionLevelEntity::criterionId)
    Column(Modifier.horizontalScroll(rememberScrollState())) {
        Row(
            modifier = Modifier.width(1216.dp).padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Ölçüt · Puan & Ağırlık",
                modifier = Modifier.width(260.dp).padding(horizontal = 10.dp),
                color = colors.textSecondary,
                fontWeight = FontWeight.SemiBold
            )
            performanceLevelLabels.forEach { label ->
                Text(
                    label,
                    modifier = Modifier.width(239.dp).padding(horizontal = 10.dp),
                    color = colors.textSecondary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        criteria.forEachIndexed { index, criterion ->
            val criterionLevels = levelsByCriterion[criterion.id].orEmpty()
                .sortedBy(CriterionLevelEntity::orderIndex)
            HorizontalDivider(color = colors.borderSubtle)
            Row(
                modifier = Modifier.width(1216.dp).padding(vertical = 8.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.width(260.dp).padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text("${(index + 1).toString().padStart(2, '0')}. ${criterion.description.ifBlank { "Ölçüt" }}", color = colors.textSecondary, fontSize = 12.sp)
                    Text(criterion.title, color = colors.foreground, fontWeight = FontWeight.SemiBold)
                    Text("${criterion.maxPoints} Puan · %${criterion.maxPoints}", color = colors.accent, fontWeight = FontWeight.SemiBold)
                }
                performanceLevelLabels.forEachIndexed { levelIndex, label ->
                    val level = criterionLevels.getOrNull(levelIndex)
                    Column(
                        modifier = Modifier.width(239.dp).padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            level?.let { "${it.points} Puan" } ?: "Düzey ${levelIndex + 1}",
                            color = colors.foreground,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            level?.description ?: "Bu düzey için henüz açıklama tanımlanmamış.",
                            color = colors.textSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RubricEditor(
    modifier: Modifier,
    initialDraft: RubricDraft,
    existingRubricId: String?,
    saving: Boolean,
    error: String?,
    onCancel: () -> Unit,
    onSave: (RubricDraft) -> Unit
) {
    var draft by remember(initialDraft) { mutableStateOf(initialDraft) }
    val colors = ArcTheme.colors
    val validationError = remember(draft) { runCatching { RubricDraftRules.validate(draft) }.exceptionOrNull()?.message }
    val totalPoints = draft.criteria.sumOf { it.maxPoints }
    val isValid = validationError == null

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = colors.surface,
        border = BorderStroke(1.dp, colors.border)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Rubrik Düzenleyici: ${if (existingRubricId == null) "Yeni Değerlendirme Rubriği" else "Rubriği Düzenle"}",
                    modifier = Modifier.weight(1f),
                    color = colors.foreground,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onCancel, enabled = !saving) { Text("İptal") }
                Button(
                    onClick = { onSave(draft) },
                    enabled = !saving && isValid,
                    modifier = Modifier.heightIn(min = 48.dp).testTag("rubricSaveButton")
                ) {
                    if (saving) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(Modifier.width(7.dp))
                    Text(if (saving) "Kaydediliyor" else "Rubriği Kaydet")
                }
            }
            HorizontalDivider(color = colors.borderSubtle)

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = draft.title,
                    onValueChange = { draft = draft.copy(title = it) },
                    label = { Text("Rubrik Başlığı") },
                    singleLine = true,
                    modifier = Modifier.weight(2f).testTag("rubricTitleField")
                )
                OutlinedTextField(
                    value = draft.subject,
                    onValueChange = { draft = draft.copy(subject = it) },
                    label = { Text("Ders / Alan") },
                    singleLine = true,
                    modifier = Modifier.weight(1f).testTag("rubricSubjectField")
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Sınıf Düzeyi", color = colors.textSecondary, fontWeight = FontWeight.Medium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (9..12).forEach { grade ->
                        FilterChip(
                            selected = draft.grade == grade,
                            onClick = { draft = draft.copy(grade = grade) },
                            label = { Text("$grade. Sınıf") }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = draft.description,
                onValueChange = { draft = draft.copy(description = it) },
                label = { Text("Açıklama / Kullanım Amacı") },
                modifier = Modifier.fillMaxWidth().heightIn(min = 68.dp).testTag("rubricDescriptionField")
            )

            HorizontalDivider(color = colors.borderSubtle)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Değerlendirme Ölçütleri (${draft.criteria.size})",
                    color = colors.foreground,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Toplam Puan: $totalPoints Puan  ·  Toplam Ağırlık: %$totalPoints",
                    color = if (totalPoints == RubricDraftRules.TOTAL_POINTS) colors.success else colors.textSecondary,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f).testTag("rubricTotals")
                )
                OutlinedButton(
                    onClick = {
                        val points = evenPointDistribution(draft.criteria.size)
                        draft = draft.copy(criteria = draft.criteria.mapIndexed { index, criterion ->
                            criterion.copy(
                                maxPoints = points[index],
                                levels = rebaseLevels(criterion.levels, points[index])
                            )
                        })
                    },
                    enabled = draft.criteria.isNotEmpty() && !saving,
                    modifier = Modifier.testTag("equalizeRubricPoints")
                ) { Text("Puan ve Ağırlıkları Eşitle") }
                OutlinedButton(
                    onClick = {
                        val appended = draft.criteria + defaultCriterion(
                            title = "Yeni ölçüt ${draft.criteria.size + 1}",
                            category = "Alt boyut",
                            points = 1
                        )
                        val points = evenPointDistribution(appended.size)
                        draft = draft.copy(criteria = appended.mapIndexed { index, criterion ->
                            criterion.copy(
                                maxPoints = points[index],
                                levels = rebaseLevels(criterion.levels, points[index])
                            )
                        })
                    },
                    enabled = !saving && draft.criteria.size < 100,
                    modifier = Modifier.testTag("addRubricCriterion")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(5.dp))
                    Text("Ölçüt Ekle")
                }
            }

            Text(
                "100 puanlık rubrikte ölçüt puanı ve ağırlık yüzdesi aynı değeri kullanır.",
                color = colors.textSecondary,
                fontSize = 13.sp
            )
            draft.criteria.forEachIndexed { index, criterion ->
                RubricCriterionEditor(
                    modifier = Modifier.fillMaxWidth(),
                    number = index + 1,
                    criterion = criterion,
                    canRemove = draft.criteria.size > 1,
                    enabled = !saving,
                    onChange = { updated ->
                        draft = draft.copy(criteria = draft.criteria.toMutableList().also { it[index] = updated })
                    },
                    onRemove = {
                        val remaining = draft.criteria.filterIndexed { itemIndex, _ -> itemIndex != index }
                        val points = evenPointDistribution(remaining.size)
                        draft = draft.copy(criteria = remaining.mapIndexed { itemIndex, item ->
                            item.copy(maxPoints = points[itemIndex], levels = rebaseLevels(item.levels, points[itemIndex]))
                        })
                    }
                )
            }

            if (!error.isNullOrBlank()) {
                Text(error, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("rubricSaveError"))
            } else if (!validationError.isNullOrBlank()) {
                Text(validationError, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("rubricValidationError"))
            }
            Spacer(Modifier.height(6.dp))
        }
    }
}

@Composable
private fun RubricCriterionEditor(
    modifier: Modifier,
    number: Int,
    criterion: RubricCriterionDraft,
    canRemove: Boolean,
    enabled: Boolean,
    onChange: (RubricCriterionDraft) -> Unit,
    onRemove: () -> Unit
) {
    val colors = ArcTheme.colors
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = colors.surfaceRaised,
        border = BorderStroke(1.dp, colors.borderSubtle)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = criterion.title,
                    onValueChange = { onChange(criterion.copy(title = it)) },
                    label = { Text("$number. Ölçüt Başlığı") },
                    singleLine = true,
                    enabled = enabled,
                    modifier = Modifier.weight(3f).testTag("rubricCriterionTitle-$number")
                )
                OutlinedTextField(
                    value = criterion.category,
                    onValueChange = { onChange(criterion.copy(category = it)) },
                    label = { Text("Alt Boyut / Kategori") },
                    singleLine = true,
                    enabled = enabled,
                    modifier = Modifier.weight(2f)
                )
                OutlinedTextField(
                    value = criterion.maxPoints.toString(),
                    onValueChange = { value -> onChange(criterion.copy(maxPoints = value.toIntOrNull() ?: 0)) },
                    label = { Text("Kriter Puanı") },
                    singleLine = true,
                    enabled = enabled,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1.1f).testTag("rubricCriterionPoints-$number")
                )
                OutlinedTextField(
                    value = criterion.maxPoints.toString(),
                    onValueChange = {},
                    label = { Text("Ağırlık (%)") },
                    singleLine = true,
                    readOnly = true,
                    enabled = enabled,
                    modifier = Modifier.weight(1.1f)
                )
                IconButton(onClick = onRemove, enabled = enabled && canRemove, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "$number. ölçütü kaldır")
                }
            }
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                criterion.levels.forEachIndexed { index, level ->
                    PerformanceLevelEditor(
                        modifier = Modifier.width(228.dp),
                        index = index,
                        maxPoints = criterion.maxPoints,
                        level = level,
                        enabled = enabled,
                        onChange = { updated ->
                            onChange(criterion.copy(levels = criterion.levels.toMutableList().also { it[index] = updated }))
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PerformanceLevelEditor(
    modifier: Modifier,
    index: Int,
    maxPoints: Int,
    level: RubricLevelDraft,
    enabled: Boolean,
    onChange: (RubricLevelDraft) -> Unit
) {
    val colors = ArcTheme.colors
    Surface(
        modifier = modifier.heightIn(min = 148.dp),
        shape = RoundedCornerShape(12.dp),
        color = colors.surface,
        border = BorderStroke(1.dp, colors.borderSubtle)
    ) {
        Column(modifier = Modifier.padding(11.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    performanceLevelLabels.getOrElse(index) { "Düzey ${index + 1}" },
                    modifier = Modifier.weight(1f),
                    color = colors.foreground,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                OutlinedTextField(
                    value = level.points.toString(),
                    onValueChange = { value -> onChange(level.copy(points = value.toIntOrNull() ?: 0)) },
                    label = { Text("Puan") },
                    singleLine = true,
                    enabled = enabled,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.width(88.dp).testTag("rubricLevelPoints-${index + 1}")
                )
            }
            OutlinedTextField(
                value = level.description,
                onValueChange = { onChange(level.copy(description = it)) },
                label = { Text("Düzey açıklaması") },
                enabled = enabled,
                minLines = 3,
                modifier = Modifier.fillMaxWidth().testTag("rubricLevelDescription-${index + 1}")
            )
            if (level.points !in 0..maxPoints) {
                Text("0–$maxPoints aralığında puan girin.", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            }
        }
    }
}

private fun starterRubricDraft(grade: Int): RubricDraft = RubricDraft(
    title = "Yeni Değerlendirme Rubriği",
    subject = "Genel Değerlendirme",
    description = "Öğrencilerin süreç ve ürün performanslarını ölçüt temelli değerlendirir.",
    grade = grade.coerceIn(9, 12),
    criteria = listOf(
        defaultCriterion("Hazırlık ve Planlama", "Süreç", 50),
        defaultCriterion("Uygulama ve Ürün", "Ürün", 50)
    )
)

private fun defaultCriterion(title: String, category: String, points: Int): RubricCriterionDraft =
    RubricCriterionDraft(
        title = title,
        category = category,
        maxPoints = points,
        levels = starterLevelDescriptions.mapIndexed { index, description ->
            RubricLevelDraft(points = levelAnchor(points, index), description = description)
        }
    )

private fun levelAnchor(maxPoints: Int, index: Int): Int =
    if (maxPoints <= 0) 0 else ((maxPoints.toLong() * (index + 1) + 3) / 4).toInt()

private fun evenPointDistribution(count: Int): List<Int> {
    if (count <= 0) return emptyList()
    val base = RubricDraftRules.TOTAL_POINTS / count
    val remainder = RubricDraftRules.TOTAL_POINTS % count
    return List(count) { index -> base + if (index < remainder) 1 else 0 }
}

private fun rebaseLevels(levels: List<RubricLevelDraft>, maxPoints: Int): List<RubricLevelDraft> =
    List(RubricDraftRules.PERFORMANCE_LEVEL_COUNT) { index ->
        val existingDescription = levels.getOrNull(index)?.description
        RubricLevelDraft(
            points = levelAnchor(maxPoints, index),
            description = existingDescription ?: starterLevelDescriptions[index]
        )
    }

private fun RubricDetails.toDraft(): RubricDraft {
    val levelsByCriterion = levels.groupBy(CriterionLevelEntity::criterionId)
    return RubricDraft(
        title = rubric.title,
        subject = rubric.targetTask,
        description = rubric.description,
        grade = rubric.grade,
        criteria = criteria.map { criterion ->
            val savedLevels = levelsByCriterion[criterion.id].orEmpty()
                .sortedBy(CriterionLevelEntity::orderIndex)
            val levelDrafts = if (savedLevels.size == RubricDraftRules.PERFORMANCE_LEVEL_COUNT) {
                savedLevels.map { RubricLevelDraft(it.points, it.description) }
            } else {
                List(RubricDraftRules.PERFORMANCE_LEVEL_COUNT) { index ->
                    RubricLevelDraft(
                        points = levelAnchor(criterion.maxPoints, index),
                        description = savedLevels.getOrNull(index)?.description
                            ?.takeIf(String::isNotBlank) ?: starterLevelDescriptions[index]
                    )
                }
            }
            RubricCriterionDraft(
                id = criterion.id,
                title = criterion.title,
                category = criterion.description,
                maxPoints = criterion.maxPoints,
                levels = levelDrafts
            )
        }
    )
}

private fun formatRubricDate(timestamp: Long): String {
    if (timestamp <= 0L) return "Tarih belirtilmedi"
    return Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.forLanguageTag("tr-TR")))
}
