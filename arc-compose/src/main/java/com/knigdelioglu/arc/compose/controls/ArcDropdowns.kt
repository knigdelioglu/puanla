package com.knigdelioglu.arc.compose.controls

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.knigdelioglu.arc.compose.foundation.ArcTheme

/**
 * Arc Select: standard selection trigger with dropdown.
 */
@Composable
fun <T> ArcSelect(
    selectedItem: T?,
    items: List<T>,
    onItemSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Seçiniz...",
    labelProvider: (T) -> String = { it?.toString() ?: "" }
) {
    var expanded by remember { mutableStateOf(false) }
    val colors = ArcTheme.colors

    Box(modifier = modifier) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true },
            shape = ArcTheme.shapes.control,
            color = colors.surface,
            border = BorderStroke(1.dp, if (expanded) colors.accent else colors.border)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (selectedItem != null) labelProvider(selectedItem) else placeholder,
                    fontSize = 15.sp,
                    color = if (selectedItem != null) colors.foreground else colors.textMuted
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = colors.textMuted
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(colors.surface)
        ) {
            items.forEach { item ->
                val isSelected = item == selectedItem
                DropdownMenuItem(
                    text = {
                        Text(
                            text = labelProvider(item),
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = colors.foreground
                        )
                    },
                    onClick = {
                        onItemSelected(item)
                        expanded = false
                    },
                    trailingIcon = if (isSelected) {
                        { Icon(Icons.Default.Check, contentDescription = null, tint = colors.accent) }
                    } else null
                )
            }
        }
    }
}

/**
 * Arc Combobox: searchable selection list for large student or classroom pools.
 */
@Composable
fun <T> ArcCombobox(
    selectedItem: T?,
    items: List<T>,
    onItemSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Arayarak seçiniz...",
    searchQueryFilter: (T, String) -> Boolean = { item, query -> item.toString().contains(query, ignoreCase = true) },
    labelProvider: (T) -> String = { it?.toString() ?: "" }
) {
    var isOpen by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val colors = ArcTheme.colors

    Box(modifier = modifier) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isOpen = true },
            shape = ArcTheme.shapes.control,
            color = colors.surface,
            border = BorderStroke(1.dp, colors.border)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (selectedItem != null) labelProvider(selectedItem) else placeholder,
                    fontSize = 15.sp,
                    color = if (selectedItem != null) colors.foreground else colors.textMuted
                )
                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = colors.textMuted)
            }
        }

        if (isOpen) {
            Dialog(onDismissRequest = { isOpen = false }) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .heightIn(max = 480.dp),
                    shape = ArcTheme.shapes.panel,
                    color = colors.surfaceRaised,
                    border = BorderStroke(1.dp, colors.border)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        ArcSearchField(
                            query = query,
                            onQueryChange = { query = it },
                            placeholder = "Ara..."
                        )
                        Spacer(Modifier.height(12.dp))
                        val filtered = remember(query, items) {
                            if (query.isBlank()) items else items.filter { searchQueryFilter(it, query) }
                        }
                        LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                            items(filtered) { item ->
                                val isSelected = item == selectedItem
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(ArcTheme.shapes.control)
                                        .clickable {
                                            onItemSelected(item)
                                            isOpen = false
                                        }
                                        .padding(horizontal = 12.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = labelProvider(item),
                                        fontSize = 15.sp,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                        color = colors.foreground
                                    )
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = colors.accent)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Arc Multi-Select: selection of multiple students, filters, or groups.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> ArcMultiSelect(
    selectedItems: Set<T>,
    items: List<T>,
    onSelectionChanged: (Set<T>) -> Unit,
    modifier: Modifier = Modifier,
    labelProvider: (T) -> String = { it.toString() }
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEach { item ->
            val isSelected = selectedItems.contains(item)
            val colors = ArcTheme.colors
            Surface(
                modifier = Modifier
                    .clip(ArcTheme.shapes.pill)
                    .clickable {
                        val next = if (isSelected) selectedItems - item else selectedItems + item
                        onSelectionChanged(next)
                    },
                shape = ArcTheme.shapes.pill,
                color = if (isSelected) colors.accent else colors.surfaceRaised,
                border = BorderStroke(1.dp, if (isSelected) colors.accent else colors.borderSubtle)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isSelected) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = colors.accentForeground,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        text = labelProvider(item),
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) colors.accentForeground else colors.foreground
                    )
                }
            }
        }
    }
}

/**
 * Arc Morph Select: interactive dropdown variant with fluid expanded card container.
 */
@Composable
fun <T> ArcMorphSelect(
    selectedItem: T?,
    items: List<T>,
    onItemSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    labelProvider: (T) -> String = { it?.toString() ?: "" }
) {
    ArcCombobox(
        selectedItem = selectedItem,
        items = items,
        onItemSelected = onItemSelected,
        modifier = modifier,
        labelProvider = labelProvider
    )
}

/**
 * Arc Dropdown Menu wrapper: custom styled contextual popover menu.
 */
@Composable
fun ArcDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier.background(ArcTheme.colors.surfaceRaised)
    ) {
        content()
    }
}

/** Context actions are real callbacks; clicking one closes the menu before it runs. */
data class ArcContextAction(val label: String, val onSelect: () -> Unit)

/**
 * Arc Context Menu: long press an item on tablets to access its real actions.
 * The original composable content API remains available for the gallery.
 */
@Composable
fun ArcContextMenu(
    menuContent: @Composable () -> Unit = {},
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
    actions: List<ArcContextAction> = emptyList()
) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = modifier.pointerInput(Unit) {
            detectTapGestures(onLongPress = { expanded = true })
        }
    ) {
        content()
        ArcDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            actions.forEach { action ->
                DropdownMenuItem(
                    text = { Text(action.label) },
                    onClick = {
                        expanded = false
                        action.onSelect()
                    }
                )
            }
            menuContent()
        }
    }
}
