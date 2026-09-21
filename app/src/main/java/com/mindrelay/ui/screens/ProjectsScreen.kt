package com.mindrelay.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.PauseCircle
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.mindrelay.data.model.ProjectStatus
import com.mindrelay.nav.MindNavController
import com.mindrelay.nav.MindScreen
import com.mindrelay.nav.MindTransition
import com.mindrelay.ui.AppViewModel
import com.mindrelay.ui.components.ConnectedChipGroup
import com.mindrelay.ui.search.MindSearchField
import com.mindrelay.ui.search.MindSearchResults
import com.mindrelay.ui.search.rememberMindSearchState
import com.mindrelay.util.workedAgo

private fun statusLabel(s: ProjectStatus) = when (s) {
    ProjectStatus.ACTIVE -> "Active"
    ProjectStatus.PAUSED -> "Paused"
    ProjectStatus.DONE -> "Done"
}

@Composable
private fun EmptyProjectsArt(filter: String) {
    val icon = when (filter) {
        "Paused" -> Icons.Rounded.PauseCircle
        "Done" -> Icons.Rounded.CheckCircle
        else -> Icons.Rounded.FolderOpen
    }
    val headline = when (filter) {
        "Paused" -> "Nothing on hold"
        "Done" -> "No finished projects yet"
        else -> "Ready for a new venture"
    }
    val body = when (filter) {
        "Paused" -> "Projects that need a break will safely wait for you here."
        "Done" -> "Your completed missions and accomplishments will be logged here."
        else -> "Create an active project to track sessions, state, and next actions."
    }
    RootEmptyArt(
        icon = icon,
        headline = headline,
        body = body,
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        onContainerColor = MaterialTheme.colorScheme.onSecondaryContainer,
    )
}

@Composable
private fun ExpressiveProjectCard(
    headline: String,
    status: ProjectStatus,
    sessionCount: Int,
    lastWorked: String,
    currentState: String,
    nextAction: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    // Tactile spring scale animation
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 600f),
        label = "project_card_scale"
    )

    val badgeContainerColor = when (status) {
        ProjectStatus.ACTIVE -> MaterialTheme.colorScheme.primaryContainer
        ProjectStatus.PAUSED -> MaterialTheme.colorScheme.surfaceVariant
        ProjectStatus.DONE -> MaterialTheme.colorScheme.secondaryContainer
    }
    val badgeContentColor = when (status) {
        ProjectStatus.ACTIVE -> MaterialTheme.colorScheme.onPrimaryContainer
        ProjectStatus.PAUSED -> MaterialTheme.colorScheme.onSurfaceVariant
        ProjectStatus.DONE -> MaterialTheme.colorScheme.onSecondaryContainer
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        interactionSource = interactionSource,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Title and Status Badge
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = headline,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(12.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = badgeContainerColor,
                    contentColor = badgeContentColor
                ) {
                    Text(
                        text = statusLabel(status),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            // Body: State & Next Action
            if (currentState.isNotBlank() || nextAction.isNotBlank()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (currentState.isNotBlank()) {
                        Text(
                            text = currentState.substringBefore('\n'),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (nextAction.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = "Next Action",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = nextAction.substringBefore('\n'),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Footer: Sessions & Time
            Text(
                text = "$sessionCount ${if (sessionCount == 1) "session" else "sessions"} · last worked $lastWorked",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ProjectsScreen(vm: AppViewModel, nav: MindNavController) {
    val repo = vm.repository
    val projects by repo.projects.all().collectAsStateWithLifecycle(initialValue = emptyList())
    val sessions by repo.sessions.all().collectAsStateWithLifecycle(initialValue = emptyList())
    
    val sessionCounts = remember(sessions) {
        sessions.groupingBy { it.projectId }.eachCount()
    }
    
    var filter by remember { mutableStateOf("Active") }
    val search = rememberMindSearchState()

    TabScaffold(
        nav = nav,
        selected = MindScreen.PROJECTS,
        topBar = {
            MindTopBar(
                title = "Projects",
            )
        },
        fab = { QuickCaptureFab(nav) },
    ) { _ ->
        val q = search.query.trim()
        val filtered = projects
            .filter { p ->
                when (filter) {
                    "Paused" -> p.status == ProjectStatus.PAUSED
                    "Done" -> p.status == ProjectStatus.DONE
                    else -> p.status == ProjectStatus.ACTIVE
                }
            }
            .filter { p ->
                q.isEmpty() || "${p.name} ${p.currentState} ${p.nextAction}".contains(q, ignoreCase = true)
            }

        // The project list, shared by the collapsed screen and the expanded search.
        val projectRows: LazyListScope.() -> Unit = {
            items(filtered, key = { it.id }) { p ->
                val count = sessionCounts[p.id] ?: 0
                ExpressiveProjectCard(
                    headline = p.name,
                    status = p.status,
                    sessionCount = count,
                    lastWorked = workedAgo(p.lastWorkedAt),
                    currentState = p.currentState,
                    nextAction = p.nextAction,
                    onClick = {
                        nav.navigate(MindScreen.PROJECT_DETAIL, MindTransition.SLIDE_RIGHT, p.id.toString())
                    }
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                MindSearchField(
                    state = search,
                    placeholder = "Search projects",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )

                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    // Smooth animated content transition for the list below the field
                    AnimatedContent(
                        targetState = filter,
                        transitionSpec = {
                            (fadeIn(animationSpec = tween(250, delayMillis = 50)) +
                                scaleIn(initialScale = 0.95f, animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f)))
                                .togetherWith(fadeOut(animationSpec = tween(150)))
                        },
                        label = "projects_tab_transition",
                        modifier = Modifier.fillMaxSize()
                    ) { targetFilter ->
                        // Key the content on the target filter so each filter's list has
                        // its own identity for AnimatedContent, and the target-state
                        // parameter is actually used by the transition.
                        key(targetFilter) {
                            // The filtered list already reflects the selected filter.
                            if (filtered.isEmpty()) {
                                Spacer(modifier = Modifier.fillMaxSize())
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    // Spacious padding for the modern, expressive feel
                                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 120.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                ) {
                                    projectRows()
                                }
                            }
                        }
                    }

                    // Expanded search covers the list instead of pushing it.
                    MindSearchResults(expanded = search.expanded) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 20.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                ConnectedChipGroup(
                                    options = listOf("Active", "Paused", "Done"),
                                    selected = filter,
                                    onSelect = { filter = it },
                                )
                            }
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 120.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                            ) {
                                projectRows()
                            }
                        }
                    }
                }
            }

            if (filtered.isEmpty() && !search.expanded) {
                RootEmptyState(stateKey = filter) {
                    EmptyProjectsArt(filter)
                }
            }
        }
    }
}
