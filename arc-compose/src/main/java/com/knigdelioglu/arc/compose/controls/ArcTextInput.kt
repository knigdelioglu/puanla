package com.knigdelioglu.arc.compose.controls

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.knigdelioglu.arc.compose.foundation.ArcTheme

/**
 * Arc Input: single-line text input with clean borders and active accent state.
 */
@Composable
fun ArcInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    enabled: Boolean = true,
    isError: Boolean = false,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = true,
    maxLines: Int = 1,
    label: String? = null
) {
    var isFocused by remember { mutableStateOf(false) }
    val colors = ArcTheme.colors
    val borderColor = when {
        isError -> colors.danger
        isFocused -> colors.accent
        else -> colors.border
    }

    Column(modifier = modifier) {
        if (label != null) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = colors.textSecondary,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }
        Surface(
            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 44.dp),
            shape = ArcTheme.shapes.control,
            color = colors.surface,
            border = BorderStroke(if (isFocused || isError) 1.5.dp else 1.dp, borderColor)
        ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingIcon != null) {
                leadingIcon()
                Spacer(Modifier.width(8.dp))
            }
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty() && placeholder.isNotEmpty()) {
                    Text(
                        text = placeholder,
                        color = colors.textMuted,
                        fontSize = 15.sp
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { isFocused = it.isFocused },
                    enabled = enabled,
                    singleLine = singleLine,
                    maxLines = maxLines,
                    textStyle = TextStyle(
                        color = if (enabled) colors.foreground else colors.textMuted,
                        fontSize = 15.sp
                    ),
                    cursorBrush = SolidColor(colors.accent),
                    keyboardOptions = keyboardOptions,
                    keyboardActions = keyboardActions
                )
            }
            if (trailingIcon != null) {
                Spacer(Modifier.width(8.dp))
                trailingIcon()
            }
        }
    }
}
}


/**
 * Arc Textarea: multi-line input for teacher observation and evidence notes.
 */
@Composable
fun ArcTextarea(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    label: String? = null,
    minLines: Int = 3,
    maxLines: Int = 8,
    enabled: Boolean = true
) {
    var isFocused by remember { mutableStateOf(false) }
    val colors = ArcTheme.colors
    val borderColor = if (isFocused) colors.accent else colors.border

    Column(modifier = modifier) {
        if (label != null) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = colors.textSecondary,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }
        Surface(
            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 88.dp),
            shape = ArcTheme.shapes.control,
            color = colors.surface,
            border = BorderStroke(if (isFocused) 1.5.dp else 1.dp, borderColor)
        ) {
            Box(
                modifier = Modifier
                    .padding(14.dp)
                    .fillMaxWidth(),

            contentAlignment = Alignment.TopStart
        ) {
            if (value.isEmpty() && placeholder.isNotEmpty()) {
                Text(
                    text = placeholder,
                    color = colors.textMuted,
                    fontSize = 15.sp
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { isFocused = it.isFocused },
                enabled = enabled,
                minLines = minLines,
                maxLines = maxLines,
                textStyle = TextStyle(
                    color = if (enabled) colors.foreground else colors.textMuted,
                    fontSize = 15.sp,
                    lineHeight = 22.sp
                ),
                cursorBrush = SolidColor(colors.accent)
            )
        }
    }
}
}



/**
 * Arc Search Field: clean search box with clear button.
 */
@Composable
fun ArcSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Öğrenci veya numara ara..."
) {
    ArcInput(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        placeholder = placeholder,
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Ara",
                tint = ArcTheme.colors.textMuted,
                modifier = Modifier.size(20.dp)
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Temizle",
                    tint = ArcTheme.colors.textMuted,
                    modifier = Modifier
                        .size(18.dp)
                        .clickable { onQueryChange("") }
                )
            }
        }
    )
}

/**
 * Arc Expanding Search: collapses into an icon button and expands when activated.
 */
@Composable
fun ArcExpandingSearch(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Ara..."
) {
    var expanded by remember { mutableStateOf(query.isNotEmpty()) }

    Row(modifier = modifier.animateContentSize(), verticalAlignment = Alignment.CenterVertically) {
        if (!expanded && query.isEmpty()) {
            ArcActionButton(
                onClick = { expanded = true },
                size = ArcButtonSize.Sm
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Aramayı Aç",
                    modifier = Modifier.size(18.dp)
                )
            }
        } else {
            ArcSearchField(
                query = query,
                onQueryChange = onQueryChange,
                modifier = Modifier.width(260.dp),
                placeholder = placeholder
            )
            Spacer(Modifier.width(4.dp))
            IconButton(onClick = {
                expanded = false
                onQueryChange("")
            }) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Kapat",
                    tint = ArcTheme.colors.textMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Arc Number Field: accessible integer scoring stepper with buttons and direct keyboard input.
 */
@Composable
fun ArcNumberField(
    value: Int?,
    onValueChange: (Int) -> Unit,
    min: Int = 0,
    max: Int = 100,
    modifier: Modifier = Modifier,
    step: Int = 1,
    label: String? = null
) {
    val currentValue = value ?: min

    Surface(
        modifier = modifier,
        shape = ArcTheme.shapes.control,
        color = ArcTheme.colors.surfaceRaised,
        border = BorderStroke(1.dp, ArcTheme.colors.border)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ArcActionButton(
                onClick = {
                    val next = (currentValue - step).coerceAtLeast(min)
                    onValueChange(next)
                },
                size = ArcButtonSize.Sm,
                enabled = currentValue > min,
                variant = ArcButtonVariant.Secondary
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Puan Azalt", modifier = Modifier.size(16.dp))
            }

            Box(
                modifier = Modifier
                    .defaultMinSize(minWidth = 56.dp)
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = value?.toString() ?: "—",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (value != null) ArcTheme.colors.foreground else ArcTheme.colors.textMuted,
                    textAlign = TextAlign.Center
                )
            }

            ArcActionButton(
                onClick = {
                    val next = (currentValue + step).coerceAtMost(max)
                    onValueChange(next)
                },
                size = ArcButtonSize.Sm,
                enabled = currentValue < max,
                variant = ArcButtonVariant.Secondary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Puan Artır", modifier = Modifier.size(16.dp))
            }

            if (label != null) {
                Text(
                    text = label,
                    fontSize = 13.sp,
                    color = ArcTheme.colors.textSecondary,
                    modifier = Modifier.padding(end = 6.dp)
                )
            }
        }
    }
}

/**
 * Arc Tag Input: for tags and grouping students/rubrics.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ArcTagInput(
    tags: List<String>,
    onAddTag: (String) -> Unit,
    onRemoveTag: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Etiket ekle...",
    label: String? = null
) {
    var input by remember { mutableStateOf("") }

    Column(modifier = modifier) {
        if (label != null) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = ArcTheme.colors.textSecondary,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
        tags.forEach { tag ->
            Surface(
                shape = ArcTheme.shapes.pill,
                color = ArcTheme.colors.surfaceMuted,
                border = BorderStroke(1.dp, ArcTheme.colors.borderSubtle)
            ) {
                Row(
                    modifier = Modifier.padding(start = 10.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(tag, fontSize = 13.sp, color = ArcTheme.colors.foreground)
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Sil",
                        modifier = Modifier
                            .size(14.dp)
                            .clickable { onRemoveTag(tag) },
                        tint = ArcTheme.colors.textMuted
                    )
                }
            }
        }

        Row(
            modifier = Modifier.width(160.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = input,
                onValueChange = { input = it },
                singleLine = true,
                textStyle = TextStyle(fontSize = 14.sp, color = ArcTheme.colors.foreground),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (input.isNotBlank()) {
                        onAddTag(input.trim())
                        input = ""
                    }
                }),
                cursorBrush = SolidColor(ArcTheme.colors.accent),
                decorationBox = { inner ->
                    Box(modifier = Modifier.padding(4.dp)) {
                        if (input.isEmpty()) Text(placeholder, fontSize = 13.sp, color = ArcTheme.colors.textMuted)
                        inner()
                    }
                }
            )
        }
    }
}
}


/**
 * Arc Inline Edit: tap to edit student name, number, or classroom label directly.
 */
@Composable
fun ArcInlineEdit(
    value: String,
    onCommit: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Düzenle...",
    textStyle: TextStyle = LocalTextStyle.current
) {
    var isEditing by remember { mutableStateOf(false) }
    var tempValue by remember { mutableStateOf(value) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(value) {
        tempValue = value
    }

    if (isEditing) {
        Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
            BasicTextField(
                value = tempValue,
                onValueChange = { tempValue = it },
                modifier = Modifier
                    .weight(1f, fill = false)
                    .focusRequester(focusRequester),
                singleLine = true,
                textStyle = textStyle.copy(color = ArcTheme.colors.foreground),
                cursorBrush = SolidColor(ArcTheme.colors.accent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    isEditing = false
                    onCommit(tempValue)
                })
            )
            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }
            Spacer(Modifier.width(6.dp))
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Kaydet",
                tint = ArcTheme.colors.success,
                modifier = Modifier
                    .size(18.dp)
                    .clickable {
                        isEditing = false
                        onCommit(tempValue)
                    }
            )
            Spacer(Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "İptal",
                tint = ArcTheme.colors.danger,
                modifier = Modifier
                    .size(18.dp)
                    .clickable {
                        isEditing = false
                        tempValue = value
                    }
            )
        }
    } else {
        Row(
            modifier = modifier
                .clip(ArcTheme.shapes.small)
                .clickable { isEditing = true }
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = value.ifEmpty { placeholder },
                style = textStyle,
                color = if (value.isEmpty()) ArcTheme.colors.textMuted else ArcTheme.colors.foreground
            )
            Spacer(Modifier.width(6.dp))
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Düzenle",
                tint = ArcTheme.colors.textMuted,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
