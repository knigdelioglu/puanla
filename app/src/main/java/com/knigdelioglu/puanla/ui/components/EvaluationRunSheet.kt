package com.knigdelioglu.puanla.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.knigdelioglu.arc.compose.controls.ArcButton
import com.knigdelioglu.arc.compose.controls.ArcButtonVariant
import com.knigdelioglu.arc.compose.display.ArcCard
import com.knigdelioglu.arc.compose.foundation.ArcTheme
import com.knigdelioglu.arc.compose.navigation.ArcBottomSheet
import com.knigdelioglu.puanla.data.local.AssessmentRunEntity
import com.knigdelioglu.puanla.ui.viewmodel.PuanlaViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun EvaluationRunSheet(
    viewModel: PuanlaViewModel,
    isOpen: Boolean,
    onDismiss: () -> Unit
) {
    val classroom by viewModel.selectedClassroom.collectAsState()
    val rubric by viewModel.selectedRubric.collectAsState()
    val classrooms by viewModel.classrooms.collectAsState()
    val runs by viewModel.assessmentRuns.collectAsState()
    val run by viewModel.selectedRun.collectAsState()
    val progress by viewModel.runClassProgress.collectAsState()
    var newRunForm by remember { mutableStateOf(false) }
    var addClassForm by remember { mutableStateOf(false) }
    var newTitle by remember(rubric?.id) { mutableStateOf("") }
    var newClassIds by remember(classroom?.id, rubric?.id) {
        mutableStateOf(setOfNotNull(classroom?.id))
    }
    var additionalClassIds by remember(run?.id) { mutableStateOf(emptySet<String>()) }
    val eligible = classrooms.filter { it.grade == rubric?.grade }
    val alreadyAssigned = progress.map { it.classroomId }.toSet()
    val availableToAdd = eligible.filter { it.id !in alreadyAssigned }

    ArcBottomSheet(isOpen = isOpen, onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().heightIn(max = 650.dp)
                .verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Değerlendirmelerim", color = ArcTheme.colors.foreground, fontWeight = FontWeight.Bold)
            Text(
                "${classroom?.name ?: "Sınıf seçilmedi"} · ${rubric?.title ?: "Rubrik seçilmedi"}",
                color = ArcTheme.colors.textSecondary
            )
            if (classroom == null || rubric == null) {
                Text("Önce bir sınıf ve rubrik seçin.", color = ArcTheme.colors.textSecondary)
            } else {
                if (!newRunForm) {
                    ArcButton(
                        text = "+ Yeni değerlendirme",
                        onClick = {
                            newTitle = "${rubric?.title.orEmpty()} · " +
                                LocalDate.now().format(DateTimeFormatter.ofPattern("LLLL yyyy", Locale.forLanguageTag("tr-TR")))
                            newClassIds = setOfNotNull(classroom?.id)
                            newRunForm = true
                        },
                        variant = ArcButtonVariant.Primary
                    )
                } else {
                    ArcCard {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("Yeni değerlendirme", fontWeight = FontWeight.Bold, color = ArcTheme.colors.foreground)
                            OutlinedTextField(
                                value = newTitle, onValueChange = { newTitle = it },
                                label = { Text("Değerlendirme adı") },
                                singleLine = true, modifier = Modifier.fillMaxWidth()
                            )
                            Text("Bu değerlendirmeyi kullanacak sınıflar", color = ArcTheme.colors.textSecondary)
                            eligible.forEach { item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        newClassIds = if (item.id in newClassIds) newClassIds - item.id
                                        else newClassIds + item.id
                                    },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = item.id in newClassIds,
                                        onCheckedChange = { checked ->
                                            newClassIds = if (checked) newClassIds + item.id else newClassIds - item.id
                                        }
                                    )
                                    Text(item.name, color = ArcTheme.colors.foreground)
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                ArcButton(
                                    text = "Vazgeç", onClick = { newRunForm = false },
                                    variant = ArcButtonVariant.Ghost
                                )
                                ArcButton(
                                    text = "Oluştur",
                                    onClick = {
                                        viewModel.createRun(newTitle.trim(), newClassIds.toList())
                                        newRunForm = false
                                        onDismiss()
                                    },
                                    enabled = newTitle.isNotBlank() && newClassIds.isNotEmpty(),
                                    variant = ArcButtonVariant.Primary
                                )
                            }
                        }
                    }
                }
                if (runs.isEmpty()) {
                    Text(
                        "Bu sınıfta henüz değerlendirme yok. Yeni değerlendirme oluşturup puanlamaya başlayın.",
                        color = ArcTheme.colors.textSecondary
                    )
                } else {
                    Text("Bu sınıftaki değerlendirmeler", color = ArcTheme.colors.foreground, fontWeight = FontWeight.SemiBold)
                    runs.forEach { item ->
                        ArcCard {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.title, color = ArcTheme.colors.foreground, fontWeight = FontWeight.SemiBold)
                                        Text(
                                            runDescription(item),
                                            color = ArcTheme.colors.textSecondary
                                        )
                                    }
                                    ArcButton(
                                        text = if (run?.id == item.id) "Seçili" else "Aç",
                                        enabled = run?.id != item.id,
                                        onClick = {
                                            viewModel.selectRun(item)
                                            onDismiss()
                                        },
                                        variant = if (run?.id == item.id) ArcButtonVariant.Outline else ArcButtonVariant.Primary
                                    )
                                }
                            }
                        }
                    }
                }
                if (run != null) {
                    HorizontalDivider()
                    Text("Seçili değerlendirme · ${run?.title}", color = ArcTheme.colors.foreground, fontWeight = FontWeight.Bold)
                    val completed = progress.sumOf { it.completed }
                    val total = progress.sumOf { it.total }
                    Text(
                        "$completed / $total öğrenci tamamlandı",
                        color = ArcTheme.colors.textSecondary
                    )
                    progress.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                classrooms.firstOrNull { it.id == item.classroomId }?.let { target ->
                                    run?.let { active -> viewModel.openRunClassroom(active, target) }
                                }
                                onDismiss()
                            }.padding(vertical = 9.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(item.classroomName, color = ArcTheme.colors.foreground)
                            Text(
                                "${item.completed}/${item.total}" +
                                    if (item.partial > 0) " · ${item.partial} kısmi" else "",
                                color = ArcTheme.colors.textSecondary
                            )
                        }
                    }
                    if (run?.status == "ACTIVE" && availableToAdd.isNotEmpty()) {
                        ArcButton(
                            text = if (addClassForm) "Sınıf eklemeyi kapat" else "Bu değerlendirmeye sınıf ekle",
                            onClick = { addClassForm = !addClassForm },
                            variant = ArcButtonVariant.Outline
                        )
                        if (addClassForm) {
                            availableToAdd.forEach { item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        additionalClassIds = if (item.id in additionalClassIds)
                                            additionalClassIds - item.id else additionalClassIds + item.id
                                    },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = item.id in additionalClassIds,
                                        onCheckedChange = { checked ->
                                            additionalClassIds = if (checked) additionalClassIds + item.id
                                            else additionalClassIds - item.id
                                        }
                                    )
                                    Text(item.name, color = ArcTheme.colors.foreground)
                                }
                            }
                            ArcButton(
                                text = "Sınıfları Ekle",
                                enabled = additionalClassIds.isNotEmpty(),
                                onClick = {
                                    run?.let { viewModel.addClassroomsToRun(it, additionalClassIds.toList()) }
                                    additionalClassIds = emptySet()
                                    addClassForm = false
                                },
                                variant = ArcButtonVariant.Primary
                            )
                        }
                    }
                    ArcButton(
                        text = if (run?.status == "ARCHIVED") "Arşivden çıkar" else "Tamamla ve arşivle",
                        onClick = { run?.let { viewModel.setRunArchived(it, it.status != "ARCHIVED") } },
                        variant = ArcButtonVariant.Outline
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

private fun runDescription(run: AssessmentRunEntity): String {
    if (run.createdAt <= 0L) return "Eski kayıt · ${if (run.status == "ARCHIVED") "Arşivlendi" else "Aktif"}"
    val date = Instant.ofEpochMilli(run.createdAt).atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.forLanguageTag("tr-TR")))
    return "$date · ${if (run.status == "ARCHIVED") "Arşivlendi" else "Aktif"}"
}
