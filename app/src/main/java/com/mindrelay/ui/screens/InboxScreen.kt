package com.mindrelay.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.Help
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Notes
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.mindrelay.ui.components.SectionLabel
import com.mindrelay.util.relativeAgo
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState

private fun kindIcon(kind: CaptureKind): ImageVector = when (kind) {
    CaptureKind.IDEA -> Icons.Rounded.Lightbulb
    CaptureKind.TODO -> Icons.Rounded.CheckBox
    CaptureKind.QUESTION -> Icons.Rounded.Help
    CaptureKind.NOTE -> Icons.Rounded.Notes
    CaptureKind.VOICE -> Icons.Rounded.Mic
}

private fun kindLabel(kind: CaptureKind): String = when (kind) {
    CaptureKind.IDEA -> "Idea"
    CaptureKind.TODO -> "To-do"
    CaptureKind.QUESTION -> "Question"
    CaptureKind.NOTE -> "Note"
    CaptureKind.VOICE -> "Voice"
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun InboxScreen(vm: AppViewModel, nav: MindNavController) {
    val repo = vm.repository
    val captures by repo.captures.active().collectAsStateWithLifecycle(initialValue = emptyList())
    var filter by remember { mutableStateOf("All") }

    val filtered = when (filter) {
        "Ideas" -> captures.filter { it.kind == CaptureKind.IDEA }
        "To-dos" -> captures.filter { it.kind == CaptureKind.TODO }
        "Questions" -> captures.filter { it.kind == CaptureKind.QUESTION }
        "Voice" -> captures.filter { it.isVoice }
        else -> captures
    }

    TabScaffold(
        nav = nav,
        selected = MindScreen.INBOX,
        topBar = {
            MindTopBar(
                title = "Inbox",
                onBack = { nav.navigate(MindScreen.HOME, MindTransition.SLIDE_RIGHT) },
            )
        },
        fab = { QuickCaptureFab(nav) },
    ) { _ ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item {
                androidx.compose.foundation.layout.Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    com.mindrelay.ui.components.ConnectedChipGroup(
                        options = listOf("All", "Ideas", "To-dos", "Questions", "Voice"),
                        selected = filter,
                        onSelect = { filter = it },
                    )
                }
            }
            if (filtered.isEmpty()) {
                item {
                    Text(
                        "Nothing here yet. Capture a thought and it lands in the Inbox for sorting.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier,
                    )
                }
            } else {
                items(filtered, key = { it.id }) { c ->
                    BodyListItem(
                        headline = c.text,
                        supporting = "${kindLabel(c.kind)} · ${relativeAgo(c.createdAt)}" +
                            if (c.isVoice) " · voice" else "",
                        icon = kindIcon(c.kind),
                        onClick = {
                            nav.navigate(MindScreen.CAPTURE_DETAIL, MindTransition.SLIDE_RIGHT, c.id.toString())
                        },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                }
            }
        }
    }
}
