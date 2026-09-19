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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mindrelay.data.model.CaptureKind
import com.mindrelay.data.settings.toCaptureKindOrNull
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
 * Zero-friction capture: a large field, an (explicitly text-only, non-voice)
 * mic-illustrated affordance, optional kind chips and an optional project link.
 * Saving never requires metadata. The "default capture kind" setting (when the
 * user has chosen one) pre-selects the chip; Quick Capture never hardcodes a
 * kind that contradicts the user's chosen default.
 *
 * A route argument may carry an initial project id (e.g. "Add note" from a
 * Project Detail screen); when present, the capture starts linked to that
 * project and the picker + currency hint call it out.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)
@Composable
fun QuickCaptureScreen(vm: AppViewModel, nav: MindNavController, initialProjectId: Long? = null) {
    // Data & State (Untouched)
    val repo = vm.repository
    val projects by repo.projects.all().collectAsStateWithLifecycle(initialValue = emptyList())
    val settings by vm.settings.collectAsStateWithLifecycle()

    var text by remember { mutableStateOf("") }
    var kind by remember {
        mutableStateOf(friendlyKindLabel(settings.defaultCaptureKind))
    }
    var linkedProjectId by remember { mutableStateOf<Long?>(initialProjectId) }

    val kindEnum = when (kind) {
        "To-do" -> CaptureKind.TODO
        "Question" -> CaptureKind.QUESTION
        "Note" -> CaptureKind.NOTE
        else -> CaptureKind.IDEA
    }

    val projectLinkedLabel = linkedProjectId?.let { id ->
        projects.firstOrNull { it.id == id }?.name
    }

    // Main Layout (Redesigned visual hierarchy)
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .imePadding()
    ) {
        MindTopBar(title = "Quick Capture", onClose = { nav.pop() })
        
        // Scrollable Workspace Area
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            // Header
            SectionLabel("What's on your mind?", size = 28)
            Spacer(Modifier.height(4.dp))
            Text(
                "Get it out now. Sort it later.",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            
            Spacer(Modifier.height(24.dp))

            // Primary Input Canvas
            MindTextField(
                value = text,
                onValueChange = { text = it },
                label = "Type a thought...",
                leadingIcon = Icons.Rounded.Mic,
                supportingText = "Text notes only — no audio is recorded or stored. Saved locally.",
                filled = true,
                singleLine = false,
                minLines = 6,
                maxLines = 10,
            )
            
            Spacer(Modifier.height(32.dp))

            // Metadata Group Card (Groups Type and Project together cleanly)
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .padding(20.dp)
            ) {
                Column {
                    Text(
                        "Categorize as", 
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(12.dp))
                    
                    ConnectedChipGroup(
                        options = listOf("Idea", "To-do", "Question", "Note"),
                        selected = kind,
                        onSelect = { kind = it },
                    )

                    Spacer(Modifier.height(20.dp))
                    
                    // Subtle Divider
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f))
                    )
                    
                    Spacer(Modifier.height(20.dp))

                    ProjectLinkPicker(projects, linkedProjectId) { picked ->
                        linkedProjectId = if (linkedProjectId == picked) null else picked
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        // Fixed Bottom Action Bar
        Column(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                "🔒 Stays on your device. No cloud, no account." +
                    if (projectLinkedLabel != null) "\nLinking to: $projectLinkedLabel" else "",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(Modifier.height(16.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                TextButton(
                    onClick = {
                        if (text.isNotBlank()) {
                            vm.launch {
                                val auto = if (settings.defaultSave == "Current project") repo.currentProject()?.id else null
                                repo.saveCapture(text, kind = kindEnum, projectId = linkedProjectId ?: auto)
                            }
                            text = ""
                        }
                    },
                    enabled = text.isNotBlank(),
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(28.dp, 8.dp, 8.dp, 28.dp),
                ) {
                    Text("Save & keep writing", style = MaterialTheme.typography.labelLarge)
                }
                
                Button(
                    onClick = {
                        vm.launchAndRun(
                            block = {
                                if (text.isBlank()) {
                                    "Nothing to save"
                                } else {
                                    val auto = if (settings.defaultSave == "Current project") repo.currentProject()?.id else null
                                    repo.saveCapture(text, kind = kindEnum, projectId = linkedProjectId ?: auto)
                                    null
                                }
                            },
                            andThen = { nav.resetTo(listOf(MindRoute(MindScreen.HOME)), MindTransition.SLIDE_DOWN) },
                        )
                    },
                    enabled = text.isNotBlank(),
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(8.dp, 28.dp, 28.dp, 8.dp),
                ) {
                    Text("Save to Inbox", style = MaterialTheme.typography.labelLarge)
                }
            }
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
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(44.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.FolderOpen, 
                    contentDescription = null, 
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(22.dp)
                )
            }
            
            Spacer(Modifier.width(16.dp))
            
            Column(Modifier.weight(1f)) {
                Text(
                    "Link to project (optional)", 
                    style = MaterialTheme.typography.labelMedium, 
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                val selectedProject = projects.firstOrNull { it.id == linkedProjectId }
                Text(
                    selectedProject?.name ?: "None selected",
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (selectedProject != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                )
            }
            
            IconButton(
                onClick = { projects.firstOrNull()?.let { onPick(it.id) } },
                modifier = Modifier.background(MaterialTheme.colorScheme.surface, CircleShape)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
        }
        
        if (projects.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                projects.forEach { p ->
                    PillChip(label = p.name, selected = linkedProjectId == p.id, onClick = { onPick(p.id) })
                }
            }
        }
    }
}

/**
 * Maps a stored default-capture-kind value (either a friendly label like
 * "To-do" or an enum name like "NOTE") to the friendly label the chip group
 * uses, so the selected chip always reflects the user's chosen default.
 */
private fun friendlyKindLabel(stored: String): String = when (stored.toCaptureKindOrNull()) {
    CaptureKind.TODO -> "To-do"
    CaptureKind.QUESTION -> "Question"
    CaptureKind.NOTE -> "Note"
    CaptureKind.IDEA -> "Idea"
    else -> when (stored) {
        "To-do" -> "To-do"
        "Question" -> "Question"
        "Note" -> "Note"
        "Idea" -> "Idea"
        else -> "Note"
    }
}
