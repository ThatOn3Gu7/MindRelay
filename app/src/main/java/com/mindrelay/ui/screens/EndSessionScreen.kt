package com.mindrelay.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EmojiObjects
import androidx.compose.material.icons.rounded.Help
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mindrelay.nav.MindNavController
import com.mindrelay.nav.MindScreen
import com.mindrelay.nav.MindTransition
import com.mindrelay.ui.AppViewModel
import com.mindrelay.ui.components.MindTextField
import com.mindrelay.ui.components.MindTopBar
import com.mindrelay.ui.components.PillButton
import com.mindrelay.ui.components.SectionLabel

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun EndSessionScreen(vm: AppViewModel, nav: MindNavController, sessionId: Long?) {
    // -------------------------------------------------------------------------
    // DATA AND STATE FLOW (UNTOUCHED)
    // -------------------------------------------------------------------------
    val repo = vm.repository
    if (sessionId == null) {
        LaunchedEffect(Unit) { nav.pop() }
        return
    }
    val sessionFlow = repo.sessions.byId(sessionId).collectAsStateWithLifecycle(initialValue = null)
    val session = sessionFlow.value

    var completed by remember { mutableStateOf("") }
    var discoveries by remember { mutableStateOf("") }
    var unresolved by remember { mutableStateOf("") }
    var nextAction by remember { mutableStateOf("") }
    var promoteChecked by remember { mutableStateOf(true) }

    // Entrance animation trigger
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }

    if (session == null) {
        Text("This session is no longer available.", modifier = Modifier.padding(16.dp))
        return
    }
    // -------------------------------------------------------------------------

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .imePadding()
    ) {
        MindTopBar(
            title = "End session",
            onClose = { nav.pop() },
        )

        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(tween(250)) + slideInVertically(
                initialOffsetY = { 30 },
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 350f)
            ),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                // Header Hero Info
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    SectionLabel("Write a handoff for Future You.", size = 24)
                    Text(
                        "Leave a clear bridge so you can seamlessly drop back into context later.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // Handoff Reflection Section Container
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Reflections & Progress",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )

                        MindTextField(
                            value = completed,
                            onValueChange = { completed = it },
                            label = "What did I complete?",
                            leadingIcon = Icons.Rounded.CheckCircle,
                            supportingText = "e.g. Relay wiring fixed. Pump manual test passed.",
                            filled = true,
                            singleLine = false,
                            minLines = 2,
                            maxLines = 4,
                        )

                        MindTextField(
                            value = discoveries,
                            onValueChange = { discoveries = it },
                            label = "What did I discover?",
                            leadingIcon = Icons.Rounded.EmojiObjects,
                            supportingText = "e.g. Loose neutral wire caused the issue.",
                            filled = true,
                            singleLine = false,
                            minLines = 2,
                            maxLines = 4,
                        )

                        MindTextField(
                            value = unresolved,
                            onValueChange = { unresolved = it },
                            label = "What is unresolved?",
                            leadingIcon = Icons.Rounded.Help,
                            supportingText = "e.g. Pump timer not yet installed.",
                            filled = true,
                            singleLine = false,
                            minLines = 2,
                            maxLines = 4,
                        )
                    }
                }

                // Next Action Hero Card
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PlayArrow,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Text(
                                text = "Immediate Next Action",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        MindTextField(
                            value = nextAction,
                            onValueChange = { nextAction = it },
                            label = "Single next action",
                            leadingIcon = Icons.Rounded.PlayArrow,
                            supportingText = "The one concrete thing to start with next time.",
                            filled = true,
                        )
                    }
                }

                // Promote to Memory Card
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Bookmark,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Promote to Memory?",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(enabled = discoveries.isNotBlank()) {
                                    promoteChecked = !promoteChecked
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = promoteChecked,
                                onCheckedChange = { promoteChecked = it },
                                enabled = discoveries.isNotBlank(),
                                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (discoveries.isNotBlank())
                                    "${discoveries.substringBefore('\n').take(48)} (discovery)"
                                else "Loose neutral wire fix (discovery)",
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (discoveries.isNotBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
            }
        }

        // Fixed Action Bar Bottom Area
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            PillButton(
                text = "Save handoff & end session",
                icon = Icons.Rounded.Check,
                enabled = completed.isNotBlank() || discoveries.isNotBlank() || nextAction.isNotBlank(),
                onClick = {
                    vm.launchAndRun(
                        block = {
                            repo.completeSession(
                                completedText = completed,
                                discoveries = discoveries,
                                unresolved = unresolved,
                                nextAction = nextAction,
                                sessionId = session.id,
                                promoteDiscovery = promoteChecked,
                            )?.let { it.message ?: "Could not end the session" }
                        },
                        andThen = {
                            nav.resetTo(
                                listOf(com.mindrelay.nav.MindRoute(MindScreen.PROJECT_DETAIL, session.projectId.toString())),
                                MindTransition.SLIDE_DOWN,
                            )
                        },
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            )
        }
    }
}
