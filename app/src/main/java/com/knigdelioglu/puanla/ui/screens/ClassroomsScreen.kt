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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.knigdelioglu.arc.compose.controls.ArcHoldToConfirm
import com.knigdelioglu.arc.compose.controls.ArcInlineEdit
import com.knigdelioglu.arc.compose.controls.ArcInput
import com.knigdelioglu.arc.compose.controls.ArcSearchField
import com.knigdelioglu.arc.compose.controls.ArcSegmentedControl
import com.knigdelioglu.arc.compose.datavis.ArcChipGroup
import com.knigdelioglu.arc.compose.datavis.ArcSortableDataTable
import com.knigdelioglu.arc.compose.datavis.ArcTableColumn
import com.knigdelioglu.arc.compose.display.ArcAvatar
import com.knigdelioglu.arc.compose.display.ArcBadge
import com.knigdelioglu.arc.compose.display.ArcBadgeVariant
import com.knigdelioglu.arc.compose.display.ArcCard
import com.knigdelioglu.arc.compose.display.ArcDialog
import com.knigdelioglu.arc.compose.display.ArcEmptyState
import com.knigdelioglu.arc.compose.foundation.ArcTheme
import com.knigdelioglu.puanla.data.local.ClassroomEntity
import com.knigdelioglu.puanla.data.local.StudentEntity
import com.knigdelioglu.puanla.data.local.fullName
import com.knigdelioglu.puanla.ui.viewmodel.PuanlaViewModel

@Composable
fun ClassroomsScreen(viewModel: PuanlaViewModel) {
    val classrooms by viewModel.classrooms.collectAsState()
    val selectedClassroom by viewModel.selectedClassroom.collectAsState()
    val students by viewModel.students.collectAsState()
    val classAssessments by viewModel.classAssessments.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedGradeFilter by remember { mutableIntStateOf(0) } // 0: Tümü, 9..12
    var isAddClassDialogOpen by remember { mutableStateOf(false) }
    var isAddStudentDialogOpen by remember { mutableStateOf(false) }

    val gradeChips = listOf("Tümü", "9. Sınıf", "10. Sınıf", "11. Sınıf", "12. Sınıf")

    val filteredClassrooms = remember(classrooms, selectedGradeFilter) {
        if (selectedGradeFilter == 0) classrooms
        else {
            val targetGrade = selectedGradeFilter + 8
            classrooms.filter { it.grade == targetGrade }
        }
    }

    val filteredStudents = remember(students, searchQuery) {
        if (searchQuery.isBlank()) students
        else students.filter {
            it.fullName.contains(searchQuery, ignoreCase = true) || it.studentNumber.contains(searchQuery)
        }
    }

    val assessmentMap = remember(classAssessments) {
        classAssessments.associateBy { it.studentId }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Top Toolbar: Class cards and actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Sınıf ve Öğrenci Yönetimi",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = ArcTheme.colors.foreground
                )
                Text(
                    text = "Şubeleri yönetin, öğrenci ekleyin veya basılı listeden fotoğraf ile aktarın.",
                    fontSize = 14.sp,
                    color = ArcTheme.colors.textSecondary
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ArcButton(
                    text = "Yeni Sınıf Ekle",
                    onClick = { isAddClassDialogOpen = true },
                    variant = ArcButtonVariant.Outline,
                    size = ArcButtonSize.Md
                )
                ArcButton(
                    text = "Öğrenci Ekle",
                    onClick = { isAddStudentDialogOpen = true },
                    variant = ArcButtonVariant.Primary,
                    size = ArcButtonSize.Md,
                    enabled = selectedClassroom != null
                )
            }
        }

        // Grade Filter Chips
        ArcChipGroup(
            items = gradeChips,
            selectedItem = gradeChips.getOrNull(selectedGradeFilter) ?: gradeChips.first(),
            onItemSelected = { selectedGradeFilter = gradeChips.indexOf(it).coerceAtLeast(0) }
        )


        // Classrooms Carousel / Row
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(filteredClassrooms) { cls ->
                val isSelected = cls.id == selectedClassroom?.id
                ArcCard(
                    modifier = Modifier
                        .clickable { viewModel.selectClassroom(cls) },
                    color = if (isSelected) ArcTheme.colors.surfaceRaised else ArcTheme.colors.surface,
                    border = BorderStroke(
                        if (isSelected) 2.dp else 1.dp,
                        if (isSelected) ArcTheme.colors.accent else ArcTheme.colors.borderSubtle
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = ArcTheme.shapes.pill,
                            color = if (isSelected) ArcTheme.colors.accent else ArcTheme.colors.surfaceMuted,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "${cls.grade}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = if (isSelected) ArcTheme.colors.accentForeground else ArcTheme.colors.foreground
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = cls.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = ArcTheme.colors.foreground
                            )
                            Text(
                                text = cls.academicYear,
                                fontSize = 12.sp,
                                color = ArcTheme.colors.textSecondary
                            )
                        }
                        if (isSelected) {
                            Spacer(Modifier.width(10.dp))
                            ArcBadge(text = "Aktif", variant = ArcBadgeVariant.Success)
                        }
                    }
                }
            }
        }

        Divider(color = ArcTheme.colors.borderSubtle)

        // Students Table Section
        if (selectedClassroom == null) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                ArcEmptyState(
                    title = "Sınıf Seçilmedi",
                    description = "Öğrencileri görüntülemek için yukarıdan bir sınıf seçin."
                )
            }
        } else {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${selectedClassroom?.name} Öğrenci Kadrosu (${filteredStudents.size} Öğrenci)",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = ArcTheme.colors.foreground
                    )
                    ArcSearchField(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        placeholder = "Öğrenci ara...",
                        modifier = Modifier.width(280.dp)
                    )
                }

                Spacer(Modifier.height(12.dp))

                if (filteredStudents.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        ArcEmptyState(
                            title = "Öğrenci Bulunmuyor",
                            description = "Bu sınıfa henüz öğrenci eklenmemiş. 'Öğrenci Ekle' veya 'Fotoğraftan Aktarım' menüsünü kullanabilirsiniz."
                        )
                    }
                } else {
                    val columns = listOf(
                        ArcTableColumn<StudentEntity>(
                            title = "No",
                            weight = 0.8f,
                            comparator = compareBy { it.studentNumber.toIntOrNull() ?: 0 }
                        ) { student ->
                            Text(
                                text = student.studentNumber,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = ArcTheme.colors.foreground
                            )
                        },
                        ArcTableColumn<StudentEntity>(
                            title = "Adı",
                            weight = 1.5f,
                            comparator = compareBy { it.firstName }
                        ) { student ->
                            ArcInlineEdit(
                                value = student.firstName,
                                onCommit = { newName ->
                                    viewModel.updateStudent(student.copy(firstName = newName.trim()))
                                }
                            )
                        },
                        ArcTableColumn<StudentEntity>(
                            title = "Soyadı",
                            weight = 1.5f,
                            comparator = compareBy { it.lastName }
                        ) { student ->
                            ArcInlineEdit(
                                value = student.lastName,
                                onCommit = { newSurname ->
                                    viewModel.updateStudent(student.copy(lastName = newSurname.trim()))
                                }
                            )
                        },
                        ArcTableColumn<StudentEntity>(
                            title = "Değerlendirme Durumu",
                            weight = 1.5f
                        ) { student ->
                            val asm = assessmentMap[student.id]
                            when {
                                asm?.isCompleted == true -> ArcBadge(
                                    text = "Tamamlandı (${asm.definitiveTotal})",
                                    variant = ArcBadgeVariant.Success
                                )
                                (asm?.scoredCount ?: 0) > 0 -> ArcBadge(
                                    text = "${asm?.scoredCount} Puanlandı",
                                    variant = ArcBadgeVariant.Warning
                                )
                                else -> ArcBadge(text = "Puanlanmadı", variant = ArcBadgeVariant.Default)
                            }
                        },
                        ArcTableColumn<StudentEntity>(
                            title = "İşlem",
                            weight = 0.8f
                        ) { student ->
                            var showDeleteDialog by remember { mutableStateOf(false) }

                            IconButton(onClick = { showDeleteDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Sil",
                                    tint = ArcTheme.colors.danger,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            ArcDialog(
                                isOpen = showDeleteDialog,
                                onDismissRequest = { showDeleteDialog = false },
                                title = "Öğrenciyi Sil",
                                description = "${student.fullName} (${student.studentNumber}) kaydı silinecektir. Bu işlem geri alınamaz.",
                                confirmText = "Sil",
                                cancelText = "Vazgeç",
                                onConfirm = {
                                    viewModel.deleteStudent(student)
                                    showDeleteDialog = false
                                }
                            )
                        }
                    )

                    ArcSortableDataTable(
                        items = filteredStudents,
                        columns = columns,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    // Add Classroom Dialog
    if (isAddClassDialogOpen) {
        AddClassroomDialog(
            onDismiss = { isAddClassDialogOpen = false },
            onConfirm = { grade, section ->
                viewModel.addClassroom(grade, section) { success, message ->
                    if (success) {
                        isAddClassDialogOpen = false
                    }
                    viewModel.showToast(message)
                }
            }
        )
    }

    // Add Student Dialog
    if (isAddStudentDialogOpen && selectedClassroom != null) {
        AddStudentDialog(
            classroomName = selectedClassroom?.name ?: "",
            onDismiss = { isAddStudentDialogOpen = false },
            onConfirm = { no, name, surname ->
                viewModel.addStudent(no, name, surname) { success, message ->
                    if (success) {
                        isAddStudentDialogOpen = false
                    }
                    viewModel.showToast(message)
                }
            }
        )
    }
}

@Composable
private fun AddClassroomDialog(
    onDismiss: () -> Unit,
    onConfirm: (Int, String) -> Unit
) {
    var selectedGradeIndex by remember { mutableIntStateOf(2) } // default 11
    var sectionText by remember { mutableStateOf("A") }
    val grades = listOf("9", "10", "11", "12")

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = ArcTheme.shapes.panel,
            color = ArcTheme.colors.surfaceRaised,
            border = BorderStroke(1.dp, ArcTheme.colors.border),
            shadowElevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "Yeni Sınıf / Şube Ekle",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = ArcTheme.colors.foreground
                )
                Spacer(Modifier.height(16.dp))

                Text("Sınıf Düzeyi:", fontSize = 13.sp, color = ArcTheme.colors.textSecondary)
                Spacer(Modifier.height(6.dp))
                ArcSegmentedControl(
                    items = grades.map { "$it. Sınıf" },
                    selectedIndex = selectedGradeIndex,
                    onSelectIndex = { selectedGradeIndex = it }
                )

                Spacer(Modifier.height(16.dp))

                ArcInput(
                    value = sectionText,
                    onValueChange = { sectionText = it.uppercase() },
                    label = "Şube Harfi",
                    placeholder = "Örn: A, B, C, D..."
                )

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
                        text = "Oluştur",
                        onClick = {
                            val grade = selectedGradeIndex + 9
                            if (sectionText.isNotBlank()) {
                                onConfirm(grade, sectionText.trim())
                            }
                        },
                        variant = ArcButtonVariant.Primary
                    )
                }
            }
        }
    }
}

@Composable
private fun AddStudentDialog(
    classroomName: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    var studentNumber by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = ArcTheme.shapes.panel,
            color = ArcTheme.colors.surfaceRaised,
            border = BorderStroke(1.dp, ArcTheme.colors.border),
            shadowElevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "$classroomName Sınıfına Öğrenci Ekle",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = ArcTheme.colors.foreground
                )
                Spacer(Modifier.height(16.dp))

                ArcInput(
                    value = studentNumber,
                    onValueChange = { studentNumber = it.filter { ch -> ch.isDigit() } },
                    label = "Öğrenci Numarası",
                    placeholder = "Örn: 105"
                )

                Spacer(Modifier.height(12.dp))

                ArcInput(
                    value = firstName,
                    onValueChange = { firstName = it },
                    label = "Adı",
                    placeholder = "Örn: Ahmet"
                )

                Spacer(Modifier.height(12.dp))

                ArcInput(
                    value = lastName,
                    onValueChange = { lastName = it },
                    label = "Soyadı",
                    placeholder = "Örn: Yılmaz"
                )

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
                        text = "Kaydet",
                        onClick = {
                            if (studentNumber.isNotBlank() && firstName.isNotBlank() && lastName.isNotBlank()) {
                                onConfirm(studentNumber.trim(), firstName.trim(), lastName.trim())
                            }
                        },
                        variant = ArcButtonVariant.Primary
                    )
                }
            }
        }
    }
}
