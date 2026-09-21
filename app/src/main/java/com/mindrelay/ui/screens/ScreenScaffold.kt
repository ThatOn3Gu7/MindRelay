package com.mindrelay.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.mindrelay.nav.MindNavController

/**
 * Shared top app bar: 64dp on surface, drawn behind the status bar (top inset
 * applied by the bar itself), title in titleLarge, 48dp icon buttons.
 *
 * @param overflowMenu optional lambda rendered at the end of the actions row.
 *   Callers pass [MindOverflowMenu], which renders the three-dot button and its
 *   menu(s) inside one anchored [Box], so the popup opens beside/below the
 *   button instead of inheriting the full screen as its anchor.
 * @param leadingIcon optional non-clickable icon rendered in place of the
 *   navigation slot, for root-level tabs (e.g. Inbox, Search) where the leading
 *   glyph is pure visual context rather than a back/close action.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MindTopBar(
    title: String,
    nav: MindNavController? = null,
    onBack: (() -> Unit)? = null,
    onClose: (() -> Unit)? = null,
    leadingIcon: ImageVector? = null,
    actions: List<Pair<ImageVector, () -> Unit>> = emptyList(),
    modifier: Modifier = Modifier,
    overflowMenu: (@Composable () -> Unit)? = null,
) {
    TopAppBar(
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        navigationIcon = {
            when {
                leadingIcon != null -> Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                )
                onClose != null -> IconButton(onClick = onClose) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Close")
                }
                onBack != null -> IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                }
            }
        },
        actions = {
            actions.forEach { (icon, onClick) ->
                IconButton(onClick = onClick, modifier = Modifier.padding(end = 4.dp)) {
                    Icon(icon, contentDescription = null)
                }
            }
            if (overflowMenu != null) overflowMenu()
        },
        modifier = modifier,
        windowInsets = WindowInsets.statusBars,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
        ),
    )
}

/**
 * Shared empty-state slot for root tabs. The artwork composables carry their
 * own 48dp top padding, so the shared host starts at the same top position on
 * every root tab.
 */
@Composable
fun RootEmptyState(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        content()
    }
}

/**
 * Overflow control anchored to a three-dot ("MoreVert") button. Renders the
 * button and whatever the caller places in [menuContent] inside a single
 * Box, so the DropdownMenus in [menuContent] anchor their popups to that box —
 * beside/below the button — rather than to the surrounding screen.
 *
 * Supports more than one DropdownMenu in [menuContent] (e.g. a secondary
 * "Change status" menu on Project Detail); each anchors to the same button.
 */
@Composable
fun MindOverflowMenu(
    onOpen: () -> Unit,
    menuContent: @Composable () -> Unit,
) {
    Box {
        IconButton(onClick = onOpen, modifier = Modifier.padding(end = 4.dp)) {
            Icon(Icons.Rounded.MoreVert, contentDescription = "More options")
        }
        menuContent()
    }
}

/**
 * Standard screen column: top bar pinned, content scrollable under it, with the
 * navigation bar (if any) floating the FAB above it.
 */
@Composable
fun ScreenColumn(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(
        start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp,
    ),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding),
        content = content,
    )
}
