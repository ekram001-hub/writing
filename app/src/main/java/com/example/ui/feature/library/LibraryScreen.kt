package com.example.ui.feature.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.DifficultyBadge
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.LanguageBadge
import com.example.ui.components.ProgressStatusBadge

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    onTopicSelected: (String) -> Unit,
    onNavigateToManage: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var showSortMenu by remember { mutableStateOf(false) }
    var showCategoryMenu by remember { mutableStateOf(false) }
    var showDifficultyMenu by remember { mutableStateOf(false) }
    var showStatusMenu by remember { mutableStateOf(false) }

    val hasActiveFilters = state.searchQuery.isNotEmpty() ||
            state.selectedCategory != "All" ||
            state.selectedLanguage != "All" ||
            state.selectedWritingType != "All" ||
            state.selectedDifficulty != "All" ||
            state.selectedStatus != "All" ||
            state.bookmarkedOnly

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Topic Library",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                actions = {
                    Box {
                        IconButton(
                            onClick = { showSortMenu = true },
                            modifier = Modifier.testTag("sort_menu_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Sort,
                                contentDescription = "Sort topics"
                            )
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            SortOption.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.label) },
                                    onClick = {
                                        viewModel.onSortChange(option)
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search Bar
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                placeholder = { Text("Search English/বাংলা, category, tags...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "Search"
                    )
                },
                trailingIcon = {
                    if (state.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Filled.Clear,
                                contentDescription = "Clear search"
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .testTag("library_search_field")
            )

            // Filter Chips Scrollable Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Bookmarked filter
                FilterChip(
                    selected = state.bookmarkedOnly,
                    onClick = { viewModel.toggleBookmarkedOnly() },
                    label = { Text("Bookmarked") },
                    leadingIcon = {
                        Icon(
                            imageVector = if (state.bookmarkedOnly) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier.testTag("filter_bookmark")
                )

                // Category filter chip with menu
                Box {
                    FilterChip(
                        selected = state.selectedCategory != "All",
                        onClick = { showCategoryMenu = true },
                        label = { Text(if (state.selectedCategory == "All") "Category" else state.selectedCategory) },
                        modifier = Modifier.testTag("filter_category")
                    )
                    DropdownMenu(
                        expanded = showCategoryMenu,
                        onDismissRequest = { showCategoryMenu = false }
                    ) {
                        state.availableCategories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    viewModel.onCategoryChange(cat)
                                    showCategoryMenu = false
                                }
                            )
                        }
                    }
                }

                // Difficulty filter chip with menu
                Box {
                    FilterChip(
                        selected = state.selectedDifficulty != "All",
                        onClick = { showDifficultyMenu = true },
                        label = { Text(if (state.selectedDifficulty == "All") "Difficulty" else state.selectedDifficulty) },
                        modifier = Modifier.testTag("filter_difficulty")
                    )
                    DropdownMenu(
                        expanded = showDifficultyMenu,
                        onDismissRequest = { showDifficultyMenu = false }
                    ) {
                        listOf("All", "Beginner", "Intermediate", "Advanced").forEach { diff ->
                            DropdownMenuItem(
                                text = { Text(diff) },
                                onClick = {
                                    viewModel.onDifficultyChange(diff)
                                    showDifficultyMenu = false
                                }
                            )
                        }
                    }
                }

                // Status filter chip with menu
                Box {
                    FilterChip(
                        selected = state.selectedStatus != "All",
                        onClick = { showStatusMenu = true },
                        label = { Text(if (state.selectedStatus == "All") "Status" else state.selectedStatus.replace("_", " ")) },
                        modifier = Modifier.testTag("filter_status")
                    )
                    DropdownMenu(
                        expanded = showStatusMenu,
                        onDismissRequest = { showStatusMenu = false }
                    ) {
                        listOf("All", "NOT_STARTED", "IN_PROGRESS", "COMPLETED").forEach { s ->
                            DropdownMenuItem(
                                text = { Text(if (s == "All") "All Statuses" else s.replace("_", " ")) },
                                onClick = {
                                    viewModel.onStatusChange(s)
                                    showStatusMenu = false
                                }
                            )
                        }
                    }
                }

                if (hasActiveFilters) {
                    TextButton(
                        onClick = { viewModel.clearAllFilters() },
                        modifier = Modifier.testTag("clear_filters_button")
                    ) {
                        Text("Clear All", fontSize = 12.sp)
                    }
                }
            }

            // Results count and active sort label
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${state.topics.size} topic${if (state.topics.size == 1) "" else "s"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Sorted by: ${state.sortOption.label}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Topic List or Empty State
            if (state.topics.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (hasActiveFilters) {
                        EmptyStateCard(
                            icon = Icons.Filled.FilterList,
                            title = "No Topics Found",
                            description = "No topics match your current search and filter combination.",
                            actionText = "Clear Filters",
                            onActionClick = { viewModel.clearAllFilters() }
                        )
                    } else {
                        EmptyStateCard(
                            icon = Icons.Filled.Search,
                            title = "Library Is Empty",
                            description = "You haven't imported any writing topics yet. Import a topic pack to start practicing.",
                            actionText = "Import Topic Pack",
                            onActionClick = onNavigateToManage
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("topic_list"),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.topics, key = { it.entity.topicId }) { item ->
                        val topic = item.entity
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onTopicSelected(topic.topicId) }
                                .testTag("topic_item_${topic.topicId}"),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(12.dp),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                // Category, Badges, Bookmark Action
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = topic.category,
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        DifficultyBadge(difficulty = topic.difficulty)
                                        LanguageBadge(language = topic.language)
                                    }
                                    IconButton(
                                        onClick = { viewModel.toggleBookmark(topic.topicId, topic.isBookmarked) },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .testTag("bookmark_button_${topic.topicId}")
                                    ) {
                                        Icon(
                                            imageVector = if (topic.isBookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                                            contentDescription = if (topic.isBookmarked) "Unbookmark" else "Bookmark",
                                            tint = if (topic.isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // English Title
                                Text(
                                    text = topic.title,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        lineHeight = 22.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                // Bengali Title
                                if (!topic.titleBn.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = topic.titleBn,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            lineHeight = 20.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Writing types tags
                                if (item.writingTypes.isNotEmpty()) {
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        item.writingTypes.forEach { wType ->
                                            Box(
                                                modifier = Modifier
                                                    .background(
                                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                                        shape = RoundedCornerShape(4.dp)
                                                    )
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = wType.replace("_", " ").replaceFirstChar { it.uppercase() },
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                }

                                // Bottom Row: Estimated minutes & Progress
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Est. ${topic.estimatedMinutes} min practice",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    ProgressStatusBadge(status = item.progressStatus)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
