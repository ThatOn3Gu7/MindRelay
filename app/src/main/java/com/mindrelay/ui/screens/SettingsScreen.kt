package com.mindrelay.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.ChevronRight
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mindrelay.data.model.CaptureKind
import com.mindrelay.data.settings.toCaptureKindOrNull
import com.mindrelay.nav.MindNavController
import com.mindrelay.nav.MindScreen
import com.mindrelay.nav.MindTransition
import com.mindrelay.ui.AppViewModel
import com.mindrelay.ui.components.BodyListItem
import com.mindrelay.ui.components.SectionLabel

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsScreen(vm: AppViewModel, nav: MindNavController) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    var themeMenu by remember { mutableStateOf(false) }
    var captureKindMenu by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        MindTopBar(
            title = "Settings",
            onBack = { nav.popToRoot() },
        )
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

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            SectionLabel("Appearance", size = 13)
            BodyListItem(
                headline = "Theme",
                supporting = settings.theme,
                icon = Icons.Rounded.Palette,
                onClick = { themeMenu = true },
                trailing = { Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            BodyListItem(
                headline = "Expressive style",
                supporting = "Rounded · Roboto Flex",
                icon = Icons.Rounded.Brush,
                trailing = { Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
            )

            SectionLabel("Capture", size = 13, modifier = Modifier.padding(top = 16.dp))
            BodyListItem(
                headline = "Default save behavior",
                supporting = settings.defaultSave,
                icon = Icons.Rounded.Save,
                onClick = { vm.launch { vm.settingsStore.setDefaultSave(if (settings.defaultSave == "Inbox") "Current project" else "Inbox") } },
                trailing = { Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
            )
            BodyListItem(
                headline = "Default capture kind",
                supporting = settings.defaultCaptureKind.toCaptureKindOrNull()?.let {
                    captureKindLabel(it)
                } ?: settings.defaultCaptureKind,
                icon = Icons.Rounded.Save,
                onClick = { captureKindMenu = true },
                trailing = { Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
            )
            SwitchRow(
                title = "Voice capture available",
                checked = settings.voiceCapture,
                onChecked = { vm.launch { vm.settingsStore.setVoiceCapture(it) } },
                supporting = "Text-only captures are always available",
            )
            SwitchRow(
                title = "Automatic project linking",
                checked = settings.autoLink,
                onChecked = { vm.launch { vm.settingsStore.setAutoLink(it) } },
            )

            SectionLabel("Reminders", size = 13, modifier = Modifier.padding(top = 16.dp))
            SwitchRow(
                title = "Revisit reminders",
                checked = settings.revisitReminders,
                onChecked = { vm.launch { vm.settingsStore.setRevisitReminders(it) } },
            )

            SectionLabel("Data", size = 13, modifier = Modifier.padding(top = 16.dp))
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                BodyListItem(
                    headline = "Data & Backup",
                    supporting = "Local only · Last export: ${settings.lastBackupAt?.let { com.mindrelay.util.dateSlash(it) } ?: "never"}",
                    icon = Icons.Rounded.Storage,
                    iconContainer = MaterialTheme.colorScheme.primaryContainer,
                    iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    onClick = { nav.navigate(MindScreen.DATA_BACKUP, MindTransition.SLIDE_RIGHT) },
                    trailing = {
                        Icon(
                            Icons.Rounded.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    },
                )
            }
            Spacer(Modifier.padding(bottom = 24.dp))
        }
    }
}

private fun captureKindLabel(kind: CaptureKind): String = when (kind) {
    CaptureKind.IDEA -> "Idea"
    CaptureKind.TODO -> "To-do"
    CaptureKind.QUESTION -> "Question"
    CaptureKind.NOTE -> "Note"
    CaptureKind.VOICE -> "Voice"
}

@Composable
private fun SwitchRow(
    title: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit,
    supporting: String? = null,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            supporting?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                uncheckedBorderColor = MaterialTheme.colorScheme.outline,
            ),
        )
    }
}
