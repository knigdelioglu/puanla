package com.knigdelioglu.puanla.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.Dispatchers
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.key
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.knigdelioglu.arc.compose.controls.ArcButton
import com.knigdelioglu.arc.compose.controls.ArcButtonSize
import com.knigdelioglu.arc.compose.controls.ArcButtonVariant
import com.knigdelioglu.arc.compose.controls.ArcConfirmMorph
import com.knigdelioglu.arc.compose.controls.ArcSwitch
import com.knigdelioglu.arc.compose.display.ArcAlert
import com.knigdelioglu.arc.compose.display.ArcAlertType
import com.knigdelioglu.arc.compose.display.ArcCard
import com.knigdelioglu.arc.compose.display.ArcThemeSwitch
import com.knigdelioglu.arc.compose.display.ArcTimeline
import com.knigdelioglu.arc.compose.display.ArcTimelineEntry
import com.knigdelioglu.arc.compose.foundation.ArcTheme
import com.knigdelioglu.puanla.ui.viewmodel.PuanlaViewModel
import com.knigdelioglu.puanla.domain.backup.BackupImport
import com.knigdelioglu.puanla.domain.backup.BackupPreview
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(viewModel: PuanlaViewModel) {
    val darkMode by viewModel.darkMode.collectAsState()
    val reduceMotion by viewModel.reduceMotion.collectAsState()
    val recentLogs by viewModel.recentLogs.collectAsState()

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var pendingBackup by remember { mutableStateOf<String?>(null) }
    var pendingPreview by remember { mutableStateOf<BackupPreview?>(null) }
    var restoring by remember { mutableStateOf(false) }
    var selectingBackup by remember { mutableStateOf(false) }
    val saveBackup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            try {
                val backup = viewModel.createBackup()
                withContext(Dispatchers.IO) {
                    val stream = requireNotNull(context.contentResolver.openOutputStream(uri)) { "Yedek dosyası açılamadı." }
                    stream.bufferedWriter(Charsets.UTF_8).use { it.write(backup) }
                }
                viewModel.showToast("Tam JSON yedeği dosyaya kaydedildi.")
            } catch (e: Exception) {
                viewModel.showToast("Yedekleme başarısız: ${e.message}")
            }
        }
    }
    val openBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        // Picking a file must NEVER modify the current database.
        pendingBackup = null
        pendingPreview = null
        if (uri != null && !selectingBackup && !restoring) scope.launch {
            selectingBackup = true
            try {
                val content = withContext(Dispatchers.IO) {
                    val stream = requireNotNull(context.contentResolver.openInputStream(uri)) { "Yedek okunamadı." }
                    stream.use { BackupImport.readLimited(it) }
                }
                val preview = withContext(Dispatchers.Default) { BackupImport.inspect(content) }
                pendingBackup = content
                pendingPreview = preview
            } catch (e: Exception) {
                pendingBackup = null
                pendingPreview = null
                viewModel.showToast("Yedek doğrulanamadı: ${e.message}")
            } finally {
                selectingBackup = false
            }
        }
    }
    val scrollState = rememberScrollState()

    val dateFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    val timelineEntries = remember(recentLogs) {
        if (recentLogs.isEmpty()) {
            listOf(
                ArcTimelineEntry(
                    title = "Sistem Başlatıldı",
                    subtitle = "Yerel Room veritabanı hazırlandı. Onaylı rubrikler yalnızca gerçek veri kaynağından yüklenir.",
                    time = "Şimdi"
                )
            )
        } else {
            recentLogs.take(8).map { log ->
                ArcTimelineEntry(
                    title = log.action,
                    subtitle = log.details,
                    time = dateFormat.format(Date(log.timestamp))
                )
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        // Header
        Column {
            Text(
                text = "Ayarlar & Veri Güvenliği",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = ArcTheme.colors.foreground
            )
            Text(
                text = "Görünüm, çalışan klavye kısayolu ve güvenli yerel yedekleme.",
                fontSize = 14.sp,
                color = ArcTheme.colors.textSecondary
            )
        }

        Divider(color = ArcTheme.colors.borderSubtle)

        // Visuals and Ergonomics Section
        Text(
            text = "Görünüm ve Hareket Ergonomisi",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = ArcTheme.colors.foreground
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ArcCard(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Tema Tercihi", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = ArcTheme.colors.foreground)
                        Text(if (darkMode == true) "Karanlık Mod Etkin" else "Aydınlık Mod Etkin", fontSize = 12.sp, color = ArcTheme.colors.textSecondary)
                    }
                    ArcThemeSwitch(
                        isDark = darkMode ?: false,
                        onToggle = { isDark ->
                            viewModel.darkMode.value = isDark
                        }
                    )
                }
            }

            ArcCard(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Animasyonları Azalt (Reduce Motion)", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = ArcTheme.colors.foreground)
                        Text("Tablet pil ömrü ve sakin geçişler", fontSize = 12.sp, color = ArcTheme.colors.textSecondary)
                    }
                    ArcSwitch(
                        checked = reduceMotion,
                        onCheckedChange = {
                            viewModel.reduceMotion.value = it
                        }
                    )
                }
            }
        }

        // Only supported keyboard actions belong in Settings. The old shortcut
        // recorder and signature canvas were unsaved demonstrations.
        Text(
            text = "Fiziksel Klavye",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = ArcTheme.colors.foreground
        )
        ArcAlert(
            title = "Komut Paleti",
            message = "Bağlı fiziksel klavyede Ctrl+K veya ⌘K tuşlarıyla komut paletini açabilirsiniz. " +
                "Öğrenciye hızlı geçiş ve geri alma eylemleri paletten erişilebilir. " +
                "Diğer tuşlar için henüz özelleştirilebilir bir kısayol kaydı bulunmuyor.",
            type = ArcAlertType.Info
        )

        // Backup and Restore Section
        Text(
            text = "Çevrimdışı Veri Güvenliği ve Yedekleme (SAF)",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = ArcTheme.colors.foreground
        )

        ArcCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "Tüm sınıf listeleri, rubrik puanları ve grup çalışmaları cihaz içi Room veritabanında saklanır. Harici sunucuya veri aktarılmaz.",
                    fontSize = 14.sp,
                    color = ArcTheme.colors.textSecondary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ArcButton(
                        text = "Tam JSON Yedeği Kaydet",
                        onClick = { saveBackup.launch("puanla-yedek.json") },
                        variant = ArcButtonVariant.Primary,
                        size = ArcButtonSize.Md
                    )
                    ArcButton(
                        text = if (selectingBackup) "Yedek Doğrulanıyor..." else "Yedek Dosyası Seç",
                        onClick = { if (!selectingBackup && !restoring) openBackup.launch(arrayOf("application/json", "text/plain")) },
                        enabled = !selectingBackup && !restoring,
                        variant = ArcButtonVariant.Outline,
                        size = ArcButtonSize.Md
                    )
                }

                val preview = pendingPreview
                if (preview != null && pendingBackup != null) {
                    ArcAlert(
                        title = "Doğrulandı — geri yükleme henüz yapılmadı",
                        message = "Yedek: ${preview.classrooms} sınıf, ${preview.students} öğrenci, " +
                            "${preview.rubrics} rubrik, ${preview.assessments} değerlendirme, " +
                            "${preview.scores} ölçüt puanı ve ${preview.groups} grup. " +
                            "Bu işlem cihazdaki mevcut TÜM verilerin yerine bu yedeği koyacak. " +
                            "Devam etmeden önce mevcut verilerin ayrı bir yedeğini alın.",
                        type = ArcAlertType.Warning
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ArcButton(
                            text = "Geri Yüklemeyi İptal Et",
                            onClick = { pendingBackup = null; pendingPreview = null },
                            variant = ArcButtonVariant.Ghost,
                            enabled = !restoring
                        )
                        if (!restoring) {
                            key(preview.exportedAt, preview.students, preview.assessments) {
                                ArcConfirmMorph(
                                    prompt = "Mevcut veriler değiştirilsin mi?",
                                    initialLabel = "Bu Yedeği Geri Yükle",
                                    confirmLabel = "Evet, Üzerine Yaz",
                                    onConfirm = {
                                        val selected = pendingBackup
                                        if (selected != null && !restoring) scope.launch {
                                            restoring = true
                                            try {
                                                val outcome = viewModel.restoreBackup(selected)
                                                outcome.onSuccess {
                                                    pendingBackup = null
                                                    pendingPreview = null
                                                    viewModel.showToast("$it öğrenci ve ilişkili kayıtları geri yüklendi.")
                                                }.onFailure {
                                                    viewModel.showToast("Geri yükleme reddedildi: ${it.message}")
                                                }
                                            } catch (cancel: kotlinx.coroutines.CancellationException) {
                                                throw cancel
                                            } catch (e: Exception) {
                                                viewModel.showToast("Geri yükleme başarısız: ${e.message}")
                                            } finally {
                                                restoring = false
                                            }
                                        }
                                    }
                                )
                            }
                        } else {
                            Text("Geri yükleme sürüyor...", color = ArcTheme.colors.textSecondary)
                        }
                    }
                }
            }
        }

        // Activity Timeline
        Text(
            text = "Etkinlik ve Denetim Günlüğü (Audit Log)",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = ArcTheme.colors.foreground
        )

        ArcCard(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.padding(18.dp)) {
                ArcTimeline(entries = timelineEntries)
            }
        }

        // Example FAQ, fake rubric JSON and static changelog stay in the
        // component gallery; Settings only shows working user-facing controls.
    }
}
