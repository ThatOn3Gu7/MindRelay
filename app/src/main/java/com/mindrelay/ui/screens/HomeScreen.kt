package com.mindrelay.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.mindrelay.data.model.ProjectStatus
import com.mindrelay.data.model.SessionStatus
import com.mindrelay.nav.MindNavController
import com.mindrelay.nav.MindScreen
import com.mindrelay.nav.MindTransition
import com.mindrelay.ui.AppViewModel
import com.mindrelay.ui.components.ConnectedChipGroup
import com.mindrelay.ui.search.MindSearchField
import com.mindrelay.ui.search.MindSearchResults
import com.mindrelay.ui.search.SearchResultRow
import com.mindrelay.ui.search.SearchSectionHeader
import com.mindrelay.ui.search.UNIVERSAL_FILTERS
import com.mindrelay.ui.search.UNIVERSAL_SECTIONS
import com.mindrelay.ui.search.rememberMindSearchState
import com.mindrelay.ui.search.universalSearch
import com.mindrelay.util.relativeAgo

private data class HomeNextAction(
    val key: String,
    val text: String,
    val supporting: String,
    val isTask: Boolean,
    val projectId: Long?,
    val captureId: Long?,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeExpressiveCard(
    headline: String,
    body: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    minHeight: Dp = 180.dp
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    // Bouncy scale animation makes the card feel alive
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f),
        label = "card_scale"
    )

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        interactionSource = interactionSource,
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = minHeight)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxSize()
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = headline,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyLarge,
                lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.2f
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InteractiveActionItem(
    headline: String,
    supporting: String,
    icon: ImageVector,
    onClick: () -> Unit,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f),
        label = "item_scale"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = containerColor,
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
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
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
        }
    }
}

@Composable
private fun ExpressiveSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 4.dp)
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeScreen(vm: AppViewModel, nav: MindNavController) {
    // -------------------------------------------------------------------------
    // DATA AND STATE FLOW (UNTOUCHED AS REQUESTED)
    // -------------------------------------------------------------------------
    val repo = vm.repository
    val captures by repo.captures.active().collectAsStateWithLifecycle(initialValue = emptyList())
    val projects by repo.projects.all().collectAsStateWithLifecycle(initialValue = emptyList())
    val tasks by repo.tasks.open().collectAsStateWithLifecycle(initialValue = emptyList())
    val sessions by repo.sessions.all().collectAsStateWithLifecycle(initialValue = emptyList())
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
                isTask = true, projectId = t.projectId, captureId = t.captureId,
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
                        isTask = false, projectId = p.id, captureId = null,
                    )
                )
            }
        }
    val nextActionsLimited = nextActions.take(6)
    // -------------------------------------------------------------------------

    // Universal search: Home is the one place that searches every entity, so it
    // needs the full tables rather than the curated slices the cards above use.
    val search = rememberMindSearchState()
    var searchFilter by remember { mutableStateOf("All") }
    val allCaptures by repo.captures.all().collectAsStateWithLifecycle(initialValue = emptyList())
    val allEntries by repo.entries.all().collectAsStateWithLifecycle(initialValue = emptyList())
    val allMemories by repo.memories.all().collectAsStateWithLifecycle(initialValue = emptyList())
    val allTasks by repo.tasks.all().collectAsStateWithLifecycle(initialValue = emptyList())

    val q = search.query.trim()
    val hits = remember(projects, sessions, allEntries, allCaptures, allMemories, allTasks, q, searchFilter) {
        universalSearch(
            projects = projects,
            sessions = sessions,
            entries = allEntries,
            captures = allCaptures,
            memories = allMemories,
            tasks = allTasks,
            query = q,
            filter = searchFilter,
        )
    }
    val groups = hits.groupBy { it.type }

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
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                MindSearchField(
                    state = search,
                    placeholder = "Search everything",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )

                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        // Increased paddings for a breathable, modern spacing style
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 120.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        item {
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
                                HomeExpressiveCard(
                                    headline = "Continue",
                                    body = body,
                                    onClick = { nav.navigate(MindScreen.PROJECT_DETAIL, MindTransition.SLIDE_RIGHT, continueProject.id.toString()) },
                                    minHeight = 180.dp,
                                )
                            } else {
                                HomeExpressiveCard(
                                    headline = "Continue",
                                    body = "Create your first project to keep a living record of where you are and what to do next.",
                                    onClick = { nav.navigate(MindScreen.NEW_PROJECT, MindTransition.SLIDE_UP) },
                                    minHeight = 180.dp,
                                )
                            }
                        }

                        item { Spacer(Modifier.height(8.dp)) }
                        item { ExpressiveSectionLabel("Next actions") }
                        if (nextActionsLimited.isEmpty()) {
                            item {
                                Text(
                                    text = "Nothing pending. Capture a thought or set a project's next action.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                            }
                        } else {
                            items(nextActionsLimited, key = { it.key }) { na ->
                                InteractiveActionItem(
                                    headline = na.text,
                                    supporting = na.supporting,
                                    icon = if (na.isTask) Icons.Rounded.TaskAlt else Icons.Rounded.PlayArrow,
                                    onClick = {
                                        // Tapping a Next Action opens its context; it must
                                        // never complete/remove the item. Both task-backed
                                        // and project-derived actions open the project they
                                        // belong to; a task without a project falls back to
                                        // its origin capture.
                                        when {
                                            na.projectId != null -> nav.navigate(
                                                MindScreen.PROJECT_DETAIL, MindTransition.SLIDE_RIGHT, na.projectId.toString()
                                            )
                                            na.isTask && na.captureId != null -> nav.navigate(
                                                MindScreen.CAPTURE_DETAIL, MindTransition.SLIDE_RIGHT, na.captureId.toString()
                                            )
                                        }
                                    }
                                )
                            }
                        }

                        item { Spacer(Modifier.height(8.dp)) }
                        item {
                            HomeExpressiveCard(
                                headline = "Inbox",
                                body = if (inboxCount == 0) "Nothing waiting to be sorted" else
                                    "$inboxCount ${if (inboxCount == 1) "item" else "items"} waiting to be sorted",
                                onClick = { nav.navigate(MindScreen.INBOX, MindTransition.SLIDE_RIGHT) },
                                minHeight = 100.dp,
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }

                        item { Spacer(Modifier.height(8.dp)) }
                        item { ExpressiveSectionLabel("Revisit soon") }
                        if (memories.isEmpty()) {
                            item {
                                Text(
                                    text = "No memories due. Give durable knowledge a revisit date and it will resurface here.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                            }
                        } else {
                            items(memories.take(5), key = { it.id }) { m ->
                                InteractiveActionItem(
                                    headline = m.title,
                                    supporting = "Fix · Revisit today",
                                    icon = Icons.Rounded.EventRepeat,
                                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                    onClick = { nav.navigate(MindScreen.MEMORY_DETAIL, MindTransition.SLIDE_RIGHT, m.id.toString()) },
                                )
                            }
                        }
                    }

                    // Expanded universal search covers the Home cards.
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
                                    options = UNIVERSAL_FILTERS,
                                    selected = searchFilter,
                                    onSelect = { searchFilter = it },
                                )
                            }
                            if (hits.isEmpty() && q.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(top = 32.dp),
                                    contentAlignment = Alignment.TopCenter,
                                ) {
                                    RootEmptyArt(
                                        icon = Icons.Rounded.Search,
                                        headline = "No matches found",
                                        body = "Nothing in your projects, sessions, tasks, " +
                                            "memories or captures matches \"$q\".",
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        onContainerColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                    )
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 120.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    UNIVERSAL_SECTIONS.forEach { section ->
                                        val inSection = groups[section].orEmpty()
                                        if (inSection.isNotEmpty()) {
                                            item(key = "header_$section") {
                                                SearchSectionHeader(title = section, count = inSection.size)
                                            }
                                            items(inSection, key = { it.key }) { hit ->
                                                SearchResultRow(
                                                    headline = hit.title,
                                                    supporting = hit.supporting,
                                                    icon = hit.icon,
                                                    type = hit.type,
                                                    // A hit with no target has
                                                    // nowhere specific to open,
                                                    // so the row stays inert
                                                    // rather than pushing Home
                                                    // on top of Home.
                                                    onClick = {
                                                        hit.arg?.let { arg ->
                                                            nav.navigate(hit.screen, MindTransition.SLIDE_RIGHT, arg)
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
        }
    }
}

