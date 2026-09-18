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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Notes
import androidx.compose.material.icons.rounded.Tag
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mindrelay.data.model.MemoryType
import com.mindrelay.nav.MindNavController
import com.mindrelay.nav.MindScreen
import com.mindrelay.nav.MindTransition
import com.mindrelay.ui.AppViewModel
import com.mindrelay.ui.components.MindDropdown
import com.mindrelay.ui.components.MindTextField
import com.mindrelay.ui.components.PillButton
import com.mindrelay.ui.components.PillChip
import com.mindrelay.util.epochDayToLocalDate
import com.mindrelay.util.nowEpochDay

private val TYPE_OPTIONS = listOf("Fix", "Person", "Idea", "Place", "Recipe", "Note", "Other")

private fun optionToType(option: String): MemoryType = when (option) {
    "Fix" -> MemoryType.FIX
    "Person" -> MemoryType.PERSON
    "Idea" -> MemoryType.IDEA
    "Place" -> MemoryType.PLACE
    "Recipe" -> MemoryType.RECIPE
    "Note" -> MemoryType.NOTE
    else -> MemoryType.OTHER
}

private fun typeToOption(type: MemoryType): String = when (type) {
    MemoryType.FIX -> "Fix"
    MemoryType.PERSON -> "Person"
    MemoryType.IDEA -> "Idea"
    MemoryType.PLACE -> "Place"
    MemoryType.RECIPE -> "Recipe"
    MemoryType.NOTE -> "Note"
    else -> "Other"
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)
@Composable
fun NewMemoryScreen(vm: AppViewModel, nav: MindNavController, memoryId: Long? = null) {
    val repo = vm.repository
    val projects by repo.projects.all().collectAsStateWithLifecycle(initialValue = emptyList())

    var title by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("Fix") }
    var content by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf("") }
    var linkedProjectId by remember { mutableStateOf<Long?>(null) }
    var revisitDay by remember { mutableStateOf<Int?>(null) }

    val editing = memoryId != null
    val existing = if (editing) {
        repo.memories.byId(memoryId!!).collectAsStateWithLifecycle(initialValue = null).value
    } else null

    // Prefill once when editing an existing memory.
    LaunchedEffect(existing) {
        val m = existing ?: return@LaunchedEffect
        title = m.title
        type = typeToOption(m.type)
        content = m.content
        tags = m.tags.trim().split(" ").filter { it.startsWith("#") }.joinToString(" ") { it.removePrefix("#") }
        linkedProjectId = m.projectId
        revisitDay = m.revisitAt?.let { com.mindrelay.util.millisToEpochDay(it) }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .imePadding()
    ) {
        MindTopBar(title = if (editing) "Edit Memory" else "New Memory", onClose = { nav.pop() })
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MindTextField(
                value = title,
                onValueChange = { title = it },
                label = "Title",
                leadingIcon = Icons.Rounded.Bookmark,
                supportingText = "What will Future You search for?",
                filled = false,
            )
            MindDropdown(
                label = "Type",
                options = TYPE_OPTIONS,
                selected = type,
                onSelect = { type = it },
                leadingIcon = Icons.Rounded.Category,
            )
            MindTextField(
                value = content,
                onValueChange = { content = it },
                label = "Durable note",
                leadingIcon = Icons.Rounded.Notes,
                supportingText = "Write it so Future You can use it without remembering this moment.",
                filled = false,
                singleLine = false,
                minLines = 4,
                maxLines = 10,
            )
            MindTextField(
                value = tags,
                onValueChange = { tags = it },
                label = "Tags",
                leadingIcon = Icons.Rounded.Tag,
                supportingText = "#tags separated by spaces",
                filled = false,
            )

            ProjectPickRow(
                projects = projects,
                linkedProjectId = linkedProjectId,
                onPick = { linkedProjectId = if (linkedProjectId == it) null else it },
            )
            RevisitRow(
                revisitDay = revisitDay,
                onPickDay = { day -> revisitDay = if (revisitDay == day) null else day },
            )

            Spacer(Modifier.padding(top = 4.dp))
            PillButton(
                text = if (editing) "Save Changes" else "Save Memory",
                icon = Icons.Rounded.Check,
                enabled = title.isNotBlank(),
                onClick = {
                    val original = existing
                    val revisitMillis = revisitDay?.let { com.mindrelay.util.epochDayToMillis(it) }
                    var targetId: Long? = null
                    vm.launchAndRun(
                        block = {
                            if (editing && original != null) {
                                repo.updateMemory(
                                    original.copy(
                                        title = title.trim(),
                                        content = content.trim(),
                                        type = optionToType(type),
                                        tags = tags,
                                        projectId = linkedProjectId,
                                        revisitAt = revisitMillis,
                                    )
                                )
                                targetId = original.id
                            } else {
                                targetId = repo.saveMemory(
                                    title = title,
                                    content = content,
                                    type = optionToType(type),
                                    tags = tags,
                                    projectId = linkedProjectId,
                                    revisitAt = revisitMillis,
                                )
                            }
                            null
                        },
                        andThen = {
                            nav.resetTo(
                                listOf(com.mindrelay.nav.MindRoute(MindScreen.MEMORY_DETAIL, targetId.toString())),
                                MindTransition.SLIDE_RIGHT,
                            )
                        },
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.padding(bottom = 16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)
@Composable
private fun ProjectPickRow(
    projects: List<com.mindrelay.data.db.ProjectEntity>,
    linkedProjectId: Long?,
    onPick: (Long) -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.background(MaterialTheme.colorScheme.primaryContainer, CircleShape).padding(10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.FolderOpen, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text("Linked project (optional)", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(
                projects.firstOrNull { it.id == linkedProjectId }?.name ?: "None",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    if (projects.isNotEmpty()) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            projects.forEach { p ->
                PillChip(p.name, selected = linkedProjectId == p.id, onClick = { onPick(p.id) })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)
@Composable
private fun RevisitRow(
    revisitDay: Int?,
    onPickDay: (Int) -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.background(MaterialTheme.colorScheme.primaryContainer, CircleShape).padding(10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.EventRepeat, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text("Revisit date (optional)", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(
                revisitDay?.let { epochDayToLocalDate(it).toString() } ?: "Not set",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    // Quick +1 / +7 / +30 day affordance.
    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        listOf("Today", "+7 days", "+30 days").forEachIndexed { i, label ->
            val day = when (i) {
                0 -> nowEpochDay()
                1 -> nowEpochDay() + 7
                else -> nowEpochDay() + 30
            }
            PillChip(label, selected = revisitDay == day, onClick = { onPickDay(day) })
        }
    }
}
