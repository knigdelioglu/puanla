package com.knigdelioglu.puanla.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.knigdelioglu.arc.compose.controls.ArcButton
import com.knigdelioglu.arc.compose.controls.ArcButtonSize
import com.knigdelioglu.arc.compose.controls.ArcButtonVariant
import com.knigdelioglu.arc.compose.controls.ArcCheckbox
import com.knigdelioglu.arc.compose.controls.ArcConfirmMorph
import com.knigdelioglu.arc.compose.controls.ArcInput
import com.knigdelioglu.arc.compose.display.ArcAlert
import com.knigdelioglu.arc.compose.display.ArcAlertType
import com.knigdelioglu.arc.compose.display.ArcAvatarGroup
import com.knigdelioglu.arc.compose.display.ArcBadge
import com.knigdelioglu.arc.compose.display.ArcBadgeVariant
import com.knigdelioglu.arc.compose.display.ArcCard
import com.knigdelioglu.arc.compose.display.ArcEmptyState
import com.knigdelioglu.arc.compose.display.ArcExpandableCard
import com.knigdelioglu.arc.compose.display.ArcProgress
import com.knigdelioglu.arc.compose.foundation.ArcTheme
import com.knigdelioglu.puanla.data.local.GroupTaskEntity
import com.knigdelioglu.puanla.data.local.StudentEntity
import com.knigdelioglu.puanla.data.local.fullName
import com.knigdelioglu.puanla.data.local.initials
import com.knigdelioglu.puanla.ui.viewmodel.PuanlaViewModel
import org.json.JSONArray

@Composable
fun GroupWorkScreen(viewModel: PuanlaViewModel) {
    val selectedClassroom by viewModel.selectedClassroom.collectAsState()
    val selectedRubric by viewModel.selectedRubric.collectAsState()
    val students by viewModel.students.collectAsState()
    val groupTasks by viewModel.groupTasks.collectAsState()

    var isCreateGroupDialogOpen by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Grup Çalışmaları & Hazırlık Takibi",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = ArcTheme.colors.foreground
                )
                Text(
                    text = "Grup içi hazırlık adımlarını izleyin. Hazırlık kontrolü bireysel puandan ayrı tutulur.",
                    fontSize = 14.sp,
                    color = ArcTheme.colors.textSecondary
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (selectedClassroom != null) {
                    ArcBadge(
                        text = "${selectedClassroom?.name} Sınıfı",
                        variant = ArcBadgeVariant.Accent
                    )
                }
                ArcButton(
                    text = "Yeni Grup Oluştur",
                    onClick = { isCreateGroupDialogOpen = true },
                    variant = ArcButtonVariant.Primary,
                    size = ArcButtonSize.Md,
                    enabled = selectedClassroom != null
                )
            }
        }

        // Strict Separation Rule Notice
        ArcAlert(
            title = "MEB Değerlendirme İlkesi: Bireysel Not Bağımsızlığı",
            message = "Grup hazırlık kontrolü ile bireysel performans notu ayrı yönetilir. Grup hazırlık işaretleri bireysel 100 puanlık toplama kendiliğinden eklenmez.",
            type = ArcAlertType.Info
        )

        Divider(color = ArcTheme.colors.borderSubtle)

        if (selectedClassroom == null) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                ArcEmptyState(
                    title = "Sınıf Seçilmedi",
                    description = "Grup çalışmalarını yönetmek için üst menüden bir sınıf seçin."
                )
            }
            return@Column
        }

        if (groupTasks.isEmpty()) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                ArcEmptyState(
                    title = "Grup Çalışması Bulunmuyor",
                    description = "Bu sınıfta henüz bir grup projesi oluşturulmamış. 'Yeni Grup Oluştur' butonuna tıklayarak ilk grubu kurabilirsiniz."
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(groupTasks) { task ->
                    GroupTaskCard(
                        task = task,
                        students = students,
                        onToggleChecklist = { idx ->
                            viewModel.toggleChecklistItem(task, idx)
                        }
                    )
                }
            }
        }
    }

    if (isCreateGroupDialogOpen) {
        CreateGroupDialog(
            students = students,
            onDismiss = { isCreateGroupDialogOpen = false },
            onConfirm = { title, memberIds ->
                viewModel.addGroupTask(title, memberIds)
                isCreateGroupDialogOpen = false
            }
        )
    }
}

@Composable
private fun GroupTaskCard(
    task: GroupTaskEntity,
    students: List<StudentEntity>,
    onToggleChecklist: (Int) -> Unit
) {
    val studentMap = remember(students) { students.associateBy { it.id } }

    val memberIds = remember(task.memberStudentIdsJson) {
        try {
            val arr = JSONArray(task.memberStudentIdsJson)
            (0 until arr.length()).map { arr.getString(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    val members = memberIds.mapNotNull { studentMap[it] }
    val memberInitials = members.map { it.initials }

    val checklist = remember(task.checklistJson) {
        try {
            val arr = JSONArray(task.checklistJson)
            (0 until arr.length()).map {
                val obj = arr.getJSONObject(it)
                obj.getString("title") to obj.getBoolean("checked")
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    val completedCount = checklist.count { it.second }
    val progressFraction = if (checklist.isNotEmpty()) completedCount.toFloat() / checklist.size else 0f

    ArcExpandableCard(
        title = task.title,
        subtitle = "${members.size} Üye • %${(progressFraction * 100).toInt()} Hazırlık Tamamlandı",
        badge = {
            ArcAvatarGroup(membersInitials = memberInitials, avatarSize = 28.dp)
        },
        initiallyExpanded = true
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Member tags
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Grup Üyeleri: ",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ArcTheme.colors.textSecondary
                )
                Spacer(Modifier.width(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    members.forEach { m ->
                        ArcBadge(
                            text = "${m.studentNumber} ${m.fullName}",
                            variant = ArcBadgeVariant.Default
                        )
                    }
                }
            }

            // Progress bar
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Hazırlık Kontrol Listesi ($completedCount / ${checklist.size})",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = ArcTheme.colors.foreground
                    )
                    Text(
                        text = "%${(progressFraction * 100).toInt()}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = ArcTheme.colors.accent
                    )
                }
                Spacer(Modifier.height(6.dp))
                ArcProgress(progress = progressFraction)
            }

            Divider(color = ArcTheme.colors.borderSubtle.copy(alpha = 0.5f))

            // 7 Preparation Checklist items
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                checklist.forEachIndexed { idx, (itemTitle, isChecked) ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleChecklist(idx) },
                        shape = ArcTheme.shapes.control,
                        color = if (isChecked) ArcTheme.colors.surfaceMuted else ArcTheme.colors.surface,
                        border = BorderStroke(1.dp, ArcTheme.colors.borderSubtle)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ArcCheckbox(
                                checked = isChecked,
                                onCheckedChange = { onToggleChecklist(idx) }
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = itemTitle,
                                fontSize = 14.sp,
                                color = if (isChecked) ArcTheme.colors.textSecondary else ArcTheme.colors.foreground,
                                fontWeight = if (isChecked) FontWeight.Normal else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateGroupDialog(
    students: List<StudentEntity>,
    onDismiss: () -> Unit,
    onConfirm: (String, List<String>) -> Unit
) {
    var groupTitle by remember { mutableStateOf("") }
    var selectedStudentIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = ArcTheme.shapes.panel,
            color = ArcTheme.colors.surfaceRaised,
            border = BorderStroke(1.dp, ArcTheme.colors.border),
            shadowElevation = 8.dp,
            modifier = Modifier.width(460.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "Yeni Grup Oluştur",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = ArcTheme.colors.foreground
                )
                Spacer(Modifier.height(14.dp))

                ArcInput(
                    value = groupTitle,
                    onValueChange = { groupTitle = it },
                    label = "Grup Adı",
                    placeholder = "Örn: 1. Münazara Grubu / Canlandırma Ekibi"
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "Grup Üyelerini Seçin (${selectedStudentIds.size} Seçildi):",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ArcTheme.colors.textSecondary
                )
                Spacer(Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier.height(200.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(students) { std ->
                        val isSelected = selectedStudentIds.contains(std.id)
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedStudentIds = if (isSelected) {
                                        selectedStudentIds - std.id
                                    } else {
                                        selectedStudentIds + std.id
                                    }
                                },
                            shape = ArcTheme.shapes.control,
                            color = if (isSelected) ArcTheme.colors.surfaceMuted else ArcTheme.colors.surface,
                            border = BorderStroke(1.dp, ArcTheme.colors.borderSubtle)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ArcCheckbox(
                                    checked = isSelected,
                                    onCheckedChange = { checked ->
                                        selectedStudentIds = if (checked) {
                                            selectedStudentIds + std.id
                                        } else {
                                            selectedStudentIds - std.id
                                        }
                                    }
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "${std.studentNumber} - ${std.fullName}",
                                    fontSize = 13.sp,
                                    color = ArcTheme.colors.foreground
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    ArcButton(
                        text = "Vazgeç",
                        onClick = onDismiss,
                        variant = ArcButtonVariant.Ghost
                    )
                    Spacer(Modifier.width(8.dp))
                    ArcButton(
                        text = "Grubu Kur",
                        onClick = {
                            if (groupTitle.isNotBlank() && selectedStudentIds.isNotEmpty()) {
                                onConfirm(groupTitle.trim(), selectedStudentIds.toList())
                            }
                        },
                        variant = ArcButtonVariant.Primary,
                        enabled = groupTitle.isNotBlank() && selectedStudentIds.isNotEmpty()
                    )
                }
            }
        }
    }
}
