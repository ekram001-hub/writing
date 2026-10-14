package com.example

import com.example.data.demo.DemoTopicPack
import com.example.data.model.TopicPack
import com.example.data.validation.ParsedContentType
import com.example.data.validation.TopicPackValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TopicPackValidationTest {
    private lateinit var validator: TopicPackValidator

    @Before
    fun setUp() {
        validator = TopicPackValidator()
    }

    @Test
    fun `valid single topic pack passes validation`() {
        val adapter = validator.moshi.adapter(TopicPack::class.java)
        val validJson = adapter.toJson(DemoTopicPack.samplePack)

        val (payload, topics) = validator.inspectAndValidate(validJson)
        if (!payload.validationResult.isValid) {
            println("VALIDATION ERRORS: " + payload.validationResult.errors)
        }
        assertTrue("Expected valid payload: " + payload.validationResult.errors, payload.validationResult.isValid)
        assertEquals(ParsedContentType.TOPIC_PACK, payload.type)
        assertEquals(1, topics.size)
        assertEquals("BB-AD-2026-FINTECH", topics[0].topicId)
        assertEquals(DemoTopicPack.samplePack.titleBn, topics[0].titleBn)
    }

    @Test
    fun `malformed json reports syntax error`() {
        val malformed = "{ this is definitely not valid json }"
        val (payload, _) = validator.inspectAndValidate(malformed)

        assertFalse("Expected invalid payload", payload.validationResult.isValid)
        assertTrue(payload.validationResult.errors.any { 
            it.contains("JSON deserialization error") || it.contains("Malformed JSON") || it.contains("syntax") 
        })
    }

    @Test
    fun `missing title returns error`() {
        val json = """
            {
              "schemaVersion": 1,
              "topicId": "TEST-01",
              "title": "",
              "category": "Economy",
              "writingTypes": ["english_focus"],
              "sections": [
                {
                  "sectionId": "sec-1",
                  "type": "overview",
                  "title": "Intro",
                  "order": 1
                }
              ]
            }
        """.trimIndent()

        val (payload, _) = validator.inspectAndValidate(json)
        assertFalse(payload.validationResult.isValid)
        assertTrue(payload.validationResult.errors.any { it.contains("Title is required") })
    }

    @Test
    fun `missing topicId returns error`() {
        val json = """
            {
              "schemaVersion": 1,
              "topicId": "",
              "title": "Valid Title",
              "category": "Economy",
              "writingTypes": ["english_focus"],
              "sections": [
                {
                  "sectionId": "sec-1",
                  "type": "overview",
                  "title": "Intro",
                  "order": 1
                }
              ]
            }
        """.trimIndent()

        val (payload, _) = validator.inspectAndValidate(json)
        assertFalse(payload.validationResult.isValid)
        assertTrue(payload.validationResult.errors.any { it.contains("topicId cannot be empty") })
    }

    @Test
    fun `duplicate section IDs report error`() {
        val json = """
            {
              "schemaVersion": 1,
              "topicId": "TEST-DUP",
              "title": "Duplicate Section Test",
              "category": "Banking",
              "writingTypes": ["english_focus"],
              "sections": [
                {
                  "sectionId": "duplicate-sec",
                  "type": "overview",
                  "title": "First",
                  "order": 1
                },
                {
                  "sectionId": "duplicate-sec",
                  "type": "facts",
                  "title": "Second",
                  "order": 2
                }
              ]
            }
        """.trimIndent()

        val (payload, _) = validator.inspectAndValidate(json)
        assertFalse(payload.validationResult.isValid)
        assertTrue(payload.validationResult.errors.any { it.contains("Duplicate sectionId") })
    }

    @Test
    fun `writing_task without prompt reports error`() {
        val json = """
            {
              "schemaVersion": 1,
              "topicId": "TEST-TASK",
              "title": "Writing Task Test",
              "category": "Banking",
              "writingTypes": ["english_focus"],
              "sections": [
                {
                  "sectionId": "task-1",
                  "type": "writing_task",
                  "title": "Notebook Task",
                  "order": 1,
                  "prompt": ""
                }
              ]
            }
        """.trimIndent()

        val (payload, _) = validator.inspectAndValidate(json)
        assertFalse(payload.validationResult.isValid)
        assertTrue(payload.validationResult.errors.any { it.contains("writing_task' requires a non-blank 'prompt'") })
    }

    @Test
    fun `unsupported schema version reports error`() {
        val json = """
            {
              "schemaVersion": 99,
              "topicId": "TEST-VER",
              "title": "Version Test",
              "category": "Banking",
              "writingTypes": ["english_focus"],
              "sections": [
                {
                  "sectionId": "sec-1",
                  "type": "overview",
                  "title": "Overview",
                  "order": 1
                }
              ]
            }
        """.trimIndent()

        val (payload, _) = validator.inspectAndValidate(json)
        assertFalse(payload.validationResult.isValid)
        assertTrue(payload.validationResult.errors.any { it.contains("Unsupported schemaVersion") })
    }
}
