package com.mindrelay.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PauseCircle
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mindrelay.data.model.ProjectStatus
import com.mindrelay.nav.MindNavController
import com.mindrelay.nav.MindRoute
import com.mindrelay.nav.MindScreen
import com.mindrelay.nav.MindTransition
import com.mindrelay.ui.AppViewModel
import com.mindrelay.ui.components.PillButton
import com.mindrelay.ui.components.SectionLabel
import com.mindrelay.ui.components.TonalPillButton
import com.mindrelay.util.durationText
import com.mindrelay.util.relativeAgo

@Composable
private fun ExpressiveDetailCard(
    headline: String,
    body: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    iconContainerColor: Color,
    iconContentColor: Color,
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = containerColor,
        contentColor = contentColor,
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
                    color = iconContainerColor,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconContentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Text(
                    text = headline,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Text(
                text = body,
                style = MaterialTheme.typography.bodyLarge,
                lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.25f,
            )
        }
    }
}

@Composable
private fun ExpressiveProjectListItem(
    headline: String,
    supporting: String,
    icon: ImageVector,
    onClick: () -> Unit,
    iconContainerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    iconContentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val targetScale = if (isPressed) 0.97f else 1f
    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 500f),
        label = "list_item_scale"
    )

    Surface(
        onClick = onClick,
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
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconContentColor,
                        modifier = Modifier.size(20.dp)
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

            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ProjectDetailScreen(vm: AppViewModel, nav: MindNavController, projectId: Long?) {
    // -------------------------------------------------------------------------
    // DATA AND STATE FLOW (UNTOUCHED)
    // -------------------------------------------------------------------------
    val repo = vm.repository
    if (projectId == null) {
        LaunchedEffect(Unit) { nav.pop() }
        return
    }
    val projectFlow = repo.projects.byId(projectId).collectAsStateWithLifecycle(initialValue = null)
    val project = projectFlow.value
    val sessions by repo.sessions.byProject(projectId).collectAsStateWithLifecycle(initialValue = emptyList())
    val linked by repo.memories.byProject(projectId).collectAsStateWithLifecycle(initialValue = emptyList())
    var menuOpen by remember { mutableStateOf(false) }
    var statusMenu by remember { mutableStateOf(false) }

    // Entrance animation state
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }

    if (project == null) {
        Text("This project is no longer available.", modifier = Modifier.padding(16.dp))
        return
    }
    // -------------------------------------------------------------------------

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
                leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                onClick = {
                    menuOpen = false
                    nav.navigate(MindScreen.NEW_PROJECT, MindTransition.SLIDE_UP, project.id.toString())
                },
            )
            DropdownMenuItem(
                text = { Text("Delete project", color = MaterialTheme.colorScheme.error) },
                leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                onClick = {
                    menuOpen = false
                    vm.launchAndRun(
                        block = {
                            repo.deleteProject(project.id)
                            null
                        },
                        andThen = { nav.resetTo(listOf(MindRoute(MindScreen.PROJECTS)), MindTransition.SLIDE_DOWN) },
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

        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(tween(250)) + slideInVertically(
                initialOffsetY = { 40 },
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 350f)
            ),
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Header Status Pill
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatusChip(status = project.status)
                    Text(
                        text = "Project Overview",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Next Action Hero Card
                ExpressiveDetailCard(
                    headline = "Next Action",
                    body = project.nextAction.ifBlank { "No next action yet — set one when you resume." },
                    icon = Icons.Rounded.PlayArrow,
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    iconContainerColor = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.12f),
                    iconContentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                )

                // Current State Card (if present)
                if (project.currentState.isNotBlank()) {
                    ExpressiveDetailCard(
                        headline = "Current State",
                        body = project.currentState,
                        icon = Icons.Rounded.Info,
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        iconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }

                // Goal Card (if present)
                if (project.goal.isNotBlank()) {
                    ExpressiveDetailCard(
                        headline = "Goal / Outcome",
                        body = project.goal,
                        icon = Icons.Rounded.Flag,
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        iconContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }

                // Primary Action Bar Buttons
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PillButton(
                        text = if (project.status == ProjectStatus.DONE) "Reactivate" else "Resume session",
                        icon = if (project.status == ProjectStatus.DONE) Icons.Rounded.Refresh else Icons.Rounded.PlayArrow,
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
                                        Unit
                                    } else if (resultingSessionId != null) {
                                        nav.navigate(MindScreen.SESSION, MindTransition.SLIDE_RIGHT, resultingSessionId.toString())
                                    }
                                },
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                    )

                    TonalPillButton(
                        text = "Add note",
                        icon = Icons.Rounded.Add,
                        onClick = { nav.navigate(MindScreen.QUICK_CAPTURE, MindTransition.EXPAND, project.id.toString()) },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                    )
                }

                Spacer(Modifier.height(4.dp))

                // Recent Sessions Section
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionLabel("Recent sessions")
                    if (sessions.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "No sessions yet. Resume to start one.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    } else {
                        sessions.forEach { s ->
                            ExpressiveProjectListItem(
                                headline = s.title,
                                supporting = "${relativeAgo(s.startedAt)} · ${durationText(s.startedAt, s.endedAt)}",
                                icon = Icons.Rounded.Schedule,
                                iconContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                iconContentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                onClick = { nav.navigate(MindScreen.SESSION, MindTransition.SLIDE_RIGHT, s.id.toString()) },
                            )
                        }
                    }
                }

                // Linked Memories Section
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionLabel("Linked memories", size = 18)
                    if (linked.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "None linked yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    } else {
                        linked.forEach { m ->
                            ExpressiveProjectListItem(
                                headline = m.title,
                                supporting = "Linked memory",
                                icon = Icons.Rounded.Bookmark,
                                iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                iconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                onClick = { nav.navigate(MindScreen.MEMORY_DETAIL, MindTransition.SLIDE_RIGHT, m.id.toString()) },
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

private fun statusLabel(s: ProjectStatus) = when (s) {
    ProjectStatus.ACTIVE -> "Active"
    ProjectStatus.PAUSED -> "Paused"
    ProjectStatus.DONE -> "Done"
}

@Composable
private fun StatusChip(status: ProjectStatus) {
    val (icon, containerColor, contentColor) = when (status) {
        ProjectStatus.ACTIVE -> Triple(
            Icons.Rounded.PlayArrow,
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer
        )
        ProjectStatus.PAUSED -> Triple(
            Icons.Rounded.PauseCircle,
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer
        )
        ProjectStatus.DONE -> Triple(
            Icons.Rounded.CheckCircle,
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer
        )
    }

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
                text = statusLabel(status),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
