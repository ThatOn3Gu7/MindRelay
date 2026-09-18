package com.mindrelay.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.EmojiObjects
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Notes
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mindrelay.data.model.EntryKind
import com.mindrelay.data.model.SessionStatus
import com.mindrelay.nav.MindNavController
import com.mindrelay.nav.MindScreen
import com.mindrelay.nav.MindTransition
import com.mindrelay.ui.AppViewModel
import com.mindrelay.ui.components.BodyListItem
import com.mindrelay.ui.components.ConnectedChipGroup
import com.mindrelay.ui.components.ExpressiveCard
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SessionScreen(vm: AppViewModel, nav: MindNavController, sessionId: Long?) {
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

    androidx.compose.foundation.layout.Box(Modifier.fillMaxSize()) {
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

            Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                if (project != null) {
                    Text(project.name, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TimedChip(
                        if (session.status == SessionStatus.ACTIVE) "Active · $clock"
                        else "Completed · ${durationText(session.startedAt, session.endedAt)}"
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    ConnectedChipGroup(
                        options = listOf("Log", "Discoveries", "Questions"),
                        selected = tab,
                        onSelect = { tab = it },
                    )
                }
                Spacer(Modifier.height(8.dp))

                LazyColumn(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (filtered.isEmpty()) {
                        item {
                            Text(
                                "Nothing logged yet. Tap + to capture a note, discovery, question, decision or task.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    items(filtered, key = { it.id }) { e ->
                        BodyListItem(
                            headline = e.text,
                            supporting = "${clockTime(e.createdAt)} · ${kindLabel(e.kind)}",
                            icon = kindIcon(e.kind),
                            onClick = null,
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    }
                    item {
                        ExpressiveCard(
                            headline = "Current next action",
                            body = session.currentNextAction.ifBlank {
                                project?.nextAction ?: "Set your next action before you finish."
                            },
                            container = MaterialTheme.colorScheme.surfaceContainerHighest,
                            content = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                }
                Spacer(Modifier.height(16.dp))
                androidx.compose.material3.Button(
                    onClick = { nav.navigate(MindScreen.END_SESSION, MindTransition.SLIDE_UP, session.id.toString()) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
                    enabled = session.status == SessionStatus.ACTIVE,
                ) {
                    Icon(Icons.Rounded.Flag, contentDescription = null)
                    Text("End session & write handoff", Modifier.padding(start = 8.dp))
                }
                Spacer(Modifier.height(20.dp))
            }
        }

        // Tonal FAB overlapping the "Current next action" card.
        FloatingActionButton(
            onClick = { addDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            elevation = androidx.compose.material3.FloatingActionButtonDefaults.elevation(3.dp, 3.dp, 3.dp, 3.dp),
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

@Composable
private fun TimedChip(label: String) {
    androidx.compose.material3.Surface(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
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
                OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("Entry") }, modifier = Modifier.fillMaxWidth())
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
