package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.local.entity.PracticeSessionEntity
import com.example.data.local.entity.SectionEntity
import com.example.data.local.entity.SectionProgressEntity
import com.example.data.local.entity.SelfReviewEntity
import com.example.data.local.entity.TopicEntity
import com.example.data.local.entity.TopicProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TopicDao {
    @Query("SELECT * FROM topics ORDER BY createdAt DESC")
    fun getAllTopics(): Flow<List<TopicEntity>>

    @Query("SELECT * FROM topics WHERE topicId = :topicId LIMIT 1")
    suspend fun getTopicById(topicId: String): TopicEntity?

    @Query("SELECT * FROM topics WHERE topicId = :topicId LIMIT 1")
    fun observeTopicById(topicId: String): Flow<TopicEntity?>

    @Query("SELECT * FROM topics WHERE isBookmarked = 1 ORDER BY createdAt DESC")
    fun getBookmarkedTopics(): Flow<List<TopicEntity>>

    @Query("SELECT COUNT(*) FROM topics")
    fun getTopicCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM topics WHERE isBookmarked = 1")
    fun getBookmarkedCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTopic(topic: TopicEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTopics(topics: List<TopicEntity>)

    @Update
    suspend fun updateTopic(topic: TopicEntity)

    @Query("UPDATE topics SET isBookmarked = :isBookmarked WHERE topicId = :topicId")
    suspend fun setBookmark(topicId: String, isBookmarked: Boolean)

    @Query("DELETE FROM topics WHERE topicId = :topicId")
    suspend fun deleteTopicById(topicId: String)

    @Query("DELETE FROM topics")
    suspend fun clearAllTopics()

    @Query("""
        SELECT * FROM topics 
        WHERE (title LIKE '%' || :query || '%' 
           OR (titleBn IS NOT NULL AND titleBn LIKE '%' || :query || '%')
           OR category LIKE '%' || :query || '%'
           OR tagsJson LIKE '%' || :query || '%'
           OR topicId LIKE '%' || :query || '%')
        ORDER BY createdAt DESC
    """)
    fun searchTopics(query: String): Flow<List<TopicEntity>>
}

@Dao
interface SectionDao {
    @Query("SELECT * FROM sections WHERE topicId = :topicId ORDER BY orderIndex ASC")
    fun getSectionsForTopic(topicId: String): Flow<List<SectionEntity>>

    @Query("SELECT * FROM sections WHERE topicId = :topicId ORDER BY orderIndex ASC")
    suspend fun getSectionsForTopicSync(topicId: String): List<SectionEntity>

    @Query("SELECT * FROM sections WHERE topicId = :topicId AND sectionId = :sectionId LIMIT 1")
    suspend fun getSection(topicId: String, sectionId: String): SectionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSections(sections: List<SectionEntity>)

    @Query("DELETE FROM sections WHERE topicId = :topicId")
    suspend fun deleteSectionsForTopic(topicId: String)
}

@Dao
interface TopicProgressDao {
    @Query("SELECT * FROM topic_progress WHERE topicId = :topicId AND writingMode = :writingMode LIMIT 1")
    suspend fun getProgress(topicId: String, writingMode: String): TopicProgressEntity?

    @Query("SELECT * FROM topic_progress WHERE topicId = :topicId AND writingMode = :writingMode LIMIT 1")
    fun observeProgress(topicId: String, writingMode: String): Flow<TopicProgressEntity?>

    @Query("SELECT * FROM topic_progress WHERE topicId = :topicId")
    fun getAllProgressForTopic(topicId: String): Flow<List<TopicProgressEntity>>

    @Query("SELECT * FROM topic_progress WHERE status = 'IN_PROGRESS' ORDER BY startedAt DESC LIMIT 1")
    fun observeLatestActiveTopic(): Flow<TopicProgressEntity?>

    @Query("SELECT * FROM topic_progress WHERE status = 'COMPLETED'")
    fun getAllCompletedProgress(): Flow<List<TopicProgressEntity>>

    @Query("SELECT COUNT(DISTINCT topicId) FROM topic_progress WHERE status = 'COMPLETED'")
    fun getCompletedTopicsCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProgress(progress: TopicProgressEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(progressList: List<TopicProgressEntity>)

    @Query("SELECT * FROM topic_progress")
    suspend fun getAllProgressSync(): List<TopicProgressEntity>
}

@Dao
interface SectionProgressDao {
    @Query("SELECT * FROM section_progress WHERE topicId = :topicId AND writingMode = :writingMode")
    fun getSectionProgressForMode(topicId: String, writingMode: String): Flow<List<SectionProgressEntity>>

    @Query("SELECT * FROM section_progress WHERE topicId = :topicId AND writingMode = :writingMode AND sectionId = :sectionId LIMIT 1")
    suspend fun getSectionProgress(topicId: String, writingMode: String, sectionId: String): SectionProgressEntity?

    @Query("SELECT * FROM section_progress WHERE topicId = :topicId AND writingMode = :writingMode AND sectionId = :sectionId LIMIT 1")
    fun observeSectionProgress(topicId: String, writingMode: String, sectionId: String): Flow<SectionProgressEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSectionProgress(progress: SectionProgressEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(progressList: List<SectionProgressEntity>)

    @Query("SELECT * FROM section_progress")
    suspend fun getAllSectionProgressSync(): List<SectionProgressEntity>
}

@Dao
interface PracticeSessionDao {
    @Query("SELECT * FROM practice_sessions ORDER BY startedAt DESC")
    fun getAllSessions(): Flow<List<PracticeSessionEntity>>

    @Query("SELECT * FROM practice_sessions WHERE topicId = :topicId ORDER BY startedAt DESC")
    fun getSessionsForTopic(topicId: String): Flow<List<PracticeSessionEntity>>

    @Query("SELECT * FROM practice_sessions WHERE startedAt >= :startOfDayTimestamp ORDER BY startedAt DESC")
    fun getTodaySessions(startOfDayTimestamp: Long): Flow<List<PracticeSessionEntity>>

    @Query("SELECT * FROM practice_sessions WHERE startedAt >= :sinceTimestamp ORDER BY startedAt DESC")
    fun getRecentSessions(sinceTimestamp: Long): Flow<List<PracticeSessionEntity>>

    @Query("SELECT COUNT(*) FROM practice_sessions WHERE startedAt >= :startOfDayTimestamp AND status = 'COMPLETED'")
    fun getTodayCompletedCount(startOfDayTimestamp: Long): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: PracticeSessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sessions: List<PracticeSessionEntity>)

    @Query("SELECT * FROM practice_sessions")
    suspend fun getAllSessionsSync(): List<PracticeSessionEntity>
}

@Dao
interface SelfReviewDao {
    @Query("SELECT * FROM self_reviews WHERE sessionId = :sessionId")
    fun getReviewsForSession(sessionId: String): Flow<List<SelfReviewEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReview(review: SelfReviewEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(reviews: List<SelfReviewEntity>)

    @Query("SELECT * FROM self_reviews")
    suspend fun getAllReviewsSync(): List<SelfReviewEntity>
}
