package com.mindrelay.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlayArrow
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mindrelay.nav.MindNavController
import com.mindrelay.nav.MindRoute
import com.mindrelay.nav.MindScreen
import com.mindrelay.nav.MindTransition
import com.mindrelay.ui.AppViewModel
import com.mindrelay.ui.components.MindTextField
import com.mindrelay.ui.components.PillButton
import com.mindrelay.ui.components.SectionLabel

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NewProjectScreen(vm: AppViewModel, nav: MindNavController, projectId: Long? = null) {
    // -------------------------------------------------------------------------
    // DATA AND STATE FLOW (UNTOUCHED)
    // -------------------------------------------------------------------------
    val repo = vm.repository
    var name by remember { mutableStateOf("") }
    var currentState by remember { mutableStateOf("") }
    var nextAction by remember { mutableStateOf("") }
    var goal by remember { mutableStateOf("") }

    val editing = projectId != null
    val existing = if (editing) {
        repo.projects.byId(projectId!!).collectAsStateWithLifecycle(initialValue = null).value
    } else null

    // Prefill once when editing an existing project.
    LaunchedEffect(existing) {
        val p = existing ?: return@LaunchedEffect
        name = p.name
        currentState = p.currentState
        nextAction = p.nextAction
        goal = p.goal
    }

    // Entrance animation state
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }
    // -------------------------------------------------------------------------

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .imePadding()
    ) {
        MindTopBar(
            title = if (editing) "Edit Project" else "New Project",
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
                // Header Subtitle & Guidance
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    SectionLabel(
                        if (editing) "Refine your project scope." else "Define your new initiative.",
                        size = 24
                    )
                    Text(
                        "Give it a clear name, immediate next step, and desired target outcome.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // Main Essential Info Hero Card (Project Identity)
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
                                    imageVector = Icons.Rounded.FolderOpen,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Text(
                                text = "Project Identity",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        MindTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = "Project name",
                            leadingIcon = Icons.Rounded.FolderOpen,
                            supportingText = "Required",
                            filled = true,
                            capitalization = KeyboardCapitalization.Words,
                        )
                    }
                }

                // Context & Alignment Container
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
                            text = "Context & Alignment (Optional)",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )

                        MindTextField(
                            value = nextAction,
                            onValueChange = { nextAction = it },
                            label = "First / next action",
                            leadingIcon = Icons.Rounded.PlayArrow,
                            supportingText = "What is the single concrete next step?",
                            filled = true,
                        )

                        MindTextField(
                            value = currentState,
                            onValueChange = { currentState = it },
                            label = "Current state",
                            leadingIcon = Icons.Rounded.Info,
                            supportingText = "Where are you right now?",
                            filled = true,
                            singleLine = false,
                            minLines = 2,
                            maxLines = 4,
                        )

                        MindTextField(
                            value = goal,
                            onValueChange = { goal = it },
                            label = "Goal / outcome",
                            leadingIcon = Icons.Rounded.Flag,
                            supportingText = "What outcome are you working toward?",
                            filled = true,
                            singleLine = false,
                            minLines = 2,
                            maxLines = 4,
                        )
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
                text = if (editing) "Save Changes" else "Create Project",
                icon = Icons.Rounded.Check,
                enabled = name.isNotBlank(),
                onClick = {
                    val original = existing
                    var targetId: Long? = null
                    vm.launchAndRun(
                        block = {
                            if (editing && original != null) {
                                repo.updateProject(
                                    original.copy(
                                        name = name.trim(),
                                        currentState = currentState.trim(),
                                        nextAction = nextAction.trim(),
                                        goal = goal.trim(),
                                    )
                                )
                                targetId = original.id
                            } else {
                                targetId = repo.createProject(name, currentState, nextAction, goal)
                            }
                            null
                        },
                        andThen = {
                            nav.resetTo(
                                listOf(MindRoute(MindScreen.PROJECT_DETAIL, targetId.toString())),
                                MindTransition.SLIDE_RIGHT,
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
