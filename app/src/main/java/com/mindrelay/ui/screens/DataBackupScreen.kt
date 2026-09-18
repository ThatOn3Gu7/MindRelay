package com.mindrelay.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.mindrelay.nav.MindNavController
import com.mindrelay.nav.MindScreen
import com.mindrelay.nav.MindTransition
import com.mindrelay.ui.AppViewModel
import com.mindrelay.ui.components.BodyListItem
import com.mindrelay.ui.components.ExpressiveCard
import com.mindrelay.ui.components.SectionLabel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DataBackupScreen(vm: AppViewModel, nav: MindNavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var pendingRestore by remember { mutableStateOf<android.net.Uri?>(null) }
    var restoreConfirm by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val ok = vm.backup.export(context, uri)
                if (ok) {
                    vm.settingsStore.setLastBackupAt(System.currentTimeMillis())
                    snackbar.showSnackbar("Backup exported")
                } else {
                    snackbar.showSnackbar("Export failed")
                }
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val result = vm.backup.import(context, uri)
                when (result) {
                    is com.mindrelay.data.backup.ImportResult.Ok ->
                        snackbar.showSnackbar("Imported ${result.projects} projects, ${result.captures} captures, ${result.memories} memories")
                    is com.mindrelay.data.backup.ImportResult.ParseError ->
                        snackbar.showSnackbar(result.reason)
                }
            }
        }
    }
    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val snapshot = vm.backup.inspect(context, uri)
                if (snapshot == null) {
                    snackbar.showSnackbar("Invalid backup file")
                } else {
                    pendingRestore = uri
                    restoreConfirm = true
                }
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        MindTopBar(
            title = "Data & Backup",
            onBack = { nav.navigate(MindScreen.SETTINGS, MindTransition.SLIDE_RIGHT) },
        )
        SnackbarHost(snackbar)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            ExpressiveCard(
                headline = "Your data stays on your device",
                body = "MindRelay stores everything locally in a Room/SQLite database. There is no account, no cloud sync, and no silent upload. You own your data.",
                container = MaterialTheme.colorScheme.tertiaryContainer,
                content = MaterialTheme.colorScheme.onTertiaryContainer,
                minHeight = 150,
            )
            Spacer(Modifier.padding(top = 6.dp))

            BodyListItem(
                headline = "Export backup",
                supporting = "Save all projects, sessions, memories, captures, tasks to a file",
                icon = Icons.Rounded.FileDownload,
                onClick = { exportLauncher.launch("mindrelay-backup.json") },
            )
            BodyListItem(
                headline = "Import backup",
                supporting = "Validate schema before merging",
                icon = Icons.Rounded.FileUpload,
                onClick = { importLauncher.launch(arrayOf("application/json", "text/*")) },
            )
            BodyListItem(
                headline = "Restore from backup",
                supporting = "Preview before replacing data",
                icon = Icons.Rounded.Restore,
                onClick = { restoreLauncher.launch(arrayOf("application/json", "text/*")) },
            )

            SectionLabel("Backup includes", size = 14, modifier = Modifier.padding(top = 12.dp))
            Text(
                "• Projects & current state\n• Sessions & chronological entries\n• Memories with tags and revisit dates\n• Captures with provenance\n• Tasks\n• All relationships and metadata",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            SectionLabel("Coming soon", size = 13, modifier = Modifier.padding(top = 12.dp))
            Text(
                "Keystore-backed encrypted exports, app lock, biometric unlock.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.padding(bottom = 24.dp))
        }
    }

    if (restoreConfirm) {
        val uri = pendingRestore
        if (uri != null) {
            ConfirmDialog(
                title = "Restore from backup?",
                text = "This previews and then replaces all current local data with the selected backup.",
                confirmLabel = "Restore",
                destructive = true,
                onConfirm = {
                    restoreConfirm = false
                    scope.launch {
                        val result = vm.backup.restore(context, uri)
                        when (result) {
                            is com.mindrelay.data.backup.RestoreResult.Ok -> snackbar.showSnackbar("Data restored")
                            is com.mindrelay.data.backup.RestoreResult.Failed -> snackbar.showSnackbar(result.reason)
                        }
                    }
                },
                onDismiss = { restoreConfirm = false },
            )
        }
    }
}
