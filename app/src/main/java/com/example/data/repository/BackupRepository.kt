package com.example.data.repository

import com.example.data.local.dao.PracticeSessionDao
import com.example.data.local.dao.SectionDao
import com.example.data.local.dao.SectionProgressDao
import com.example.data.local.dao.SelfReviewDao
import com.example.data.local.dao.TopicDao
import com.example.data.local.dao.TopicProgressDao
import com.example.data.local.database.MicroWriteDatabase
import com.example.data.local.entity.SectionEntity
import com.example.data.local.entity.TopicEntity
import com.example.data.model.FullBackupData
import com.example.data.model.SectionPack
import com.example.data.model.SectionProgressBackup
import com.example.data.model.SelfReviewBackup
import com.example.data.model.SessionBackup
import com.example.data.model.TopicPack
import com.example.data.model.TopicProgressBackup
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

data class RestoreOutcome(
    val success: Boolean,
    val message: String,
    val topicsRestored: Int = 0,
    val progressRestored: Int = 0,
    val sessionsRestored: Int = 0
)

class BackupRepository(
    private val database: MicroWriteDatabase,
    private val topicRepository: TopicRepository,
    private val moshi: Moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
) {
    private val topicDao: TopicDao = database.topicDao()
    private val sectionDao: SectionDao = database.sectionDao()
    private val topicProgressDao: TopicProgressDao = database.topicProgressDao()
    private val sectionProgressDao: SectionProgressDao = database.sectionProgressDao()
    private val sessionDao: PracticeSessionDao = database.practiceSessionDao()
    private val selfReviewDao: SelfReviewDao = database.selfReviewDao()

    private val topicPackAdapter = moshi.adapter(TopicPack::class.java)
    private val topicListType = Types.newParameterizedType(List::class.java, TopicPack::class.java)
    private val topicListAdapter = moshi.adapter<List<TopicPack>>(topicListType)
    private val backupAdapter = moshi.adapter(FullBackupData::class.java)
    private val sectionPackAdapter = moshi.adapter(SectionPack::class.java)
    private val stringListAdapter = moshi.adapter<List<String>>(
        Types.newParameterizedType(List::class.java, String::class.java)
    )

    suspend fun exportSingleTopic(topicId: String): String? = withContext(Dispatchers.IO) {
        val topic = topicDao.getTopicById(topicId) ?: return@withContext null
        val sections = sectionDao.getSectionsForTopicSync(topicId)
        val pack = entityToTopicPack(topic, sections)
        topicPackAdapter.indent("  ").toJson(pack)
    }

    suspend fun exportAllTopics(): String = withContext(Dispatchers.IO) {
        val topics = topicDao.getAllTopics().first()
        val packs = topics.map { t ->
            val sections = sectionDao.getSectionsForTopicSync(t.topicId)
            entityToTopicPack(t, sections)
        }
        topicListAdapter.indent("  ").toJson(packs)
    }

    suspend fun exportFullBackup(): String = withContext(Dispatchers.IO) {
        val topics = topicDao.getAllTopics().first()
        val topicPacks = topics.map { t ->
            val sections = sectionDao.getSectionsForTopicSync(t.topicId)
            entityToTopicPack(t, sections)
        }
        val topicProgressList = topicProgressDao.getAllProgressSync().map {
            TopicProgressBackup(
                topicId = it.topicId,
                writingMode = it.writingMode,
                currentSectionId = it.currentSectionId,
                status = it.status,
                startedAt = it.startedAt,
                completedAt = it.completedAt,
                totalElapsedSeconds = it.totalElapsedSeconds
            )
        }
        val sectionProgressList = sectionProgressDao.getAllSectionProgressSync().map {
            SectionProgressBackup(
                topicId = it.topicId,
                writingMode = it.writingMode,
                sectionId = it.sectionId,
                reviewed = it.reviewed,
                completed = it.completed,
                isBookmarked = it.isBookmarked,
                elapsedSeconds = it.elapsedSeconds,
                updatedAt = it.updatedAt
            )
        }
        val sessionsList = sessionDao.getAllSessionsSync().map {
            SessionBackup(
                sessionId = it.sessionId,
                topicId = it.topicId,
                writingMode = it.writingMode,
                startedAt = it.startedAt,
                endedAt = it.endedAt,
                durationSeconds = it.durationSeconds,
                status = it.status,
                sectionsVisited = it.sectionsVisited,
                sectionsReviewed = it.sectionsReviewed
            )
        }
        val reviewList = selfReviewDao.getAllReviewsSync().map {
            SelfReviewBackup(
                sessionId = it.sessionId,
                topicId = it.topicId,
                criterionId = it.criterionId,
                criterionText = it.criterionText,
                isChecked = it.isChecked,
                updatedAt = it.updatedAt
            )
        }
        val bookmarks = topics.filter { it.isBookmarked }.map { it.topicId }

        val backup = FullBackupData(
            backupVersion = 1,
            exportedAt = System.currentTimeMillis(),
            topics = topicPacks,
            topicProgress = topicProgressList,
            sectionProgress = sectionProgressList,
            sessions = sessionsList,
            selfReviews = reviewList,
            bookmarks = bookmarks
        )

        backupAdapter.indent("  ").toJson(backup)
    }

    suspend fun restoreFullBackup(backup: FullBackupData): RestoreOutcome = withContext(Dispatchers.IO) {
        try {
            database.runInTransaction {
                kotlinx.coroutines.runBlocking {
                    // Restore topics
                    for (topic in backup.topics) {
                        topicRepository.importTopicPack(topic, ConflictResolution.UPDATE)
                    }

                    // Restore bookmarks
                    for (bId in backup.bookmarks) {
                        topicDao.setBookmark(bId, true)
                    }

                    // Restore topic progress
                    val topicProgressEntities = backup.topicProgress.map {
                        com.example.data.local.entity.TopicProgressEntity(
                            id = "${it.topicId}_${it.writingMode}",
                            topicId = it.topicId,
                            writingMode = it.writingMode,
                            currentSectionId = it.currentSectionId,
                            status = it.status,
                            startedAt = it.startedAt,
                            completedAt = it.completedAt,
                            totalElapsedSeconds = it.totalElapsedSeconds
                        )
                    }
                    topicProgressDao.insertAll(topicProgressEntities)

                    // Restore section progress
                    val sectionProgressEntities = backup.sectionProgress.map {
                        com.example.data.local.entity.SectionProgressEntity(
                            id = "${it.topicId}_${it.writingMode}_${it.sectionId}",
                            topicId = it.topicId,
                            writingMode = it.writingMode,
                            sectionId = it.sectionId,
                            reviewed = it.reviewed,
                            completed = it.completed,
                            isBookmarked = it.isBookmarked,
                            elapsedSeconds = it.elapsedSeconds,
                            updatedAt = it.updatedAt
                        )
                    }
                    sectionProgressDao.insertAll(sectionProgressEntities)

                    // Restore sessions
                    val sessionEntities = backup.sessions.map {
                        com.example.data.local.entity.PracticeSessionEntity(
                            sessionId = it.sessionId,
                            topicId = it.topicId,
                            writingMode = it.writingMode,
                            startedAt = it.startedAt,
                            endedAt = it.endedAt,
                            durationSeconds = it.durationSeconds,
                            status = it.status,
                            sectionsVisited = it.sectionsVisited,
                            sectionsReviewed = it.sectionsReviewed
                        )
                    }
                    sessionDao.insertAll(sessionEntities)

                    // Restore self reviews
                    val reviewEntities = backup.selfReviews.map {
                        com.example.data.local.entity.SelfReviewEntity(
                            id = "${it.sessionId}_${it.criterionId}",
                            sessionId = it.sessionId,
                            topicId = it.topicId,
                            criterionId = it.criterionId,
                            criterionText = it.criterionText,
                            isChecked = it.isChecked,
                            updatedAt = it.updatedAt
                        )
                    }
                    selfReviewDao.insertAll(reviewEntities)
                }
            }

            RestoreOutcome(
                success = true,
                message = "Restored ${backup.topics.size} topic(s), ${backup.sessions.size} session(s) successfully.",
                topicsRestored = backup.topics.size,
                progressRestored = backup.topicProgress.size,
                sessionsRestored = backup.sessions.size
            )
        } catch (e: Exception) {
            RestoreOutcome(
                success = false,
                message = "Failed to restore backup: ${e.message}"
            )
        }
    }

    private fun entityToTopicPack(topic: TopicEntity, sections: List<SectionEntity>): TopicPack {
        val tags = try {
            stringListAdapter.fromJson(topic.tagsJson) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
        val writingTypes = try {
            stringListAdapter.fromJson(topic.writingTypesJson) ?: listOf("english_focus", "bangla_focus")
        } catch (e: Exception) {
            listOf("english_focus", "bangla_focus")
        }

        val sectionPacks = sections.map { s ->
            try {
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
        }

        return TopicPack(
            schemaVersion = 1,
            topicId = topic.topicId,
            revision = topic.revision,
            title = topic.title,
            titleBn = topic.titleBn,
            category = topic.category,
            tags = tags,
            writingTypes = writingTypes,
            language = topic.language,
            difficulty = topic.difficulty,
            estimatedMinutes = topic.estimatedMinutes,
            sections = sectionPacks
        )
    }
}
