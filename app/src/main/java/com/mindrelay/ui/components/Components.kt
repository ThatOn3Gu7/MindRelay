package com.mindrelay.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mindrelay.data.model.MemoryType

// ---------------------------------------------------------------------------
// Section label — bold, sized, onSurface.
// ---------------------------------------------------------------------------

@Composable
fun SectionLabel(text: String, size: Int = 18, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge.copy(
            fontSize = size.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = (size * 1.3).sp,
        ),
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier,
    )
}

// ---------------------------------------------------------------------------
// Expressive card — 20dp corners on a role-correct container.
// ---------------------------------------------------------------------------

@Composable
fun ExpressiveCard(
    headline: String?,
    body: String,
    modifier: Modifier = Modifier,
    container: Color = MaterialTheme.colorScheme.primaryContainer,
    content: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    onClick: (() -> Unit)? = null,
    minHeight: Int? = null,
) {
    val shape = RoundedCornerShape(20.dp)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && onClick != null) 0.98f else 1f,
        animationSpec = spring(dampingRatio = 0.9f),
        label = "card-scale",
    )
    Card(
        onClick = onClick ?: {},
        enabled = onClick != null,
        modifier = modifier
            .scale(scale)
            .then(if (minHeight != null) Modifier.heightIn(min = minHeight.dp) else Modifier),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = container, contentColor = content),
        interactionSource = interaction,
    ) {
        Column(Modifier.padding(20.dp)) {
            if (!headline.isNullOrBlank()) {
                Text(headline, style = MaterialTheme.typography.titleMedium, color = content)
                Spacer(Modifier.height(4.dp))
            }
            Text(body, style = MaterialTheme.typography.bodyMedium, color = content)
        }
    }
}

// ---------------------------------------------------------------------------
// Body list item — 40dp icon circle + headline + supporting text + trailing.
// Ripple + slight press-scale on tap (per spec).
// ---------------------------------------------------------------------------

@Composable
fun BodyListItem(
    headline: String,
    supporting: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    iconContainer: Color = MaterialTheme.colorScheme.primaryContainer,
    iconTint: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    color: Color = MaterialTheme.colorScheme.onSurface,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && onClick != null) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.9f),
        label = "item-scale",
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interaction,
                        indication = LocalIndication.current,
                        onClick = onClick,
                    )
                } else Modifier
            )
            .padding(horizontal = 4.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(iconContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                headline,
                style = MaterialTheme.typography.bodyLarge,
                color = color,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (supporting.isNotBlank()) {
                Text(
                    supporting,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (trailing != null) trailing()
    }
}

// ---------------------------------------------------------------------------
// Connected chip group — 3dp gaps, inner corners shrink to 4dp, outer round.
// ---------------------------------------------------------------------------

@Composable
fun ConnectedChipGroup(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        options.forEachIndexed { index, option ->
            val start = index == 0
            val end = index == options.lastIndex
            val shape = when {
                options.size == 1 -> RoundedCornerShape(8.dp)
                start -> RoundedCornerShape(8.dp, 4.dp, 4.dp, 8.dp)
                end -> RoundedCornerShape(4.dp, 8.dp, 8.dp, 4.dp)
                else -> RoundedCornerShape(4.dp)
            }
            val isSelected = selected == option
            Surface(
                shape = shape,
                color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer
                else MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant,
            ) {
                Row(
                    modifier = Modifier
                        .clip(shape)
                        .clickable { onSelect(option) }
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (isSelected) {
                        Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(option, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Text fields (outlined or filled-style) with leading icon + supporting text.
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MindTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    supportingText: String? = null,
    filled: Boolean = true,
    singleLine: Boolean = true,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.Sentences,
    keyboardType: KeyboardType = KeyboardType.Text,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else 6,
) {
    val shape = RoundedCornerShape(16.dp)
    val colors = if (filled) {
        OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = Color.Transparent,
            disabledBorderColor = Color.Transparent,
        )
    } else {
        OutlinedTextFieldDefaults.colors()
    }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = leadingIcon?.let { { Icon(it, contentDescription = null) } },
        supportingText = supportingText?.let { { Text(it) } },
        singleLine = singleLine,
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        colors = colors,
        keyboardOptions = KeyboardOptions(
            capitalization = capitalization,
            keyboardType = keyboardType,
        ),
        minLines = minLines,
        maxLines = maxLines,
    )
}

// ---------------------------------------------------------------------------
// Dropdown — anchored exposed menu (canonical M3 pattern). The whole field is
// tappable, and the menu is anchored to the field via menuAnchor, so opening it
// does not rely on a stacked text-field click listener.
// ---------------------------------------------------------------------------

/** Two-way label/type mapping, shared by memory fields so a label always maps
 *  back to exactly the same [MemoryType]. */
@Immutable
private data class MemoryTypeOption(val label: String, val type: MemoryType)

private val MEMORY_TYPE_OPTIONS: List<MemoryTypeOption> = listOf(
    MemoryTypeOption("Fix", MemoryType.FIX),
    MemoryTypeOption("Person", MemoryType.PERSON),
    MemoryTypeOption("Idea", MemoryType.IDEA),
    MemoryTypeOption("Place", MemoryType.PLACE),
    MemoryTypeOption("Recipe", MemoryType.RECIPE),
    MemoryTypeOption("Note", MemoryType.NOTE),
    MemoryTypeOption("Other", MemoryType.OTHER),
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MindDropdown(
    label: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(16.dp)
    val colors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = Color.Transparent,
        disabledBorderColor = Color.Transparent,
    )

    // The anchored dropdown box makes the entire field tappable and positions
    // the menu against the field; the inner text field must NOT be focusable so
    // taps toggle the menu instead of being captured by the field itself.
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(label) },
            leadingIcon = leadingIcon?.let { { Icon(it, contentDescription = null) } },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            shape = shape,
            colors = colors,
            modifier = Modifier
                .menuAnchor(
                    ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                    enabled = true,
                )
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            options.forEach { option ->
                val isSelected = option == selected
                DropdownMenuItem(
                    text = {
                        Text(
                            option,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface,
                        )
                    },
                    // Selected state is shown via a leading check; the transparent
                    // counterpart keeps every option's text aligned on one line.
                    leadingIcon = {
                        Icon(
                            Icons.Rounded.Check,
                            contentDescription = null,
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        )
                    },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

/** The fixed set of memory type options shown by the Memory Type selector. */
val MEMORY_TYPE_LABELS: List<String> = MEMORY_TYPE_OPTIONS.map { it.label }

fun memoryTypeToLabel(type: MemoryType): String =
    MEMORY_TYPE_OPTIONS.firstOrNull { it.type == type }?.label ?: MEMORY_TYPE_OPTIONS.last().label

fun labelToMemoryType(label: String): MemoryType =
    MEMORY_TYPE_OPTIONS.firstOrNull { it.label == label }?.type ?: MemoryType.OTHER

// ---------------------------------------------------------------------------
// Buttons — pill (fully rounded), filled / tonal / outlined.
// ---------------------------------------------------------------------------

@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    filled: Boolean = true,
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(28.dp)
    if (filled) {
        Button(
            onClick = onClick,
            modifier = modifier.defaultMinSize(minHeight = 56.dp),
            shape = shape,
            enabled = enabled,
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.defaultMinSize(minHeight = 56.dp),
            shape = shape,
            enabled = enabled,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** A small selectable pill chip used in compact pickers (e.g. project links). */
@Composable
fun PillChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .clickable(onClick = onClick)
            .background(
                if (selected) MaterialTheme.colorScheme.secondaryContainer
                else MaterialTheme.colorScheme.surfaceContainerHigh
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (selected) {
                Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                Spacer(Modifier.width(6.dp))
            }
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

@Composable
fun TonalPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    TextButton(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = 56.dp),
        shape = RoundedCornerShape(28.dp),
        enabled = enabled,
        colors = ButtonDefaults.textButtonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}
