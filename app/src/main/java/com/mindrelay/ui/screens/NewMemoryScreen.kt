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
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Notes
import androidx.compose.material.icons.rounded.Tag
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mindrelay.data.model.MemoryType
import com.mindrelay.nav.MindNavController
import com.mindrelay.nav.MindRoute
import com.mindrelay.nav.MindScreen
import com.mindrelay.nav.MindTransition
import com.mindrelay.ui.AppViewModel
import com.mindrelay.ui.components.MindDropdown
import com.mindrelay.ui.components.MindTextField
import com.mindrelay.ui.components.MindTopBar
import com.mindrelay.ui.components.PillButton
import com.mindrelay.ui.components.PillChip
import com.mindrelay.ui.components.SectionLabel
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
    // -------------------------------------------------------------------------
    // DATA AND STATE FLOW (UNTOUCHED)
    // -------------------------------------------------------------------------
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

    // Entrance animation state
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }
    // -------------------------------------------------------------------------

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .imePadding()
    ) {
        MindTopBar(
            title = if (editing) "Edit Memory" else "New Memory",
            onClose = { nav.pop() }
        )

        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(tween(250)) + slideInVertically(
                initialOffsetY = { 30 },
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 350f)
            ),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                // Header Hero Info
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    SectionLabel("Preserve a durable insight.", size = 24)
                    Text(
                        "Write it so Future You can find and use it without remembering this exact moment.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // Primary Identity Hero Card
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Bookmark,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Text(
                                text = "Core Details",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        MindTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = "Title",
                            leadingIcon = Icons.Rounded.Bookmark,
                            supportingText = "What will Future You search for?",
                            filled = true,
                        )

                        MindDropdown(
                            label = "Type",
                            options = TYPE_OPTIONS,
                            selected = type,
                            onSelect = { type = it },
                            leadingIcon = Icons.Rounded.Category,
                            filled = true
                        )
                    }
                }

                // Content & Tags Container
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Content & Metadata",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )

                        MindTextField(
                            value = content,
                            onValueChange = { content = it },
                            label = "Durable note",
                            leadingIcon = Icons.Rounded.Notes,
                            filled = true,
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
                            filled = true,
                        )
                    }
                }

                // Connections (Links & Revisit) Container
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        Text(
                            text = "Connections (Optional)",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )

                        ProjectPickRow(
                            projects = projects,
                            linkedProjectId = linkedProjectId,
                            onPick = { linkedProjectId = if (linkedProjectId == it) null else it },
                        )
                        
                        // Subtle Divider
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f))
                        )

                        RevisitRow(
                            revisitDay = revisitDay,
                            onPickDay = { day -> revisitDay = if (revisitDay == day) null else day },
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
            }
        }

        // Fixed Action Bar Bottom Area
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
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
                                listOf(MindRoute(MindScreen.MEMORY_DETAIL, targetId.toString())),
                                MindTransition.SLIDE_RIGHT,
                            )
                        },
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProjectPickRow(
    projects: List<com.mindrelay.data.db.ProjectEntity>,
    linkedProjectId: Long?,
    onPick: (Long) -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(44.dp)
                    .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.FolderOpen, 
                    contentDescription = null, 
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(22.dp)
                )
            }
            
            Spacer(Modifier.width(16.dp))
            
            Column(Modifier.weight(1f)) {
                Text(
                    "Linked project", 
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RevisitRow(
    revisitDay: Int?,
    onPickDay: (Int) -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(44.dp)
                    .background(MaterialTheme.colorScheme.tertiaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.EventRepeat, 
                    contentDescription = null, 
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(22.dp)
                )
            }
            
            Spacer(Modifier.width(16.dp))
            
            Column(Modifier.weight(1f)) {
                Text(
                    "Revisit date", 
                    style = MaterialTheme.typography.labelMedium, 
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    revisitDay?.let { epochDayToLocalDate(it).toString() } ?: "Not scheduled",
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (revisitDay != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        // Quick +1 / +7 / +30 day affordance.
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp), 
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Today", "+7 days", "+30 days").forEachIndexed { i, label ->
                val day = when (i) {
                    0 -> nowEpochDay()
                    1 -> nowEpochDay() + 7
                    else -> nowEpochDay() + 30
                }
                PillChip(label = label, selected = revisitDay == day, onClick = { onPickDay(day) })
            }
        }
    }
}
