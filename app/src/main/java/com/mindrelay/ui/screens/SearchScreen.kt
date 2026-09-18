package com.mindrelay.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Help
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Notes
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.mindrelay.data.db.CaptureEntity
import com.mindrelay.data.db.ProjectEntity
import com.mindrelay.data.db.SessionEntity
import com.mindrelay.data.model.CaptureKind
import com.mindrelay.nav.MindNavController
import com.mindrelay.nav.MindRoute
import com.mindrelay.nav.MindScreen
import com.mindrelay.nav.MindTransition
import com.mindrelay.ui.AppViewModel
import com.mindrelay.ui.components.ConnectedChipGroup
import com.mindrelay.util.relativeAgo

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

private fun kindIcon(kind: CaptureKind): ImageVector = when (kind) {
    CaptureKind.IDEA -> Icons.Rounded.Lightbulb
    CaptureKind.TODO -> Icons.Rounded.CheckBox
    CaptureKind.QUESTION -> Icons.Rounded.Help
    CaptureKind.NOTE -> Icons.Rounded.Notes
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

@Composable
private fun EmptySearchArt(query: String) {
    val isSearch = query.isNotBlank()
    
    val icon = if (isSearch) Icons.Rounded.Search else Icons.Rounded.Tune
    val headline = if (isSearch) "No matches found" else "Search your mind"
    val body = if (isSearch)
        "We couldn't find anything matching \"$query\". Try searching for different keywords or changing filters."
    else
        "Quickly search across projects, session notes, tasks, durable memories, and captured thoughts."

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
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
private fun SearchSectionHeader(title: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 8.dp, start = 4.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Text(
                text = "$count",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun ExpressiveSearchItem(
    headline: String,
    supporting: String,
    icon: ImageVector,
    type: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 600f),
        label = "search_item_scale"
    )

    val (iconContainerColor, iconContentColor) = when (type) {
        "Projects" -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        "Sessions" -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        "Tasks" -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        "Memories" -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
    }

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
                if (supporting.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = supporting,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SearchScreen(vm: AppViewModel, nav: MindNavController) {
    // -------------------------------------------------------------------------
    // DATA AND STATE FLOW (UNTOUCHED)
    // -------------------------------------------------------------------------
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

    val groups = rows.groupBy { it.type }
    // -------------------------------------------------------------------------

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
        Column(modifier = Modifier.fillMaxSize()) {
            // Search field & Horizontal chip bar pinned at top
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search anything...") },
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

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ConnectedChipGroup(
                        options = listOf("All", "Projects", "Sessions", "Tasks", "Memories", "Captures"),
                        selected = filter,
                        onSelect = { filter = it },
                    )
                }
            }

            // Animated transition for list contents
            AnimatedContent(
                targetState = filter,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(250, delayMillis = 50)) +
                        scaleIn(initialScale = 0.95f, animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f)))
                        .togetherWith(fadeOut(animationSpec = tween(150)))
                },
                label = "search_filter_transition",
                modifier = Modifier.weight(1f)
            ) { targetFilter ->
                // Key the content on the target filter so each tab's list has its
                // own identity for AnimatedContent, and the target-state parameter
                // is actually used by the transition.
                key(targetFilter) {
                    if (rows.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                            EmptySearchArt(query = q)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 120.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            for ((type, groupRows) in groups) {
                                item(key = "header_$type") {
                                    SearchSectionHeader(title = type, count = groupRows.size)
                                }
                                items(groupRows, key = { it.key }) { row ->
                                    ExpressiveSearchItem(
                                        headline = row.title,
                                        supporting = row.supporting,
                                        icon = row.icon,
                                        type = row.type,
                                        onClick = {
                                            when (row.screen) {
                                                MindScreen.PROJECT_DETAIL -> nav.navigate(MindScreen.PROJECT_DETAIL, MindTransition.SLIDE_RIGHT, row.arg)
                                                MindScreen.SESSION -> nav.navigate(MindScreen.SESSION, MindTransition.SLIDE_RIGHT, row.arg)
                                                MindScreen.MEMORY_DETAIL -> nav.navigate(MindScreen.MEMORY_DETAIL, MindTransition.SLIDE_RIGHT, row.arg)
                                                MindScreen.CAPTURE_DETAIL -> nav.navigate(MindScreen.CAPTURE_DETAIL, MindTransition.SLIDE_RIGHT, row.arg)
                                                MindScreen.HOME -> nav.resetTo(
                                                    listOf(MindRoute(MindScreen.HOME)),
                                                    MindTransition.FADE,
                                                )
                                                else -> {}
                                            }
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
