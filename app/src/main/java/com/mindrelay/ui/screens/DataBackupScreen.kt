package com.mindrelay.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notes
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mindrelay.nav.MindNavController
import com.mindrelay.ui.AppViewModel
import com.mindrelay.ui.components.SectionLabel
import kotlinx.coroutines.launch

@Composable
private fun HeroPrivacyCard() {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.12f),
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = Icons.Rounded.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                
                Column(modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.15f),
                    ) {
                        Text(
                            text = "100% Offline & Private",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Your data stays on your device",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Text(
                text = "MindRelay stores everything locally in a SQLite database. There is no account, no mandatory cloud sync, and no silent upload. You own your data entirely.",
                style = MaterialTheme.typography.bodyLarge,
                lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.25f,
                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.9f)
            )
        }
    }
}

@Composable
private fun ExpressiveBackupActionTile(
    headline: String,
    supporting: String,
    tag: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 600f),
        label = "backup_tile_scale"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
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
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = containerColor,
                modifier = Modifier.size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = headline,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Text(
                            text = tag,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = supporting,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.15f
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BackupCoverageSection() {
    val coveredItems = remember {
        listOf(
            "Projects" to Icons.Rounded.FolderOpen,
            "Sessions" to Icons.Rounded.Schedule,
            "Memories" to Icons.Rounded.Bookmark,
            "Captures" to Icons.Rounded.Notes,
            "Tasks" to Icons.Rounded.CheckBox
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionLabel("Backup includes", size = 16)

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            coveredItems.forEach { (label, icon) ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "All relationships, metadata, and detail entries are fully preserved. Settings (theme preferences, defaults, reminders) are stored separately and are excluded from JSON backups.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = MaterialTheme.typography.bodySmall.lineHeight * 1.25f
                )
            }
        }
    }
}

@Composable
private fun ComingSoonCard() {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Security Roadmap",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Keystore-backed encrypted exports, app lock, biometric unlock coming in future updates.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DataBackupScreen(vm: AppViewModel, nav: MindNavController) {
    // -------------------------------------------------------------------------
    // DATA AND STATE FLOW (UNTOUCHED)
    // -------------------------------------------------------------------------
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var pendingRestore by remember { mutableStateOf<android.net.Uri?>(null) }
    var restoreConfirm by remember { mutableStateOf(false) }

    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val ok = vm.backup.export(context, uri)
                if (ok) {
                    vm.settingsStore.setLastBackupAt(System.currentTimeMillis())
                    snackbar.showSnackbar("Backup exported successfully")
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
                    is com.mindrelay.data.backup.ImportResult.Ok -> {
                        val extras = if (result.warnings.isNotBlank()) " · ${result.warnings}" else ""
                        snackbar.showSnackbar("Imported ${result.projects} projects, ${result.captures} captures, ${result.memories} memories$extras")
                    }
                    is com.mindrelay.data.backup.ImportResult.ParseError ->
                        snackbar.showSnackbar(result.reason)
                }
            }
        }
    }

    var restorePreview by remember { mutableStateOf<com.mindrelay.data.backup.Snapshot?>(null) }

    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val snapshot = vm.backup.inspect(context, uri)
                if (snapshot == null) {
                    snackbar.showSnackbar("Invalid backup file — nothing was changed")
                } else {
                    restorePreview = snapshot
                    pendingRestore = uri
                    restoreConfirm = true
                }
            }
        }
    }
    // -------------------------------------------------------------------------

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            MindTopBar(
                title = "Data & Backup",
                onBack = { nav.pop() },
            )
        }
    ) { paddingValues ->
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(tween(300)) + slideInVertically(
                initialOffsetY = { 60 },
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f)
            ),
            modifier = Modifier.padding(paddingValues)
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                // Privacy Hero Banner
                HeroPrivacyCard()

                // Action Cards
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionLabel("Data Operations", size = 16)

                    ExpressiveBackupActionTile(
                        headline = "Export Backup",
                        supporting = "Save all projects, sessions, memories, captures, and tasks into a JSON file",
                        tag = "Export",
                        icon = Icons.Rounded.FileDownload,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        onClick = { exportLauncher.launch("mindrelay-backup.json") },
                    )

                    ExpressiveBackupActionTile(
                        headline = "Import Backup",
                        supporting = "Validate the file and merge data into your database without losing existing items",
                        tag = "Merge",
                        icon = Icons.Rounded.FileUpload,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        onClick = { importLauncher.launch(arrayOf("application/json", "text/*")) },
                    )

                    ExpressiveBackupActionTile(
                        headline = "Restore from Backup",
                        supporting = "Inspect contents before completely replacing local database data",
                        tag = "Replace",
                        icon = Icons.Rounded.Restore,
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        onClick = { restoreLauncher.launch(arrayOf("application/json", "text/*")) },
                    )
                }

                // Coverage Section
                BackupCoverageSection()

                // Security Roadmap Preview
                ComingSoonCard()

                Spacer(Modifier.height(32.dp))
            }
        }
    }

    if (restoreConfirm) {
        val uri = pendingRestore
        if (uri != null) {
            val preview = restorePreview
            ConfirmDialog(
                title = "Restore from backup?",
                text = buildString {
                    append("This replaces ALL current local data with the selected backup.")
                    append("\n\nThe backup contains:\n")
                    if (preview != null) {
                        append("• ${preview.projects} projects\n")
                        append("• ${preview.sessions} sessions\n")
                        append("• ${preview.captures} captures\n")
                        append("• ${preview.memories} memories\n")
                        append("• ${preview.tasks} tasks\n")
                    }
                    append("\nA safety backup of your current data is saved before restoring.")
                },
                confirmLabel = "Restore",
                destructive = true,
                onConfirm = {
                    restoreConfirm = false
                    scope.launch {
                        val result = vm.backup.restore(context, uri)
                        when (result) {
                            is com.mindrelay.data.backup.RestoreResult.Ok -> snackbar.showSnackbar("Data restored (safety backup saved locally)")
                            is com.mindrelay.data.backup.RestoreResult.Failed -> snackbar.showSnackbar(result.reason)
                        }
                    }
                },
                onDismiss = {
                    restoreConfirm = false
                    restorePreview = null
                },
            )
        }
    }
}

