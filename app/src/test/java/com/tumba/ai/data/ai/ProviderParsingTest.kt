package com.tumba.ai.data.ai

import com.tumba.ai.domain.ai.AiError
import com.tumba.ai.domain.ai.AiMessage
import com.tumba.ai.domain.model.MessageRole
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProviderParsingTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun extractsOpenAiStyleErrorMessage() {
        val body = """{"error":{"message":"Incorrect API key provided","type":"invalid_request_error"}}"""
        assertEquals("Incorrect API key provided", extractApiError(json, body))
    }

    @Test
    fun extractsGeminiStyleErrorMessage() {
        val body = """{"error":{"code":400,"message":"API key not valid. Please pass a valid API key."}}"""
        assertEquals(
            "API key not valid. Please pass a valid API key.",
            extractApiError(json, body),
        )
    }

    @Test
    fun toleratesNonJsonBodies() {
        assertNull(extractApiError(json, "<html>gateway error</html>"))
        assertNull(extractApiError(json, null))
    }

    @Test
    fun maps404ToModelNotFound() {
        assertEquals(
            AiError.ModelNotFound,
            aiErrorFor(404, """{"error":{"message":"models/gemini-2.0-flash is not found"}}"""),
        )
    }

    @Test
    fun mergesConsecutiveMessagesWithTheSameRole() {
        val merged = listOf(
            AiMessage(MessageRole.USER, "first"),
            AiMessage(MessageRole.USER, "second"),
            AiMessage(MessageRole.ASSISTANT, "answer"),
            AiMessage(MessageRole.USER, "third"),
        ).mergeAdjacentRoles()

        assertEquals(3, merged.size)
        assertEquals("first\n\nsecond", merged[0].content)
        assertEquals(MessageRole.ASSISTANT, merged[1].role)
        assertEquals("third", merged[2].content)
    }

    @Test
    fun dropsCompletelyEmptyMessages() {
        val merged = listOf(
            AiMessage(MessageRole.USER, ""),
            AiMessage(MessageRole.USER, "real"),
        ).mergeAdjacentRoles()

        assertEquals(1, merged.size)
        assertEquals("real", merged.single().content)
    }
}
