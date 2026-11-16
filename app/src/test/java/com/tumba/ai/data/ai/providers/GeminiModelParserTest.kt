package com.tumba.ai.data.ai.providers

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiModelParserTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun keepsChatModelsAndReadsMetadata() {
        val body = """
            {
              "models": [
                {
                  "name": "models/gemini-flash-latest",
                  "displayName": "Gemini Flash Latest",
                  "inputTokenLimit": 1048576,
                  "supportedGenerationMethods": ["generateContent", "countTokens"]
                },
                {
                  "name": "models/gemini-2.5-pro",
                  "displayName": "Gemini 2.5 Pro",
                  "inputTokenLimit": 1048576,
                  "supportedGenerationMethods": ["generateContent"]
                }
              ]
            }
        """.trimIndent()

        val models = parseGeminiModels(json, body)

        assertEquals(listOf("gemini-flash-latest", "gemini-2.5-pro"), models.map { it.id })
        assertEquals("Gemini Flash Latest", models.first().displayName)
        assertEquals(1_048_576, models.first().contextWindow)
        assertTrue(models.first().isDefault)
        assertTrue(models.first().capabilities.supportsImages)
    }

    @Test
    fun filtersEmbeddingsImagesAndLiveModels() {
        val body = """
            {
              "models": [
                {"name":"models/gemini-embedding-001","supportedGenerationMethods":["embedContent"]},
                {"name":"models/gemini-3.1-flash-image","supportedGenerationMethods":["generateContent"]},
                {"name":"models/gemini-3.1-flash-live-preview","supportedGenerationMethods":["generateContent"]},
                {"name":"models/gemini-3.1-pro-preview","displayName":"Gemini 3.1 Pro","supportedGenerationMethods":["generateContent"]}
              ]
            }
        """.trimIndent()

        val models = parseGeminiModels(json, body)

        assertEquals(listOf("gemini-3.1-pro-preview"), models.map { it.id })
    }

    @Test
    fun skipsModelsWithoutGenerateContent() {
        val body = """
            {"models":[{"name":"models/test-only","supportedGenerationMethods":["countTokens"]}]}
        """.trimIndent()

        assertTrue(parseGeminiModels(json, body).isEmpty())
    }

    @Test
    fun handlesMalformedOrEmptyResponses() {
        assertTrue(parseGeminiModels(json, "").isEmpty())
        assertTrue(parseGeminiModels(json, "not json").isEmpty())
        assertTrue(parseGeminiModels(json, "{}").isEmpty())
    }
}
