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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.knigdelioglu.arc.compose.controls.ArcActionButton
import com.knigdelioglu.arc.compose.controls.ArcButton
import com.knigdelioglu.arc.compose.controls.ArcButtonGroup
import com.knigdelioglu.arc.compose.controls.ArcButtonSize
import com.knigdelioglu.arc.compose.controls.ArcButtonVariant
import com.knigdelioglu.arc.compose.controls.ArcCalendar
import com.knigdelioglu.arc.compose.controls.ArcCheckbox
import com.knigdelioglu.arc.compose.controls.ArcColorPicker
import com.knigdelioglu.arc.compose.controls.ArcCombobox
import com.knigdelioglu.arc.compose.controls.ArcConfirmMorph
import com.knigdelioglu.arc.compose.controls.ArcDatePicker
import com.knigdelioglu.arc.compose.controls.ArcDateRangePicker
import com.knigdelioglu.arc.compose.controls.ArcDropdownMenu
import com.knigdelioglu.arc.compose.controls.ArcElasticSlider
import com.knigdelioglu.arc.compose.controls.ArcExpandingButtonGroup
import com.knigdelioglu.arc.compose.controls.ArcExpandingSearch
import com.knigdelioglu.arc.compose.controls.ArcHoldToConfirm
import com.knigdelioglu.arc.compose.controls.ArcInlineEdit
import com.knigdelioglu.arc.compose.controls.ArcInput
import com.knigdelioglu.arc.compose.controls.ArcMorphSelect
import com.knigdelioglu.arc.compose.controls.ArcMultiSelect
import com.knigdelioglu.arc.compose.controls.ArcNumberField
import com.knigdelioglu.arc.compose.controls.ArcRadioCards
import com.knigdelioglu.arc.compose.controls.ArcRadioGroup
import com.knigdelioglu.arc.compose.controls.ArcSearchField
import com.knigdelioglu.arc.compose.controls.ArcSegmentedControl
import com.knigdelioglu.arc.compose.controls.ArcSelect
import com.knigdelioglu.arc.compose.controls.ArcShortcutRecorder
import com.knigdelioglu.arc.compose.controls.ArcSignaturePad
import com.knigdelioglu.arc.compose.controls.ArcSlider
import com.knigdelioglu.arc.compose.controls.ArcSplitButton
import com.knigdelioglu.arc.compose.controls.ArcSwitch
import com.knigdelioglu.arc.compose.controls.ArcTagInput
import com.knigdelioglu.arc.compose.controls.ArcTextarea
import com.knigdelioglu.arc.compose.controls.ArcTimePicker
import com.knigdelioglu.arc.compose.datavis.ArcActivityHeatmap
import com.knigdelioglu.arc.compose.datavis.ArcAnimatedCounter
import com.knigdelioglu.arc.compose.datavis.ArcBarChart
import com.knigdelioglu.arc.compose.datavis.ArcBarData
import com.knigdelioglu.arc.compose.datavis.ArcChipGroup
import com.knigdelioglu.arc.compose.datavis.ArcDonutChart
import com.knigdelioglu.arc.compose.datavis.ArcDonutSegment
import com.knigdelioglu.arc.compose.datavis.ArcGauge
import com.knigdelioglu.arc.compose.datavis.ArcLineChart
import com.knigdelioglu.arc.compose.datavis.ArcMetricCard
import com.knigdelioglu.arc.compose.datavis.ArcSlopeChart
import com.knigdelioglu.arc.compose.datavis.ArcSlotText
import com.knigdelioglu.arc.compose.datavis.ArcSparkline
import com.knigdelioglu.arc.compose.datavis.ArcStatsBand
import com.knigdelioglu.arc.compose.datavis.ArcTextMorph
import com.knigdelioglu.arc.compose.datavis.ArcTextReveal
import com.knigdelioglu.arc.compose.datavis.ArcTextShimmer
import com.knigdelioglu.arc.compose.datavis.ArcUsageMeter
import com.knigdelioglu.arc.compose.datavis.ArcWaffleChart
import com.knigdelioglu.arc.compose.display.ArcAccordion
import com.knigdelioglu.arc.compose.display.ArcAlert
import com.knigdelioglu.arc.compose.display.ArcAlertType
import com.knigdelioglu.arc.compose.display.ArcAnnouncementBar
import com.knigdelioglu.arc.compose.display.ArcAvatar
import com.knigdelioglu.arc.compose.display.ArcAvatarGroup
import com.knigdelioglu.arc.compose.display.ArcBadge
import com.knigdelioglu.arc.compose.display.ArcBadgeVariant
import com.knigdelioglu.arc.compose.display.ArcCard
import com.knigdelioglu.arc.compose.display.ArcChangelogFeed
import com.knigdelioglu.arc.compose.display.ArcComparisonTable
import com.knigdelioglu.arc.compose.display.ArcCopyButton
import com.knigdelioglu.arc.compose.display.ArcDialog
import com.knigdelioglu.arc.compose.display.ArcEmptyState
import com.knigdelioglu.arc.compose.display.ArcExpandableCard
import com.knigdelioglu.arc.compose.display.ArcFaqSection
import com.knigdelioglu.arc.compose.display.ArcFileDropzone
import com.knigdelioglu.arc.compose.display.ArcFileUpload
import com.knigdelioglu.arc.compose.display.ArcImageCompare
import com.knigdelioglu.arc.compose.display.ArcJsonViewer
import com.knigdelioglu.arc.compose.display.ArcNotificationCenter
import com.knigdelioglu.arc.compose.display.ArcPopover
import com.knigdelioglu.arc.compose.display.ArcProgress
import com.knigdelioglu.arc.compose.display.ArcSkeleton
import com.knigdelioglu.arc.compose.display.ArcThemeSwitch
import com.knigdelioglu.arc.compose.display.ArcTimeline
import com.knigdelioglu.arc.compose.display.ArcTimelineEntry
import com.knigdelioglu.arc.compose.display.ArcTooltip
import com.knigdelioglu.arc.compose.display.ArcTreeNode
import com.knigdelioglu.arc.compose.display.ArcTreeView
import com.knigdelioglu.arc.compose.display.ArcUserMenu
import com.knigdelioglu.arc.compose.foundation.ArcTheme
import com.knigdelioglu.arc.compose.navigation.ArcBreadcrumb
import com.knigdelioglu.arc.compose.navigation.ArcCardStack
import com.knigdelioglu.arc.compose.navigation.ArcCarousel
import com.knigdelioglu.arc.compose.navigation.ArcPagination
import com.knigdelioglu.arc.compose.navigation.ArcStepper
import com.knigdelioglu.arc.compose.navigation.ArcTabs

@Composable
fun CatalogGalleryScreen() {
    val scrollState = rememberScrollState()
    val colors = ArcTheme.colors

    var activeTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Kontroller", "Gezinme & Düzen", "Gösterim & Geri Bildirim", "Veri & Grafikler")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        // Header
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Arc Compose Bileşen Kataloğu",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.foreground
                )
                Spacer(Modifier.width(12.dp))
                ArcBadge(text = "99 / 99 Kayıt Hazır", variant = ArcBadgeVariant.Success)
            }
            Text(
                text = "P0, P1 ve P2 kapsamındaki tüm Arc Library bileşenlerinin canlı Android Jetpack Compose karşılıkları.",
                fontSize = 14.sp,
                color = colors.textSecondary
            )
        }

        ArcTabs(
            tabs = tabs,
            selectedTabIndex = activeTab,
            onTabSelected = { activeTab = it }
        )

        Divider(color = colors.borderSubtle)

        when (activeTab) {
            0 -> ControlsSection()
            1 -> NavigationSection()
            2 -> DisplaySection()
            3 -> DataVisSection()
        }
    }
}

@Composable
private fun ControlsSection() {
    val colors = ArcTheme.colors
    var buttonClicks by remember { mutableIntStateOf(0) }
    var inputText by remember { mutableStateOf("Örnek Metin") }
    var sliderValue by remember { mutableFloatStateOf(65f) }
    var elasticValue by remember { mutableFloatStateOf(20f) }
    var checkboxState by remember { mutableStateOf(true) }
    var switchState by remember { mutableStateOf(false) }
    var selectedRadio by remember { mutableStateOf("Seçenek A") }
    var selectedSegment by remember { mutableIntStateOf(1) }
    var selectedColor by remember { mutableStateOf(Color(0xFF6366F1)) }
    val tags = remember { mutableStateListOf("Türk Dili", "Edebiyat", "Hitabet") }

    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        // Buttons
        ArcCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Düğmeler (Buttons & Action Groups)", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.foreground)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    ArcButton(text = "Primary", onClick = { buttonClicks++ }, variant = ArcButtonVariant.Primary)
                    ArcButton(text = "Secondary", onClick = { buttonClicks++ }, variant = ArcButtonVariant.Secondary)
                    ArcButton(text = "Outline", onClick = { buttonClicks++ }, variant = ArcButtonVariant.Outline)
                    ArcButton(text = "Ghost", onClick = { buttonClicks++ }, variant = ArcButtonVariant.Ghost)
                    ArcButton(text = "Danger", onClick = { buttonClicks++ }, variant = ArcButtonVariant.Danger)
                    ArcActionButton(onClick = { buttonClicks++ }) {
                        Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    ArcButtonGroup(
                        buttons = listOf(
                            "Kaydet" to { buttonClicks++ },
                            "Önizle" to { buttonClicks++ },
                            "Paylaş" to { buttonClicks++ }
                        )
                    )
                    ArcSplitButton(
                        primaryText = "Dışa Aktar",
                        onPrimaryClick = { buttonClicks++ },
                        menuItems = listOf("PDF İndir", "CSV İndir", "JSON İndir"),
                        onMenuItemClick = { buttonClicks++ }
                    )
                }
            }
        }

        // Sliders & Numeric Inputs
        ArcCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Sürgüler ve Hassas Sayı Alanları (Sliders & Steppers)", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.foreground)
                ArcElasticSlider(
                    value = elasticValue,
                    onValueChange = { elasticValue = it },
                    valueRange = 0f..25f,
                    label = "Arc Elastic Slider (Balon İpucu ve Direnç)",
                    formatValue = { "${it.toInt()} / 25 Puan" }
                )
                ArcSlider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    valueRange = 0f..100f
                )
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    ArcNumberField(
                        value = elasticValue.toInt(),
                        onValueChange = { elasticValue = it.toFloat() },
                        min = 0,
                        max = 25,
                        label = "Hassas Sayı Girişi"
                    )
                    ArcInlineEdit(
                        value = "Doğrudan Düzenlenebilir Metin",
                        onCommit = {}
                    )
                }
            }
        }

        // Selection & Toggles
        ArcCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Seçim Kontrolleri (Selection Controls)", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.foreground)
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    ArcCheckbox(
                        checked = checkboxState,
                        onCheckedChange = { checkboxState = it },
                        label = "Onay Kutusu"
                    )
                    ArcSwitch(
                        checked = switchState,
                        onCheckedChange = { switchState = it },
                        label = "Geçiş Anahtarı"
                    )
                }
                ArcSegmentedControl(
                    items = listOf("Yetersiz", "Geliştirilmeli", "İyi", "Çok İyi"),
                    selectedIndex = selectedSegment,
                    onSelectIndex = { selectedSegment = it }
                )
                ArcRadioGroup(
                    options = listOf("Seçenek A", "Seçenek B", "Seçenek C"),
                    selectedOption = selectedRadio,
                    onOptionSelected = { selectedRadio = it }
                )
                ArcTagInput(
                    tags = tags,
                    onAddTag = { tags.add(it) },
                    onRemoveTag = { tags.remove(it) },
                    label = "Etiket Girişi (Tag Input)"
                )
            }
        }

        // Confirmation & Pickers
        ArcCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Onay ve Seçiciler (Confirmation & Pickers)", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.foreground)
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    ArcHoldToConfirm(
                        label = "Basılı Tutarak Sil",
                        onConfirm = {},
                        variant = ArcButtonVariant.Danger
                    )
                    ArcConfirmMorph(
                        prompt = "Öğrenci silinsin mi?",
                        onConfirm = {},
                        initialLabel = "Silme Talebi"
                    )
                    ArcColorPicker(
                        selectedColor = selectedColor,
                        onColorSelected = { selectedColor = it }
                    )
                }
            }
        }
    }
}

@Composable
private fun NavigationSection() {
    val colors = ArcTheme.colors
    var activeStep by remember { mutableIntStateOf(1) }
    var activePage by remember { mutableIntStateOf(1) }

    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        ArcCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Gezinme Yolları (Breadcrumbs, Steppers & Pagination)", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.foreground)
                ArcBreadcrumb(
                    items = listOf("Puanla", "11. Sınıf", "11-A", "Sözlü Sunum", "Ahmet Yılmaz")
                )
                Spacer(Modifier.height(4.dp))
                ArcStepper(
                    steps = listOf("Fotoğraf", "Algılama", "Doğrulama", "Tamamlandı"),
                    currentStepIndex = activeStep,
                    onStepClick = { activeStep = it }
                )
                Spacer(Modifier.height(4.dp))
                ArcPagination(
                    currentPage = activePage,
                    totalPages = 5,
                    onPageChange = { activePage = it }
                )
            }
        }

        ArcCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Kart Yığını & Karusel (Card Stack & Carousel)", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.foreground)
                ArcCarousel(
                    items = listOf("11-A Şubesi", "11-B Şubesi", "11-C Şubesi", "12-A Şubesi"),
                    modifier = Modifier.fillMaxWidth().height(120.dp)
                ) { item ->
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        shape = ArcTheme.shapes.panel,
                        color = colors.surfaceRaised,
                        border = BorderStroke(1.dp, colors.borderSubtle)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(item, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colors.foreground)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DisplaySection() {
    val colors = ArcTheme.colors
    val treeData = listOf(
        ArcTreeNode("11. Sınıf Edebiyat", listOf(
            ArcTreeNode("Sözlü İletişim", listOf(ArcTreeNode("Sunum"), ArcTreeNode("Münazara"))),
            ArcTreeNode("Yazma Becerisi", listOf(ArcTreeNode("Deneme"), ArcTreeNode("Eleştiri")))
        ))
    )

    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        ArcAnnouncementBar(
            message = "Yeni MEB 11. Sınıf Türk Dili ve Edebiyatı rubrik güncellemesi hazır.",
            actionLabel = "İncele"
        )

        ArcCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Rozetler ve Avatarlar (Badges & Avatars)", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.foreground)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    ArcBadge(text = "Varsayılan", variant = ArcBadgeVariant.Default)
                    ArcBadge(text = "Tamamlandı", variant = ArcBadgeVariant.Success)
                    ArcBadge(text = "Eksik", variant = ArcBadgeVariant.Warning)
                    ArcBadge(text = "Kritik", variant = ArcBadgeVariant.Danger)
                    ArcBadge(text = "Vurgulu", variant = ArcBadgeVariant.Accent)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    ArcAvatar(initials = "AY")
                    ArcAvatar(initials = "MD")
                    ArcAvatarGroup(membersInitials = listOf("AY", "MD", "BK", "ZÇ", "EÖ", "TK"))
                }
            }
        }

        ArcAlert(
            title = "Sistem Doğrulama Bildirimi",
            message = "Tüm Arc Library bileşenleri yerel Compose motoru üzerinde çalışır. Hiçbir webview veya çapraz platform katmanı kullanılmaz.",
            type = ArcAlertType.Success
        )

        ArcCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Hiyerarşik Ağaç Görünümü (Tree View)", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.foreground)
                ArcTreeView(nodes = treeData)
            }
        }
    }
}

@Composable
private fun DataVisSection() {
    val colors = ArcTheme.colors
    val barSample = listOf(
        ArcBarData("0-49", 2f, colors.danger),
        ArcBarData("50-69", 6f, colors.warning),
        ArcBarData("70-84", 14f, colors.accent),
        ArcBarData("85-100", 8f, colors.success)
    )
    val donutSample = listOf(
        ArcDonutSegment("Tamamlandı", 22f, colors.success),
        ArcDonutSegment("Kısmi", 6f, colors.warning),
        ArcDonutSegment("Başlanmadı", 2f, colors.surfaceMuted)
    )

    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        ArcStatsBand(
            stats = listOf(
                com.knigdelioglu.arc.compose.datavis.ArcStatItem("Toplam Öğrenci", "30", "11-A Şubesi"),
                com.knigdelioglu.arc.compose.datavis.ArcStatItem("Tamamlanan", "22", "%73 Oran"),
                com.knigdelioglu.arc.compose.datavis.ArcStatItem("Sınıf Ortalaması", "78", "100 Üzerinden")
            )
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ArcBarChart(data = barSample, modifier = Modifier.weight(1.3f))
            ArcDonutChart(segments = donutSample, modifier = Modifier.weight(1.2f))
            ArcGauge(percentage = 0.73f, modifier = Modifier.weight(1f))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ArcCard(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Canlı Sayaç ve Metin Animasyonu", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = colors.foreground)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Not: ", fontSize = 16.sp, color = colors.textSecondary)
                        ArcAnimatedCounter(count = 85, fontSize = 26.sp, color = colors.success)
                    }
                    ArcTextShimmer(text = "Öğrenci puanı başarıyla senkronize edildi...")
                }
            }

            ArcCard(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Hafif Puan Eğrisi (Sparkline)", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = colors.foreground)
                    ArcSparkline(
                        points = listOf(45f, 55f, 70f, 65f, 85f, 90f, 82f, 95f),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    )
                }
            }
        }
    }
}
