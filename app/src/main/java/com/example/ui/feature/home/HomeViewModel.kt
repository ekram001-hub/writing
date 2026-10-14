package com.example.ui.feature.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.demo.DemoTopicPack
import com.example.data.local.database.MicroWriteDatabase
import com.example.data.local.entity.PracticeSessionEntity
import com.example.data.local.entity.TopicEntity
import com.example.data.local.entity.TopicProgressEntity
import com.example.data.repository.AppSettings
import com.example.data.repository.ConflictResolution
import com.example.data.repository.PracticeRepository
import com.example.data.repository.SettingsRepository
import com.example.data.repository.TopicRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val totalTopicsCount: Int = 0,
    val completedTopicsCount: Int = 0,
    val bookmarkedTopicsCount: Int = 0,
    val todayCompletedSessionsCount: Int = 0,
    val dailyGoalSessions: Int = 2,
    val activeSession: TopicProgressEntity? = null,
    val activeTopic: TopicEntity? = null,
    val recentSessions: List<PracticeSessionEntity> = emptyList(),
    val recentTopics: List<TopicEntity> = emptyList(),
    val isLoading: Boolean = false,
    val userMessage: String? = null
)

private data class HomeCoreStats(
    val allTopics: List<TopicEntity>,
    val bookmarkedCount: Int,
    val completedTopics: Int,
    val todayCompleted: Int,
    val settings: AppSettings
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val database = MicroWriteDatabase.getDatabase(application)
    private val topicRepository = TopicRepository(database)
    private val practiceRepository = PracticeRepository(database)
    private val settingsRepository = SettingsRepository(application)

    private val _userMessage = MutableStateFlow<String?>(null)

    private val coreStatsFlow: Flow<HomeCoreStats> = combine(
        topicRepository.getAllTopics(),
        topicRepository.getBookmarkedCount(),
        practiceRepository.observeCompletedTopicsCount(),
        practiceRepository.getTodayCompletedSessionsCount(),
        settingsRepository.settingsFlow
    ) { allTopics, bookmarkedCount, completedTopics, todayCompleted, settings ->
        HomeCoreStats(allTopics, bookmarkedCount, completedTopics, todayCompleted, settings)
    }

    val uiState: StateFlow<HomeUiState> = combine(
        coreStatsFlow,
        practiceRepository.observeLatestActiveSession(),
        practiceRepository.getRecentSessions(7),
        _userMessage
    ) { stats, activeProgress, recentSessions, msg ->
        val activeTopic = if (activeProgress != null) {
            stats.allTopics.find { it.topicId == activeProgress.topicId }
        } else null

        val recentTopicIds = recentSessions.map { it.topicId }.distinct()
        val recentTopics = recentTopicIds.mapNotNull { tId -> stats.allTopics.find { it.topicId == tId } }

        HomeUiState(
            totalTopicsCount = stats.allTopics.size,
            completedTopicsCount = stats.completedTopics,
            bookmarkedTopicsCount = stats.bookmarkedCount,
            todayCompletedSessionsCount = stats.todayCompleted,
            dailyGoalSessions = stats.settings.dailyGoalSessions,
            activeSession = activeProgress,
            activeTopic = activeTopic,
            recentSessions = recentSessions.take(5),
            recentTopics = recentTopics.take(3),
            userMessage = msg
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState(isLoading = true)
    )

    fun importDemoPack() {
        viewModelScope.launch {
            val outcome = topicRepository.importTopicPack(DemoTopicPack.samplePack, ConflictResolution.UPDATE)
            _userMessage.value = outcome.message
        }
    }

    fun dismissUserMessage() {
        _userMessage.value = null
    }
}
