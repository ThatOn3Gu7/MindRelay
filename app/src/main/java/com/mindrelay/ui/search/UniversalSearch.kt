package com.mindrelay.ui.search

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mindrelay.data.db.CaptureEntity
import com.mindrelay.data.db.MemoryEntity
import com.mindrelay.data.db.ProjectEntity
import com.mindrelay.data.db.SessionEntity
import com.mindrelay.data.db.SessionEntryEntity
import com.mindrelay.data.db.TaskEntity
import com.mindrelay.data.model.CaptureKind
import com.mindrelay.nav.MindScreen
import com.mindrelay.util.relativeAgo

/**
 * The app's one universal search: every entity it stores, in one list.
 *
 * This is the single place that knows how to match a query against projects,
 * session entries, tasks, memories and captures, and which screen each hit
 * opens. Home's search field is its only caller — individual tabs search their
 * own list locally instead of going through here.
 */

/** One search result, already resolved to the screen it should open. */
@Immutable
data class SearchHit(
    val key: String,
    val type: String,
    val title: String,
    val supporting: String,
    val icon: ImageVector,
    val screen: MindScreen,
    val arg: String?,
)

/** Results are capped per source type so one entity can't crowd out the rest. */
const val MAX_RESULTS_PER_GROUP = 40

/** Filter chips offered above the universal results. */
val UNIVERSAL_FILTERS = listOf("All", "Projects", "Sessions", "Tasks", "Memories", "Captures")

/** Section order used when grouping [SearchHit]s for display. */
val UNIVERSAL_SECTIONS = listOf("Projects", "Session entries", "Tasks", "Memories", "Captures")

fun universalSearch(
    projects: List<ProjectEntity>,
    sessions: List<SessionEntity>,
    entries: List<SessionEntryEntity>,
    captures: List<CaptureEntity>,
    memories: List<MemoryEntity>,
    tasks: List<TaskEntity>,
    query: String,
    filter: String = "All",
): List<SearchHit> {
    val q = query.trim()
    val projectsById: Map<Long, ProjectEntity> = projects.associateBy { it.id }
    val sessionsById: Map<Long, SessionEntity> = sessions.associateBy { it.id }
    fun projectName(p: Long?): String = p?.let { projectsById[it]?.name } ?: ""

    val out = mutableListOf<SearchHit>()

    if (filter == "All" || filter == "Projects") {
        projects.filter { it.name.contains(q, true) }
            .take(MAX_RESULTS_PER_GROUP)
            .forEach { p ->
                out.add(
                    SearchHit(
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
                SearchHit(
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
                // A task result opens the screen its displayed entity lives
                // in, never Home: the owning project first, falling back to
                // its origin capture (mirrors Home's Next Actions). Without
                // either context there is nothing specific to open, so the
                // row stays inert instead of dumping the user on Home.
                val taskScreen = when {
                    t.projectId != null -> MindScreen.PROJECT_DETAIL
                    t.captureId != null -> MindScreen.CAPTURE_DETAIL
                    else -> null
                }
                val taskArg = when (taskScreen) {
                    MindScreen.PROJECT_DETAIL -> t.projectId?.toString()
                    MindScreen.CAPTURE_DETAIL -> t.captureId?.toString()
                    else -> null
                }
                out.add(
                    SearchHit(
                        key = "t${t.id}", type = "Tasks", title = t.text,
                        supporting = projectName(t.projectId).ifBlank { t.tag.ifBlank { "Task" } },
                        icon = Icons.Rounded.CheckBox,
                        screen = taskScreen ?: MindScreen.HOME, arg = taskArg,
                    )
                )
            }
    }
    if (filter == "All" || filter == "Memories") {
        memories.filter { "${it.title} ${it.content} ${it.tags}".contains(q, true) }
            .take(MAX_RESULTS_PER_GROUP)
            .forEach { m ->
                out.add(
                    SearchHit(
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
                    SearchHit(
                        key = "c${c.id}", type = "Captures", title = c.text,
                        supporting = "${captureKindLabel(c)} · ${relativeAgo(c.createdAt)}",
                        icon = captureKindIcon(c.kind),
                        screen = MindScreen.CAPTURE_DETAIL, arg = c.id.toString(),
                    )
                )
            }
    }
    return out
}

fun captureKindIcon(kind: CaptureKind): ImageVector = when (kind) {
    CaptureKind.IDEA -> Icons.Rounded.Lightbulb
    CaptureKind.TODO -> Icons.Rounded.CheckBox
    CaptureKind.QUESTION -> Icons.Rounded.Help
    CaptureKind.NOTE -> Icons.Rounded.Notes
    CaptureKind.VOICE -> Icons.Rounded.Mic
}

fun captureKindLabel(c: CaptureEntity): String = when (c.kind) {
    CaptureKind.IDEA -> "Idea"
    CaptureKind.TODO -> "To-do"
    CaptureKind.QUESTION -> "Question"
    CaptureKind.NOTE -> "Note"
    CaptureKind.VOICE -> "Voice"
}

/** Group header for one source type, with its result count. */
@Composable
fun SearchSectionHeader(title: String, count: Int) {
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

/** One universal result row; the icon container is tinted by source type. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SearchResultRow(
    headline: String,
    supporting: String,
    icon: ImageVector,
    type: String,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 600f),
        label = "search_result_scale"
    )

    val (iconContainerColor, iconContentColor) = when (type) {
        "Projects" -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        "Session entries" -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
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
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (supporting.isNotBlank()) {
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
