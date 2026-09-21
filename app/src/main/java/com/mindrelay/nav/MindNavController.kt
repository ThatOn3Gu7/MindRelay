package com.mindrelay.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Immutable
enum class MindTransition {
    SLIDE_RIGHT,   // new screen slides in from the right (push)
    SLIDE_DOWN,    // new screen slides down from the top
    SLIDE_UP,      // new screen slides up from the bottom (sheet)
    FADE,          // cross-fade between tabs
    EXPAND,        // FAB expansion into Quick Capture
}

@Immutable
enum class MindScreen {
    HOME, INBOX, PROJECTS, MEMORIES,
    QUICK_CAPTURE, CAPTURE_DETAIL, PROJECT_DETAIL, SESSION, END_SESSION,
    MEMORY_DETAIL, NEW_MEMORY, NEW_PROJECT, SETTINGS, DATA_BACKUP,
}

@Immutable
data class MindRoute(val screen: MindScreen, val arg: String? = null)

/** Describes the most recent stack mutation so the host can animate it. */
@Immutable
data class NavChange(val transition: MindTransition, val backward: Boolean)

/**
 * Owns the in-app back stack (4 main tabs + push screens). Because every screen
 * is driven from a single state list, the system back gesture pops through the
 * same path as the on-screen back arrows and the entry transition plays in reverse.
 */
class MindNavController {
    var stack: List<MindRoute> by mutableStateOf(listOf(MindRoute(MindScreen.HOME)))
        private set
    var change: NavChange by mutableStateOf(NavChange(MindTransition.FADE, false))
        private set

    val current: MindRoute get() = stack.last()

    fun navigate(screen: MindScreen, transition: MindTransition, arg: String? = null) {
        change = NavChange(transition, backward = false)
        stack = stack + MindRoute(screen, arg)
    }

    /** Swap the top of the stack (completion flows: e.g. Create → Project Detail). */
    fun replaceTop(screen: MindScreen, transition: MindTransition, arg: String? = null) {
        change = NavChange(transition, backward = true)
        stack = stack.dropLast(1) + MindRoute(screen, arg)
    }

    /** Set the entire stack (e.g. "Save to Inbox" → the Home screen). */
    fun resetTo(routes: List<MindRoute>, transition: MindTransition) {
        change = NavChange(transition, backward = false)
        stack = routes
    }

    fun pop() {
        if (stack.size > 1) {
            change = NavChange(MindTransition.SLIDE_RIGHT, backward = true)
            stack = stack.dropLast(1)
        }
    }

    /** Pop to the bottom-most destination (a tab), used by overlaid "back" arrows. */
    fun popToRoot() {
        if (stack.size > 1) {
            change = NavChange(MindTransition.SLIDE_RIGHT, backward = true)
            stack = listOf(stack.first())
        }
    }

    fun popTo(screen: MindScreen) {
        if (stack.size > 1) {
            val idx = stack.indexOfLast { it.screen == screen }
            if (idx >= 0) {
                change = NavChange(MindTransition.SLIDE_RIGHT, backward = true)
                stack = stack.subList(0, idx + 1)
            }
        }
    }

    val canPop: Boolean get() = stack.size > 1
}

@Composable
fun rememberMindNavController(): MindNavController =
    remember { MindNavController() }

/** Parse-friendly helpers for route arguments. */
fun String?.asLongOrNull(): Long? = this?.toLongOrNull()
