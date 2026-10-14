package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "topics")
data class TopicEntity(
    @PrimaryKey
    val topicId: String,
    val revision: Int,
    val title: String,
    val titleBn: String?,
    val category: String,
    val tagsJson: String,
    val writingTypesJson: String,
    val language: String,
    val difficulty: String,
    val estimatedMinutes: Int,
    val contentHash: String,
    val isBookmarked: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "sections",
    foreignKeys = [
        ForeignKey(
            entity = TopicEntity::class,
            parentColumns = ["topicId"],
            childColumns = ["topicId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("topicId"),
        Index(value = ["topicId", "sectionId"], unique = true)
    ]
)
data class SectionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val topicId: String,
    val sectionId: String,
    val type: String,
    val title: String,
    val orderIndex: Int,
    val durationSeconds: Int,
    val prompt: String?,
    val requiredForCompletion: Boolean,
    val contentJson: String
)

@Entity(
    tableName = "topic_progress",
    foreignKeys = [
        ForeignKey(
            entity = TopicEntity::class,
            parentColumns = ["topicId"],
            childColumns = ["topicId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("topicId"),
        Index(value = ["topicId", "writingMode"], unique = true)
    ]
)
data class TopicProgressEntity(
    @PrimaryKey
    val id: String, // "${topicId}_${writingMode}"
    val topicId: String,
    val writingMode: String,
    val currentSectionId: String,
    val status: String, // "NOT_STARTED", "IN_PROGRESS", "COMPLETED"
    val startedAt: Long,
    val completedAt: Long?,
    val totalElapsedSeconds: Long
)

@Entity(
    tableName = "section_progress",
    foreignKeys = [
        ForeignKey(
            entity = TopicEntity::class,
            parentColumns = ["topicId"],
            childColumns = ["topicId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("topicId"),
        Index(value = ["topicId", "writingMode", "sectionId"], unique = true)
    ]
)
data class SectionProgressEntity(
    @PrimaryKey
    val id: String, // "${topicId}_${writingMode}_${sectionId}"
    val topicId: String,
    val writingMode: String,
    val sectionId: String,
    val reviewed: Boolean,
    val completed: Boolean,
    val isBookmarked: Boolean,
    val elapsedSeconds: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "practice_sessions",
    foreignKeys = [
        ForeignKey(
            entity = TopicEntity::class,
            parentColumns = ["topicId"],
            childColumns = ["topicId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("topicId"), Index("startedAt")]
)
data class PracticeSessionEntity(
    @PrimaryKey
    val sessionId: String,
    val topicId: String,
    val writingMode: String,
    val startedAt: Long,
    val endedAt: Long,
    val durationSeconds: Long,
    val status: String, // "COMPLETED", "INCOMPLETE"
    val sectionsVisited: Int,
    val sectionsReviewed: Int
)

@Entity(
    tableName = "self_reviews",
    foreignKeys = [
        ForeignKey(
            entity = PracticeSessionEntity::class,
            parentColumns = ["sessionId"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId")]
)
data class SelfReviewEntity(
    @PrimaryKey
    val id: String, // "${sessionId}_${criterionId}"
    val sessionId: String,
    val topicId: String,
    val criterionId: String,
    val criterionText: String,
    val isChecked: Boolean,
    val updatedAt: Long
)
