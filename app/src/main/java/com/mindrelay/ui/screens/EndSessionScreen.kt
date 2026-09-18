package com.mindrelay.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EmojiObjects
import androidx.compose.material.icons.rounded.Help
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mindrelay.nav.MindNavController
import com.mindrelay.nav.MindScreen
import com.mindrelay.nav.MindTransition
import com.mindrelay.ui.AppViewModel
import com.mindrelay.ui.components.MindTextField
import com.mindrelay.ui.components.PillButton
import com.mindrelay.ui.components.SectionLabel

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun EndSessionScreen(vm: AppViewModel, nav: MindNavController, sessionId: Long?) {
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

    if (session == null) {
        Text("This session is no longer available.", modifier = Modifier.padding(16.dp))
        return
    }

    Column(Modifier.fillMaxSize()) {
        MindTopBar(
            title = "End session",
            onClose = { nav.pop() },
        )
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionLabel("Write a handoff for Future You.", size = 18)

            MindTextField(
                value = completed,
                onValueChange = { completed = it },
                label = "What did I complete?",
                leadingIcon = Icons.Rounded.CheckCircle,
                supportingText = "e.g. Relay wiring fixed. Pump manual test passed.",
                filled = false,
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
                filled = false,
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
                filled = false,
                singleLine = false,
                minLines = 2,
                maxLines = 4,
            )
            MindTextField(
                value = nextAction,
                onValueChange = { nextAction = it },
                label = "Single next action",
                leadingIcon = Icons.Rounded.PlayArrow,
                supportingText = "The one concrete thing to do next.",
                filled = true,
            )

            SectionLabel("Promote to Memory?", size = 14)
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Checkbox(
                    checked = promoteChecked,
                    onCheckedChange = { promoteChecked = it },
                    enabled = discoveries.isNotBlank(),
                    colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary),
                )
                Text(
                    if (discoveries.isNotBlank())
                        "${discoveries.substringBefore('\n').take(48)} (discovery)"
                    else "Loose neutral wire fix (discovery)",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }

            Spacer(Modifier.height(8.dp))
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
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}
