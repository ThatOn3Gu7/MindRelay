package com.mindrelay.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mindrelay.data.model.MemoryType
import com.mindrelay.nav.MindNavController
import com.mindrelay.nav.MindScreen
import com.mindrelay.nav.MindTransition
import com.mindrelay.ui.AppViewModel
import com.mindrelay.ui.components.BodyListItem
import com.mindrelay.ui.components.ConnectedChipGroup

private fun typeIcon(type: MemoryType): ImageVector = when (type) {
    MemoryType.FIX -> Icons.Rounded.Build
    MemoryType.PERSON -> Icons.Rounded.Person
    MemoryType.PLACE -> Icons.Rounded.Place
    MemoryType.RECIPE -> Icons.Rounded.Restaurant
    else -> Icons.Rounded.Build
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MemoriesScreen(vm: AppViewModel, nav: MindNavController) {
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

    TabScaffold(
        nav = nav,
        selected = MindScreen.MEMORIES,
        topBar = { MindTopBar(title = "Memories") },
        fab = { QuickCaptureFab(nav) },
    ) { _ ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item {
                // Rounded search bar: leading search icon, trailing tune icon —
                // a stable field styled to the spec (56dp, fully rounded).
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search memories...") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    trailingIcon = { Icon(Icons.Rounded.Tune, contentDescription = null) },
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
            }
            item {
                ConnectedChipGroup(
                    options = listOf("All", "Fixes", "People", "Ideas", "Places"),
                    selected = filter,
                    onSelect = { filter = it },
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
            if (filtered.isEmpty()) {
                item {
                    Text(
                        "No memories yet. Preserve durable knowledge here — with tags and revisit dates.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(filtered, key = { it.id }) { m ->
                    val tags = m.tags.split(" ").filter { it.startsWith("#") }.joinToString(" ")
                    BodyListItem(
                        headline = m.title,
                        supporting = "${typeLabel(m.type)}${if (tags.isNotBlank()) " · $tags" else ""}",
                        icon = typeIcon(m.type),
                        onClick = { nav.navigate(MindScreen.MEMORY_DETAIL, MindTransition.SLIDE_RIGHT, m.id.toString()) },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                }
            }
        }
    }
}
