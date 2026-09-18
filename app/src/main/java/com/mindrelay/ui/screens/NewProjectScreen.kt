package com.mindrelay.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mindrelay.nav.MindNavController
import com.mindrelay.nav.MindRoute
import com.mindrelay.nav.MindScreen
import com.mindrelay.nav.MindTransition
import com.mindrelay.ui.AppViewModel
import com.mindrelay.ui.components.MindTextField
import com.mindrelay.ui.components.PillButton
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NewProjectScreen(vm: AppViewModel, nav: MindNavController, projectId: Long? = null) {
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
    androidx.compose.runtime.LaunchedEffect(existing) {
        val p = existing ?: return@LaunchedEffect
        name = p.name
        currentState = p.currentState
        nextAction = p.nextAction
        goal = p.goal
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .imePadding()
    ) {
        MindTopBar(title = if (editing) "Edit Project" else "New Project", onClose = { nav.pop() })
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MindTextField(
                value = name,
                onValueChange = { name = it },
                label = "Project name",
                leadingIcon = Icons.Rounded.FolderOpen,
                supportingText = "Required",
                filled = false,
                capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Words,
            )
            MindTextField(
                value = currentState,
                onValueChange = { currentState = it },
                label = "Current state (optional)",
                leadingIcon = Icons.Rounded.Info,
                supportingText = "Where are you right now?",
                filled = false,
                singleLine = false,
                minLines = 2,
                maxLines = 4,
            )
            MindTextField(
                value = nextAction,
                onValueChange = { nextAction = it },
                label = "First / next action (optional)",
                leadingIcon = Icons.Rounded.PlayArrow,
                supportingText = "What is the single concrete next step?",
                filled = false,
            )
            MindTextField(
                value = goal,
                onValueChange = { goal = it },
                label = "Goal / notes (optional)",
                leadingIcon = Icons.Rounded.Flag,
                supportingText = "What outcome are you working toward?",
                filled = false,
                singleLine = false,
                minLines = 2,
                maxLines = 4,
            )
            Spacer(Modifier.padding(top = 4.dp))
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
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.padding(bottom = 16.dp))
        }
    }
}
