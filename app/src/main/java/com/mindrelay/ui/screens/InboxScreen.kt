package com.mindrelay.ui.screens

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
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.Help
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Notes
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.mindrelay.data.model.CaptureKind
import com.mindrelay.nav.MindNavController
import com.mindrelay.nav.MindScreen
import com.mindrelay.nav.MindTransition
import com.mindrelay.ui.AppViewModel
import com.mindrelay.util.relativeAgo

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

@Composable
private fun EmptyInboxArt() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Custom composed art piece using standard material shapes and icons
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(140.dp)
        ) {
            // Background ambient ring
            Surface(
                shape = RoundedCornerShape(40.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(120.dp)
            ) {}
            // Inner vibrant ring
            Surface(
                shape = RoundedCornerShape(32.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(80.dp)
            ) {}
            // Centered icon
            Icon(
                imageVector = Icons.Rounded.Lightbulb,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(40.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "Your mind is clear",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = "Nothing here right now. Capture a new thought, idea, or task and it will safely land here for sorting.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.2f
        )
    }
}

@Composable
private fun ExpressiveInboxItem(
    headline: String,
    supporting: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    // Smooth, tactile scaling animation
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 600f),
        label = "inbox_item_scale"
    )

    Surface(
        onClick = onClick,
        // Reduced corner radius compared to the home screen for a more list-like feel
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
            // Expressive icon container
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = headline,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun InboxScreen(vm: AppViewModel, nav: MindNavController) {
    // -------------------------------------------------------------------------
    // DATA AND STATE FLOW (UNTOUCHED)
    // -------------------------------------------------------------------------
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
    // -------------------------------------------------------------------------

    TabScaffold(
        nav = nav,
        selected = MindScreen.INBOX,
        topBar = {
            MindTopBar(
                title = "Inbox",
            )
        },
        fab = { QuickCaptureFab(nav) },
    ) { _ ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            // Kept padding tighter to address the sizing feedback
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
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
                    EmptyInboxArt()
                }
            } else {
                items(filtered, key = { it.id }) { c ->
                    ExpressiveInboxItem(
                        headline = c.text,
                        supporting = "${kindLabel(c.kind)} · ${relativeAgo(c.createdAt)}" +
                            if (c.isVoice) " · voice" else "",
                        icon = kindIcon(c.kind),
                        onClick = {
                            nav.navigate(MindScreen.CAPTURE_DETAIL, MindTransition.SLIDE_RIGHT, c.id.toString())
                        }
                    )
                }
            }
        }
    }
}

