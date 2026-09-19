package com.mindrelay.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.mindrelay.data.model.CaptureKind
import com.mindrelay.data.settings.toCaptureKindOrNull
import com.mindrelay.nav.MindNavController
import com.mindrelay.nav.MindScreen
import com.mindrelay.nav.MindTransition
import com.mindrelay.ui.AppViewModel
import com.mindrelay.ui.components.SectionLabel

private fun captureKindLabel(kind: CaptureKind): String = when (kind) {
    CaptureKind.IDEA -> "Idea"
    CaptureKind.TODO -> "To-do"
    CaptureKind.QUESTION -> "Question"
    CaptureKind.NOTE -> "Note"
    CaptureKind.VOICE -> "Voice"
}

@Composable
private fun ExpressiveSettingsTile(
    headline: String,
    supporting: String,
    icon: ImageVector,
    onClick: (() -> Unit)? = null,
    iconContainerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    iconContentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
    trailing: @Composable (() -> Unit)? = {
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        )
    }
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val targetScale = if (onClick != null && isPressed) 0.96f else 1f
    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 600f),
        label = "settings_tile_scale"
    )

    Surface(
        onClick = onClick ?: {},
        enabled = onClick != null,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        interactionSource = interactionSource,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = iconContainerColor,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconContentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = headline,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (supporting.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = supporting,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            trailing?.invoke()
        }
    }
}

@Composable
private fun ExpressiveSwitchTile(
    title: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit,
    icon: ImageVector,
    supporting: String? = null,
    iconContainerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    iconContentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = iconContainerColor,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconContentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                supporting?.let {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Switch(
                checked = checked,
                onCheckedChange = onChecked,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    uncheckedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                ),
            )
        }
    }
}

@Composable
private fun DataBackupHeroCard(
    headline: String,
    supporting: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 600f),
        label = "backup_card_scale"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        interactionSource = interactionSource,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f),
                modifier = Modifier.size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = Icons.Rounded.Storage,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = headline,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = supporting,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                    lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.15f
                )
            }

            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsScreen(vm: AppViewModel, nav: MindNavController) {
    // -------------------------------------------------------------------------
    // DATA AND STATE FLOW (UNTOUCHED)
    // -------------------------------------------------------------------------
    val settings by vm.settings.collectAsStateWithLifecycle()
    var themeMenu by remember { mutableStateOf(false) }
    var captureKindMenu by remember { mutableStateOf(false) }

    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }
    // -------------------------------------------------------------------------

    Column(Modifier.fillMaxSize()) {
        MindTopBar(
            title = "Settings",
            onBack = { nav.popToRoot() },
        )

        // Dropdown menus preserved exactly as originally implemented
        DropdownMenu(expanded = themeMenu, onDismissRequest = { themeMenu = false }) {
            listOf("System", "Light", "Dark").forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt) },
                    onClick = {
                        themeMenu = false
                        vm.launch { vm.settingsStore.setTheme(opt) }
                    },
                )
            }
        }

        DropdownMenu(expanded = captureKindMenu, onDismissRequest = { captureKindMenu = false }) {
            // VOICE is intentionally excluded: capture is text-only today, so a
            // voice "default" would leave Quick Capture with no selected chip.
            CaptureKind.entries.filter { it != CaptureKind.VOICE }.forEach { kind ->
                DropdownMenuItem(
                    text = { Text(captureKindLabel(kind)) },
                    onClick = {
                        captureKindMenu = false
                        vm.launch { vm.settingsStore.setDefaultCaptureKind(kind.name) }
                    },
                )
            }
        }

        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(tween(300)) + slideInVertically(
                initialOffsetY = { 60 },
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f)
            ),
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // APPEARANCE SECTION
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionLabel("Appearance", size = 15)

                    ExpressiveSettingsTile(
                        headline = "Theme",
                        supporting = settings.theme,
                        icon = Icons.Rounded.Palette,
                        onClick = { themeMenu = true },
                        iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        iconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    )

                    ExpressiveSettingsTile(
                        headline = "Expressive style",
                        supporting = "Rounded · Roboto Flex",
                        icon = Icons.Rounded.Brush,
                        onClick = null,
                        iconContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        iconContentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                }

                // CAPTURE SECTION
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionLabel("Capture", size = 15)

                    ExpressiveSettingsTile(
                        headline = "Default save behavior",
                        supporting = settings.defaultSave,
                        icon = Icons.Rounded.FolderOpen,
                        onClick = {
                            vm.launch {
                                vm.settingsStore.setDefaultSave(
                                    if (settings.defaultSave == "Inbox") "Current project" else "Inbox"
                                )
                            }
                        },
                    )

                    ExpressiveSettingsTile(
                        headline = "Default capture kind",
                        supporting = settings.defaultCaptureKind.toCaptureKindOrNull()?.let {
                            captureKindLabel(it)
                        } ?: settings.defaultCaptureKind,
                        icon = Icons.Rounded.Save,
                        onClick = { captureKindMenu = true },
                    )

                    ExpressiveSwitchTile(
                        title = "Voice capture available",
                        checked = settings.voiceCapture,
                        onChecked = { vm.launch { vm.settingsStore.setVoiceCapture(it) } },
                        icon = Icons.Rounded.Mic,
                        supporting = "Text-only captures are always available",
                    )

                    ExpressiveSwitchTile(
                        title = "Automatic project linking",
                        checked = settings.autoLink,
                        onChecked = { vm.launch { vm.settingsStore.setAutoLink(it) } },
                        icon = Icons.Rounded.Link,
                    )
                }

                // REMINDERS SECTION
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionLabel("Reminders", size = 15)

                    ExpressiveSwitchTile(
                        title = "Revisit reminders",
                        checked = settings.revisitReminders,
                        onChecked = { vm.launch { vm.settingsStore.setRevisitReminders(it) } },
                        icon = Icons.Rounded.Notifications,
                        iconContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        iconContentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                }

                // DATA SECTION
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionLabel("Data", size = 15)

                    DataBackupHeroCard(
                        headline = "Data & Backup",
                        supporting = "Local only · Last export: ${
                            settings.lastBackupAt?.let { com.mindrelay.util.dateSlash(it) } ?: "never"
                        }",
                        onClick = { nav.navigate(MindScreen.DATA_BACKUP, MindTransition.SLIDE_RIGHT) },
                    )
                }

                Spacer(Modifier.height(32.dp))
            }
        }
    }
}
