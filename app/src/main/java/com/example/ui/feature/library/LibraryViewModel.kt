package com.example.ui.feature.library

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.database.MicroWriteDatabase
import com.example.data.local.entity.TopicEntity
import com.example.data.repository.TopicRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SortOption(val label: String) {
    RECENTLY_ADDED("Recently Added"),
    TITLE_ASC("Title (A–Z)"),
    TITLE_DESC("Title (Z–A)"),
    DIFFICULTY("Difficulty"),
    DURATION("Duration")
}

data class TopicDisplayItem(
    val entity: TopicEntity,
    val tags: List<String>,
    val writingTypes: List<String>,
    val sectionCount: Int,
    val progressStatus: String // "NOT_STARTED", "IN_PROGRESS", "COMPLETED"
)

data class LibraryFilters(
    val query: String = "",
    val category: String = "All",
    val language: String = "All",
    val writingType: String = "All",
    val difficulty: String = "All",
    val status: String = "All",
    val bookmarkedOnly: Boolean = false,
    val sortOption: SortOption = SortOption.RECENTLY_ADDED
)

data class LibraryUiState(
    val searchQuery: String = "",
    val selectedCategory: String = "All",
    val selectedLanguage: String = "All",
    val selectedWritingType: String = "All",
    val selectedDifficulty: String = "All",
    val selectedStatus: String = "All",
    val bookmarkedOnly: Boolean = false,
    val sortOption: SortOption = SortOption.RECENTLY_ADDED,
    val availableCategories: List<String> = emptyList(),
    val availableWritingTypes: List<String> = emptyList(),
    val topics: List<TopicDisplayItem> = emptyList(),
    val isLoading: Boolean = false
)

class LibraryViewModel(application: Application) : AndroidViewModel(application) {
    private val database = MicroWriteDatabase.getDatabase(application)
    private val topicRepository = TopicRepository(database)
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val stringListAdapter = moshi.adapter<List<String>>(
        Types.newParameterizedType(List::class.java, String::class.java)
    )

    private val _filters = MutableStateFlow(LibraryFilters())

    val uiState: StateFlow<LibraryUiState> = combine(
        topicRepository.getAllTopics(),
        database.topicProgressDao().getAllCompletedProgress(),
        _filters
    ) { allTopics, progressList, filters ->
        val categories = listOf("All") + allTopics.map { it.category }.filter { it.isNotBlank() }.distinct().sorted()

        val allWritingTypes = allTopics.flatMap { t ->
            try { stringListAdapter.fromJson(t.writingTypesJson) ?: emptyList() } catch (e: Exception) { emptyList() }
        }.distinct().sorted()
        val writingTypes = listOf("All") + allWritingTypes

        val progressMap = mutableMapOf<String, String>()
        progressList.forEach { p ->
            progressMap[p.topicId] = p.status
        }

        var filtered = allTopics.filter { topic ->
            val q = filters.query.trim().lowercase()
            val matchesQuery = if (q.isEmpty()) true else {
                topic.title.lowercase().contains(q) ||
                (topic.titleBn?.lowercase()?.contains(q) == true) ||
                topic.category.lowercase().contains(q) ||
                topic.topicId.lowercase().contains(q) ||
                topic.tagsJson.lowercase().contains(q)
            }

            val matchesCat = if (filters.category == "All") true else topic.category.equals(filters.category, ignoreCase = true)
            val matchesLang = if (filters.language == "All") true else topic.language.equals(filters.language, ignoreCase = true)
            val matchesType = if (filters.writingType == "All") true else {
                val tTypes = try { stringListAdapter.fromJson(topic.writingTypesJson) ?: emptyList() } catch (e: Exception) { emptyList() }
                tTypes.any { it.equals(filters.writingType, ignoreCase = true) }
            }
            val matchesDiff = if (filters.difficulty == "All") true else topic.difficulty.equals(filters.difficulty, ignoreCase = true)
            val matchesBookmark = if (!filters.bookmarkedOnly) true else topic.isBookmarked
            val topicStatus = progressMap[topic.topicId] ?: "NOT_STARTED"
            val matchesStatus = if (filters.status == "All") true else topicStatus.equals(filters.status, ignoreCase = true)

            matchesQuery && matchesCat && matchesLang && matchesType && matchesDiff && matchesBookmark && matchesStatus
        }

        filtered = when (filters.sortOption) {
            SortOption.RECENTLY_ADDED -> filtered.sortedByDescending { it.createdAt }
            SortOption.TITLE_ASC -> filtered.sortedBy { it.title.lowercase() }
            SortOption.TITLE_DESC -> filtered.sortedByDescending { it.title.lowercase() }
            SortOption.DIFFICULTY -> filtered.sortedBy {
                when (it.difficulty.lowercase()) {
                    "beginner" -> 1
                    "intermediate" -> 2
                    "advanced" -> 3
                    else -> 4
                }
            }
            SortOption.DURATION -> filtered.sortedBy { it.estimatedMinutes }
        }

        val displayItems = filtered.map { topic ->
            val tags = try { stringListAdapter.fromJson(topic.tagsJson) ?: emptyList() } catch (e: Exception) { emptyList() }
            val wTypes = try { stringListAdapter.fromJson(topic.writingTypesJson) ?: emptyList() } catch (e: Exception) { emptyList() }
            val status = progressMap[topic.topicId] ?: "NOT_STARTED"
            TopicDisplayItem(
                entity = topic,
                tags = tags,
                writingTypes = wTypes,
                sectionCount = 0,
                progressStatus = status
            )
        }

        LibraryUiState(
            searchQuery = filters.query,
            selectedCategory = filters.category,
            selectedLanguage = filters.language,
            selectedWritingType = filters.writingType,
            selectedDifficulty = filters.difficulty,
            selectedStatus = filters.status,
            bookmarkedOnly = filters.bookmarkedOnly,
            sortOption = filters.sortOption,
            availableCategories = categories,
            availableWritingTypes = writingTypes,
            topics = displayItems
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LibraryUiState(isLoading = true)
    )

    fun onSearchQueryChange(query: String) {
        _filters.value = _filters.value.copy(query = query)
    }

    fun onCategoryChange(cat: String) {
        _filters.value = _filters.value.copy(category = cat)
    }

    fun onLanguageChange(lang: String) {
        _filters.value = _filters.value.copy(language = lang)
    }

    fun onWritingTypeChange(type: String) {
        _filters.value = _filters.value.copy(writingType = type)
    }

    fun onDifficultyChange(diff: String) {
        _filters.value = _filters.value.copy(difficulty = diff)
    }

    fun onStatusChange(status: String) {
        _filters.value = _filters.value.copy(status = status)
    }

    fun toggleBookmarkedOnly() {
        _filters.value = _filters.value.copy(bookmarkedOnly = !_filters.value.bookmarkedOnly)
    }

    fun onSortChange(sort: SortOption) {
        _filters.value = _filters.value.copy(sortOption = sort)
    }

    fun clearAllFilters() {
        _filters.value = LibraryFilters()
    }

    fun toggleBookmark(topicId: String, current: Boolean) {
        viewModelScope.launch {
            topicRepository.setBookmark(topicId, !current)
        }
    }
}
