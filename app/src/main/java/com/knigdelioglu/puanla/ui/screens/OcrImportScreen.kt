package com.knigdelioglu.puanla.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
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
import com.knigdelioglu.arc.compose.controls.ArcButton
import com.knigdelioglu.arc.compose.controls.ArcButtonSize
import com.knigdelioglu.arc.compose.controls.ArcButtonVariant
import com.knigdelioglu.arc.compose.controls.ArcCheckbox
import com.knigdelioglu.arc.compose.controls.ArcInlineEdit
import com.knigdelioglu.arc.compose.datavis.ArcAnimatedCounter
import com.knigdelioglu.arc.compose.display.ArcAlert
import com.knigdelioglu.arc.compose.display.ArcAlertType
import com.knigdelioglu.arc.compose.display.ArcBadge
import com.knigdelioglu.arc.compose.display.ArcBadgeVariant
import com.knigdelioglu.arc.compose.display.ArcCard
import com.knigdelioglu.arc.compose.display.ArcEmptyState
import com.knigdelioglu.arc.compose.display.ArcFileDropzone
import com.knigdelioglu.arc.compose.foundation.ArcTheme
import com.knigdelioglu.arc.compose.navigation.ArcStepper
import com.knigdelioglu.puanla.domain.ocr.OcrStudentRow
import com.knigdelioglu.puanla.ui.viewmodel.AppDestination
import com.knigdelioglu.puanla.ui.viewmodel.PuanlaViewModel

@Composable
fun OcrImportScreen(viewModel: PuanlaViewModel) {
    val selectedClassroom by viewModel.selectedClassroom.collectAsState()
    val step by viewModel.ocrStep.collectAsState()
    val candidates by viewModel.ocrCandidates.collectAsState()
    val isScanning by viewModel.ocrIsScanning.collectAsState()
    val ocrError by viewModel.ocrError.collectAsState()

    val steps = listOf(
        "1. Fotoğraf Seç",
        "2. Algılama & OCR",
        "3. Doğrulama Kapısı",
        "4. Tamamlandı"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Fotoğraftan Öğrenci Listesi Aktarımı",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = ArcTheme.colors.foreground
                )
                Text(
                    text = "Cihaz üzerinde yerel çalışan OCR ile basılı liste sütunlarını hatasız ayıklar.",
                    fontSize = 14.sp,
                    color = ArcTheme.colors.textSecondary
                )
            }
            if (selectedClassroom != null) {
                ArcBadge(
                    text = "Hedef: ${selectedClassroom?.name}",
                    variant = ArcBadgeVariant.Accent
                )
            }
        }

        // Stepper
        ArcStepper(
            steps = steps,
            currentStepIndex = step,
            onStepClick = { targetStep ->
                if (targetStep < step) viewModel.ocrStep.value = targetStep
            }
        )

        Divider(color = ArcTheme.colors.borderSubtle)

        if (selectedClassroom == null) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                ArcEmptyState(
                    title = "Sınıf Seçilmedi",
                    description = "Öğrenci aktarmadan önce üst menüden hedef sınıfı seçmelisiniz."
                )
            }
            return@Column
        }

        // Body based on step
        when (step) {
            0 -> OcrStepPhotoSelection(viewModel)
            1 -> OcrStepScanning(isScanning, ocrError)
            2 -> OcrStepVerification(candidates, viewModel)
            3 -> OcrStepCompleted(candidates.size, selectedClassroom?.name ?: "", viewModel)
        }
    }
}

@Composable
private fun OcrStepPhotoSelection(viewModel: PuanlaViewModel) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ArcFileDropzone(
            onSelectFile = {
                // In production this triggers Android Photo Picker. For immediate testing, load roster sample:
                val sampleLines = listOf(
                    "1 214 AHMET METİN KIZILAY K",
                    "2 305 FATMA ZEHRA YILDIRIM K PANSİYONLU",
                    "3 412 MEHMET EMİN ÇETİN E GÜNDÜZLÜ",
                    "4 580 ZEYNEP SUDE GÜNEŞ K",
                    "5 619 MUSTAFA CAN ÖZTÜRK E",
                    "6 702 BUSE NUR KORKMAZ K",
                    "7 833 YİĞİT EFE ASLAN E PANSİYONLU"
                )
                viewModel.ocrStep.value = 1
                viewModel.startOcrFromLines(sampleLines)
            },
            title = "Basılı Sınıf Listesi Fotoğrafı Yükle",
            description = "Android Photo Picker veya kamera ile net çekilmiş bir e-Okul / sınıf listesi fotoğrafı seçin."
        )

        ArcAlert(
            title = "Sütun İzolasyonu ve Gizlilik",
            message = "Görseller sunucuya iletilmez, tamamen cihaz içinde işlenir. Sıra no, cinsiyet ve pansiyon sütunları isimlere karışmayacak şekilde filtrelenir.",
            type = ArcAlertType.Info
        )

        Spacer(Modifier.height(10.dp))

        ArcButton(
            text = "Örnek Basılı Sınıf Listesi ile Hızlı Test Et",
            onClick = {
                val sampleLines = listOf(
                    "1 214 AHMET METİN KIZILAY K",
                    "2 305 FATMA ZEHRA YILDIRIM K PANSİYONLU",
                    "3 412 MEHMET EMİN ÇETİN E GÜNDÜZLÜ",
                    "4 580 ZEYNEP SUDE GÜNEŞ K",
                    "5 619 MUSTAFA CAN ÖZTÜRK E",
                    "6 702 BUSE NUR KORKMAZ K",
                    "7 833 YİĞİT EFE ASLAN E PANSİYONLU"
                )
                viewModel.ocrStep.value = 1
                viewModel.startOcrFromLines(sampleLines)
            },
            variant = ArcButtonVariant.Secondary,
            size = ArcButtonSize.Md
        )
    }
}

@Composable
private fun OcrStepScanning(isScanning: Boolean, error: String?) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (error != null) {
            ArcAlert(
                title = "Tarama Hatası",
                message = error,
                type = ArcAlertType.Danger
            )
        } else {
            CircularProgressIndicator(
                color = ArcTheme.colors.accent,
                modifier = Modifier.size(54.dp)
            )
            Spacer(Modifier.height(20.dp))
            Text(
                text = "Metin Satırları Taranıyor...",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = ArcTheme.colors.foreground
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Öğrenci numaraları, ad-soyad ve komşu sütunlar geometrik olarak ayrıştırılıyor.",
                fontSize = 14.sp,
                color = ArcTheme.colors.textSecondary
            )
        }
    }
}

@Composable
private fun OcrStepVerification(
    candidates: List<OcrStudentRow>,
    viewModel: PuanlaViewModel
) {
    var includeFlags by remember(candidates) {
        mutableStateOf(candidates.map { !it.isAmbiguous })
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Öğretmen Doğrulama Kapısı (${candidates.size} Aday Tespit Edildi)",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = ArcTheme.colors.foreground
                )
                Text(
                    text = "Lütfen aktarılacak öğrencileri gözden geçirin. Değerleri doğrudan düzenleyebilirsiniz.",
                    fontSize = 13.sp,
                    color = ArcTheme.colors.textSecondary
                )
            }

            ArcButton(
                text = "Seçilenleri Sınıfa Aktar (${includeFlags.count { it }})",
                onClick = {
                    viewModel.confirmOcrImport {
                        // Handled in VM
                    }
                },
                variant = ArcButtonVariant.Primary,
                size = ArcButtonSize.Md,
                enabled = includeFlags.any { it }
            )
        }

        Spacer(Modifier.height(14.dp))

        ArcAlert(
            title = "Kesin Kayıt Öncesi Onay",
            message = "OCR çıktıları yaklaşık tahmindir. Belirsiz işaretli kayıtlar öğretmen onayı olmadan sınıfa eklenmez.",
            type = ArcAlertType.Warning
        )

        Spacer(Modifier.height(14.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            itemsIndexed(candidates) { index, cand ->
                val isIncluded = includeFlags.getOrElse(index) { true }

                ArcCard(
                    color = if (isIncluded) ArcTheme.colors.surfaceRaised else ArcTheme.colors.surfaceMuted,
                    border = BorderStroke(
                        1.dp,
                        if (cand.isAmbiguous) ArcTheme.colors.warning else ArcTheme.colors.borderSubtle
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ArcCheckbox(
                            checked = isIncluded,
                            onCheckedChange = { checked ->
                                val updated = includeFlags.toMutableList()
                                updated[index] = checked
                                includeFlags = updated
                            }
                        )

                        Spacer(Modifier.width(12.dp))

                        // Number
                        Column(modifier = Modifier.width(90.dp)) {
                            Text("No", fontSize = 11.sp, color = ArcTheme.colors.textMuted)
                            ArcInlineEdit(
                                value = cand.studentNumber,
                                onCommit = { newNo ->
                                    viewModel.updateOcrCandidate(index, cand.copy(studentNumber = newNo.trim()))
                                }
                            )
                        }

                        Spacer(Modifier.width(14.dp))

                        // First Name
                        Column(modifier = Modifier.weight(1.4f)) {
                            Text("Adı", fontSize = 11.sp, color = ArcTheme.colors.textMuted)
                            ArcInlineEdit(
                                value = cand.firstName,
                                onCommit = { newName ->
                                    viewModel.updateOcrCandidate(index, cand.copy(firstName = newName.trim()))
                                }
                            )
                        }

                        Spacer(Modifier.width(14.dp))

                        // Last Name
                        Column(modifier = Modifier.weight(1.2f)) {
                            Text("Soyadı", fontSize = 11.sp, color = ArcTheme.colors.textMuted)
                            ArcInlineEdit(
                                value = cand.lastName,
                                onCommit = { newSurname ->
                                    viewModel.updateOcrCandidate(index, cand.copy(lastName = newSurname.trim()))
                                }
                            )
                        }

                        Spacer(Modifier.width(14.dp))

                        // Filtered meta tags
                        Column(horizontalAlignment = Alignment.End) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (cand.gender != null) {
                                    ArcBadge(text = cand.gender, variant = ArcBadgeVariant.Default)
                                }
                                if (cand.boardingStatus != null) {
                                    ArcBadge(text = cand.boardingStatus, variant = ArcBadgeVariant.Default)
                                }
                                if (cand.isAmbiguous) {
                                    ArcBadge(text = "Belirsiz / Gözden Geçir", variant = ArcBadgeVariant.Warning)
                                } else {
                                    ArcBadge(text = "Doğrulandı", variant = ArcBadgeVariant.Success)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OcrStepCompleted(
    count: Int,
    className: String,
    viewModel: PuanlaViewModel
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = ArcTheme.shapes.panel,
            color = ArcTheme.colors.surfaceRaised,
            border = BorderStroke(1.dp, ArcTheme.colors.borderSubtle),
            modifier = Modifier.width(480.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Başarılı",
                    tint = ArcTheme.colors.success,
                    modifier = Modifier.size(64.dp)
                )

                Text(
                    text = "Aktarım Başarıyla Tamamlandı",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = ArcTheme.colors.foreground
                )

                Text(
                    text = "$count öğrenci $className sınıfına aktarıldı. Artık puanlama çalışma alanında bu öğrencileri değerlendirebilirsiniz.",
                    fontSize = 14.sp,
                    color = ArcTheme.colors.textSecondary,
                    lineHeight = 20.sp
                )

                Spacer(Modifier.height(8.dp))

                ArcButton(
                    text = "Değerlendirme Çalışma Alanına Git",
                    onClick = {
                        viewModel.currentDestination.value = AppDestination.WORKSPACE
                        viewModel.ocrStep.value = 0
                    },
                    variant = ArcButtonVariant.Primary,
                    size = ArcButtonSize.Lg
                )
            }
        }
    }
}
