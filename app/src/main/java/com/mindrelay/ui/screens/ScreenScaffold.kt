package com.mindrelay.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
 * The one top offset for every root empty state, measured from the top of the
 * tab content area (just under the top bar).
 *
 * Every tab now pins the same search field, so the collapsed header band is the
 * same height everywhere (8 + 56 + 8 = 72dp) and this only has to clear that,
 * leaving a 24dp gap. Change this one number to move all of them together.
 */
private val RootEmptyStateTop = 96.dp

/**
 * Shared empty-state slot for the root tabs.
 *
 * Every tab hosts this as a direct child of its content Box — never stacked
 * under its own headers — so the artwork sits at exactly the same position on
 * Inbox, Projects, Memories and Search. Switching tabs then reads as the icon
 * swapping in place rather than the whole block being redrawn somewhere else.
 *
 * @param stateKey identifies the artwork currently shown. When it changes (a
 *   filter chip tapped while the tab is empty, for example) the artwork
 *   crossfades and scales instead of popping, matching the transition the tab
 *   lists already use.
 */
@Composable
fun RootEmptyState(stateKey: String, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = RootEmptyStateTop),
        contentAlignment = Alignment.TopCenter,
    ) {
        AnimatedContent(
            targetState = stateKey,
            transitionSpec = {
                (fadeIn(animationSpec = tween(250, delayMillis = 50)) +
                    scaleIn(initialScale = 0.95f, animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f)))
                    .togetherWith(fadeOut(animationSpec = tween(150)))
            },
            label = "root_empty_state_transition",
        ) { targetKey ->
            key(targetKey) { content() }
        }
    }
}

/**
 * The single empty-state artwork behind [RootEmptyState]: ambient ring, colored
 * container ring, 40dp icon, then headline and body copy.
 *
 * Keeping the geometry here is what stops the four tabs drifting apart again —
 * callers only supply the icon, the copy and the two container colors, so no
 * screen can end up with its own padding or icon size.
 */
@Composable
fun RootEmptyArt(
    icon: ImageVector,
    headline: String,
    body: String,
    containerColor: Color,
    onContainerColor: Color,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(140.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(40.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(120.dp)
            ) {}
            Surface(
                shape = RoundedCornerShape(32.dp),
                color = containerColor,
                modifier = Modifier.size(80.dp)
            ) {}
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = onContainerColor,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = headline,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = body,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.2f
        )
    }
}

/**
 * Inline placeholder for an empty section inside a detail screen (Project
 * detail's "Recent sessions" and "Linked memories", Session's log).
 *
 * One component so all three read identically. Unlike the root tabs these stay
 * anchored inside their own section: a detail screen has a hero card and action
 * buttons above the section, so a fixed absolute position would overlap them.
 */
@Composable
fun SectionEmptyPlaceholder(text: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp),
        )
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
