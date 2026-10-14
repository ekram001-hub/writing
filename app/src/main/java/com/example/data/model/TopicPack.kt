package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TopicPack(
    val schemaVersion: Int = 1,
    val topicId: String,
    val revision: Int = 1,
    val title: String,
    val titleBn: String? = null,
    val category: String,
    val tags: List<String> = emptyList(),
    val writingTypes: List<String> = listOf("english_focus", "bangla_focus"),
    val language: String = "mixed",
    val difficulty: String = "intermediate",
    val estimatedMinutes: Int = 3,
    val sections: List<SectionPack> = emptyList()
)

@JsonClass(generateAdapter = true)
data class SectionPack(
    val sectionId: String,
    val type: String,
    val title: String,
    val order: Int = 1,
    val durationSeconds: Int? = null,
    val prompt: String? = null,
    val requiredForCompletion: Boolean = true,
    val content: List<ContentBlock>? = null,
    val items: List<ContentItem>? = null
)

@JsonClass(generateAdapter = true)
data class ContentBlock(
    val type: String = "paragraph",
    val text: String,
    val note: String? = null
)

@JsonClass(generateAdapter = true)
data class ContentItem(
    val id: String,
    val text: String,
    val sourceName: String? = null,
    val sourceUrl: String? = null,
    val publicationYear: Int? = null,
    val referenceDate: String? = null,
    val category: String? = null,
    val subtext: String? = null
)

@JsonClass(generateAdapter = true)
data class FullBackupData(
    val backupVersion: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val topics: List<TopicPack> = emptyList(),
    val topicProgress: List<TopicProgressBackup> = emptyList(),
    val sectionProgress: List<SectionProgressBackup> = emptyList(),
    val sessions: List<SessionBackup> = emptyList(),
    val selfReviews: List<SelfReviewBackup> = emptyList(),
    val bookmarks: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class TopicProgressBackup(
    val topicId: String,
    val writingMode: String,
    val currentSectionId: String,
    val status: String,
    val startedAt: Long,
    val completedAt: Long?,
    val totalElapsedSeconds: Long
)

@JsonClass(generateAdapter = true)
data class SectionProgressBackup(
    val topicId: String,
    val writingMode: String,
    val sectionId: String,
    val reviewed: Boolean,
    val completed: Boolean,
    val isBookmarked: Boolean,
    val elapsedSeconds: Long,
    val updatedAt: Long
)

@JsonClass(generateAdapter = true)
data class SessionBackup(
    val sessionId: String,
    val topicId: String,
    val writingMode: String,
    val startedAt: Long,
    val endedAt: Long,
    val durationSeconds: Long,
    val status: String,
    val sectionsVisited: Int,
    val sectionsReviewed: Int
)

@JsonClass(generateAdapter = true)
data class SelfReviewBackup(
    val sessionId: String,
    val topicId: String,
    val criterionId: String,
    val criterionText: String,
    val isChecked: Boolean,
    val updatedAt: Long
)

enum class DifficultyLevel(val key: String, val label: String) {
    BEGINNER("beginner", "Beginner"),
    INTERMEDIATE("intermediate", "Intermediate"),
    ADVANCED("advanced", "Advanced");

    companion object {
        fun fromKey(key: String): DifficultyLevel =
            entries.find { it.key.equals(key, ignoreCase = true) } ?: INTERMEDIATE
    }
}

enum class SectionType(val key: String, val label: String) {
    OVERVIEW("overview", "Overview"),
    FACTS("facts", "Facts & Data"),
    ARGUMENTS("arguments", "Key Arguments"),
    VOCABULARY("vocabulary", "Vocabulary"),
    STRUCTURE("structure", "Writing Structure"),
    EXAMPLES("examples", "Sample Excerpt"),
    WRITING_TASK("writing_task", "Writing Task"),
    SELF_REVIEW("self_review", "Self Review"),
    NOTES("notes", "Examiner Notes"),
    CUSTOM("custom", "Additional Guide");

    companion object {
        fun fromKey(key: String): SectionType =
            entries.find { it.key.equals(key, ignoreCase = true) } ?: CUSTOM
    }
}

enum class ProgressStatus(val label: String) {
    NOT_STARTED("Not Started"),
    IN_PROGRESS("In Progress"),
    COMPLETED("Completed");

    companion object {
        fun fromString(s: String): ProgressStatus =
            entries.find { it.name.equals(s, ignoreCase = true) } ?: NOT_STARTED
    }
}
