package com.mindrelay.nav

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mindrelay.ui.AppViewModel
import com.mindrelay.ui.screens.CaptureDetailScreen
import com.mindrelay.ui.screens.DataBackupScreen
import com.mindrelay.ui.screens.EndSessionScreen
import com.mindrelay.ui.screens.HomeScreen
import com.mindrelay.ui.screens.InboxScreen
import com.mindrelay.ui.screens.MemoryDetailScreen
import com.mindrelay.ui.screens.MemoriesScreen
import com.mindrelay.ui.screens.NewMemoryScreen
import com.mindrelay.ui.screens.NewProjectScreen
import com.mindrelay.ui.screens.ProjectDetailScreen
import com.mindrelay.ui.screens.ProjectsScreen
import com.mindrelay.ui.screens.QuickCaptureScreen
import com.mindrelay.ui.screens.SessionScreen
import com.mindrelay.ui.screens.SettingsScreen

/**
 * Hosts the whole app on a single back stack and animates each mutation with the
 * expressive motion scheme (spring-bouncy spatial, calm near-critically-damped
 * effects, as the M3 Expressive guidance prescribes). Back pops replay the entry
 * transition in reverse.
 */
@Composable
fun MindNavHost(vm: AppViewModel, nav: MindNavController) {
    val change = nav.change

    // Surface repository/backup failures from any screen as a snackbar.
    val errorState by vm.errors.collectAsStateWithLifecycle(initialValue = null)
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(errorState) {
        errorState?.let { snackbarHostState.showSnackbar(it) }
    }

    // System back gesture / button pops the stack (transition plays in reverse).
    BackHandler(enabled = nav.canPop) { nav.pop() }

    val spatial: FiniteAnimationSpec<IntOffset> = spring(
        dampingRatio = SpringBounce,
        stiffness = 420f,
    )

    val enter: EnterTransition = when {
        change.transition == MindTransition.FADE -> fadeIn(tween(180))
        change.transition == MindTransition.SLIDE_DOWN ->
            slideInVertically(spatial) { -it } + fadeIn(tween(160))
        change.transition == MindTransition.SLIDE_UP ->
            slideInVertically(spatial) { it } + fadeIn(tween(160))
        change.transition == MindTransition.EXPAND ->
            scaleIn(initialScale = 0.6f, animationSpec = spring<Float>(dampingRatio = SpringBounce)) + fadeIn(tween(150))
        change.backward -> slideInHorizontally(spatial) { -it / 6 }
        else -> slideInHorizontally(spatial) { it }
    }
    val exit: ExitTransition = when {
        change.transition == MindTransition.FADE -> fadeOut(tween(160))
        change.backward -> slideOutHorizontally(spatial) { it / 3 } + fadeOut(tween(140))
        else -> fadeOut(tween(140))
    }

    AnimatedContent(
        targetState = nav.current,
        transitionSpec = {
            (enter togetherWith exit) as ContentTransform
        },
        label = "mindrelay-nav",
    ) { route ->
        Box(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            Screen(vm = vm, nav = nav, route = route)
            SnackbarHost(
                snackbarHostState,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

private const val SpringBounce = 0.9f

@Composable
private fun Screen(vm: AppViewModel, nav: MindNavController, route: MindRoute) {
    when (route.screen) {
        MindScreen.HOME -> HomeScreen(vm, nav)
        MindScreen.INBOX -> InboxScreen(vm, nav)
        MindScreen.PROJECTS -> ProjectsScreen(vm, nav)
        MindScreen.MEMORIES -> MemoriesScreen(vm, nav)
        MindScreen.QUICK_CAPTURE -> QuickCaptureScreen(vm, nav, route.arg.asLongOrNull())
        MindScreen.CAPTURE_DETAIL -> CaptureDetailScreen(vm, nav, route.arg.asLongOrNull())
        MindScreen.PROJECT_DETAIL -> ProjectDetailScreen(vm, nav, route.arg.asLongOrNull())
        MindScreen.SESSION -> SessionScreen(vm, nav, route.arg.asLongOrNull())
        MindScreen.END_SESSION -> EndSessionScreen(vm, nav, route.arg.asLongOrNull())
        MindScreen.MEMORY_DETAIL -> MemoryDetailScreen(vm, nav, route.arg.asLongOrNull())
        MindScreen.NEW_MEMORY -> NewMemoryScreen(vm, nav, route.arg.asLongOrNull())
        MindScreen.NEW_PROJECT -> NewProjectScreen(vm, nav, route.arg.asLongOrNull())
        MindScreen.SETTINGS -> SettingsScreen(vm, nav)
        MindScreen.DATA_BACKUP -> DataBackupScreen(vm, nav)
    }
}
