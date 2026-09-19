package com.mindrelay.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EmojiObjects
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Notes
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mindrelay.data.model.EntryKind
import com.mindrelay.data.model.SessionStatus
import com.mindrelay.nav.MindNavController
import com.mindrelay.nav.MindScreen
import com.mindrelay.nav.MindTransition
import com.mindrelay.ui.AppViewModel
import com.mindrelay.ui.components.ConnectedChipGroup
import com.mindrelay.ui.components.MindTextField
import com.mindrelay.ui.components.PillButton
import com.mindrelay.ui.components.SectionLabel
import com.mindrelay.util.clockTime
import com.mindrelay.util.durationText
import com.mindrelay.util.stopwatch
import kotlinx.coroutines.delay

private fun kindIcon(kind: EntryKind): ImageVector = when (kind) {
    EntryKind.NOTE -> Icons.Rounded.Notes
    EntryKind.DISCOVERY -> Icons.Rounded.EmojiObjects
    EntryKind.QUESTION -> Icons.Rounded.Warning
    EntryKind.DECISION -> Icons.Rounded.EmojiObjects
    EntryKind.TASK -> Icons.Rounded.ShoppingCart
}

private fun kindLabel(kind: EntryKind): String = when (kind) {
    EntryKind.NOTE -> "Note"
    EntryKind.DISCOVERY -> "Discovery"
    EntryKind.QUESTION -> "Question"
    EntryKind.DECISION -> "Decision"
    EntryKind.TASK -> "Task"
}

@Composable
private fun kindIconColors(kind: EntryKind): Pair<Color, Color> = when (kind) {
    EntryKind.NOTE -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
    EntryKind.DISCOVERY -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
    EntryKind.QUESTION -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
    EntryKind.DECISION -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
    EntryKind.TASK -> MaterialTheme.colorScheme.surfaceContainerHighest to MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
private fun ExpressiveSessionEntryItem(
    headline: String,
    supporting: String,
    icon: ImageVector,
    kind: EntryKind,
) {
    val (iconContainerColor, iconContentColor) = kindIconColors(kind)

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
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
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
    }
}

@Composable
private fun SessionStatusChip(active: Boolean, label: String) {
    val (icon, containerColor, contentColor) = if (active) Triple(
        Icons.Rounded.PlayArrow,
        MaterialTheme.colorScheme.primaryContainer,
        MaterialTheme.colorScheme.onPrimaryContainer
    ) else Triple(
        Icons.Rounded.CheckCircle,
        MaterialTheme.colorScheme.tertiaryContainer,
        MaterialTheme.colorScheme.onTertiaryContainer
    )

    Surface(
        shape = CircleShape,
        color = containerColor,
        contentColor = contentColor,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun CurrentNextActionCard(text: String) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.12f),
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Text(
                    text = "Current next action",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.25f,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SessionScreen(vm: AppViewModel, nav: MindNavController, sessionId: Long?) {
    // -------------------------------------------------------------------------
    // DATA AND STATE FLOW (UNTOUCHED)
    // -------------------------------------------------------------------------
    val repo = vm.repository
    if (sessionId == null) {
        LaunchedEffect(Unit) { nav.pop() }
        return
    }
    val sessionFlow = repo.sessions.byId(sessionId).collectAsStateWithLifecycle(initialValue = null)
    val session = sessionFlow.value
    val entries by repo.entries.bySession(sessionId).collectAsStateWithLifecycle(initialValue = emptyList())
    val projects by repo.projects.all().collectAsStateWithLifecycle(initialValue = emptyList())
    var tab by remember { mutableStateOf("Log") }
    var addDialog by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var clock by remember { mutableStateOf("") }

    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }

    val project = session?.let { s -> projects.firstOrNull { it.id == s.projectId } }

    LaunchedEffect(session) {
        val s = session
        if (s != null && s.status == SessionStatus.ACTIVE) {
            while (true) {
                clock = stopwatch(s.startedAt)
                delay(1000)
            }
        } else {
            clock = ""
        }
    }

    if (session == null) {
        Text("This session is no longer available.", modifier = Modifier.padding(16.dp))
        return
    }

    val filtered = when (tab) {
        "Discoveries" -> entries.filter { it.kind == EntryKind.DISCOVERY || it.kind == EntryKind.DECISION }
        "Questions" -> entries.filter { it.kind == EntryKind.QUESTION }
        else -> entries
    }
    // -------------------------------------------------------------------------

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            MindTopBar(
                title = session.title.substringBefore("·").trim(),
                onBack = { nav.pop() },
                actions = listOf(Icons.Rounded.MoreVert to { menuOpen = true }),
            )
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text("Delete session", color = MaterialTheme.colorScheme.error) },
                    onClick = {
                        menuOpen = false
                        vm.launchAndRun(
                            block = {
                                repo.deleteSession(session.id)
                                null
                            },
                            andThen = { nav.pop() },
                        )
                    },
                )
            }

            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(tween(250)) + slideInVertically(
                    initialOffsetY = { 40 },
                    animationSpec = spring(dampingRatio = 0.8f, stiffness = 350f)
                ),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    // Header: live status chip + owning project.
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SessionStatusChip(
                            active = session.status == SessionStatus.ACTIVE,
                            label = if (session.status == SessionStatus.ACTIVE) "Active · $clock"
                            else "Completed · ${durationText(session.startedAt, session.endedAt)}",
                        )
                        if (project != null) {
                            Text(
                                project.name,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    // Log filter chips, centered.
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        ConnectedChipGroup(
                            options = listOf("Log", "Discoveries", "Questions"),
                            selected = tab,
                            onSelect = { tab = it },
                        )
                    }

                    SectionLabel("Log")

                    LazyColumn(
                        Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (filtered.isEmpty()) {
                            item {
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        "Nothing logged yet. Tap + to capture a note, discovery, question, decision or task.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            }
                        }
                        items(filtered, key = { it.id }) { e ->
                            ExpressiveSessionEntryItem(
                                headline = e.text,
                                supporting = "${clockTime(e.createdAt)} · ${kindLabel(e.kind)}",
                                icon = kindIcon(e.kind),
                                kind = e.kind,
                            )
                        }
                        item {
                            Spacer(Modifier.height(4.dp))
                            CurrentNextActionCard(
                                text = session.currentNextAction.ifBlank {
                                    project?.nextAction ?: "Set your next action before you finish."
                                },
                            )
                        }
                    }
                }
            }

            // Fixed action bar for the terminal handoff action.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                PillButton(
                    text = "End session & write handoff",
                    icon = Icons.Rounded.Flag,
                    enabled = session.status == SessionStatus.ACTIVE,
                    onClick = {
                        nav.navigate(MindScreen.END_SESSION, MindTransition.SLIDE_UP, session.id.toString())
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                )
            }
        }

        // Tonal FAB for quick-add entry, floated above the pinned action bar.
        FloatingActionButton(
            onClick = { addDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 96.dp),
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shape = RoundedCornerShape(16.dp),
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 3.dp),
        ) {
            Icon(Icons.Rounded.Add, contentDescription = "Quick-add entry")
        }
    }

    if (addDialog) {
        AddEntryDialog(
            onDismiss = { addDialog = false },
            onAdd = { text, kind ->
                vm.launch { repo.addSessionEntry(session.id, text, kind) }
                addDialog = false
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AddEntryDialog(
    onDismiss: () -> Unit,
    onAdd: (String, EntryKind) -> Unit,
) {
    var text by remember { mutableStateOf("") }
    var kind by remember { mutableStateOf(EntryKind.NOTE) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Quick-add entry") },
        text = {
            Column {
                MindTextField(value = text, onValueChange = { text = it }, label = "Entry", modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    ConnectedChipGroup(
                        options = listOf("Note", "Discovery", "Question", "Decision", "Task"),
                        selected = when (kind) {
                            EntryKind.DISCOVERY -> "Discovery"
                            EntryKind.QUESTION -> "Question"
                            EntryKind.DECISION -> "Decision"
                            EntryKind.TASK -> "Task"
                            else -> "Note"
                        },
                        onSelect = { label ->
                            kind = when (label) {
                                "Discovery" -> EntryKind.DISCOVERY
                                "Question" -> EntryKind.QUESTION
                                "Decision" -> EntryKind.DECISION
                                "Task" -> EntryKind.TASK
                                else -> EntryKind.NOTE
                            }
                        },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onAdd(text, kind) }, enabled = text.isNotBlank()) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
