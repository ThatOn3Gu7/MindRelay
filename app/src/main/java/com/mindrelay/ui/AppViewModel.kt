package com.mindrelay.ui

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import com.mindrelay.data.backup.BackupStore
import com.mindrelay.data.repo.MindRepository
import com.mindrelay.data.repo.RepoError
import com.mindrelay.data.settings.SettingsStore
import com.mindrelay.graph
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
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

    private val _errors = MutableSharedFlow<String>(extraBufferCapacity = 8)
    /** Human-readable failure messages from repository operations (nullable so
     *  screens can collect with a null initial state). */
    val errors: SharedFlow<String?> = _errors.asSharedFlow()

    /**
     * Fire-and-forget writes from any screen. Prefer [launchAndRun] for
     * mutations that must be followed by navigation or UI changes, so the UI
     * only advances after the repository call has actually succeeded.
     */
    fun launch(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: RepoError) {
                _errors.tryEmit(e.message ?: "Operation failed")
            } catch (e: Exception) {
                _errors.tryEmit(e.message ?: "Something went wrong")
            }
        }
    }

    /**
     * Runs [block], collecting [block]'s returned failure message so callers can
     * show it, then runs [andThen] only on success. Designed for "write, handle
     * failure, navigate only after success".
     */
    fun launchAndRun(
        block: suspend () -> String?,
        andThen: () -> Unit,
    ) {
        viewModelScope.launch {
            val error = try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: RepoError) {
                e.message
            } catch (e: Exception) {
                e.message ?: "Something went wrong"
            }
            if (error == null) andThen() else _errors.tryEmit(error)
        }
    }
}

@Composable
fun appViewModel(): AppViewModel = viewModel()
