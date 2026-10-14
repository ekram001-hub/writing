package com.example.data.validation

import com.example.data.model.FullBackupData
import com.example.data.model.SectionPack
import com.example.data.model.SectionType
import com.example.data.model.TopicPack
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class TopicPackValidator {
    val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val topicPackAdapter = moshi.adapter(TopicPack::class.java)
    private val topicListType = Types.newParameterizedType(List::class.java, TopicPack::class.java)
    private val topicListAdapter = moshi.adapter<List<TopicPack>>(topicListType)
    private val backupAdapter = moshi.adapter(FullBackupData::class.java)

    companion object {
        const val MAX_INPUT_SIZE_BYTES = 10 * 1024 * 1024 // 10MB
        const val MAX_TOPICS_PER_BATCH = 100
        const val MAX_SECTIONS_PER_TOPIC = 50
        const val MAX_TITLE_LENGTH = 300
        const val MAX_DURATION_SECONDS = 7200 // 2 hours
    }

    fun inspectAndValidate(jsonString: String): Pair<ParsedPayload, List<TopicPack>> {
        val trimmed = jsonString.trim()
        if (trimmed.isEmpty()) {
            return ParsedPayload(
                ParsedContentType.UNKNOWN,
                ValidationResult(
                    isValid = false,
                    errors = listOf("JSON content is empty.")
                ),
                trimmed
            ) to emptyList()
        }

        if (trimmed.toByteArray(Charsets.UTF_8).size > MAX_INPUT_SIZE_BYTES) {
            return ParsedPayload(
                ParsedContentType.UNKNOWN,
                ValidationResult(
                    isValid = false,
                    errors = listOf("Content exceeds maximum allowed size of 10 MB.")
                ),
                trimmed
            ) to emptyList()
        }

        // Try determining if it's a backup JSON, a single TopicPack, or an array of TopicPacks
        if (trimmed.startsWith("{")) {
            if (trimmed.contains("\"backupVersion\"") && trimmed.contains("\"topics\"")) {
                return validateBackup(trimmed)
            } else {
                return validateSingleTopic(trimmed)
            }
        } else if (trimmed.startsWith("[")) {
            return validateTopicArray(trimmed)
        } else {
            return ParsedPayload(
                ParsedContentType.UNKNOWN,
                ValidationResult(
                    isValid = false,
                    errors = listOf("Content must be a JSON object {...} or JSON array [...].")
                ),
                trimmed
            ) to emptyList()
        }
    }

    private fun validateSingleTopic(rawJson: String): Pair<ParsedPayload, List<TopicPack>> {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        val topic: TopicPack? = try {
            topicPackAdapter.fromJson(rawJson)
        } catch (e: Exception) {
            errors.add("JSON deserialization error: ${e.message}")
            null
        }

        if (topic == null) {
            return ParsedPayload(
                ParsedContentType.TOPIC_PACK,
                ValidationResult(false, errors, warnings),
                rawJson
            ) to emptyList()
        }

        validateTopicPackStructure(topic, errors, warnings)

        val isValid = errors.isEmpty()
        val result = ValidationResult(
            isValid = isValid,
            errors = errors,
            warnings = warnings,
            topicCount = if (isValid) 1 else 0,
            sectionCount = if (isValid) topic.sections.size else 0
        )

        return ParsedPayload(
            ParsedContentType.TOPIC_PACK,
            result,
            rawJson
        ) to if (isValid) listOf(topic) else emptyList()
    }

    private fun validateTopicArray(rawJson: String): Pair<ParsedPayload, List<TopicPack>> {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        val topics: List<TopicPack>? = try {
            topicListAdapter.fromJson(rawJson)
        } catch (e: Exception) {
            errors.add("JSON array deserialization error: ${e.message}")
            null
        }

        if (topics.isNullOrEmpty()) {
            if (errors.isEmpty()) {
                errors.add("The JSON array contains no topic entries.")
            }
            return ParsedPayload(
                ParsedContentType.TOPIC_PACK_ARRAY,
                ValidationResult(false, errors, warnings),
                rawJson
            ) to emptyList()
        }

        if (topics.size > MAX_TOPICS_PER_BATCH) {
            errors.add("Batch contains ${topics.size} topics, exceeding maximum limit of $MAX_TOPICS_PER_BATCH.")
        }

        val seenTopicIds = mutableSetOf<String>()
        var totalSections = 0

        topics.forEachIndexed { index, topic ->
            if (seenTopicIds.contains(topic.topicId)) {
                errors.add("Duplicate topicId '${topic.topicId}' found in batch (topic #${index + 1}).")
            } else {
                seenTopicIds.add(topic.topicId)
            }
            validateTopicPackStructure(topic, errors, warnings, "Topic #${index + 1} (${topic.topicId})")
            totalSections += topic.sections.size
        }

        val isValid = errors.isEmpty()
        val result = ValidationResult(
            isValid = isValid,
            errors = errors,
            warnings = warnings,
            topicCount = if (isValid) topics.size else 0,
            sectionCount = if (isValid) totalSections else 0
        )

        return ParsedPayload(
            ParsedContentType.TOPIC_PACK_ARRAY,
            result,
            rawJson
        ) to if (isValid) topics else emptyList()
    }

    private fun validateBackup(rawJson: String): Pair<ParsedPayload, List<TopicPack>> {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        val backup: FullBackupData? = try {
            backupAdapter.fromJson(rawJson)
        } catch (e: Exception) {
            errors.add("Backup JSON deserialization error: ${e.message}")
            null
        }

        if (backup == null) {
            return ParsedPayload(
                ParsedContentType.FULL_BACKUP,
                ValidationResult(false, errors, warnings),
                rawJson
            ) to emptyList()
        }

        if (backup.backupVersion != 1) {
            errors.add("Unsupported backupVersion: ${backup.backupVersion}. Supported version is 1.")
        }

        var totalSections = 0
        val seenTopicIds = mutableSetOf<String>()
        backup.topics.forEachIndexed { index, topic ->
            if (seenTopicIds.contains(topic.topicId)) {
                errors.add("Duplicate topicId '${topic.topicId}' in backup.")
            } else {
                seenTopicIds.add(topic.topicId)
            }
            validateTopicPackStructure(topic, errors, warnings, "Backup topic '${topic.topicId}'")
            totalSections += topic.sections.size
        }

        val isValid = errors.isEmpty()
        val result = ValidationResult(
            isValid = isValid,
            errors = errors,
            warnings = warnings,
            topicCount = if (isValid) backup.topics.size else 0,
            sectionCount = if (isValid) totalSections else 0
        )

        return ParsedPayload(
            ParsedContentType.FULL_BACKUP,
            result,
            rawJson
        ) to if (isValid) backup.topics else emptyList()
    }

    private fun validateTopicPackStructure(
        topic: TopicPack,
        errors: MutableList<String>,
        warnings: MutableList<String>,
        prefix: String = "Topic '${topic.topicId}'"
    ) {
        if (topic.schemaVersion != 1) {
            errors.add("$prefix: Unsupported schemaVersion '${topic.schemaVersion}'. Supported version is 1.")
        }

        if (topic.topicId.isBlank()) {
            errors.add("$prefix: topicId cannot be empty or whitespace.")
        } else if (!topic.topicId.matches(Regex("^[a-zA-Z0-9_-]+$"))) {
            errors.add("$prefix: topicId '${topic.topicId}' contains invalid characters. Use letters, numbers, hyphens, and underscores only.")
        }

        if (topic.title.isBlank()) {
            errors.add("$prefix: Title is required and cannot be blank.")
        } else if (topic.title.length > MAX_TITLE_LENGTH) {
            errors.add("$prefix: Title exceeds maximum length of $MAX_TITLE_LENGTH characters.")
        }

        if (topic.titleBn.isNullOrBlank()) {
            warnings.add("$prefix: Bengali title (titleBn) is not provided.")
        }

        if (topic.category.isBlank()) {
            warnings.add("$prefix: Category is empty.")
        }

        val validLanguages = setOf("en", "bn", "mixed", "english", "bengali", "bangla")
        if (!validLanguages.contains(topic.language.lowercase())) {
            warnings.add("$prefix: Language '${topic.language}' is non-standard. Expected one of: en, bn, mixed.")
        }

        val validDifficulties = setOf("beginner", "intermediate", "advanced")
        if (!validDifficulties.contains(topic.difficulty.lowercase())) {
            warnings.add("$prefix: Difficulty '${topic.difficulty}' is non-standard. Expected beginner, intermediate, or advanced.")
        }

        if (topic.estimatedMinutes <= 0) {
            warnings.add("$prefix: estimatedMinutes is ${topic.estimatedMinutes}. Recommended is at least 1.")
        }

        if (topic.writingTypes.isEmpty()) {
            errors.add("$prefix: writingTypes list cannot be empty.")
        }

        if (topic.sections.isEmpty()) {
            errors.add("$prefix: Topic must contain at least one section.")
        }

        if (topic.sections.size > MAX_SECTIONS_PER_TOPIC) {
            errors.add("$prefix: Exceeds maximum section limit of $MAX_SECTIONS_PER_TOPIC sections.")
        }

        val seenSectionIds = mutableSetOf<String>()
        val seenOrders = mutableSetOf<Int>()

        topic.sections.forEachIndexed { sIdx, section ->
            validateSection(section, topic.topicId, sIdx + 1, seenSectionIds, seenOrders, errors, warnings)
        }
    }

    private fun validateSection(
        section: SectionPack,
        topicId: String,
        position: Int,
        seenSectionIds: MutableSet<String>,
        seenOrders: MutableSet<Int>,
        errors: MutableList<String>,
        warnings: MutableList<String>
    ) {
        val sPrefix = "Topic '$topicId', Section #$position ('${section.sectionId}')"

        if (section.sectionId.isBlank()) {
            errors.add("$sPrefix: sectionId cannot be blank.")
        } else {
            if (seenSectionIds.contains(section.sectionId)) {
                errors.add("$sPrefix: Duplicate sectionId '${section.sectionId}' within topic.")
            } else {
                seenSectionIds.add(section.sectionId)
            }
        }

        if (section.title.isBlank()) {
            errors.add("$sPrefix: Title is required and cannot be blank.")
        }

        val validTypes = setOf(
            "overview", "facts", "arguments", "vocabulary",
            "structure", "examples", "writing_task", "self_review", "notes", "custom"
        )
        if (!validTypes.contains(section.type.lowercase())) {
            errors.add("$sPrefix: Unsupported section type '${section.type}'. Allowed types: ${validTypes.joinToString()}.")
        }

        if (section.order <= 0) {
            errors.add("$sPrefix: Order must be a positive integer >= 1.")
        }

        section.durationSeconds?.let { dur ->
            if (dur < 0 || dur > MAX_DURATION_SECONDS) {
                errors.add("$sPrefix: durationSeconds ($dur) must be between 0 and $MAX_DURATION_SECONDS.")
            }
        }

        if (section.type.equals("writing_task", ignoreCase = true)) {
            if (section.prompt.isNullOrBlank()) {
                errors.add("$sPrefix: Section of type 'writing_task' requires a non-blank 'prompt'.")
            }
        }

        // Validate items uniqueness if present
        section.items?.let { items ->
            val seenItemIds = mutableSetOf<String>()
            items.forEachIndexed { iIdx, item ->
                if (item.id.isNotBlank()) {
                    if (seenItemIds.contains(item.id)) {
                        warnings.add("$sPrefix: Duplicate item id '${item.id}' in items list.")
                    } else {
                        seenItemIds.add(item.id)
                    }
                }
                if (item.text.isBlank()) {
                    warnings.add("$sPrefix: Item #${iIdx + 1} has blank text.")
                }
            }
        }
    }
}
