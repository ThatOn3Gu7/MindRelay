package com.mindrelay.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mindrelay.data.model.ProjectStatus
import com.mindrelay.nav.MindNavController
import com.mindrelay.nav.MindScreen
import com.mindrelay.nav.MindTransition
import com.mindrelay.ui.AppViewModel
import com.mindrelay.ui.components.BodyListItem
import com.mindrelay.ui.components.ConnectedChipGroup
import com.mindrelay.ui.components.ExpressiveCard
import com.mindrelay.util.workedAgo

private fun statusLabel(s: ProjectStatus) = when (s) {
    ProjectStatus.ACTIVE -> "Active"
    ProjectStatus.PAUSED -> "Paused"
    ProjectStatus.DONE -> "Done"
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

    val filtered = when (filter) {
        "Paused" -> projects.filter { it.status == ProjectStatus.PAUSED }
        "Done" -> projects.filter { it.status == ProjectStatus.DONE }
        else -> projects.filter { it.status == ProjectStatus.ACTIVE }
    }

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
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                ConnectedChipGroup(
                    options = listOf("Active", "Paused", "Done"),
                    selected = filter,
                    onSelect = { filter = it },
                )
            }
            if (filtered.isEmpty()) {
                item {
                    Text(
                        if (filter == "Active") "No active projects. Create one to start a session."
                        else "No ${filter.lowercase()} projects.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                }
            } else {
                items(filtered, key = { it.id }) { p ->
                    val count = sessionCounts[p.id] ?: 0
                    val subtitle = buildString {
                        append(statusLabel(p.status))
                        append(" · ")
                        append("$count ${if (count == 1) "session" else "sessions"}")
                        if (p.currentState.isNotBlank()) {
                            append(" · ")
                            append(p.currentState.substringBefore('\n'))
                        }
                        append(" · last worked ")
                        append(workedAgo(p.lastWorkedAt))
                        if (p.nextAction.isNotBlank()) {
                            append(" · next: ")
                            append(p.nextAction.substringBefore('\n'))
                        }
                    }
                    ExpressiveCard(
                        headline = p.name,
                        body = subtitle,
                        onClick = { nav.navigate(MindScreen.PROJECT_DETAIL, MindTransition.SLIDE_RIGHT, p.id.toString()) },
                        minHeight = 110,
                        container = MaterialTheme.colorScheme.surfaceContainerLow,
                        content = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}
