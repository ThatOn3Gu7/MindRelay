package com.mindrelay.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.mindrelay.nav.MindNavController
import com.mindrelay.nav.MindScreen
import com.mindrelay.nav.MindTransition

/**
 * The 5-destination navigation bar used by Home, Inbox, Projects, Memories and
 * Search. 80dp tall on surfaceContainer, extended through the gesture inset.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MindNavBar(
    nav: MindNavController,
    selected: MindScreen,
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        windowInsets = WindowInsets.navigationBars,
        contentColor = MaterialTheme.colorScheme.secondary,
    ) {
        NavDest(nav, MindScreen.HOME, "Home", Icons.Rounded.Home, selected)
        NavDest(nav, MindScreen.INBOX, "Inbox", Icons.Rounded.Inbox, selected)
        NavDest(nav, MindScreen.PROJECTS, "Projects", Icons.Rounded.FolderOpen, selected)
        NavDest(nav, MindScreen.MEMORIES, "Memories", Icons.Rounded.Bookmark, selected)
        NavDest(nav, MindScreen.SEARCH, "Search", Icons.Rounded.Search, selected)
    }
}

@Composable
private fun RowScope.NavDest(
    nav: MindNavController,
    screen: MindScreen,
    label: String,
    icon: ImageVector,
    selected: MindScreen,
) {
    NavigationBarItem(
        selected = screen == selected,
        onClick = {
            // Only when tapping a different tab.
            if (screen != selected) {
                nav.resetTo(listOf(com.mindrelay.nav.MindRoute(screen)), MindTransition.FADE)
            }
        },
        icon = { Icon(icon, contentDescription = label) },
        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
            selectedTextColor = MaterialTheme.colorScheme.onSecondaryContainer,
            indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    )
}

@Composable
fun QuickCaptureFab(nav: MindNavController, modifier: Modifier = Modifier) {
    FloatingActionButton(
        onClick = { nav.navigate(MindScreen.QUICK_CAPTURE, MindTransition.EXPAND) },
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 3.dp),
    ) {
        Icon(Icons.Rounded.Add, contentDescription = "Quick capture")
    }
}
