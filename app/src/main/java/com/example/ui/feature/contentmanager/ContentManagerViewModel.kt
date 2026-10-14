package com.example.ui.feature.contentmanager

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.demo.DemoTopicPack
import com.example.data.local.database.MicroWriteDatabase
import com.example.data.local.entity.TopicEntity
import com.example.data.model.FullBackupData
import com.example.data.model.TopicPack
import com.example.data.repository.BackupRepository
import com.example.data.repository.ConflictResolution
import com.example.data.repository.TopicRepository
import com.example.data.validation.ParsedContentType
import com.example.data.validation.TopicPackValidator
import com.example.data.validation.ValidationResult
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ContentManagerUiState(
    val rawJsonInput: String = "",
    val validationResult: ValidationResult? = null,
    val parsedContentType: ParsedContentType = ParsedContentType.UNKNOWN,
    val parsedTopics: List<TopicPack> = emptyList(),
    val conflictResolution: ConflictResolution = ConflictResolution.UPDATE,
    val existingTopics: List<TopicEntity> = emptyList(),
    val isImporting: Boolean = false,
    val exportedJson: String? = null,
    val exportTitle: String? = null,
    val lastImportMessage: String? = null,
    val userMessage: String? = null,
    val isLoading: Boolean = false
)

private data class ValidationInputBundle(
    val input: String,
    val vResult: ValidationResult?,
    val cType: ParsedContentType,
    val pTopics: List<TopicPack>,
    val resolution: ConflictResolution
)

class ContentManagerViewModel(application: Application) : AndroidViewModel(application) {
    private val database = MicroWriteDatabase.getDatabase(application)
    private val topicRepository = TopicRepository(database)
    private val backupRepository = BackupRepository(database, topicRepository)
    private val validator = TopicPackValidator()
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val backupAdapter = moshi.adapter(FullBackupData::class.java)

    private val _rawJsonInput = MutableStateFlow("")
    private val _validationResult = MutableStateFlow<ValidationResult?>(null)
    private val _parsedContentType = MutableStateFlow(ParsedContentType.UNKNOWN)
    private val _parsedTopics = MutableStateFlow<List<TopicPack>>(emptyList())
    private val _conflictResolution = MutableStateFlow(ConflictResolution.UPDATE)
    private val _isImporting = MutableStateFlow(false)
    private val _exportedJson = MutableStateFlow<String?>(null)
    private val _exportTitle = MutableStateFlow<String?>(null)
    private val _lastImportMessage = MutableStateFlow<String?>(null)
    private val _userMessage = MutableStateFlow<String?>(null)

    private val validationInputFlow: Flow<ValidationInputBundle> = combine(
        _rawJsonInput,
        _validationResult,
        _parsedContentType,
        _parsedTopics,
        _conflictResolution
    ) { input, vResult, cType, pTopics, resolution ->
        ValidationInputBundle(input, vResult, cType, pTopics, resolution)
    }

    val uiState: StateFlow<ContentManagerUiState> = combine(
        topicRepository.getAllTopics(),
        validationInputFlow,
        _isImporting,
        _lastImportMessage
    ) { allTopics, vBundle, importing, lastMsg ->
        ContentManagerUiState(
            rawJsonInput = vBundle.input,
            validationResult = vBundle.vResult,
            parsedContentType = vBundle.cType,
            parsedTopics = vBundle.pTopics,
            conflictResolution = vBundle.resolution,
            existingTopics = allTopics,
            isImporting = importing,
            exportedJson = _exportedJson.value,
            exportTitle = _exportTitle.value,
            lastImportMessage = lastMsg,
            userMessage = _userMessage.value
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ContentManagerUiState(isLoading = true)
    )

    fun onJsonInputChanged(input: String) {
        _rawJsonInput.value = input
        validateCurrentInput(input)
    }

    fun validateCurrentInput(input: String) {
        if (input.isBlank()) {
            _validationResult.value = null
            _parsedContentType.value = ParsedContentType.UNKNOWN
            _parsedTopics.value = emptyList()
            return
        }

        val (payload, topics) = validator.inspectAndValidate(input)
        _validationResult.value = payload.validationResult
        _parsedContentType.value = payload.type
        _parsedTopics.value = topics
    }

    fun setConflictResolution(resolution: ConflictResolution) {
        _conflictResolution.value = resolution
    }

    fun importParsedContent() {
        val topics = _parsedTopics.value
        val cType = _parsedContentType.value
        val raw = _rawJsonInput.value

        if (topics.isEmpty() && cType != ParsedContentType.FULL_BACKUP) {
            _userMessage.value = "No valid topics to import."
            return
        }

        viewModelScope.launch {
            _isImporting.value = true
            try {
                if (cType == ParsedContentType.FULL_BACKUP) {
                    val backup = backupAdapter.fromJson(raw)
                    if (backup != null) {
                        val outcome = backupRepository.restoreFullBackup(backup)
                        _lastImportMessage.value = outcome.message
                        _userMessage.value = outcome.message
                        clearInput()
                    } else {
                        _userMessage.value = "Failed to deserialize backup."
                    }
                } else {
                    val outcome = topicRepository.importTopicBatch(topics, _conflictResolution.value)
                    _lastImportMessage.value = outcome.message
                    _userMessage.value = outcome.message
                    clearInput()
                }
            } catch (e: Exception) {
                _userMessage.value = "Import error: ${e.message}"
            } finally {
                _isImporting.value = false
            }
        }
    }

    fun importDemoPack() {
        viewModelScope.launch {
            val outcome = topicRepository.importTopicPack(DemoTopicPack.samplePack, ConflictResolution.UPDATE)
            _lastImportMessage.value = outcome.message
            _userMessage.value = outcome.message
        }
    }

    fun exportCompleteLibrary() {
        viewModelScope.launch {
            val json = backupRepository.exportAllTopics()
            _exportedJson.value = json
            _exportTitle.value = "Topic Library Export (JSON Array)"
        }
    }

    fun exportFullBackup() {
        viewModelScope.launch {
            val json = backupRepository.exportFullBackup()
            _exportedJson.value = json
            _exportTitle.value = "Full Backup (Content, Sessions, Progress)"
        }
    }

    fun clearExportedJson() {
        _exportedJson.value = null
        _exportTitle.value = null
    }

    fun deleteTopic(topicId: String) {
        viewModelScope.launch {
            topicRepository.deleteTopic(topicId)
            _userMessage.value = "Topic '$topicId' removed."
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            topicRepository.clearAllData()
            _userMessage.value = "All topics and progress data deleted."
        }
    }

    fun clearInput() {
        _rawJsonInput.value = ""
        _validationResult.value = null
        _parsedContentType.value = ParsedContentType.UNKNOWN
        _parsedTopics.value = emptyList()
    }

    fun dismissUserMessage() {
        _userMessage.value = null
    }
}
