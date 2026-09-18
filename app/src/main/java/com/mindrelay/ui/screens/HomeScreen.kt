package com.mindrelay.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.Settings
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
import androidx.compose.runtime.remember
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
import com.mindrelay.util.relativeAgo

private data class HomeNextAction(
    val key: String,
    val text: String,
    val supporting: String,
    val isTask: Boolean,
    val taskId: Long?,
    val projectId: Long?,
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
    // -------------------------------------------------------------------------

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
                        icon = Icons.Rounded.CheckCircle,
                        onClick = {
                            if (na.isTask && na.taskId != null) {
                                vm.launch { repo.setTaskDone(na.taskId, true) }
                            } else if (na.projectId != null) {
                                nav.navigate(MindScreen.PROJECT_DETAIL, MindTransition.SLIDE_RIGHT, na.projectId.toString())
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
    }
}

