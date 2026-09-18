package com.mindrelay.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FabPosition
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mindrelay.nav.MindNavController
import com.mindrelay.nav.MindScreen

/**
 * Shared scaffold for the five tab screens: top bar + scrollable content +
 * bottom navigation bar + an optional FAB.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TabScaffold(
    nav: MindNavController,
    selected: MindScreen,
    topBar: @Composable () -> Unit,
    fab: (@Composable () -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = topBar,
        bottomBar = { MindNavBar(nav, selected) },
        floatingActionButton = fab ?: {},
        floatingActionButtonPosition = FabPosition.End,
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(
                    // keep the list's own horizontal 16dp margin; the bottom bar
                    // and FAB area is accounted for in the content padding.
                    top = padding.calculateTopPadding(),
                    bottom = padding.calculateBottomPadding(),
                )
        ) {
            content(
                PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = 104.dp,
                )
            )
        }
    }
}
