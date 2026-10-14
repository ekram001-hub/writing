package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.dao.SectionDao
import com.example.data.local.dao.TopicDao
import com.example.data.local.database.MicroWriteDatabase
import com.example.data.local.entity.SectionEntity
import com.example.data.local.entity.TopicEntity
import com.example.data.model.TopicPack
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.security.MessageDigest

enum class ConflictResolution {
    SKIP,
    UPDATE,
    COPY
}

data class ImportOutcome(
    val success: Boolean,
    val message: String,
    val importedCount: Int = 0,
    val skippedCount: Int = 0,
    val updatedCount: Int = 0
)

class TopicRepository(
    private val database: MicroWriteDatabase,
    private val moshi: Moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
) {
    private val topicDao: TopicDao = database.topicDao()
    private val sectionDao: SectionDao = database.sectionDao()

    private val stringListAdapter = moshi.adapter<List<String>>(
        Types.newParameterizedType(List::class.java, String::class.java)
    )

    fun getAllTopics(): Flow<List<TopicEntity>> = topicDao.getAllTopics()

    fun getTopicById(topicId: String): Flow<TopicEntity?> = topicDao.observeTopicById(topicId)

    suspend fun getTopicByIdSync(topicId: String): TopicEntity? = topicDao.getTopicById(topicId)

    fun getSections(topicId: String): Flow<List<SectionEntity>> = sectionDao.getSectionsForTopic(topicId)

    suspend fun getSectionsSync(topicId: String): List<SectionEntity> = sectionDao.getSectionsForTopicSync(topicId)

    fun getBookmarkedTopics(): Flow<List<TopicEntity>> = topicDao.getBookmarkedTopics()

    fun getTopicCount(): Flow<Int> = topicDao.getTopicCount()

    fun getBookmarkedCount(): Flow<Int> = topicDao.getBookmarkedCount()

    fun searchTopics(query: String): Flow<List<TopicEntity>> = topicDao.searchTopics(query)

    suspend fun setBookmark(topicId: String, isBookmarked: Boolean) = withContext(Dispatchers.IO) {
        topicDao.setBookmark(topicId, isBookmarked)
    }

    suspend fun deleteTopic(topicId: String) = withContext(Dispatchers.IO) {
        topicDao.deleteTopicById(topicId)
    }

    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        topicDao.clearAllTopics()
    }

    suspend fun importTopicPack(
        pack: TopicPack,
        resolution: ConflictResolution
    ): ImportOutcome = withContext(Dispatchers.IO) {
        val existing = topicDao.getTopicById(pack.topicId)
        val targetTopicId: String
        var isUpdate = false
        var isSkip = false

        if (existing != null) {
            when (resolution) {
                ConflictResolution.SKIP -> {
                    return@withContext ImportOutcome(
                        success = true,
                        message = "Topic '${pack.topicId}' already exists and was skipped.",
                        skippedCount = 1
                    )
                }
                ConflictResolution.UPDATE -> {
                    targetTopicId = pack.topicId
                    isUpdate = true
                }
                ConflictResolution.COPY -> {
                    var suffix = 1
                    var newId = "${pack.topicId}_copy"
                    while (topicDao.getTopicById(newId) != null) {
                        suffix++
                        newId = "${pack.topicId}_copy_$suffix"
                    }
                    targetTopicId = newId
                }
            }
        } else {
            targetTopicId = pack.topicId
        }

        val tagsJson = stringListAdapter.toJson(pack.tags)
        val writingTypesJson = stringListAdapter.toJson(pack.writingTypes)
        val contentHash = computeHash(pack.title + pack.sections.size)

        val topicEntity = TopicEntity(
            topicId = targetTopicId,
            revision = pack.revision,
            title = pack.title,
            titleBn = pack.titleBn,
            category = pack.category,
            tagsJson = tagsJson,
            writingTypesJson = writingTypesJson,
            language = pack.language,
            difficulty = pack.difficulty,
            estimatedMinutes = pack.estimatedMinutes,
            contentHash = contentHash,
            isBookmarked = existing?.isBookmarked ?: false,
            createdAt = existing?.createdAt ?: System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        val sectionEntities = pack.sections.map { s ->
            SectionEntity(
                topicId = targetTopicId,
                sectionId = s.sectionId,
                type = s.type,
                title = s.title,
                orderIndex = s.order,
                durationSeconds = s.durationSeconds ?: 0,
                prompt = s.prompt,
                requiredForCompletion = s.requiredForCompletion,
                contentJson = moshi.adapter(com.example.data.model.SectionPack::class.java).toJson(s)
            )
        }

        database.withTransaction {
            if (isUpdate) {
                sectionDao.deleteSectionsForTopic(targetTopicId)
            }
            topicDao.insertTopic(topicEntity)
            sectionDao.insertSections(sectionEntities)
        }

        ImportOutcome(
            success = true,
            message = if (isUpdate) "Topic '$targetTopicId' updated successfully." else "Topic '$targetTopicId' imported successfully.",
            importedCount = if (!isUpdate) 1 else 0,
            updatedCount = if (isUpdate) 1 else 0
        )
    }

    suspend fun importTopicBatch(
        topics: List<TopicPack>,
        resolution: ConflictResolution
    ): ImportOutcome = withContext(Dispatchers.IO) {
        var imported = 0
        var skipped = 0
        var updated = 0

        for (topic in topics) {
            val outcome = importTopicPack(topic, resolution)
            imported += outcome.importedCount
            skipped += outcome.skippedCount
            updated += outcome.updatedCount
        }

        ImportOutcome(
            success = true,
            message = "Processed ${topics.size} topic(s): $imported imported, $updated updated, $skipped skipped.",
            importedCount = imported,
            skippedCount = skipped,
            updatedCount = updated
        )
    }

    private fun computeHash(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }.take(16)
    }
}
