package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.demo.DemoTopicPack
import com.example.data.local.database.MicroWriteDatabase
import com.example.data.model.FullBackupData
import com.example.data.model.TopicPack
import com.example.data.repository.BackupRepository
import com.example.data.repository.ConflictResolution
import com.example.data.repository.PracticeRepository
import com.example.data.repository.TopicRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MicroWriteDatabaseTest {
    private lateinit var db: MicroWriteDatabase
    private lateinit var topicRepo: TopicRepository
    private lateinit var practiceRepo: PracticeRepository
    private lateinit var backupRepo: BackupRepository
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, MicroWriteDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        topicRepo = TopicRepository(db)
        practiceRepo = PracticeRepository(db)
        backupRepo = BackupRepository(db, topicRepo)
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun `insert topic and retrieve sections in configured order`() = runBlocking {
        val outcome = topicRepo.importTopicPack(DemoTopicPack.samplePack, ConflictResolution.UPDATE)
        assertTrue(outcome.success)

        val retrievedTopic = topicRepo.getTopicByIdSync("BB-AD-2026-FINTECH")
        assertNotNull(retrievedTopic)
        assertEquals("Central Bank Digital Currency & Cashless Economy in Bangladesh: Monetary Policy Challenges", retrievedTopic!!.title)

        val sections = topicRepo.getSectionsSync("BB-AD-2026-FINTECH")
        assertEquals(9, sections.size)
        // Verify orderIndex is strictly increasing
        for (i in 0 until sections.size - 1) {
            assertTrue(sections[i].orderIndex <= sections[i + 1].orderIndex)
        }
    }

    @Test
    fun `topic duplicate conflict copy creates distinct topic ID`() = runBlocking {
        topicRepo.importTopicPack(DemoTopicPack.samplePack, ConflictResolution.UPDATE)

        val outcome = topicRepo.importTopicPack(DemoTopicPack.samplePack, ConflictResolution.COPY)
        assertTrue(outcome.success)

        val allTopics = topicRepo.getAllTopics().first()
        assertEquals(2, allTopics.size)
        assertTrue(allTopics.any { it.topicId.startsWith("BB-AD-2026-FINTECH_copy") })
    }

    @Test
    fun `bookmark state toggles and persists`() = runBlocking {
        topicRepo.importTopicPack(DemoTopicPack.samplePack, ConflictResolution.UPDATE)

        topicRepo.setBookmark("BB-AD-2026-FINTECH", true)
        var topic = topicRepo.getTopicByIdSync("BB-AD-2026-FINTECH")
        assertTrue(topic!!.isBookmarked)

        topicRepo.setBookmark("BB-AD-2026-FINTECH", false)
        topic = topicRepo.getTopicByIdSync("BB-AD-2026-FINTECH")
        assertTrue(!topic!!.isBookmarked)
    }

    @Test
    fun `practice session finishes and stores self review checklist`() = runBlocking {
        topicRepo.importTopicPack(DemoTopicPack.samplePack, ConflictResolution.UPDATE)

        practiceRepo.startOrResumeSession("BB-AD-2026-FINTECH", "english_focus", "overview")

        val checklistAnswers = mapOf(
            "crit-1" to Pair("Main argument clear", true),
            "crit-2" to Pair("Logical structure", true),
            "crit-3" to Pair("Facts supported", false)
        )

        practiceRepo.finishPracticeSession(
            sessionId = "session-test-123",
            topicId = "BB-AD-2026-FINTECH",
            writingMode = "english_focus",
            startedAt = System.currentTimeMillis() - 120000,
            durationSeconds = 120,
            sectionsVisited = 3,
            sectionsReviewed = 3,
            checklistAnswers = checklistAnswers,
            markTopicCompleted = true
        )

        val sessions = practiceRepo.getAllSessions().first()
        assertEquals(1, sessions.size)
        assertEquals("session-test-123", sessions[0].sessionId)
        assertEquals("COMPLETED", sessions[0].status)

        val reviews = practiceRepo.getReviewsForSession("session-test-123").first()
        assertEquals(3, reviews.size)

        val topicProgress = practiceRepo.getTopicProgress("BB-AD-2026-FINTECH", "english_focus")
        assertNotNull(topicProgress)
        assertEquals("COMPLETED", topicProgress!!.status)
    }

    @Test
    fun `export and restore full backup roundtrip`() = runBlocking {
        topicRepo.importTopicPack(DemoTopicPack.samplePack, ConflictResolution.UPDATE)
        topicRepo.setBookmark("BB-AD-2026-FINTECH", true)

        val backupJson = backupRepo.exportFullBackup()
        assertTrue(backupJson.contains("BB-AD-2026-FINTECH"))

        // Clear all data
        topicRepo.clearAllData()
        assertEquals(0, topicRepo.getAllTopics().first().size)

        // Restore
        val backupAdapter = moshi.adapter(FullBackupData::class.java)
        val parsedBackup = backupAdapter.fromJson(backupJson)
        assertNotNull(parsedBackup)

        val restoreOutcome = backupRepo.restoreFullBackup(parsedBackup!!)
        assertTrue(restoreOutcome.success)

        val restoredTopics = topicRepo.getAllTopics().first()
        assertEquals(1, restoredTopics.size)
        assertEquals("BB-AD-2026-FINTECH", restoredTopics[0].topicId)
        assertTrue(restoredTopics[0].isBookmarked)
    }
}
