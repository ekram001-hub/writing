package com.example.ui.feature.practice

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.database.MicroWriteDatabase
import com.example.data.local.entity.SectionEntity
import com.example.data.local.entity.TopicEntity
import com.example.data.model.SectionPack
import com.example.data.repository.AppSettings
import com.example.data.repository.PracticeRepository
import com.example.data.repository.SettingsRepository
import com.example.data.repository.TopicRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

enum class TimerMode(val totalSeconds: Long, val label: String) {
    OFF(0, "Stopwatch"),
    ONE_MIN(60, "1 Min"),
    TWO_MIN(120, "2 Min"),
    THREE_MIN(180, "3 Min"),
    CUSTOM(300, "Custom");

    companion object {
        fun fromMinutes(minutes: Int): TimerMode {
            return when (minutes) {
                0 -> OFF
                1 -> ONE_MIN
                2 -> TWO_MIN
                3 -> THREE_MIN
                else -> CUSTOM
            }
        }
    }
}

data class PracticeUiState(
    val topicId: String = "",
    val writingMode: String = "english_focus",
    val topic: TopicEntity? = null,
    val sections: List<SectionEntity> = emptyList(),
    val parsedSections: Map<String, SectionPack> = emptyMap(),
    val currentSectionIndex: Int = 0,
    val currentSectionId: String = "",
    val reviewedSectionIds: Set<String> = emptySet(),
    val bookmarkedSectionIds: Set<String> = emptySet(),
    val isSequentialMode: Boolean = true,
    // Timer states
    val timerMode: TimerMode = TimerMode.TWO_MIN,
    val isTimerRunning: Boolean = false,
    val remainingSeconds: Long = 120,
    val elapsedSessionSeconds: Long = 0,
    val isOvertime: Boolean = false,
    // Checklist
    val checklistState: Map<String, Boolean> = emptyMap(),
    val requireAllChecklist: Boolean = false,
    val showFinishDialog: Boolean = false,
    val sessionFinished: Boolean = false,
    val userMessage: String? = null,
    val isLoading: Boolean = false
)

class PracticeViewModel(
    application: Application,
    private val initialTopicId: String,
    private val initialWritingMode: String,
    private val initialSectionId: String
) : AndroidViewModel(application) {
    private val database = MicroWriteDatabase.getDatabase(application)
    private val topicRepository = TopicRepository(database)
    private val practiceRepository = PracticeRepository(database)
    private val settingsRepository = SettingsRepository(application)
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val sectionPackAdapter = moshi.adapter(SectionPack::class.java)

    private val _uiState = MutableStateFlow(
        PracticeUiState(
            topicId = initialTopicId,
            writingMode = initialWritingMode,
            currentSectionId = initialSectionId,
            isLoading = true
        )
    )
    val uiState: StateFlow<PracticeUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var lastMonotonicTime: Long = 0L
    private val sessionStartTime = System.currentTimeMillis()
    private val visitedSectionIds = mutableSetOf<String>()

    init {
        loadPracticeData()
    }

    private fun loadPracticeData() {
        viewModelScope.launch {
            val settings = settingsRepository.settingsFlow.first()
            val topic = topicRepository.getTopicByIdSync(initialTopicId)
            val rawSections = topicRepository.getSectionsSync(initialTopicId)

            val parsedMap = rawSections.associate { s ->
                val pack = try {
                    sectionPackAdapter.fromJson(s.contentJson) ?: SectionPack(
                        sectionId = s.sectionId,
                        type = s.type,
                        title = s.title,
                        order = s.orderIndex,
                        durationSeconds = s.durationSeconds,
                        prompt = s.prompt,
                        requiredForCompletion = s.requiredForCompletion
                    )
                } catch (e: Exception) {
                    SectionPack(
                        sectionId = s.sectionId,
                        type = s.type,
                        title = s.title,
                        order = s.orderIndex,
                        durationSeconds = s.durationSeconds,
                        prompt = s.prompt,
                        requiredForCompletion = s.requiredForCompletion
                    )
                }
                s.sectionId to pack
            }

            // Existing section progress
            val sectionProgress = practiceRepository.observeSectionProgressList(initialTopicId, initialWritingMode).first()
            val reviewed = sectionProgress.filter { it.reviewed || it.completed }.map { it.sectionId }.toSet()
            val bookmarked = sectionProgress.filter { it.isBookmarked }.map { it.sectionId }.toSet()

            // Initialize or resume session
            val activeSectionId = if (initialSectionId.isNotBlank()) initialSectionId
            else rawSections.firstOrNull()?.sectionId ?: ""

            val activeIndex = rawSections.indexOfFirst { it.sectionId == activeSectionId }.coerceAtLeast(0)
            if (activeSectionId.isNotBlank()) {
                visitedSectionIds.add(activeSectionId)
            }

            practiceRepository.startOrResumeSession(initialTopicId, initialWritingMode, activeSectionId)

            val defaultTimerMode = TimerMode.fromMinutes(settings.defaultTimerMinutes)
            val defaultSeconds = if (defaultTimerMode == TimerMode.OFF) 0L else defaultTimerMode.totalSeconds

            _uiState.value = PracticeUiState(
                topicId = initialTopicId,
                writingMode = initialWritingMode,
                topic = topic,
                sections = rawSections,
                parsedSections = parsedMap,
                currentSectionIndex = activeIndex,
                currentSectionId = activeSectionId,
                reviewedSectionIds = reviewed,
                bookmarkedSectionIds = bookmarked,
                isSequentialMode = settings.defaultNavMode != "FREE",
                timerMode = defaultTimerMode,
                remainingSeconds = defaultSeconds,
                requireAllChecklist = settings.requireAllChecklistItems,
                isLoading = false
            )
        }
    }

    fun onSectionSelected(targetIndex: Int) {
        val state = _uiState.value
        if (targetIndex in state.sections.indices) {
            val targetSection = state.sections[targetIndex]
            visitedSectionIds.add(targetSection.sectionId)
            _uiState.value = state.copy(
                currentSectionIndex = targetIndex,
                currentSectionId = targetSection.sectionId
            )
            viewModelScope.launch {
                practiceRepository.updateCurrentSection(state.topicId, state.writingMode, targetSection.sectionId)
            }
        }
    }

    fun onNextSection() {
        val state = _uiState.value
        if (state.currentSectionIndex < state.sections.size - 1) {
            onSectionSelected(state.currentSectionIndex + 1)
        } else {
            // Reached end of sections: prompt finish
            _uiState.value = state.copy(showFinishDialog = true)
        }
    }

    fun onPreviousSection() {
        val state = _uiState.value
        if (state.currentSectionIndex > 0) {
            onSectionSelected(state.currentSectionIndex - 1)
        }
    }

    fun toggleSectionReviewed() {
        val state = _uiState.value
        val currentId = state.currentSectionId
        val isCurrentlyReviewed = state.reviewedSectionIds.contains(currentId)
        val newReviewed = if (isCurrentlyReviewed) {
            state.reviewedSectionIds - currentId
        } else {
            state.reviewedSectionIds + currentId
        }

        _uiState.value = state.copy(reviewedSectionIds = newReviewed)

        viewModelScope.launch {
            practiceRepository.setSectionReviewed(
                state.topicId,
                state.writingMode,
                currentId,
                !isCurrentlyReviewed
            )
        }
    }

    fun toggleSectionBookmark() {
        val state = _uiState.value
        val currentId = state.currentSectionId
        val isCurrentlyBookmarked = state.bookmarkedSectionIds.contains(currentId)
        val newBookmarked = if (isCurrentlyBookmarked) {
            state.bookmarkedSectionIds - currentId
        } else {
            state.bookmarkedSectionIds + currentId
        }

        _uiState.value = state.copy(bookmarkedSectionIds = newBookmarked)

        viewModelScope.launch {
            practiceRepository.toggleSectionBookmark(
                state.topicId,
                state.writingMode,
                currentId,
                !isCurrentlyBookmarked
            )
        }
    }

    fun toggleNavMode() {
        val state = _uiState.value
        _uiState.value = state.copy(isSequentialMode = !state.isSequentialMode)
    }

    // Timer Controls
    fun setTimerMode(mode: TimerMode, customSeconds: Long = 300) {
        pauseTimer()
        val totalSecs = if (mode == TimerMode.CUSTOM) customSeconds else mode.totalSeconds
        _uiState.value = _uiState.value.copy(
            timerMode = mode,
            remainingSeconds = totalSecs,
            isOvertime = false
        )
    }

    fun startOrResumeTimer() {
        if (_uiState.value.isTimerRunning) return
        lastMonotonicTime = SystemClock.elapsedRealtime()
        _uiState.value = _uiState.value.copy(isTimerRunning = true)

        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_uiState.value.isTimerRunning) {
                delay(1000)
                val now = SystemClock.elapsedRealtime()
                val delta = (now - lastMonotonicTime) / 1000
                if (delta >= 1) {
                    lastMonotonicTime = now
                    val current = _uiState.value
                    val newElapsed = current.elapsedSessionSeconds + delta

                    if (current.timerMode == TimerMode.OFF) {
                        _uiState.value = current.copy(
                            elapsedSessionSeconds = newElapsed,
                            remainingSeconds = newElapsed
                        )
                    } else {
                        val newRemaining = current.remainingSeconds - delta
                        val isOvertime = newRemaining <= 0
                        _uiState.value = current.copy(
                            elapsedSessionSeconds = newElapsed,
                            remainingSeconds = if (isOvertime) kotlin.math.abs(newRemaining) else newRemaining,
                            isOvertime = isOvertime
                        )
                    }
                }
            }
        }
    }

    fun pauseTimer() {
        _uiState.value = _uiState.value.copy(isTimerRunning = false)
        timerJob?.cancel()
        timerJob = null
    }

    fun resetTimer() {
        pauseTimer()
        val defaultSecs = if (_uiState.value.timerMode == TimerMode.OFF) 0L
        else _uiState.value.timerMode.totalSeconds
        _uiState.value = _uiState.value.copy(
            remainingSeconds = defaultSecs,
            isOvertime = false
        )
    }

    // Checklist toggles
    fun toggleChecklistItem(id: String) {
        val current = _uiState.value.checklistState
        val isChecked = current[id] ?: false
        _uiState.value = _uiState.value.copy(
            checklistState = current + (id to !isChecked)
        )
    }

    fun openFinishDialog() {
        _uiState.value = _uiState.value.copy(showFinishDialog = true)
    }

    fun dismissFinishDialog() {
        _uiState.value = _uiState.value.copy(showFinishDialog = false)
    }

    fun completeSession(markTopicCompleted: Boolean) {
        pauseTimer()
        val state = _uiState.value
        val defaultCriteria = listOf(
            Pair("crit-1", "My main argument is clear and directly addresses the prompt."),
            Pair("crit-2", "My writing has a logical paragraph structure (Intro, Evidence, Conclusion)."),
            Pair("crit-3", "My arguments are supported by verified statistics and facts."),
            Pair("crit-4", "Grammar, spelling, and sentence coherence are acceptable."),
            Pair("crit-5", "Central banking terms and economic phrases were appropriately utilized.")
        )

        val checklistAnswers = defaultCriteria.associate { (cId, cText) ->
            cId to Pair(cText, state.checklistState[cId] ?: false)
        }

        viewModelScope.launch {
            val sessionId = UUID.randomUUID().toString()
            practiceRepository.finishPracticeSession(
                sessionId = sessionId,
                topicId = state.topicId,
                writingMode = state.writingMode,
                startedAt = sessionStartTime,
                durationSeconds = state.elapsedSessionSeconds,
                sectionsVisited = visitedSectionIds.size,
                sectionsReviewed = state.reviewedSectionIds.size,
                checklistAnswers = checklistAnswers,
                markTopicCompleted = markTopicCompleted
            )
            _uiState.value = state.copy(
                showFinishDialog = false,
                sessionFinished = true,
                userMessage = "Practice session recorded successfully!"
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        pauseTimer()
    }
}
