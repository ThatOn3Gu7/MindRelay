package com.mindrelay.ui

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import com.mindrelay.data.backup.BackupStore
import com.mindrelay.data.repo.MindRepository
import com.mindrelay.data.settings.SettingsStore
import com.mindrelay.graph
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

/**
 * One lightweight, activity-scoped view model: exposes the repository and the
 * user's settings. Screens collect DAO flows directly (Room keeps them reactive)
 * and run writes through the repository inside the viewModelScope.
 */
class AppViewModel(app: Application) : AndroidViewModel(app) {
    val repository: MindRepository = app.graph.repository
    val settingsStore: SettingsStore = app.graph.settings
    val backup: BackupStore = app.graph.backup

    val settings = settingsStore.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, SettingsStore.Settings())

    /** Fire-and-forget writes from any screen. */
    fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}

@Composable
fun appViewModel(): AppViewModel = viewModel()
