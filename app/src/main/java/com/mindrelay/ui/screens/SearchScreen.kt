package com.mindrelay.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mindrelay.data.model.CaptureKind
import com.mindrelay.nav.MindNavController
import com.mindrelay.nav.MindScreen
import com.mindrelay.nav.MindTransition
import com.mindrelay.ui.AppViewModel
import com.mindrelay.ui.components.BodyListItem
import com.mindrelay.ui.components.ConnectedChipGroup
import com.mindrelay.ui.components.SectionLabel
import com.mindrelay.util.relativeAgo
import com.mindrelay.data.db.CaptureEntity
import com.mindrelay.data.db.ProjectEntity
import com.mindrelay.data.db.SessionEntity

private data class SearchRow(
    val key: String,
    val type: String,
    val title: String,
    val supporting: String,
    val icon: ImageVector,
    val screen: MindScreen,
    val arg: String?,
)

private const val MAX_RESULTS_PER_GROUP = 40

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SearchScreen(vm: AppViewModel, nav: MindNavController) {
    val repo = vm.repository
    val projects by repo.projects.all().collectAsStateWithLifecycle(initialValue = emptyList())
    val sessions by repo.sessions.all().collectAsStateWithLifecycle(initialValue = emptyList())
    val entries by repo.entries.all().collectAsStateWithLifecycle(initialValue = emptyList())
    val captures by repo.captures.all().collectAsStateWithLifecycle(initialValue = emptyList())
    val memories by repo.memories.all().collectAsStateWithLifecycle(initialValue = emptyList())
    val tasks by repo.tasks.all().collectAsStateWithLifecycle(initialValue = emptyList())

    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("All") }

    val q = query.trim()
    val rows = remember(projects, sessions, entries, captures, memories, tasks, q, filter) {
        // Index id → row once, then answer "which session/name does this belong
        // to?" in O(1) instead of scanning the lists per entry.
        val projectsById: Map<Long, ProjectEntity> = projects.associateBy { it.id }
        val sessionsById: Map<Long, SessionEntity> = sessions.associateBy { it.id }
        fun projectName(p: Long?): String = p?.let { projectsById[it]?.name } ?: ""

        val out = mutableListOf<SearchRow>()

        if (filter == "All" || filter == "Projects") {
            projects.filter { it.name.contains(q, true) }
                .take(MAX_RESULTS_PER_GROUP)
                .forEach { p ->
                    out.add(
                        SearchRow(
                            key = "p${p.id}", type = "Projects", title = p.name,
                            supporting = "Next: ${p.nextAction.ifBlank { "—" }}",
                            icon = Icons.Rounded.FolderOpen,
                            screen = MindScreen.PROJECT_DETAIL, arg = p.id.toString(),
                        )
                    )
                }
        }
        if (filter == "All" || filter == "Sessions") {
            entries.mapNotNull { e ->
                val s = sessionsById[e.sessionId] ?: return@mapNotNull null
                val haystack = "${projectName(s.projectId)} ${s.title} ${e.text}"
                if (haystack.contains(q, true)) e to s else null
            }.take(MAX_RESULTS_PER_GROUP).forEach { (e, s) ->
                out.add(
                    SearchRow(
                        key = "e${e.id}", type = "Session entries", title = e.text,
                        supporting = "${projectName(s.projectId)} · ${s.title.substringBefore("·").trim()}",
                        icon = Icons.Rounded.Schedule,
                        screen = MindScreen.SESSION, arg = s.id.toString(),
                    )
                )
            }
        }
        if (filter == "All" || filter == "Tasks") {
            tasks.filter { "${it.text} ${it.tag} ${projectName(it.projectId)}".contains(q, true) }
                .take(MAX_RESULTS_PER_GROUP)
                .forEach { t ->
                    out.add(
                        SearchRow(
                            key = "t${t.id}", type = "Tasks", title = t.text,
                            supporting = projectName(t.projectId).ifBlank { t.tag.ifBlank { "Task" } },
                            icon = Icons.Rounded.CheckBox,
                            screen = MindScreen.HOME, arg = null,
                        )
                    )
                }
        }
        if (filter == "All" || filter == "Memories") {
            memories.filter { "${it.title} ${it.content} ${it.tags}".contains(q, true) }
                .take(MAX_RESULTS_PER_GROUP)
                .forEach { m ->
                    out.add(
                        SearchRow(
                            key = "m${m.id}", type = "Memories", title = m.title,
                            supporting = m.tags.take(80),
                            icon = Icons.Rounded.Bookmark,
                            screen = MindScreen.MEMORY_DETAIL, arg = m.id.toString(),
                        )
                    )
                }
        }
        if (filter == "All" || filter == "Captures") {
            captures.filter { "${it.text} ${it.detail}".contains(q, true) }
                .take(MAX_RESULTS_PER_GROUP)
                .forEach { c ->
                    out.add(
                        SearchRow(
                            key = "c${c.id}", type = "Captures", title = c.text,
                            supporting = "${kindLabel(c)} · ${relativeAgo(c.createdAt)}",
                            icon = kindIcon(c),
                            screen = MindScreen.CAPTURE_DETAIL, arg = c.id.toString(),
                        )
                    )
                }
        }
        out
    }

    // Group by type preserving order.
    val groups = rows.groupBy { it.type }

    TabScaffold(
        nav = nav,
        selected = MindScreen.SEARCH,
        topBar = {
            MindTopBar(
                title = "Search",
                onBack = { nav.popToRoot() },
            )
        },
    ) { _ ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("pump timer") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    trailingIcon = { Icon(Icons.Rounded.Mic, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                    ),
                )
            }
            item {
                ConnectedChipGroup(
                    options = listOf("All", "Projects", "Sessions", "Tasks", "Memories", "Captures"),
                    selected = filter,
                    onSelect = { filter = it },
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
            if (rows.isEmpty()) {
                item {
                    Text(
                        if (q.isBlank()) "Type to search across projects, sessions, captures, memories, tasks and tags."
                        else "No matches for \"$q\".",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            for ((type, groupRows) in groups) {
                item {
                    SectionLabel(type, size = 13, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                }
                items(groupRows, key = { it.key }) { row ->
                    BodyListItem(
                        headline = row.title,
                        supporting = row.supporting,
                        icon = row.icon,
                        onClick = {
                            when (row.screen) {
                                MindScreen.PROJECT_DETAIL -> nav.navigate(MindScreen.PROJECT_DETAIL, MindTransition.SLIDE_RIGHT, row.arg)
                                MindScreen.SESSION -> nav.navigate(MindScreen.SESSION, MindTransition.SLIDE_RIGHT, row.arg)
                                MindScreen.MEMORY_DETAIL -> nav.navigate(MindScreen.MEMORY_DETAIL, MindTransition.SLIDE_RIGHT, row.arg)
                                MindScreen.CAPTURE_DETAIL -> nav.navigate(MindScreen.CAPTURE_DETAIL, MindTransition.SLIDE_RIGHT, row.arg)
                                // Tasks don't have a detail screen; navigate to the
                                // task's project when possible, else Home.
                                MindScreen.HOME -> nav.resetTo(
                                    listOf(com.mindrelay.nav.MindRoute(MindScreen.HOME)),
                                    MindTransition.FADE,
                                )
                                else -> {}
                            }
                        },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                }
            }
        }
    }
}

private fun kindIcon(kind: CaptureKind): ImageVector = when (kind) {
    CaptureKind.IDEA -> Icons.Rounded.Bookmark
    CaptureKind.TODO -> Icons.Rounded.CheckBox
    CaptureKind.QUESTION -> Icons.Rounded.Schedule
    CaptureKind.NOTE -> Icons.Rounded.Search
    CaptureKind.VOICE -> Icons.Rounded.Mic
}

private fun kindLabel(c: CaptureEntity): String = when (c.kind) {
    CaptureKind.IDEA -> "Idea"
    CaptureKind.TODO -> "To-do"
    CaptureKind.QUESTION -> "Question"
    CaptureKind.NOTE -> "Note"
    CaptureKind.VOICE -> "Voice"
}

private fun kindIcon(c: CaptureEntity): ImageVector = kindIcon(c.kind)
