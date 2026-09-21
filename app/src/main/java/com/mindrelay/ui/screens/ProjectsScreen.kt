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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.PauseCircle
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.mindrelay.data.model.ProjectStatus
import com.mindrelay.nav.MindNavController
import com.mindrelay.nav.MindScreen
import com.mindrelay.nav.MindTransition
import com.mindrelay.ui.AppViewModel
import com.mindrelay.ui.components.ConnectedChipGroup
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Expressive ambient icon container
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(140.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(40.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(120.dp)
            ) {}
            Surface(
                shape = RoundedCornerShape(32.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(80.dp)
            ) {}
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(40.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = headline,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = body,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.2f
        )
    }
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

    TabScaffold(
        nav = nav,
        selected = MindScreen.PROJECTS,
        topBar = {
            MindTopBar(
                title = "Projects",
                actions = listOf(
                    Icons.Rounded.Search to { nav.navigate(MindScreen.SEARCH, MindTransition.SLIDE_RIGHT) },
                ),
            )
        },
        fab = { QuickCaptureFab(nav) },
    ) { _ ->
        val filtered = when (filter) {
            "Paused" -> projects.filter { it.status == ProjectStatus.PAUSED }
            "Done" -> projects.filter { it.status == ProjectStatus.DONE }
            else -> projects.filter { it.status == ProjectStatus.ACTIVE }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
            // Fixed Top Filter Row - keeps chips visible while scrolling
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ConnectedChipGroup(
                    options = listOf("Active", "Paused", "Done"),
                    selected = filter,
                    onSelect = { filter = it },
                )
            }

            // Smooth animated content transition for the list below the chips
            AnimatedContent(
                targetState = filter,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(250, delayMillis = 50)) +
                        scaleIn(initialScale = 0.95f, animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f)))
                        .togetherWith(fadeOut(animationSpec = tween(150)))
                },
                label = "projects_tab_transition",
                modifier = Modifier.weight(1f)
            ) {
                // The filtered list already reflects the selected filter, so the
                // target-state parameter is deliberately not declared.
                if (filtered.isEmpty()) {
                    Spacer(modifier = Modifier.fillMaxSize())
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        // Spacious padding for the modern, expressive feel
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 120.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
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
                }
            }

            if (filtered.isEmpty()) {
                RootEmptyState {
                    EmptyProjectsArt(filter)
                }
            }
        }
    }
}
}
