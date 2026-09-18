package com.mindrelay.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mindrelay.data.model.MemoryType
import com.mindrelay.nav.MindNavController
import com.mindrelay.nav.MindRoute
import com.mindrelay.nav.MindScreen
import com.mindrelay.nav.MindTransition
import com.mindrelay.ui.AppViewModel
import com.mindrelay.ui.components.MindTopBar
import com.mindrelay.ui.components.PillChip
import com.mindrelay.ui.components.SectionLabel
import com.mindrelay.util.millisToDate

@Composable
private fun ExpressiveContextItem(
    headline: String,
    supporting: String,
    icon: ImageVector,
    onClick: (() -> Unit)?,
    iconContainerColor: Color = MaterialTheme.colorScheme.tertiaryContainer,
    iconContentColor: Color = MaterialTheme.colorScheme.onTertiaryContainer,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    // Smooth, tactile scaling animation - only scales if it's clickable
    val targetScale = if (onClick != null && isPressed) 0.96f else 1f
    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 600f),
        label = "context_item_scale"
    )

    Surface(
        onClick = onClick ?: {},
        enabled = onClick != null,
        shape = RoundedCornerShape(20.dp),
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
                    maxLines = 1,
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
            
            if (onClick != null) {
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)
@Composable
fun MemoryDetailScreen(vm: AppViewModel, nav: MindNavController, memoryId: Long?) {
    // -------------------------------------------------------------------------
    // DATA AND STATE FLOW (UNTOUCHED)
    // -------------------------------------------------------------------------
    val repo = vm.repository
    if (memoryId == null) {
        LaunchedEffect(Unit) { nav.pop() }
        return
    }
    val memoryFlow = repo.memories.byId(memoryId).collectAsStateWithLifecycle(initialValue = null)
    val memory = memoryFlow.value
    val projects by repo.projects.all().collectAsStateWithLifecycle(initialValue = emptyList())
    var menuOpen by remember { mutableStateOf(false) }

    // Entrance animation state
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }

    if (memory == null) {
        Text("This memory is no longer available.", modifier = Modifier.padding(16.dp))
        return
    }

    val relatedProject = memory.projectId?.let { pid -> projects.firstOrNull { it.id == pid } }
    val sourceSession by repo.sessions.byId(memory.sourceSessionId ?: -1L)
        .collectAsStateWithLifecycle(initialValue = null)
    val tags = memory.tags.split(" ").filter { it.startsWith("#") }
    // -------------------------------------------------------------------------

    Column(Modifier.fillMaxSize()) {
        MindTopBar(
            title = "Memory",
            onBack = { nav.popToRoot() },
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
                    vm.launchAndRun(
                        block = {
                            repo.archiveMemory(memory.id)
                            null
                        },
                        andThen = { nav.resetTo(listOf(MindRoute(MindScreen.MEMORIES)), MindTransition.SLIDE_DOWN) },
                    )
                },
            )
            DropdownMenuItem(
                text = { Text("Delete memory", color = MaterialTheme.colorScheme.error) },
                onClick = {
                    menuOpen = false
                    vm.launchAndRun(
                        block = {
                            repo.deleteMemory(memory.id)
                            null
                        },
                        andThen = { nav.resetTo(listOf(MindRoute(MindScreen.MEMORIES)), MindTransition.SLIDE_DOWN) },
                    )
                },
            )
        }

        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(tween(300)) + slideInVertically(
                initialOffsetY = { 60 },
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f)
            ),
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                
                // Tags and Type Flow Row
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    PillChip(label = memoryLabel(memory.type), selected = true, onClick = {})
                    tags.forEach { tag ->
                        PillChip(label = tag, selected = false, onClick = {})
                    }
                }

                // Unified Hero Memory Card
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = memory.title,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                lineHeight = 32.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                        )

                        if (memory.content.isNotBlank()) {
                            Text(
                                text = memory.content,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.3f
                            )
                        }
                    }
                }

                // Linked Details Section
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionLabel("Linked details")
                    
                    ExpressiveContextItem(
                        headline = "Related project",
                        supporting = relatedProject?.name ?: "None linked",
                        icon = Icons.Rounded.FolderOpen,
                        iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        iconContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        onClick = relatedProject?.let { p ->
                            { nav.navigate(MindScreen.PROJECT_DETAIL, MindTransition.SLIDE_RIGHT, p.id.toString()) }
                        }
                    )
                    
                    ExpressiveContextItem(
                        headline = "Source session",
                        supporting = sourceSession?.title ?: "None linked",
                        icon = Icons.Rounded.Schedule,
                        iconContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        iconContentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        onClick = sourceSession?.let { s ->
                            { nav.navigate(MindScreen.SESSION, MindTransition.SLIDE_RIGHT, s.id.toString()) }
                        }
                    )
                    
                    ExpressiveContextItem(
                        headline = "Revisit",
                        supporting = if (memory.revisitAt != null) millisToDate(memory.revisitAt!!).toString() else "Not scheduled",
                        icon = Icons.Rounded.EventRepeat,
                        iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        iconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        onClick = null // No edit flow built for this directly yet
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Archive Action Button with Tactile Spring
                val archiveInteractionSource = remember { MutableInteractionSource() }
                val isArchivePressed by archiveInteractionSource.collectIsPressedAsState()
                val archiveScale by animateFloatAsState(
                    targetValue = if (isArchivePressed) 0.96f else 1f,
                    animationSpec = spring(dampingRatio = 0.6f, stiffness = 600f),
                    label = "archive_button_scale"
                )

                OutlinedButton(
                    onClick = {
                        vm.launchAndRun(
                            block = {
                                repo.archiveMemory(memory.id)
                                null
                            },
                            andThen = { nav.resetTo(listOf(MindRoute(MindScreen.MEMORIES)), MindTransition.SLIDE_DOWN) },
                        )
                    },
                    interactionSource = archiveInteractionSource,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .graphicsLayer {
                            scaleX = archiveScale
                            scaleY = archiveScale
                        },
                    shape = RoundedCornerShape(28.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                ) {
                    Icon(Icons.Rounded.Archive, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Archive",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

private fun memoryLabel(type: MemoryType): String = when (type) {
    MemoryType.FIX -> "Fix"
    MemoryType.PERSON -> "Person"
    MemoryType.IDEA -> "Idea"
    MemoryType.PLACE -> "Place"
    MemoryType.RECIPE -> "Recipe"
    MemoryType.NOTE -> "Note"
    MemoryType.OTHER -> "Other"
}
