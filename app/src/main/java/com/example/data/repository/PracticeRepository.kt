package com.example.data.repository

import com.example.data.local.dao.PracticeSessionDao
import com.example.data.local.dao.SectionDao
import com.example.data.local.dao.SectionProgressDao
import com.example.data.local.dao.SelfReviewDao
import com.example.data.local.dao.TopicProgressDao
import com.example.data.local.database.MicroWriteDatabase
import com.example.data.local.entity.PracticeSessionEntity
import com.example.data.local.entity.SectionProgressEntity
import com.example.data.local.entity.SelfReviewEntity
import com.example.data.local.entity.TopicProgressEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.Calendar

class PracticeRepository(
    private val database: MicroWriteDatabase
) {
    private val topicProgressDao: TopicProgressDao = database.topicProgressDao()
    private val sectionProgressDao: SectionProgressDao = database.sectionProgressDao()
    private val sessionDao: PracticeSessionDao = database.practiceSessionDao()
    private val selfReviewDao: SelfReviewDao = database.selfReviewDao()
    private val sectionDao: SectionDao = database.sectionDao()

    fun observeTopicProgress(topicId: String, writingMode: String): Flow<TopicProgressEntity?> =
        topicProgressDao.observeProgress(topicId, writingMode)

    suspend fun getTopicProgress(topicId: String, writingMode: String): TopicProgressEntity? =
        topicProgressDao.getProgress(topicId, writingMode)

    fun observeSectionProgressList(topicId: String, writingMode: String): Flow<List<SectionProgressEntity>> =
        sectionProgressDao.getSectionProgressForMode(topicId, writingMode)

    fun observeLatestActiveSession(): Flow<TopicProgressEntity?> =
        topicProgressDao.observeLatestActiveTopic()

    fun observeCompletedTopicsCount(): Flow<Int> =
        topicProgressDao.getCompletedTopicsCount()

    fun getTodayCompletedSessionsCount(): Flow<Int> {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return sessionDao.getTodayCompletedCount(cal.timeInMillis)
    }

    fun getRecentSessions(limitDays: Int = 7): Flow<List<PracticeSessionEntity>> {
        val cal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -limitDays)
        }
        return sessionDao.getRecentSessions(cal.timeInMillis)
    }

    fun getAllSessions(): Flow<List<PracticeSessionEntity>> = sessionDao.getAllSessions()

    fun getReviewsForSession(sessionId: String): Flow<List<SelfReviewEntity>> =
        selfReviewDao.getReviewsForSession(sessionId)

    suspend fun startOrResumeSession(
        topicId: String,
        writingMode: String,
        initialSectionId: String
    ): TopicProgressEntity = withContext(Dispatchers.IO) {
        val existing = topicProgressDao.getProgress(topicId, writingMode)
        if (existing == null) {
            val newProgress = TopicProgressEntity(
                id = "${topicId}_${writingMode}",
                topicId = topicId,
                writingMode = writingMode,
                currentSectionId = initialSectionId,
                status = "IN_PROGRESS",
                startedAt = System.currentTimeMillis(),
                completedAt = null,
                totalElapsedSeconds = 0
            )
            topicProgressDao.upsertProgress(newProgress)
            newProgress
        } else {
            if (existing.status == "NOT_STARTED") {
                val updated = existing.copy(status = "IN_PROGRESS", startedAt = System.currentTimeMillis())
                topicProgressDao.upsertProgress(updated)
                updated
            } else {
                existing
            }
        }
    }

    suspend fun updateCurrentSection(
        topicId: String,
        writingMode: String,
        sectionId: String
    ) = withContext(Dispatchers.IO) {
        val progress = topicProgressDao.getProgress(topicId, writingMode)
        if (progress != null) {
            topicProgressDao.upsertProgress(
                progress.copy(currentSectionId = sectionId)
            )
        }
    }

    suspend fun setSectionReviewed(
        topicId: String,
        writingMode: String,
        sectionId: String,
        reviewed: Boolean
    ) = withContext(Dispatchers.IO) {
        val existing = sectionProgressDao.getSectionProgress(topicId, writingMode, sectionId)
        val progress = existing?.copy(
            reviewed = reviewed,
            updatedAt = System.currentTimeMillis()
        ) ?: SectionProgressEntity(
            id = "${topicId}_${writingMode}_${sectionId}",
            topicId = topicId,
            writingMode = writingMode,
            sectionId = sectionId,
            reviewed = reviewed,
            completed = false,
            isBookmarked = false,
            elapsedSeconds = 0,
            updatedAt = System.currentTimeMillis()
        )
        sectionProgressDao.upsertSectionProgress(progress)
    }

    suspend fun toggleSectionBookmark(
        topicId: String,
        writingMode: String,
        sectionId: String,
        isBookmarked: Boolean
    ) = withContext(Dispatchers.IO) {
        val existing = sectionProgressDao.getSectionProgress(topicId, writingMode, sectionId)
        val progress = existing?.copy(
            isBookmarked = isBookmarked,
            updatedAt = System.currentTimeMillis()
        ) ?: SectionProgressEntity(
            id = "${topicId}_${writingMode}_${sectionId}",
            topicId = topicId,
            writingMode = writingMode,
            sectionId = sectionId,
            reviewed = false,
            completed = false,
            isBookmarked = isBookmarked,
            elapsedSeconds = 0,
            updatedAt = System.currentTimeMillis()
        )
        sectionProgressDao.upsertSectionProgress(progress)
    }

    suspend fun finishPracticeSession(
        sessionId: String,
        topicId: String,
        writingMode: String,
        startedAt: Long,
        durationSeconds: Long,
        sectionsVisited: Int,
        sectionsReviewed: Int,
        checklistAnswers: Map<String, Pair<String, Boolean>>,
        markTopicCompleted: Boolean
    ) = withContext(Dispatchers.IO) {
        val sessionEntity = PracticeSessionEntity(
            sessionId = sessionId,
            topicId = topicId,
            writingMode = writingMode,
            startedAt = startedAt,
            endedAt = System.currentTimeMillis(),
            durationSeconds = durationSeconds,
            status = if (markTopicCompleted) "COMPLETED" else "INCOMPLETE",
            sectionsVisited = sectionsVisited,
            sectionsReviewed = sectionsReviewed
        )

        val reviewEntities = checklistAnswers.map { (cId, textAndChecked) ->
            SelfReviewEntity(
                id = "${sessionId}_${cId}",
                sessionId = sessionId,
                topicId = topicId,
                criterionId = cId,
                criterionText = textAndChecked.first,
                isChecked = textAndChecked.second,
                updatedAt = System.currentTimeMillis()
            )
        }

        database.runInTransaction {
            kotlinx.coroutines.runBlocking {
                sessionDao.insertSession(sessionEntity)
                selfReviewDao.insertAll(reviewEntities)

                val existingProgress = topicProgressDao.getProgress(topicId, writingMode)
                if (existingProgress != null) {
                    val newTotalSeconds = existingProgress.totalElapsedSeconds + durationSeconds
                    val updatedProgress = existingProgress.copy(
                        status = if (markTopicCompleted) "COMPLETED" else existingProgress.status,
                        completedAt = if (markTopicCompleted) System.currentTimeMillis() else existingProgress.completedAt,
                        totalElapsedSeconds = newTotalSeconds
                    )
                    topicProgressDao.upsertProgress(updatedProgress)
                }
            }
        }
    }
}
