package com.tumba.ai.data.ai.providers

import com.tumba.ai.data.ai.AiHttpClient
import com.tumba.ai.data.network.ModelDiscoveryApi
import com.tumba.ai.domain.ai.AiError
import com.tumba.ai.domain.ai.AiException
import com.tumba.ai.domain.ai.AiMessage
import com.tumba.ai.domain.ai.AiRequest
import com.tumba.ai.domain.ai.ProviderCredential
import com.tumba.ai.domain.ai.StreamEvent
import com.tumba.ai.domain.model.MessageRole
import com.tumba.ai.domain.model.ModelCapabilities
import com.tumba.ai.domain.model.ModelInfo
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class OpenAICompatibleProviderTest {

    private lateinit var server: MockWebServer
    private lateinit var provider: OpenAICompatibleProvider

    private val json = Json { ignoreUnknownKeys = true }

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        provider = OpenAICompatibleProvider(
            http = AiHttpClient(OkHttpClient(), json),
            discovery = UnusedDiscoveryApi,
            json = json,
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun streamsSseDeltasAndUsage() = runBlocking {
        val body = buildString {
            append("data: {\"choices\":[{\"delta\":{\"content\":\"Hello\"}}]}\n\n")
            append("data: {\"choices\":[{\"delta\":{\"content\":\" world\"}}]}\n\n")
            append(
                "data: {\"choices\":[{\"delta\":{},\"finish_reason\":\"stop\"}]," +
                    "\"usage\":{\"prompt_tokens\":3,\"completion_tokens\":2}}\n\n",
            )
            append("data: [DONE]\n\n")
        }
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "text/event-stream")
                .setBody(body),
        )

        val events = provider.stream(request(stream = true), credential()).toList()

        val text = events.filterIsInstance<StreamEvent.Delta>().joinToString("") { it.text }
        assertEquals("Hello world", text)
        val completed = events.filterIsInstance<StreamEvent.Completed>().single()
        assertEquals(3, completed.promptTokens)
        assertEquals(2, completed.completionTokens)

        val recorded = server.takeRequest()
        assertEquals("/v1/chat/completions", recorded.path)
        assertTrue(recorded.getHeader("Authorization")!!.startsWith("Bearer "))
    }

    @Test
    fun sendsImagesOnlyForVisionModels() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "text/event-stream")
                .setBody("data: {\"choices\":[{\"delta\":{\"content\":\"ok\"}}]}\n\ndata: [DONE]\n\n"),
        )

        val imageMessage = AiMessage(
            role = MessageRole.USER,
            content = "describe",
            images = listOf(
                com.tumba.ai.domain.ai.AiImage(
                    bytes = byteArrayOf(1, 2, 3),
                    mimeType = "image/png",
                ),
            ),
        )
        val events = provider.stream(
            request(stream = true, supportsImages = true).copy(messages = listOf(imageMessage)),
            credential(),
        ).toList()

        assertEquals("ok", events.filterIsInstance<StreamEvent.Delta>().single().text)
        val requestBody = server.takeRequest().body.readUtf8()
        assertTrue(requestBody.contains("data:image/png;base64,"))
    }

    @Test
    fun rejectsImagesForTextOnlyModels() = runBlocking {
        val imageMessage = AiMessage(
            role = MessageRole.USER,
            content = "describe",
            images = listOf(
                com.tumba.ai.domain.ai.AiImage(byteArrayOf(1), "image/png"),
            ),
        )
        val error = expectAiError {
            provider.stream(
                request(stream = true, supportsImages = false).copy(messages = listOf(imageMessage)),
                credential(),
            ).toList()
        }
        assertEquals(AiError.ImagesNotSupported, error)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun mapsUnauthorizedResponseToInvalidKey() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"error":{"message":"Incorrect API key provided"}}"""),
        )

        val error = expectAiError {
            provider.stream(request(stream = true), credential()).toList()
        }
        assertEquals(AiError.InvalidApiKey, error)
    }

    private suspend fun expectAiError(block: suspend () -> Unit): AiError {
        return try {
            block()
            throw AssertionError("Expected AiException")
        } catch (exception: AiException) {
            exception.error
        }
    }

    private fun request(stream: Boolean, supportsImages: Boolean = false): AiRequest = AiRequest(
        model = ModelInfo(
            id = "test-model",
            providerId = "custom:test",
            displayName = "Test model",
            capabilities = ModelCapabilities(supportsImages = supportsImages),
        ),
        messages = listOf(AiMessage(MessageRole.USER, "Hello")),
        systemPrompt = "Be brief.",
        temperature = 0.4f,
        maxOutputTokens = 128,
        stream = stream,
    )

    private fun credential(): ProviderCredential = ProviderCredential(
        apiKey = "test-key",
        baseUrl = server.url("/v1").toString().trimEnd('/'),
    )

    private object UnusedDiscoveryApi : ModelDiscoveryApi {
        override suspend fun get(
            url: String,
            authorization: String?,
            apiKey: String?,
        ): Response<ResponseBody> = throw UnsupportedOperationException("Not used in streaming tests")
    }
}
