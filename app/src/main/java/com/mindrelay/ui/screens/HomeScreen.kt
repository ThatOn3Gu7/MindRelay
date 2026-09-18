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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mindrelay.data.model.ProjectStatus
import com.mindrelay.data.model.SessionStatus
import com.mindrelay.nav.MindNavController
import com.mindrelay.nav.MindScreen
import com.mindrelay.nav.MindTransition
import com.mindrelay.ui.AppViewModel
import com.mindrelay.ui.components.BodyListItem
import com.mindrelay.ui.components.ExpressiveCard
import com.mindrelay.ui.components.SectionLabel
import com.mindrelay.util.relativeAgo

private data class HomeNextAction(
    val key: String,
    val text: String,
    val supporting: String,
    val isTask: Boolean,
    val taskId: Long?,
    val projectId: Long?,
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeScreen(vm: AppViewModel, nav: MindNavController) {
    val repo = vm.repository
    val captures by repo.captures.active().collectAsStateWithLifecycle(initialValue = emptyList())
    val projects by repo.projects.all().collectAsStateWithLifecycle(initialValue = emptyList())
    val tasks by repo.tasks.open().collectAsStateWithLifecycle(initialValue = emptyList())
    val sessions by repo.sessions.all().collectAsStateWithLifecycle(initialValue = emptyList())
    // Due-revisit memories come straight from the DAO: only rows with a
    // revisitAt that is set and not in the future are "due", so future revisits
    // stay hidden until their date.
    val memories by repo.memories.dueNow(System.currentTimeMillis(), limit = 25)
        .collectAsStateWithLifecycle(initialValue = emptyList())

    val inboxCount = captures.size
    val continueProject = projects.firstOrNull { it.status == ProjectStatus.ACTIVE }
    val continueSession = continueProject?.let { p ->
        sessions.filter { it.projectId == p.id && it.status == SessionStatus.ACTIVE }
            .maxByOrNull { it.startedAt }
            ?: sessions.filter { it.projectId == p.id }.maxByOrNull { it.startedAt }
    }

    val projectById = projects.associateBy { it.id }
    val nextActions = mutableListOf<HomeNextAction>()
    tasks.forEach { t ->
        nextActions.add(
            HomeNextAction(
                key = "t${t.id}",
                text = t.text,
                supporting = projectById[t.projectId]?.name ?: t.tag.ifBlank { "Task" },
                isTask = true, taskId = t.id, projectId = t.projectId,
            )
        )
    }
    projects.filter { it.status == ProjectStatus.ACTIVE && it.nextAction.isNotBlank() }
        .forEach { p ->
            if (nextActions.none { it.text == p.nextAction }) {
                nextActions.add(
                    HomeNextAction(
                        key = "p${p.id}",
                        text = p.nextAction,
                        supporting = p.name,
                        isTask = false, taskId = null, projectId = p.id,
                    )
                )
            }
        }
    val nextActionsLimited = nextActions.take(6)

    TabScaffold(
        nav = nav,
        selected = MindScreen.HOME,
        topBar = {
            MindTopBar(
                title = "MindRelay",
                actions = listOf(Icons.Rounded.Settings to { nav.navigate(MindScreen.SETTINGS, MindTransition.SLIDE_RIGHT) }),
            )
        },
        fab = { QuickCaptureFab(nav) },
    ) { _ ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                // Continue card
                if (continueProject != null) {
                    val sessionLine = continueSession?.let {
                        "${it.title.substringBefore("·").trim()} · ${relativeAgo(it.startedAt)}"
                    } ?: "No session yet"
                    val body = buildString {
                        append(continueProject.name)
                        append(" · ")
                        append(sessionLine)
                        if (continueProject.currentState.isNotBlank()) {
                            append("\n\n")
                            append(continueProject.currentState)
                        }
                        if (continueProject.nextAction.isNotBlank()) {
                            append("\n\nNext: ")
                            append(continueProject.nextAction)
                        }
                    }
                    ExpressiveCard(
                        headline = "Continue",
                        body = body,
                        onClick = { nav.navigate(MindScreen.PROJECT_DETAIL, MindTransition.SLIDE_RIGHT, continueProject.id.toString()) },
                        minHeight = 180,
                    )
                } else {
                    ExpressiveCard(
                        headline = "Continue",
                        body = "Create your first project to keep a living record of where you are and what to do next.",
                        onClick = { nav.navigate(MindScreen.NEW_PROJECT, MindTransition.SLIDE_UP) },
                        minHeight = 180,
                    )
                }
            }

            item { Spacer(Modifier.height(2.dp)) }
            item { SectionLabel("Next actions") }
            if (nextActionsLimited.isEmpty()) {
                item {
                    androidx.compose.material3.Text(
                        "Nothing pending. Capture a thought or set a project's next action.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(nextActionsLimited, key = { it.key }) { na ->
                    BodyListItem(
                        headline = na.text,
                        supporting = na.supporting,
                        icon = Icons.Rounded.CheckCircle,
                        onClick = {
                            if (na.isTask && na.taskId != null) {
                                vm.launch { repo.setTaskDone(na.taskId, true) }
                            } else if (na.projectId != null) {
                                nav.navigate(MindScreen.PROJECT_DETAIL, MindTransition.SLIDE_RIGHT, na.projectId.toString())
                            }
                        },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                }
            }

            item {
                // Inbox card (80dp)
                ExpressiveCard(
                    headline = "Inbox",
                    body = if (inboxCount == 0) "Nothing waiting to be sorted" else
                        "$inboxCount ${if (inboxCount == 1) "item" else "items"} waiting to be sorted",
                    onClick = { nav.navigate(MindScreen.INBOX, MindTransition.SLIDE_RIGHT) },
                    minHeight = 80,
                    container = MaterialTheme.colorScheme.surfaceContainerHighest,
                    content = MaterialTheme.colorScheme.onSurface,
                )
            }

            item { Spacer(Modifier.height(2.dp)) }
            item { SectionLabel("Revisit soon") }
            if (memories.isEmpty()) {
                item {
                    androidx.compose.material3.Text(
                        "No memories due. Give durable knowledge a revisit date and it will resurface here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(memories.take(5), key = { it.id }) { m ->
                    BodyListItem(
                        headline = m.title,
                        supporting = "Fix · Revisit today",
                        icon = Icons.Rounded.EventRepeat,
                        onClick = { nav.navigate(MindScreen.MEMORY_DETAIL, MindTransition.SLIDE_RIGHT, m.id.toString()) },
                    )
                }
            }
        }
    }
}
