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
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mindrelay.nav.MindNavController
import com.mindrelay.nav.MindScreen
import com.mindrelay.nav.MindTransition
import com.mindrelay.ui.AppViewModel
import com.mindrelay.ui.components.BodyListItem
import com.mindrelay.ui.components.ExpressiveCard
import com.mindrelay.ui.components.PillButton
import com.mindrelay.ui.components.PillChip

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MemoryDetailScreen(vm: AppViewModel, nav: MindNavController, memoryId: Long?) {
    val repo = vm.repository
    if (memoryId == null) {
        androidx.compose.runtime.LaunchedEffect(Unit) { nav.pop() }
        return
    }
    val memoryFlow = repo.memories.byId(memoryId).collectAsStateWithLifecycle(initialValue = null)
    val memory = memoryFlow.value
    val projects by repo.projects.all().collectAsStateWithLifecycle(initialValue = emptyList())
    var menuOpen by remember { mutableStateOf(false) }

    if (memory == null) {
        Text("This memory is no longer available.", modifier = Modifier.padding(16.dp))
        return
    }

    val relatedProject = memory.projectId?.let { pid -> projects.firstOrNull { it.id == pid } }
    val projectSessions by repo.sessions.byProject(memory.projectId ?: -1L)
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val sourceSession = memory.sourceSessionId?.let { sid -> projectSessions.firstOrNull { it.id == sid } }
    val tags = memory.tags.split(" ").filter { it.startsWith("#") }

    Column(Modifier.fillMaxSize()) {
        MindTopBar(
            title = "Memory",
            onBack = { nav.navigate(MindScreen.MEMORIES, MindTransition.SLIDE_RIGHT) },
            actions = listOf(
                Icons.Rounded.Edit to { nav.navigate(MindScreen.NEW_MEMORY, MindTransition.SLIDE_UP, memory.id.toString()) },
                Icons.Rounded.MoreVert to { menuOpen = true },
            ),
        )
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text("Archive", color = MaterialTheme.colorScheme.onSurface) },
                onClick = {
                    menuOpen = false
                    vm.launch { repo.archiveMemory(memory.id) }
                    nav.navigate(MindScreen.MEMORIES, MindTransition.SLIDE_RIGHT)
                },
            )
            DropdownMenuItem(
                text = { Text("Delete memory", color = MaterialTheme.colorScheme.error) },
                onClick = {
                    menuOpen = false
                    vm.launch { repo.deleteMemory(memory.id) }
                    nav.navigate(MindScreen.MEMORIES, MindTransition.SLIDE_RIGHT)
                },
            )
        }

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Type + tag chips.
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                PillChip(label = memoryLabel(memory.type), selected = true, onClick = {})
                tags.forEach { tag ->
                    PillChip(label = tag, selected = false, onClick = {})
                }
            }

            Text(
                memory.title,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )

            if (memory.content.isNotBlank()) {
                ExpressiveCard(
                    headline = "",
                    body = memory.content,
                    container = MaterialTheme.colorScheme.surfaceContainerHighest,
                    content = MaterialTheme.colorScheme.onSurface,
                    minHeight = 200,
                )
            }

            BodyListItem(
                headline = "Related project",
                supporting = relatedProject?.name ?: "None",
                icon = Icons.Rounded.FolderOpen,
                onClick = relatedProject?.let { p ->
                    { nav.navigate(MindScreen.PROJECT_DETAIL, MindTransition.SLIDE_RIGHT, p.id.toString()) }
                },
                trailing = { Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
            )
            BodyListItem(
                headline = "Source session",
                supporting = sourceSession?.title ?: "None",
                icon = Icons.Rounded.Schedule,
                onClick = sourceSession?.let { s ->
                    { nav.navigate(MindScreen.SESSION, MindTransition.SLIDE_RIGHT, s.id.toString()) }
                },
                trailing = { Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
            )
            BodyListItem(
                headline = "Revisit",
                supporting = if (memory.revisitAt != null) com.mindrelay.util.millisToDate(memory.revisitAt!!).toString() else "Not scheduled",
                icon = Icons.Rounded.EventRepeat,
                trailing = { Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
            )

            androidx.compose.material3.OutlinedButton(
                onClick = {
                    vm.launch { repo.archiveMemory(memory.id) }
                    nav.navigate(MindScreen.MEMORIES, MindTransition.SLIDE_RIGHT)
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

private fun memoryLabel(type: com.mindrelay.data.model.MemoryType): String = when (type) {
    com.mindrelay.data.model.MemoryType.FIX -> "Fix"
    com.mindrelay.data.model.MemoryType.PERSON -> "Person"
    com.mindrelay.data.model.MemoryType.IDEA -> "Idea"
    com.mindrelay.data.model.MemoryType.PLACE -> "Place"
    com.mindrelay.data.model.MemoryType.RECIPE -> "Recipe"
    com.mindrelay.data.model.MemoryType.NOTE -> "Note"
    com.mindrelay.data.model.MemoryType.OTHER -> "Other"
}
