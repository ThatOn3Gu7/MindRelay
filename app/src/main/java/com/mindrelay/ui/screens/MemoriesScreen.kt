package com.mindrelay.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.mindrelay.data.model.MemoryType
import com.mindrelay.nav.MindNavController
import com.mindrelay.nav.MindScreen
import com.mindrelay.nav.MindTransition
import com.mindrelay.ui.AppViewModel
import com.mindrelay.ui.components.ConnectedChipGroup

private fun typeIcon(type: MemoryType): ImageVector = when (type) {
    MemoryType.FIX -> Icons.Rounded.Build
    MemoryType.PERSON -> Icons.Rounded.Person
    MemoryType.PLACE -> Icons.Rounded.Place
    MemoryType.RECIPE -> Icons.Rounded.Restaurant
    MemoryType.IDEA -> Icons.Rounded.Lightbulb
    else -> Icons.Rounded.AutoAwesome
}

private fun typeLabel(type: MemoryType): String = when (type) {
    MemoryType.FIX -> "Fix"
    MemoryType.PERSON -> "Person"
    MemoryType.IDEA -> "Idea"
    MemoryType.PLACE -> "Place"
    MemoryType.RECIPE -> "Recipe"
    MemoryType.NOTE -> "Note"
    MemoryType.OTHER -> "Other"
}

@Composable
private fun EmptyMemoriesArt(filter: String, query: String) {
    val isSearch = query.isNotBlank()

    val icon = when {
        isSearch -> Icons.Rounded.Search
        filter == "Fixes" -> Icons.Rounded.Build
        filter == "People" -> Icons.Rounded.Person
        filter == "Ideas" -> Icons.Rounded.Lightbulb
        filter == "Places" -> Icons.Rounded.Place
        else -> Icons.Rounded.AutoAwesome
    }
    val headline = when {
        isSearch -> "No matches found"
        filter == "All" -> "A blank canvas"
        else -> "No $filter saved yet"
    }
    val body = when {
        isSearch -> "We couldn't find any memories matching \"$query\"."
        filter == "All" -> "Preserve durable knowledge, important ideas, and facts here. Tag them and set revisit dates so they surface exactly when you need them."
        else -> "Capture new $filter and they will securely live in this space."
    }
    RootEmptyArt(
        icon = icon,
        headline = headline,
        body = body,
        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        onContainerColor = MaterialTheme.colorScheme.onTertiaryContainer,
    )
}

@Composable
private fun ExpressiveMemoryItem(
    headline: String,
    supporting: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 600f),
        label = "memory_item_scale"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        interactionSource = interactionSource,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = headline,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = supporting,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MemoriesScreen(vm: AppViewModel, nav: MindNavController) {
    // -------------------------------------------------------------------------
    // DATA AND STATE FLOW (UNTOUCHED)
    // -------------------------------------------------------------------------
    val repo = vm.repository
    val memories by repo.memories.active().collectAsStateWithLifecycle(initialValue = emptyList())
    var filter by remember { mutableStateOf("All") }
    var query by remember { mutableStateOf("") }

    val filtered = memories
        .filter { m ->
            when (filter) {
                "Fixes" -> m.type == MemoryType.FIX
                "People" -> m.type == MemoryType.PERSON
                "Ideas" -> m.type == MemoryType.IDEA
                "Places" -> m.type == MemoryType.PLACE
                else -> true
            }
        }
        .filter { m ->
            query.isBlank() ||
                m.title.contains(query, ignoreCase = true) ||
                m.content.contains(query, ignoreCase = true) ||
                m.tags.contains(query, ignoreCase = true)
        }
    // -------------------------------------------------------------------------

    TabScaffold(
        nav = nav,
        selected = MindScreen.MEMORIES,
        topBar = { MindTopBar(title = "Memories") },
        fab = { QuickCaptureFab(nav) },
    ) { _ ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
            // Pinned Top Section: Search Bar and Chips
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search memories...") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                    ),
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ConnectedChipGroup(
                        options = listOf("All", "Fixes", "People", "Ideas", "Places"),
                        selected = filter,
                        onSelect = { filter = it }
                    )
                }
            }

            // Animated List Section below the pinned headers
            AnimatedContent(
                targetState = filter, // Animate crossfade only when filter changes for smoothness
                transitionSpec = {
                    (fadeIn(animationSpec = tween(250, delayMillis = 50)) +
                        scaleIn(initialScale = 0.95f, animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f)))
                        .togetherWith(fadeOut(animationSpec = tween(150)))
                },
                label = "memories_tab_transition",
                modifier = Modifier.weight(1f)
            ) { targetFilter ->
                // Key the content on the target filter so each filter's list has
                // its own identity for AnimatedContent, and the target-state
                // parameter is actually used by the transition.
                key(targetFilter) {
                    // The filtered list already reflects the selected filter and query.
                    if (filtered.isEmpty()) {
                        Spacer(modifier = Modifier.fillMaxSize())
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 120.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(filtered, key = { it.id }) { m ->
                                val tags = m.tags.split(" ").filter { it.startsWith("#") }.joinToString(" ")
                                ExpressiveMemoryItem(
                                    headline = m.title,
                                    supporting = "${typeLabel(m.type)}${if (tags.isNotBlank()) " · $tags" else ""}",
                                    icon = typeIcon(m.type),
                                    onClick = { nav.navigate(MindScreen.MEMORY_DETAIL, MindTransition.SLIDE_RIGHT, m.id.toString()) }
                                )
                            }
                        }
                    }
                }
            }

        }

        if (filtered.isEmpty()) {
            RootEmptyState(stateKey = if (query.isNotBlank()) "search" else filter) {
                EmptyMemoriesArt(filter = filter, query = query)
            }
        }
    }
}
}
