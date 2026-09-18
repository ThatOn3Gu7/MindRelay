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
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mindrelay.data.model.CaptureKind
import com.mindrelay.nav.MindNavController
import com.mindrelay.nav.MindScreen
import com.mindrelay.nav.MindTransition
import com.mindrelay.ui.AppViewModel
import com.mindrelay.ui.components.BodyListItem
import com.mindrelay.ui.components.ExpressiveCard
import com.mindrelay.ui.components.SectionLabel
import com.mindrelay.util.relativeAgo

private fun kindLabel(kind: CaptureKind): String = when (kind) {
    CaptureKind.IDEA -> "Idea"
    CaptureKind.TODO -> "To-do"
    CaptureKind.QUESTION -> "Question"
    CaptureKind.NOTE -> "Note"
    CaptureKind.VOICE -> "Voice"
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CaptureDetailScreen(vm: AppViewModel, nav: MindNavController, captureId: Long?) {
    val repo = vm.repository
    if (captureId == null) {
        androidx.compose.runtime.LaunchedEffect(Unit) { nav.pop() }
        return
    }
    val captureFlow = repo.captures.byId(captureId).collectAsStateWithLifecycle(initialValue = null)
    val capture = captureFlow.value
    val projects by repo.projects.all().collectAsStateWithLifecycle(initialValue = emptyList())
    var menuOpen by remember { mutableStateOf(false) }

    if (capture == null) {
        Text("This capture is no longer available.", modifier = Modifier.padding(16.dp))
        return
    }
    val linkedProject = capture.projectId?.let { pid -> projects.firstOrNull { it.id == pid } }

    Column(Modifier.fillMaxSize()) {
        MindTopBar(
            title = "Capture",
            onBack = { nav.popToRoot() },
            actions = listOf(
                Icons.Rounded.MoreVert to { menuOpen = true },
            ),
        )
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text("Delete capture") },
                onClick = {
                    menuOpen = false
                    vm.launchAndRun(
                        block = {
                            repo.deleteCapture(capture)
                            null
                        },
                        andThen = { nav.resetTo(listOf(com.mindrelay.nav.MindRoute(MindScreen.INBOX)), MindTransition.SLIDE_DOWN) },
                    )
                },
            )
        }
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Selected chip (the capture's kind) + provenance line.
            RecKindChip(kindLabel(capture.kind))
            Text(
                if (capture.isVoice)
                    "Captured ${relativeAgo(capture.createdAt)} · voice entry · no audio stored"
                else
                    "Captured ${relativeAgo(capture.createdAt)} · text entry",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            ExpressiveCard(
                headline = capture.text,
                body = capture.detail.ifBlank { capture.text },
                container = MaterialTheme.colorScheme.surfaceContainerHighest,
                content = MaterialTheme.colorScheme.onSurface,
                minHeight = 140,
            )

            BodyListItem(
                headline = "Linked project",
                supporting = linkedProject?.name ?: "None",
                icon = Icons.Rounded.FolderOpen,
                onClick = linkedProject?.let { p ->
                    { nav.navigate(MindScreen.PROJECT_DETAIL, MindTransition.SLIDE_RIGHT, p.id.toString()) }
                },
            )
            BodyListItem(
                headline = "Linked session",
                supporting = capture.linkedSessionId?.let { "Session #$it" } ?: "None",
                icon = Icons.Rounded.Schedule,
            )

            SectionLabel("Convert to")
            ConvertItem(
                title = "Memory",
                supporting = "Durable knowledge Future You can find",
                icon = Icons.Rounded.Bookmark,
                onClick = {
                    vm.launchAndRun(
                        block = {
                            if (capture.converted == com.mindrelay.data.model.ConvertType.MEMORY) {
                                "This capture was already converted to a Memory"
                            } else {
                                val result = repo.convertToMemory(capture.id)
                                when (result) {
                                    is com.mindrelay.data.repo.ConvertResult.Ok -> null
                                    is com.mindrelay.data.repo.ConvertResult.Rejected -> result.reason
                                }
                            }
                        },
                        andThen = { nav.resetTo(listOf(com.mindrelay.nav.MindRoute(MindScreen.MEMORIES)), MindTransition.SLIDE_DOWN) },
                    )
                },
            )
            ConvertItem(
                title = "Project note",
                supporting = "Attach to a project or current session",
                icon = Icons.Rounded.FolderOpen,
                onClick = {
                    val pid = capture.projectId ?: projects.firstOrNull { it.status == com.mindrelay.data.model.ProjectStatus.ACTIVE }?.id
                        ?: projects.firstOrNull()?.id
                    if (pid != null) {
                        vm.launchAndRun(
                            block = {
                                if (capture.converted == com.mindrelay.data.model.ConvertType.PROJECT_NOTE) {
                                    "This capture was already converted to a Project note"
                                } else {
                                    val result = repo.convertToProjectNote(capture.id, pid)
                                    when (result) {
                                        is com.mindrelay.data.repo.ConvertResult.Ok -> null
                                        is com.mindrelay.data.repo.ConvertResult.Rejected -> result.reason
                                    }
                                }
                            },
                            andThen = { nav.resetTo(listOf(com.mindrelay.nav.MindRoute(MindScreen.PROJECT_DETAIL, pid.toString())), MindTransition.SLIDE_DOWN) },
                        )
                    } else {
                        nav.navigate(MindScreen.NEW_PROJECT, MindTransition.SLIDE_UP)
                    }
                },
            )
            ConvertItem(
                title = "Task",
                supporting = "Create a secondary to-do item",
                icon = Icons.Rounded.CheckBox,
                onClick = {
                    vm.launchAndRun(
                        block = {
                            if (capture.converted == com.mindrelay.data.model.ConvertType.TASK) {
                                "This capture was already converted to a Task"
                            } else {
                                val result = repo.convertToTask(capture.id)
                                when (result) {
                                    is com.mindrelay.data.repo.ConvertResult.Ok -> null
                                    is com.mindrelay.data.repo.ConvertResult.Rejected -> result.reason
                                }
                            }
                        },
                        andThen = { nav.resetTo(listOf(com.mindrelay.nav.MindRoute(MindScreen.HOME)), MindTransition.SLIDE_DOWN) },
                    )
                },
            )

            // Archive is an outlined full-width button per the sketch.
            androidx.compose.material3.OutlinedButton(
                onClick = {
                    vm.launchAndRun(
                        block = {
                            repo.archiveCapture(capture)
                            null
                        },
                        andThen = { nav.resetTo(listOf(com.mindrelay.nav.MindRoute(MindScreen.INBOX)), MindTransition.SLIDE_DOWN) },
                    )
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
            ) {
                Icon(Icons.Rounded.Archive, contentDescription = null)
                Text("Archive", Modifier.padding(start = 8.dp))
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun RecKindChip(label: String) {
    Surface(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Icon(Icons.Rounded.CheckBox, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun ConvertItem(
    title: String,
    supporting: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    BodyListItem(headline = title, supporting = supporting, icon = icon, onClick = onClick)
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
}
