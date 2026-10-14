package com.example.ui.feature.topicdetails

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.database.MicroWriteDatabase
import com.example.data.local.entity.SectionEntity
import com.example.data.local.entity.SectionProgressEntity
import com.example.data.local.entity.TopicEntity
import com.example.data.local.entity.TopicProgressEntity
import com.example.data.repository.BackupRepository
import com.example.data.repository.PracticeRepository
import com.example.data.repository.TopicRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SectionItemUiModel(
    val entity: SectionEntity,
    val isReviewed: Boolean,
    val isCompleted: Boolean,
    val isBookmarked: Boolean
)

data class TopicDetailsUiState(
    val topic: TopicEntity? = null,
    val sections: List<SectionItemUiModel> = emptyList(),
    val availableWritingModes: List<String> = emptyList(),
    val selectedWritingMode: String = "english_focus",
    val topicProgress: TopicProgressEntity? = null,
    val reviewedSectionsCount: Int = 0,
    val totalSectionsCount: Int = 0,
    val tags: List<String> = emptyList(),
    val exportedJson: String? = null,
    val userMessage: String? = null,
    val isLoading: Boolean = false
)

private data class TopicDataBundle(
    val topic: TopicEntity,
    val rawSections: List<SectionEntity>,
    val activeMode: String
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class TopicDetailsViewModel(
    application: Application,
    private val topicId: String
) : AndroidViewModel(application) {
    private val database = MicroWriteDatabase.getDatabase(application)
    private val topicRepository = TopicRepository(database)
    private val practiceRepository = PracticeRepository(database)
    private val backupRepository = BackupRepository(database, topicRepository)

    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val stringListAdapter = moshi.adapter<List<String>>(
        Types.newParameterizedType(List::class.java, String::class.java)
    )

    private val _selectedWritingMode = MutableStateFlow<String?>(null)
    private val _exportedJson = MutableStateFlow<String?>(null)
    private val _userMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<TopicDetailsUiState> = combine(
        topicRepository.getTopicById(topicId),
        topicRepository.getSections(topicId),
        _selectedWritingMode
    ) { topic, rawSections, modeOverride ->
        if (topic == null) null
        else {
            val writingModes = try {
                stringListAdapter.fromJson(topic.writingTypesJson) ?: listOf("english_focus")
            } catch (e: Exception) {
                listOf("english_focus")
            }
            val activeMode = modeOverride ?: writingModes.firstOrNull() ?: "english_focus"
            TopicDataBundle(topic, rawSections, activeMode)
        }
    }.filterNotNull().flatMapLatest { bundle ->
        combine(
            practiceRepository.observeTopicProgress(bundle.topic.topicId, bundle.activeMode),
            practiceRepository.observeSectionProgressList(bundle.topic.topicId, bundle.activeMode),
            _exportedJson,
            _userMessage
        ) { progress, sectionProgressList, exportJson, msg ->
            val progressMap = sectionProgressList.associateBy { it.sectionId }

            val sectionItems = bundle.rawSections.map { s ->
                val sProg = progressMap[s.sectionId]
                SectionItemUiModel(
                    entity = s,
                    isReviewed = sProg?.reviewed ?: false,
                    isCompleted = sProg?.completed ?: false,
                    isBookmarked = sProg?.isBookmarked ?: false
                )
            }

            val reviewedCount = sectionItems.count { it.isReviewed || it.isCompleted }
            val tags = try {
                stringListAdapter.fromJson(bundle.topic.tagsJson) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }

            val writingModes = try {
                stringListAdapter.fromJson(bundle.topic.writingTypesJson) ?: listOf("english_focus")
            } catch (e: Exception) {
                listOf("english_focus")
            }

            TopicDetailsUiState(
                topic = bundle.topic,
                sections = sectionItems,
                availableWritingModes = writingModes,
                selectedWritingMode = bundle.activeMode,
                topicProgress = progress,
                reviewedSectionsCount = reviewedCount,
                totalSectionsCount = sectionItems.size,
                tags = tags,
                exportedJson = exportJson,
                userMessage = msg
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TopicDetailsUiState(isLoading = true)
    )

    fun onWritingModeChanged(mode: String) {
        _selectedWritingMode.value = mode
    }

    fun toggleBookmark() {
        val currentTopic = uiState.value.topic ?: return
        viewModelScope.launch {
            topicRepository.setBookmark(currentTopic.topicId, !currentTopic.isBookmarked)
        }
    }

    fun exportTopicJson() {
        viewModelScope.launch {
            val json = backupRepository.exportSingleTopic(topicId)
            _exportedJson.value = json
        }
    }

    fun clearExportedJson() {
        _exportedJson.value = null
    }

    fun dismissUserMessage() {
        _userMessage.value = null
    }
}
