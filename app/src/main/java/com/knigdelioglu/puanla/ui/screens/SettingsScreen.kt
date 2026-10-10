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
import com.knigdelioglu.arc.compose.controls.ArcHoldToConfirm
import com.knigdelioglu.arc.compose.controls.ArcShortcutRecorder
import com.knigdelioglu.arc.compose.controls.ArcSignaturePad
import com.knigdelioglu.arc.compose.controls.ArcSwitch
import com.knigdelioglu.arc.compose.display.ArcAlert
import com.knigdelioglu.arc.compose.display.ArcAlertType
import com.knigdelioglu.arc.compose.display.ArcCard
import com.knigdelioglu.arc.compose.display.ArcChangelogFeed
import com.knigdelioglu.arc.compose.display.ArcFaqSection
import com.knigdelioglu.arc.compose.display.ArcJsonViewer
import com.knigdelioglu.arc.compose.display.ArcThemeSwitch
import com.knigdelioglu.arc.compose.display.ArcTimeline
import com.knigdelioglu.arc.compose.display.ArcTimelineEntry
import com.knigdelioglu.arc.compose.foundation.ArcTheme
import com.knigdelioglu.puanla.ui.viewmodel.PuanlaViewModel
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
        if (uri != null) scope.launch {
            try {
                val backup = withContext(Dispatchers.IO) {
                    val stream = requireNotNull(context.contentResolver.openInputStream(uri)) { "Yedek okunamadı." }
                    stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                }
                val outcome = viewModel.restoreBackup(backup)
                outcome.onSuccess { viewModel.showToast("Yedekten $it öğrenci ve bütün ilişkili kayıtlar geri yüklendi.") }
                    .onFailure { viewModel.showToast("Geri yükleme reddedildi: ${it.message}") }
            } catch (e: Exception) {
                viewModel.showToast("Geri yükleme başarısız: ${e.message}")
            }
        }
    }
    val scrollState = rememberScrollState()

    var shortcutKey by remember { mutableStateOf("Cmd+K") }
    var hasTeacherSignature by remember { mutableStateOf(false) }

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

    val sampleRubricJson = """
    {
      "id": "example_not_a_real_rubric",
      "title": "Temsili Test Rubriği",
      "grade": 11,
      "criteria_count": 5,
      "max_points": 100,
      "offline_enforced": true
    }
    """.trimIndent()

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
                text = "Karanlık mod, animasyon tercihleri, donanım kısayolları ve SAF yedekleme.",
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

        // Hardware, Stylus & Keyboard Section
        Text(
            text = "Donanım, Kalem ve Klavye Etkileşimi",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = ArcTheme.colors.foreground
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ArcShortcutRecorder(
                    shortcut = shortcutKey,
                    onShortcutChange = { shortcutKey = it },
                    label = "Hızlı Komut Paleti Kısayolu"
                )

                ArcAlert(
                    title = "Fiziksel Klavye Desteği",
                    message = "Tablet klavyesinde 1, 2, 3, 4 tuşları doğrudan rubrik niteliklerini seçer. Boşluk tuşu sonraki öğrenciye geçer.",
                    type = ArcAlertType.Info
                )
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Öğretmen Tutanak İmzası (Android Stylus / Kalem):",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ArcTheme.colors.foreground
                )
                ArcSignaturePad(
                    onSignatureChanged = { hasTeacherSignature = it }
                )
                Text(
                    text = if (hasTeacherSignature) "İmza yalnızca bu ekranda çizildi; kaydedilmedi ve resmi dışa aktarıma eklenmez." else "Kalem etkileşim denemesi; henüz resmi imza veya dışa aktarma özelliği değildir.",
                    fontSize = 12.sp,
                    color = if (hasTeacherSignature) ArcTheme.colors.success else ArcTheme.colors.textMuted
                )
            }
        }

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
                    ArcHoldToConfirm(
                        label = "Yedekten Geri Yüklemek İçin Basılı Tutun",
                        confirmedLabel = "Yedek Dosyası Seç",
                        onConfirm = { openBackup.launch(arrayOf("application/json", "text/plain")) },
                        variant = ArcButtonVariant.Danger
                    )
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

        // FAQ and JSON Schema
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(modifier = Modifier.weight(1.2f)) {
                Text(
                    text = "Sıkça Sorulan Sorular",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = ArcTheme.colors.foreground
                )
                Spacer(Modifier.height(8.dp))
                ArcFaqSection()
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Temsili JSON Örneği (gerçek rubrik değil)",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = ArcTheme.colors.foreground
                )
                Spacer(Modifier.height(8.dp))
                ArcJsonViewer(jsonText = sampleRubricJson)
            }
        }

        // Changelog Feed
        ArcChangelogFeed()
    }
}
