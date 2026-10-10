package com.knigdelioglu.puanla.ui.screens

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.runtime.produceState
import androidx.compose.runtime.key
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
import com.knigdelioglu.arc.compose.controls.ArcConfirmMorph
import com.knigdelioglu.arc.compose.controls.ArcInlineEdit
import com.knigdelioglu.arc.compose.datavis.ArcAnimatedCounter
import com.knigdelioglu.arc.compose.display.ArcAlert
import com.knigdelioglu.arc.compose.display.ArcAlertType
import com.knigdelioglu.arc.compose.display.ArcBadge
import com.knigdelioglu.arc.compose.display.ArcBadgeVariant
import com.knigdelioglu.arc.compose.display.ArcCard
import com.knigdelioglu.arc.compose.display.ArcEmptyState
import com.knigdelioglu.arc.compose.display.ArcFileDropzone
import com.knigdelioglu.arc.compose.display.ArcImageCompare
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
    val importedCount by viewModel.ocrImportedCount.collectAsState()

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
                    text = "Yerel OCR ile fotoğrafı tarar; tüm alanlar öğretmen onayına sunulur.",
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
            3 -> OcrStepCompleted(importedCount, selectedClassroom?.name ?: "", viewModel)
        }
    }
}

@Composable
private fun OcrStepPhotoSelection(viewModel: PuanlaViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            viewModel.ocrSelectedImageUri.value = uri
            // Keep this composable alive while decoding. Changing the step here would
            // dispose rememberCoroutineScope and cancel image loading before ML Kit starts.
            // ViewModel moves to the scanning step once it owns the decoded bitmap.
            scope.launch {
                try {
                    val bitmap = withContext(Dispatchers.IO) {
                        val resolver = context.contentResolver
                        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
                        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Görsel okunamadı." }
                        var sample = 1
                        while (maxOf(bounds.outWidth / sample, bounds.outHeight / sample) > 2400) sample *= 2
                        val options = BitmapFactory.Options().apply { inSampleSize = sample }
                        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
                            ?: error("Görsel çözümlenemedi.")
                    }
                    viewModel.startOcrFromBitmap(bitmap)
                } catch (e: Exception) {
                    viewModel.reportOcrImageError("Görsel açılamadı: ${e.message}")
                }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ArcFileDropzone(
            onSelectFile = { imagePicker.launch("image/*") },
            title = "Basılı Sınıf Listesi Fotoğrafı Seç",
            description = "Galeriden gerçek bir sınıf listesi fotoğrafı seçin. Fotoğraf cihazda işlenir."
        )
        ArcAlert(
            title = "Öğretmen Onayı Zorunlu",
            message = "OCR satırları önce taslak olarak gösterilir. Soyad ve numarayı doğrulamadan hiçbir satır sınıfa aktarılmaz.",
            type = ArcAlertType.Info
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

            if (candidates.any { it.isApproved }) {
                // An explicit second confirmation before writing an OCR batch to Room.
                key(candidates.filter { it.isApproved }.map { it.id }) {
                    ArcConfirmMorph(
                        prompt = "${candidates.count { it.isApproved }} öğrenci aktarılsın mı?",
                        initialLabel = "Onaylananları Aktar (${candidates.count { it.isApproved }})",
                        confirmLabel = "Aktar",
                        onConfirm = { viewModel.confirmOcrImport { } }
                    )
                }
            } else {
                ArcButton(
                    text = "Önce Öğrencileri Onaylayın",
                    onClick = {},
                    enabled = false,
                    variant = ArcButtonVariant.Secondary,
                    size = ArcButtonSize.Md
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        ArcAlert(
            title = "Kesin Kayıt Öncesi Onay",
            message = "OCR çıktıları yaklaşık tahmindir. Belirsiz işaretli kayıtlar öğretmen onayı olmadan sınıfa eklenmez.",
            type = ArcAlertType.Warning
        )

        val importError by viewModel.ocrError.collectAsState()
        if (importError != null) ArcAlert(title = "Aktarım Hatası", message = importError ?: "", type = ArcAlertType.Danger)
        Spacer(Modifier.height(12.dp))

        val photoUri by viewModel.ocrSelectedImageUri.collectAsState()
        var showOriginal by remember(photoUri) { mutableStateOf(false) }
        if (photoUri != null) {
            ArcButton(
                text = if (showOriginal) "Fotoğraf Karşılaştırmasını Gizle" else "Kaynak Fotoğrafı OCR ile Karşılaştır",
                onClick = { showOriginal = !showOriginal },
                variant = ArcButtonVariant.Outline,
                size = ArcButtonSize.Sm
            )
            if (showOriginal) {
                OcrSourceComparison(photoUri, candidates)
                Spacer(Modifier.height(10.dp))
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            itemsIndexed(candidates) { index, cand ->
                val isIncluded = cand.isApproved

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
                                viewModel.updateOcrCandidate(index, cand.copy(isApproved = checked))
                            }
                        )

                        Spacer(Modifier.width(12.dp))

                        // Number
                        Column(modifier = Modifier.width(90.dp)) {
                            Text("No", fontSize = 11.sp, color = ArcTheme.colors.textMuted)
                            ArcInlineEdit(
                                value = cand.studentNumber,
                                onCommit = { newNo ->
                                    viewModel.updateOcrCandidate(index, cand.copy(studentNumber = newNo.trim(), isApproved = false))
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
                                    viewModel.updateOcrCandidate(index, cand.copy(firstName = newName.trim(), isApproved = false))
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
                                    viewModel.updateOcrCandidate(index, cand.copy(lastName = newSurname.trim(), isApproved = false))
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
                                    ArcBadge(text = if (cand.isApproved) "Öğretmen onayladı" else "Onay bekliyor", variant = if (cand.isApproved) ArcBadgeVariant.Success else ArcBadgeVariant.Default)
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
private fun OcrSourceComparison(uri: Uri?, candidates: List<OcrStudentRow>) {
    val context = LocalContext.current
    val preview by produceState<ImageBitmap?>(initialValue = null, key1 = uri) {
        value = if (uri == null) null else withContext(Dispatchers.IO) {
            try {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
                if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null
                var sample = 1
                while (maxOf(bounds.outWidth / sample, bounds.outHeight / sample) > 1400) sample *= 2
                val options = BitmapFactory.Options().apply { inSampleSize = sample }
                context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, options)
                }?.asImageBitmap()
            } catch (_: Exception) {
                null
            }
        }
    }
    val bitmap = preview
    if (bitmap == null) {
        ArcAlert(
            title = "Fotoğraf önizlemesi",
            message = "Seçilen fotoğraf henüz yüklenemedi veya erişilemiyor. Öğrenci bilgilerini yine de tek tek kontrol edin.",
            type = ArcAlertType.Warning
        )
        return
    }
    ArcImageCompare(
        beforeContent = {
            Image(
                bitmap = bitmap,
                contentDescription = "Öğrenci listesinin orijinal fotoğrafı",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        },
        afterContent = {
            Column(
                modifier = Modifier.fillMaxSize().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("OCR çıktısından ilk satırlar", fontWeight = FontWeight.Bold, color = ArcTheme.colors.foreground)
                candidates.take(5).forEach {
                    Text(
                        text = "${it.studentNumber}  ${it.firstName}  ${it.lastName}",
                        maxLines = 1,
                        color = ArcTheme.colors.textSecondary,
                        fontSize = 13.sp
                    )
                }
                if (candidates.size > 5) Text(
                    text = "Ve ${candidates.size - 5} satır daha; tamamını aşağıdan doğrulayın.",
                    color = ArcTheme.colors.textMuted,
                    fontSize = 12.sp
                )
            }
        },
        beforeLabel = "Kaynak fotoğraf",
        afterLabel = "Okunan alanlar",
        modifier = Modifier.fillMaxWidth()
    )
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
