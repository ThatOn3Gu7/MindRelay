package com.mindrelay.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Help
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Notes
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.mindrelay.data.model.CaptureKind
import com.mindrelay.data.model.ConvertType
import com.mindrelay.data.model.ProjectStatus
import com.mindrelay.data.repo.ConvertResult
import com.mindrelay.nav.MindNavController
import com.mindrelay.nav.MindRoute
import com.mindrelay.nav.MindScreen
import com.mindrelay.nav.MindTransition
import com.mindrelay.ui.AppViewModel
import com.mindrelay.ui.components.SectionLabel
import com.mindrelay.util.relativeAgo

private fun kindLabel(kind: CaptureKind): String = when (kind) {
    CaptureKind.IDEA -> "Idea"
    CaptureKind.TODO -> "To-do"
    CaptureKind.QUESTION -> "Question"
    CaptureKind.NOTE -> "Note"
    CaptureKind.VOICE -> "Voice"
}

private fun kindIcon(kind: CaptureKind): ImageVector = when (kind) {
    CaptureKind.IDEA -> Icons.Rounded.Lightbulb
    CaptureKind.TODO -> Icons.Rounded.CheckBox
    CaptureKind.QUESTION -> Icons.Rounded.Help
    CaptureKind.NOTE -> Icons.Rounded.Notes
    CaptureKind.VOICE -> Icons.Rounded.Mic
}

@Composable
private fun RecKindChip(kind: CaptureKind) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = kindIcon(kind),
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = kindLabel(kind),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun ExpressiveInteractiveItem(
    headline: String,
    supporting: String,
    icon: ImageVector,
    onClick: (() -> Unit)?,
    iconContainerColor: Color = MaterialTheme.colorScheme.tertiaryContainer,
    iconContentColor: Color = MaterialTheme.colorScheme.onTertiaryContainer,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    // Smooth, tactile scaling animation - only scales if it's clickable
    val targetScale = if (onClick != null && isPressed) 0.96f else 1f
    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 600f),
        label = "interactive_item_scale"
    )

    Surface(
        onClick = onClick ?: {},
        enabled = onClick != null,
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CaptureDetailScreen(vm: AppViewModel, nav: MindNavController, captureId: Long?) {
    // -------------------------------------------------------------------------
    // DATA AND STATE FLOW (UNTOUCHED)
    // -------------------------------------------------------------------------
    val repo = vm.repository
    if (captureId == null) {
        LaunchedEffect(Unit) { nav.pop() }
        return
    }
    val captureFlow = repo.captures.byId(captureId).collectAsStateWithLifecycle(initialValue = null)
    val capture = captureFlow.value
    val projects by repo.projects.all().collectAsStateWithLifecycle(initialValue = emptyList())
    var menuOpen by remember { mutableStateOf(false) }
    
    // Entrance animation state
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }

    if (capture == null) {
        // Fallback for null state, un-animated for simplicity
        Text("This capture is no longer available.", modifier = Modifier.padding(16.dp))
        return
    }
    val linkedProject = capture.projectId?.let { pid -> projects.firstOrNull { it.id == pid } }
    // -------------------------------------------------------------------------

    Column(Modifier.fillMaxSize()) {
        MindTopBar(
            title = "Capture",
            onBack = { nav.popToRoot() },
            actions = listOf(
                Icons.Rounded.MoreVert to { menuOpen = true },
            ),
        )
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text("Delete capture") },
                leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null) },
                onClick = {
                    menuOpen = false
                    vm.launchAndRun(
                        block = {
                            repo.deleteCapture(capture)
                            null
                        },
                        andThen = { nav.resetTo(listOf(MindRoute(MindScreen.INBOX)), MindTransition.SLIDE_DOWN) },
                    )
                },
            )
        }
        
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(tween(300)) + slideInVertically(
                initialOffsetY = { 60 },
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f)
            ),
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Kind Chip & Provenance Line
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    RecKindChip(kind = capture.kind)
                    Text(
                        text = if (capture.isVoice)
                            "Captured ${relativeAgo(capture.createdAt)} · voice entry · no audio stored"
                        else
                            "Captured ${relativeAgo(capture.createdAt)} · text entry",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // Main Content Card - Soft, spacious, elevated feel
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = capture.text,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = MaterialTheme.typography.titleLarge.lineHeight * 1.1f
                        )
                        if (capture.detail.isNotBlank() && capture.detail != capture.text) {
                            Text(
                                text = capture.detail,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.3f
                            )
                        }
                    }
                }

                // Linked Items Section (now matching the tactile Expressive pattern)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExpressiveInteractiveItem(
                        headline = "Linked project",
                        supporting = linkedProject?.name ?: "None",
                        icon = Icons.Rounded.FolderOpen,
                        iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        iconContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        onClick = linkedProject?.let { p ->
                            { nav.navigate(MindScreen.PROJECT_DETAIL, MindTransition.SLIDE_RIGHT, p.id.toString()) }
                        },
                    )
                    ExpressiveInteractiveItem(
                        headline = "Linked session",
                        supporting = capture.linkedSessionId?.let { "Session #$it" } ?: "None",
                        icon = Icons.Rounded.Schedule,
                        iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        iconContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        onClick = null, // Sessions aren't clickable in this flow yet
                    )
                }

                // Convert Section
                Spacer(modifier = Modifier.height(8.dp))
                SectionLabel("Convert to")
                
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExpressiveInteractiveItem(
                        headline = "Memory",
                        supporting = "Durable knowledge Future You can find",
                        icon = Icons.Rounded.Bookmark,
                        onClick = {
                            vm.launchAndRun(
                                block = {
                                    if (capture.converted == ConvertType.MEMORY) {
                                        "This capture was already converted to a Memory"
                                    } else {
                                        val result = repo.convertToMemory(capture.id)
                                        when (result) {
                                            is ConvertResult.Ok -> null
                                            is ConvertResult.Rejected -> result.reason
                                        }
                                    }
                                },
                                andThen = { nav.resetTo(listOf(MindRoute(MindScreen.MEMORIES)), MindTransition.SLIDE_DOWN) },
                            )
                        },
                    )
                    
                    ExpressiveInteractiveItem(
                        headline = "Project note",
                        supporting = "Attach to a project or current session",
                        icon = Icons.Rounded.FolderOpen,
                        onClick = {
                            val pid = capture.projectId ?: projects.firstOrNull { it.status == ProjectStatus.ACTIVE }?.id
                                ?: projects.firstOrNull()?.id
                            if (pid != null) {
                                vm.launchAndRun(
                                    block = {
                                        if (capture.converted == ConvertType.PROJECT_NOTE) {
                                            "This capture was already converted to a Project note"
                                        } else {
                                            val result = repo.convertToProjectNote(capture.id, pid)
                                            when (result) {
                                                is ConvertResult.Ok -> null
                                                is ConvertResult.Rejected -> result.reason
                                            }
                                        }
                                    },
                                    andThen = { nav.resetTo(listOf(MindRoute(MindScreen.PROJECT_DETAIL, pid.toString())), MindTransition.SLIDE_DOWN) },
                                )
                            } else {
                                nav.navigate(MindScreen.NEW_PROJECT, MindTransition.SLIDE_UP)
                            }
                        },
                    )
                    
                    ExpressiveInteractiveItem(
                        headline = "Task",
                        supporting = "Create a secondary to-do item",
                        icon = Icons.Rounded.CheckBox,
                        onClick = {
                            vm.launchAndRun(
                                block = {
                                    if (capture.converted == ConvertType.TASK) {
                                        "This capture was already converted to a Task"
                                    } else {
                                        val result = repo.convertToTask(capture.id)
                                        when (result) {
                                            is ConvertResult.Ok -> null
                                            is ConvertResult.Rejected -> result.reason
                                        }
                                    }
                                },
                                andThen = { nav.resetTo(listOf(MindRoute(MindScreen.HOME)), MindTransition.SLIDE_DOWN) },
                            )
                        },
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Archive Action Button with its own tactile spring state
                val archiveInteractionSource = remember { MutableInteractionSource() }
                val isArchivePressed by archiveInteractionSource.collectIsPressedAsState()
                val archiveScale by animateFloatAsState(
                    targetValue = if (isArchivePressed) 0.96f else 1f,
                    animationSpec = spring(dampingRatio = 0.6f, stiffness = 600f),
                    label = "archive_button_scale"
                )

                OutlinedButton(
                    onClick = {
                        vm.launchAndRun(
                            block = {
                                repo.archiveCapture(capture)
                                null
                            },
                            andThen = { nav.resetTo(listOf(MindRoute(MindScreen.INBOX)), MindTransition.SLIDE_DOWN) },
                        )
                    },
                    interactionSource = archiveInteractionSource,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .graphicsLayer {
                            scaleX = archiveScale
                            scaleY = archiveScale
                        },
                    shape = RoundedCornerShape(28.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                ) {
                    Icon(Icons.Rounded.Archive, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Archive",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

