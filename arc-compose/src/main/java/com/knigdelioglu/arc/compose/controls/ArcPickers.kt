package com.knigdelioglu.arc.compose.controls

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.knigdelioglu.arc.compose.foundation.ArcTheme

/**
 * Arc Calendar: clean monthly grid calendar for selecting assessment session dates.
 */
@Composable
fun ArcCalendar(
    selectedDay: Int?,
    onSelectDay: (Int) -> Unit,
    modifier: Modifier = Modifier,
    monthName: String = "",
    daysInMonth: Int = 31,
    startDayOfWeekOffset: Int = 3, // legacy preview argument; actual calendar follows month
    onSelectDate: (String) -> Unit = {}
) {
    val colors = ArcTheme.colors
    val monthFormat = remember { DateTimeFormatter.ofPattern("LLLL yyyy", Locale("tr", "TR")) }
    var visibleMonth by remember(monthName) {
        mutableStateOf(runCatching { YearMonth.parse(monthName, monthFormat) }.getOrElse { YearMonth.now() })
    }
    var selectedMonth by remember(monthName) { mutableStateOf(visibleMonth) }
    val calendarDays = visibleMonth.lengthOfMonth()
    val calendarOffset = visibleMonth.atDay(1).dayOfWeek.value - 1 // Monday = 0
    val daysOfWeek = listOf("Pt", "Sa", "Ça", "Pe", "Cu", "Ct", "Pz")

    Surface(
        modifier = modifier.width(320.dp),
        shape = ArcTheme.shapes.panel,
        color = colors.surfaceRaised,
        border = BorderStroke(1.dp, colors.borderSubtle)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(visibleMonth.format(monthFormat), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.foreground)
                Row {
                    IconButton(onClick = { visibleMonth = visibleMonth.minusMonths(1) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Önceki Ay", tint = colors.textSecondary)
                    }
                    IconButton(onClick = { visibleMonth = visibleMonth.plusMonths(1) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Sonraki Ay", tint = colors.textSecondary)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                daysOfWeek.forEach { day ->
                    Text(
                        text = day,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(36.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            val totalSlots = calendarOffset + calendarDays
            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                modifier = Modifier.height(200.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(totalSlots) { index ->
                    if (index < calendarOffset) {
                        Box(modifier = Modifier.size(36.dp))
                    } else {
                        val dayNumber = index - calendarOffset + 1
                        val isSelected = selectedDay == dayNumber && selectedMonth == visibleMonth
                        Surface(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .clickable {
                                    selectedMonth = visibleMonth
                                    onSelectDay(dayNumber)
                                    onSelectDate(visibleMonth.atDay(dayNumber).toString())
                                },
                            shape = CircleShape,
                            color = if (isSelected) colors.accent else Color.Transparent
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = dayNumber.toString(),
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) colors.accentForeground else colors.foreground
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Arc Date Picker: dropdown-triggered or embedded date selector.
 */
/**
 * Real Material3 date selector. Selected dates are ISO-8601 (yyyy-MM-dd).
 * UTC conversion is required by Material3's DatePickerState contract.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArcDatePicker(
    selectedDate: String,
    onDateSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Değerlendirme Tarihi"
) {
    var isOpen by remember { mutableStateOf(false) }
    val dateText = runCatching { LocalDate.parse(selectedDate) }.getOrNull()
    ArcButton(
        onClick = { isOpen = true },
        modifier = modifier,
        variant = ArcButtonVariant.Outline
    ) {
        Text("$label: ${dateText?.toString() ?: "Tarih Seç"}")
    }
    if (isOpen) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = dateText?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { isOpen = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let {
                        onDateSelected(LocalDate.ofEpochDay(Math.floorDiv(it, 86_400_000L)).toString())
                    }
                    isOpen = false
                }) { Text("Seç") }
            },
            dismissButton = {
                TextButton(onClick = { isOpen = false }) { Text("Vazgeç") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

/** Start/end ISO dates. Each button opens the real native calendar picker. */
@Composable
fun ArcDateRangePicker(
    startDate: String,
    endDate: String,
    onRangeSelected: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Son güncelleme tarih aralığı", color = ArcTheme.colors.foreground, fontWeight = FontWeight.SemiBold)
        ArcDatePicker(
            selectedDate = startDate,
            onDateSelected = { onRangeSelected(it, endDate) },
            label = "Başlangıç",
            modifier = Modifier.fillMaxWidth()
        )
        ArcDatePicker(
            selectedDate = endDate,
            onDateSelected = { onRangeSelected(startDate, it) },
            label = "Bitiş",
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Arc Time Picker: pick session hour and minute.
 */
@Composable
fun ArcTimePicker(
    hour: Int,
    minute: Int,
    onTimeChange: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = ArcTheme.shapes.control,
        color = ArcTheme.colors.surfaceRaised,
        border = BorderStroke(1.dp, ArcTheme.colors.border)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = String.format("%02d:%02d", hour, minute),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = ArcTheme.colors.foreground
            )
        }
    }
}

/**
 * Arc Color Picker: select rubric criterion color tags.
 */
@Composable
fun ArcColorPicker(
    selectedColor: Color,
    onColorSelected: (Color) -> Unit,
    modifier: Modifier = Modifier,
    colors: List<Color> = listOf(
        Color(0xFF3B82F6), Color(0xFF10B981), Color(0xFFF59E0B),
        Color(0xFFEF4444), Color(0xFF8B5CF6), Color(0xFFEC4899),
        Color(0xFF06B6D4), Color(0xFF6B7280)
    )
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        colors.forEach { color ->
            val isSelected = color == selectedColor
            Surface(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .clickable { onColorSelected(color) },
                shape = CircleShape,
                color = color,
                border = if (isSelected) BorderStroke(3.dp, ArcTheme.colors.foreground) else null
            ) {}
        }
    }
}
