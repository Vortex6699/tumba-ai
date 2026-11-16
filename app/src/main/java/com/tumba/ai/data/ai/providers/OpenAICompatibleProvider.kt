package com.tumba.ai.data.ai.providers

import com.tumba.ai.data.ai.AiHttpClient
import com.tumba.ai.data.ai.baseUrlOrThrow
import com.tumba.ai.data.ai.checkConnection
import com.tumba.ai.data.ai.mergeAdjacentRoles
import com.tumba.ai.data.ai.retryIo
import com.tumba.ai.data.network.ModelDiscoveryApi
import com.tumba.ai.domain.ai.AIProvider
import com.tumba.ai.domain.ai.AiError
import com.tumba.ai.domain.ai.AiException
import com.tumba.ai.domain.ai.AiImage
import com.tumba.ai.domain.ai.AiMessage
import com.tumba.ai.domain.ai.AiRequest
import com.tumba.ai.domain.ai.ConnectionTest
import com.tumba.ai.domain.ai.ProviderCredential
import com.tumba.ai.domain.ai.StreamEvent
import com.tumba.ai.domain.model.MessageRole
import com.tumba.ai.domain.model.ModelInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Works with any /chat/completions endpoint: OpenAI, OpenRouter, Groq, Together,
 * LM Studio, Ollama's compatible API, or a user-provided gateway.
 */
@Singleton
class OpenAICompatibleProvider @Inject constructor(
    private val http: AiHttpClient,
    private val discovery: ModelDiscoveryApi,
    private val json: Json,
) : AIProvider {

    override val providerId: String = "openai-compatible"

    override fun listModels(): List<ModelInfo> = emptyList()

    override fun stream(
        request: AiRequest,
        credential: ProviderCredential,
    ): Flow<StreamEvent> = flow {
        ensureImagesSupported(request)
        val baseUrl = baseUrlOrThrow(credential.baseUrl)
        if (request.stream) {
            streamResponse(request, baseUrl, credential.apiKey).collect { emit(it) }
        } else {
            generateOnce(request, baseUrl, credential.apiKey).collect { emit(it) }
        }
    }

    override suspend fun testConnection(
        model: ModelInfo,
        credential: ProviderCredential,
    ): ConnectionTest {
        val baseUrl = baseUrlOrThrow(credential.baseUrl)
        val started = System.nanoTime()
        discovery.checkConnection(
            url = "$baseUrl/models",
            apiKeyHeader = null,
            bearerToken = credential.apiKey,
            json = json,
        )
        return ConnectionTest(latencyMs = (System.nanoTime() - started) / 1_000_000)
    }

    private fun streamResponse(
        request: AiRequest,
        baseUrl: String,
        apiKey: String?,
    ): Flow<StreamEvent> = flow {
        var promptTokens: Int? = null
        var completionTokens: Int? = null
        var receivedText = false
        var contentFiltered = false

        http.streamData(buildHttpRequest(request, baseUrl, apiKey, stream = true)).collect { payload ->
            if (payload == "[DONE]") return@collect
            val chunk = parseObject(payload) ?: return@collect
            chunk.errorOrNull()?.let { detail -> throw AiException(AiError.Server(200, detail)) }

            val choice = chunk["choices"]?.let { runCatching { it.jsonArray }.getOrNull() }
                ?.firstOrNull()
                ?.let { runCatching { it.jsonObject }.getOrNull() }

            val delta = choice?.get("delta")?.let { runCatching { it.jsonObject }.getOrNull() }
            val text = delta?.stringOrNull("content")
            if (!text.isNullOrEmpty()) {
                receivedText = true
                emit(StreamEvent.Delta(text))
            }
            if (choice?.stringOrNull("finish_reason") == "content_filter") contentFiltered = true

            chunk["usage"]?.let { runCatching { it.jsonObject }.getOrNull() }?.let { usage ->
                promptTokens = usage.intOrNull("prompt_tokens") ?: promptTokens
                completionTokens = usage.intOrNull("completion_tokens") ?: completionTokens
            }
        }

        if (contentFiltered && !receivedText) throw AiException(AiError.ContentBlocked)
        if (!receivedText) throw AiException(AiError.EmptyResponse)
        emit(StreamEvent.Completed(promptTokens, completionTokens))
    }

    private fun generateOnce(
        request: AiRequest,
        baseUrl: String,
        apiKey: String?,
    ): Flow<StreamEvent> = flow {
        val raw = retryIo { http.execute(buildHttpRequest(request, baseUrl, apiKey, stream = false)) }
        val root = parseObject(raw) ?: throw AiException(AiError.EmptyResponse)
        root.errorOrNull()?.let { detail -> throw AiException(AiError.Server(200, detail)) }

        val choice = root["choices"]?.let { runCatching { it.jsonArray }.getOrNull() }
            ?.firstOrNull()
            ?.let { runCatching { it.jsonObject }.getOrNull() }
        val message = choice?.get("message")?.let { runCatching { it.jsonObject }.getOrNull() }
        val text = message?.stringOrNull("content")
        if (text.isNullOrEmpty()) {
            if (choice?.stringOrNull("finish_reason") == "content_filter") {
                throw AiException(AiError.ContentBlocked)
            }
            throw AiException(AiError.EmptyResponse)
        }
        val usage = root["usage"]?.let { runCatching { it.jsonObject }.getOrNull() }
        emit(StreamEvent.Delta(text))
        emit(
            StreamEvent.Completed(
                promptTokens = usage?.intOrNull("prompt_tokens"),
                completionTokens = usage?.intOrNull("completion_tokens"),
            ),
        )
    }

    private fun buildHttpRequest(
        request: AiRequest,
        baseUrl: String,
        apiKey: String?,
        stream: Boolean,
    ): Request {
        val builder = Request.Builder()
            .url("$baseUrl/chat/completions")
            .header("Content-Type", JSON_CONTENT)
            .post(chatBody(request, stream).toString().toRequestBody(JSON_MEDIA_TYPE))
        if (!apiKey.isNullOrBlank()) builder.header("Authorization", "Bearer $apiKey")
        return builder.build()
    }

    private fun chatBody(request: AiRequest, stream: Boolean): JsonObject = buildJsonObject {
        put("model", request.model.id)
        put("stream", stream)
        put("temperature", request.temperature.toDouble())
        put("max_tokens", request.maxOutputTokens)
        if (stream) {
            putJsonObject("stream_options") { put("include_usage", true) }
        }
        putJsonArray("messages") {
            val systemPrompt = request.systemPrompt.takeIf {
                it.isNotBlank() && request.model.capabilities.supportsSystemPrompt
            }
            if (systemPrompt != null) {
                add(
                    buildJsonObject {
                        put("role", "system")
                        put("content", systemPrompt)
                    },
                )
            }
            request.messages.mergeAdjacentRoles().forEach { message ->
                add(messageBody(message))
            }
        }
    }

    private fun messageBody(message: AiMessage): JsonObject = buildJsonObject {
        put("role", roleName(message.role))
        if (message.images.isEmpty()) {
            put("content", message.content)
        } else {
            putJsonArray("content") {
                if (message.content.isNotBlank()) {
                    add(
                        buildJsonObject {
                            put("type", "text")
                            put("text", message.content)
                        },
                    )
                }
                message.images.forEach { image ->
                    add(
                        buildJsonObject {
                            put("type", "image_url")
                            putJsonObject("image_url") {
                                put("url", dataUrl(image))
                            }
                        },
                    )
                }
            }
        }
    }

    private fun roleName(role: MessageRole): String = when (role) {
        MessageRole.ASSISTANT -> "assistant"
        MessageRole.SYSTEM -> "system"
        MessageRole.USER -> "user"
    }

    private fun dataUrl(image: AiImage): String {
        val encoded = Base64.getEncoder().encodeToString(image.bytes)
        return "data:${image.mimeType};base64,$encoded"
    }

    private fun ensureImagesSupported(request: AiRequest) {
        if (request.messages.any { it.images.isNotEmpty() } &&
            !request.model.capabilities.supportsImages
        ) {
            throw AiException(AiError.ImagesNotSupported)
        }
    }

    private fun parseObject(raw: String): JsonObject? =
        runCatching { json.parseToJsonElement(raw).jsonObject }.getOrNull()

    private fun JsonObject.errorOrNull(): String? =
        this["error"]?.takeIf { it !is JsonNull }?.let { element ->
            when (element) {
                is JsonObject -> element.stringOrNull("message")
                is JsonPrimitive -> element.contentOrNull
                else -> null
            }
        }

    private fun JsonObject.stringOrNull(key: String): String? =
        this[key]?.let { element ->
            runCatching { element.jsonPrimitive.contentOrNull }.getOrNull()
        }

    private fun JsonObject.intOrNull(key: String): Int? =
        this[key]?.let { element ->
            runCatching { element.jsonPrimitive.intOrNull }.getOrNull()
        }

    private companion object {
        const val JSON_CONTENT = "application/json"
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
