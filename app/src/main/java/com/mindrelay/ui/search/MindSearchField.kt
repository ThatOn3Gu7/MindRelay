package com.mindrelay.ui.search

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

/**
 * The app's one search field, and the state that drives it.
 *
 * Every screen that can search — Home (universal) and Inbox, Projects and
 * Memories (their own entries) — uses this same field, so search looks and
 * behaves identically everywhere. Tapping the field focuses it and expands it
 * into a full-area search view; the back arrow (or system back, or the keyboard's
 * done key) collapses it again, playing the same animation in reverse.
 */
class MindSearchState internal constructor(
    private val focusManager: FocusManager,
) {
    /** What the user has typed. Cleared whenever the field collapses. */
    var query by mutableStateOf("")

    /** True while the field is expanded into the full-area search view. */
    var expanded by mutableStateOf(false)
        internal set

    /** Collapse back into the plain field: drop focus and forget the query. */
    fun collapse() {
        expanded = false
        query = ""
        focusManager.clearFocus()
    }
}

@Composable
fun rememberMindSearchState(): MindSearchState {
    val focusManager = LocalFocusManager.current
    return remember { MindSearchState(focusManager) }
}

/**
 * The search field itself. Collapsed it is a quiet pill with a search glyph;
 * focused it grows into the expanded search view, gaining a back arrow that
 * collapses it.
 *
 * Also owns system-back: while expanded, back collapses the search instead of
 * navigating away from the screen.
 */
@Composable
fun MindSearchField(
    state: MindSearchState,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    BackHandler(enabled = state.expanded) { state.collapse() }

    // Pill while collapsed, softer corner once it owns the screen.
    val corner by animateDpAsState(
        targetValue = if (state.expanded) 16.dp else 28.dp,
        label = "search_field_corner",
    )

    OutlinedTextField(
        value = state.query,
        onValueChange = { state.query = it },
        placeholder = { Text(placeholder) },
        leadingIcon = {
            if (state.expanded) {
                IconButton(onClick = { state.collapse() }) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Close search")
                }
            } else {
                Icon(Icons.Rounded.Search, contentDescription = null)
            }
        },
        trailingIcon = {
            if (state.expanded && state.query.isNotEmpty()) {
                IconButton(onClick = { state.query = "" }) {
                    Icon(Icons.Rounded.Close, contentDescription = "Clear search")
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(corner),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { state.collapse() }),
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { if (it.isFocused) state.expanded = true },
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            focusedBorderColor = Color.Transparent,
            unfocusedBorderColor = Color.Transparent,
        ),
    )
}

/**
 * The full-area panel an expanded [MindSearchField] reveals, drawn over the
 * screen's own content so results replace the list rather than pushing it.
 *
 * Expands downward from the field and shrinks back into it on the way out, so
 * collapsing reads as the reverse of the same motion.
 */
@Composable
fun MindSearchResults(
    expanded: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    AnimatedVisibility(
        visible = expanded,
        enter = fadeIn(tween(160)) +
            expandVertically(expandFrom = Alignment.Top, animationSpec = tween(240)),
        exit = fadeOut(tween(120)) +
            shrinkVertically(shrinkTowards = Alignment.Top, animationSpec = tween(200)),
        modifier = modifier,
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface,
        ) {
            content()
        }
    }
}
