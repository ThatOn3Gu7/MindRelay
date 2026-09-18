package com.mindrelay.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mindrelay.data.model.ProjectStatus
import com.mindrelay.nav.MindNavController
import com.mindrelay.nav.MindScreen
import com.mindrelay.nav.MindTransition
import com.mindrelay.ui.AppViewModel
import com.mindrelay.ui.components.BodyListItem
import com.mindrelay.ui.components.ExpressiveCard
import com.mindrelay.ui.components.PillButton
import com.mindrelay.ui.components.SectionLabel
import com.mindrelay.ui.components.TonalPillButton
import com.mindrelay.util.durationText
import com.mindrelay.util.relativeAgo

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ProjectDetailScreen(vm: AppViewModel, nav: MindNavController, projectId: Long?) {
    val repo = vm.repository
    if (projectId == null) {
        androidx.compose.runtime.LaunchedEffect(Unit) { nav.pop() }
        return
    }
    val projectFlow = repo.projects.byId(projectId).collectAsStateWithLifecycle(initialValue = null)
    val project = projectFlow.value
    val sessions by repo.sessions.byProject(projectId).collectAsStateWithLifecycle(initialValue = emptyList())
    val linked by repo.memories.byProject(projectId).collectAsStateWithLifecycle(initialValue = emptyList())
    var menuOpen by remember { mutableStateOf(false) }
    var statusMenu by remember { mutableStateOf(false) }

    if (project == null) {
        Text("This project is no longer available.", modifier = Modifier.padding(16.dp))
        return
    }

    Column(Modifier.fillMaxSize()) {
        MindTopBar(
            title = project.name,
            onBack = { nav.popToRoot() },
            actions = listOf(Icons.Rounded.MoreVert to { menuOpen = true }),
        )
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(text = { Text("Change status") }, onClick = { menuOpen = false; statusMenu = true })
            DropdownMenuItem(
                text = { Text("Edit project") },
                onClick = {
                    menuOpen = false
                    nav.navigate(MindScreen.NEW_PROJECT, MindTransition.SLIDE_UP, project.id.toString())
                },
            )
            DropdownMenuItem(
                text = { Text("Delete project", color = MaterialTheme.colorScheme.error) },
                onClick = {
                    menuOpen = false
                    vm.launchAndRun(
                        block = {
                            repo.deleteProject(project.id)
                            null
                        },
                        andThen = { nav.resetTo(listOf(com.mindrelay.nav.MindRoute(MindScreen.PROJECTS)), MindTransition.SLIDE_DOWN) },
                    )
                },
            )
        }
        DropdownMenu(expanded = statusMenu, onDismissRequest = { statusMenu = false }) {
            ProjectStatus.entries.forEach { st ->
                DropdownMenuItem(text = { Text(statusLabel(st)) }, onClick = {
                    statusMenu = false
                    vm.launch { repo.setProjectStatus(project.id, st) }
                })
            }
        }

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Active/Paused/Done chip
            StatusChip(statusLabel(project.status))

            if (project.currentState.isNotBlank()) {
                ExpressiveCard(
                    headline = "Current state",
                    body = project.currentState,
                    container = MaterialTheme.colorScheme.surfaceContainerHighest,
                    content = MaterialTheme.colorScheme.onSurface,
                    minHeight = 130,
                )
            }
            if (project.goal.isNotBlank()) {
                ExpressiveCard(
                    headline = "Goal",
                    body = project.goal,
                    container = MaterialTheme.colorScheme.surfaceContainerLow,
                    content = MaterialTheme.colorScheme.onSurface,
                )
            }
            ExpressiveCard(
                headline = "Next action",
                body = project.nextAction.ifBlank { "No next action yet — set one when you resume." },
                container = MaterialTheme.colorScheme.tertiaryContainer,
                content = MaterialTheme.colorScheme.onTertiaryContainer,
                minHeight = 100,
            )

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                PillButton(
                    // A DONE project must be explicitly reactivated before a new
                    // session can start; it never silently acts like an ACTIVE one.
                    text = if (project.status == ProjectStatus.DONE) "Reactivate" else "Resume session",
                    icon = Icons.Rounded.PlayArrow,
                    onClick = {
                        var resultingSessionId: Long? = null
                        var reactivated = false
                        vm.launchAndRun(
                            block = {
                                if (project.status == ProjectStatus.DONE) {
                                    repo.setProjectStatus(project.id, ProjectStatus.ACTIVE)
                                    reactivated = true
                                    null
                                } else {
                                    val sid = repo.resumeOrNewSession(project.id)
                                    if (sid == null) {
                                        "Project no longer exists"
                                    } else {
                                        resultingSessionId = sid
                                        null
                                    }
                                }
                            },
                            andThen = {
                                if (reactivated) {
                                    // Stay on the project; the label flips back to
                                    // "Resume session" via the reactive status flow.
                                    Unit
                                } else if (resultingSessionId != null) {
                                    nav.navigate(MindScreen.SESSION, MindTransition.SLIDE_RIGHT, resultingSessionId.toString())
                                }
                            },
                        )
                    },
                    modifier = Modifier.weight(1f),
                )
                TonalPillButton(
                    text = "Add note",
                    // Always opens capture pre-linked to THIS project, so the
                    // saved note is genuinely associated with the selected project.
                    onClick = { nav.navigate(MindScreen.QUICK_CAPTURE, MindTransition.EXPAND, project.id.toString()) },
                    modifier = Modifier.weight(1f),
                )
            }

            SectionLabel("Recent sessions")
            if (sessions.isEmpty()) {
                Text(
                    "No sessions yet. Resume to start one.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                sessions.forEach { s ->
                    BodyListItem(
                        headline = s.title,
                        supporting = "${relativeAgo(s.startedAt)} · ${durationText(s.startedAt, s.endedAt)}",
                        icon = Icons.Rounded.Schedule,
                        onClick = { nav.navigate(MindScreen.SESSION, MindTransition.SLIDE_RIGHT, s.id.toString()) },
                    )
                }
            }

            SectionLabel("Linked memories", size = 14)
            if (linked.isEmpty()) {
                Text(
                    "None linked yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                linked.forEach { m ->
                    BodyListItem(
                        headline = m.title,
                        supporting = "Linked memory",
                        icon = Icons.Rounded.Schedule,
                        onClick = { nav.navigate(MindScreen.MEMORY_DETAIL, MindTransition.SLIDE_RIGHT, m.id.toString()) },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

private fun statusLabel(s: ProjectStatus) = when (s) {
    ProjectStatus.ACTIVE -> "Active"
    ProjectStatus.PAUSED -> "Paused"
    ProjectStatus.DONE -> "Done"
}

@Composable
private fun StatusChip(label: String) {
    androidx.compose.material3.Surface(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
    }
}
