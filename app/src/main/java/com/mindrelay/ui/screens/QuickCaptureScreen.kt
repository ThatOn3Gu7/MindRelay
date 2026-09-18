package com.mindrelay.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mindrelay.data.model.CaptureKind
import com.mindrelay.nav.MindNavController
import com.mindrelay.nav.MindRoute
import com.mindrelay.nav.MindScreen
import com.mindrelay.nav.MindTransition
import com.mindrelay.ui.AppViewModel
import com.mindrelay.ui.components.ConnectedChipGroup
import com.mindrelay.ui.components.MindTextField
import com.mindrelay.ui.components.PillChip
import com.mindrelay.ui.components.SectionLabel

/**
 * Zero-friction capture: a large field, a mic affordance, optional chips and an
 * optional project link. Saving never requires metadata.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)
@Composable
fun QuickCaptureScreen(vm: AppViewModel, nav: MindNavController) {
    val repo = vm.repository
    val projects by repo.projects.all().collectAsStateWithLifecycle(initialValue = emptyList())

    var text by remember { mutableStateOf("") }
    var kind by remember { mutableStateOf("Idea") }
    var linkedProjectId by remember { mutableStateOf<Long?>(null) }

    val kindEnum = when (kind) {
        "To-do" -> CaptureKind.TODO
        "Question" -> CaptureKind.QUESTION
        else -> CaptureKind.IDEA
    }

    fun saveNow(): Boolean {
        if (text.isBlank()) return false
        val body = text.trim()
        val project = linkedProjectId
        vm.launch { repo.saveCapture(body, kind = kindEnum, projectId = project) }
        return true
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .imePadding()
    ) {
        MindTopBar(title = "Quick Capture", onClose = { nav.pop() })
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            SectionLabel("What's on your mind?", size = 24)
            Text(
                "Get it out now. Sort it later.",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))

            MindTextField(
                value = text,
                onValueChange = { text = it },
                label = "Type a thought...",
                leadingIcon = Icons.Rounded.Mic,
                supportingText = "Saved locally immediately. No account required.",
                filled = true,
                singleLine = false,
                minLines = 5,
                maxLines = 8,
            )
            Spacer(Modifier.height(16.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                ConnectedChipGroup(
                    options = listOf("Idea", "To-do", "Question"),
                    selected = kind,
                    onSelect = { kind = it },
                )
            }
            Spacer(Modifier.height(16.dp))

            ProjectLinkPicker(projects, linkedProjectId) { picked ->
                linkedProjectId = if (linkedProjectId == picked) null else picked
            }
            Spacer(Modifier.height(16.dp))

            Text(
                "🔒 Stays on your device. No cloud, no account.",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(20.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                TextButton(
                    onClick = { if (saveNow()) text = "" },
                    enabled = text.isNotBlank(),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(28.dp, 8.dp, 8.dp, 28.dp),
                ) {
                    Text("Save & keep writing", style = MaterialTheme.typography.labelLarge)
                }
                Button(
                    onClick = {
                        saveNow()
                        nav.resetTo(listOf(MindRoute(MindScreen.HOME)), MindTransition.SLIDE_DOWN)
                    },
                    enabled = text.isNotBlank(),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp, 28.dp, 28.dp, 8.dp),
                ) {
                    Text("Save to Inbox", style = MaterialTheme.typography.labelLarge)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)
@Composable
private fun ProjectLinkPicker(
    projects: List<com.mindrelay.data.db.ProjectEntity>,
    linkedProjectId: Long?,
    onPick: (Long) -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                .padding(10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.FolderOpen, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text("Link to project (optional)", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(
                projects.firstOrNull { it.id == linkedProjectId }?.name ?: "None",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = { projects.firstOrNull()?.let { onPick(it.id) } }) {
            Icon(Icons.Rounded.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
    }
    if (projects.isNotEmpty()) {
        Spacer(Modifier.height(4.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            projects.forEach { p ->
                PillChip(label = p.name, selected = linkedProjectId == p.id, onClick = { onPick(p.id) })
            }
        }
    }
}
