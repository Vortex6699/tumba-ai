package com.tumba.ai.data.ai.providers

import com.tumba.ai.core.ProviderIds
import com.tumba.ai.data.ai.AiHttpClient
import com.tumba.ai.data.ai.ModelCatalog
import com.tumba.ai.data.ai.aiErrorFor
import com.tumba.ai.data.ai.checkConnection
import com.tumba.ai.data.ai.extractApiError
import com.tumba.ai.data.ai.mergeAdjacentRoles
import com.tumba.ai.data.ai.requireApiKey
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
import com.tumba.ai.domain.model.ModelCapabilities
import com.tumba.ai.domain.model.ModelInfo
import com.tumba.ai.domain.model.ProviderDefaults
import com.tumba.ai.domain.model.ProviderKind
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
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

@Singleton
class GeminiProvider @Inject constructor(
    private val http: AiHttpClient,
    private val discovery: ModelDiscoveryApi,
    private val json: Json,
) : AIProvider {

    override val providerId: String = ProviderIds.GEMINI

    override fun listModels(): List<ModelInfo> = ModelCatalog.gemini

    /**
     * Reads the models currently available to this key. This is what prevents retired
     * hard-coded IDs from producing endless 404s: the Models screen can refresh from the
     * provider's own list.
     */
    override suspend fun discoverModels(credential: ProviderCredential): List<ModelInfo> {
        val apiKey = requireApiKey(credential.apiKey)
        val response = retryIo {
            discovery.get("$BASE_URL/models?pageSize=1000", null, apiKey)
        }
        if (!response.isSuccessful) {
            val detail = runCatching { response.errorBody()?.string() }.getOrNull()
            response.errorBody()?.close()
            response.body()?.close()
            throw AiException(aiErrorFor(response.code(), extractApiError(json, detail)))
        }
        val raw = response.body()?.string().orEmpty()
        return parseGeminiModels(json, raw)
    }

    override fun stream(
        request: AiRequest,
        credential: ProviderCredential,
    ): Flow<StreamEvent> = flow {
        val apiKey = requireApiKey(credential.apiKey)
        ensureImagesSupported(request)
        if (request.stream) {
            streamResponse(request, apiKey).collect { event -> emit(event) }
        } else {
            generateOnce(request, apiKey).collect { event -> emit(event) }
        }
    }

    override suspend fun testConnection(
        model: ModelInfo,
        credential: ProviderCredential,
    ): ConnectionTest {
        val apiKey = requireApiKey(credential.apiKey)
        val started = System.nanoTime()
        discovery.checkConnection(
            url = "$BASE_URL/models/${model.id}",
            apiKeyHeader = apiKey,
            bearerToken = null,
            json = json,
        )
        return ConnectionTest(latencyMs = (System.nanoTime() - started) / 1_000_000)
    }

    private fun streamResponse(request: AiRequest, apiKey: String): Flow<StreamEvent> = flow {
        var promptTokens: Int? = null
        var completionTokens: Int? = null
        var receivedText = false
        var blocked = false

        http.streamData(buildHttpRequest(request, apiKey, stream = true)).collect { payload ->
            if (payload == "[DONE]") return@collect
            val chunk = parseObject(payload) ?: return@collect
            chunk.errorOrNull()?.let { detail -> throw AiException(AiError.Server(200, detail)) }

            val candidate = chunk["candidates"]?.asArray()?.firstOrNull()?.asObject()
            val text = candidate?.textOfParts().orEmpty()
            if (text.isNotEmpty()) {
                receivedText = true
                emit(StreamEvent.Delta(text))
            }
            val finishReason = candidate?.stringOrNull("finishReason")
            if (finishReason != null && finishReason in BLOCKED_REASONS) blocked = true

            chunk["usageMetadata"]?.asObject()?.let { usage ->
                promptTokens = usage.intOrNull("promptTokenCount") ?: promptTokens
                completionTokens = usage.intOrNull("candidatesTokenCount") ?: completionTokens
            }

            val blockReason = chunk["promptFeedback"]?.asObject()?.stringOrNull("blockReason")
            if (blockReason != null) throw AiException(AiError.ContentBlocked)
        }

        if (blocked && !receivedText) throw AiException(AiError.ContentBlocked)
        if (!receivedText) throw AiException(AiError.EmptyResponse)
        emit(StreamEvent.Completed(promptTokens, completionTokens))
    }

    private fun generateOnce(request: AiRequest, apiKey: String): Flow<StreamEvent> = flow {
        val raw = http.execute(buildHttpRequest(request, apiKey, stream = false))
        val root = parseObject(raw) ?: throw AiException(AiError.EmptyResponse)
        root.errorOrNull()?.let { detail -> throw AiException(AiError.Server(200, detail)) }

        val candidate = root["candidates"]?.asArray()?.firstOrNull()?.asObject()
        val text = candidate?.textOfParts().orEmpty()
        if (text.isEmpty()) {
            val finishReason = candidate?.stringOrNull("finishReason")
            if (finishReason != null && finishReason in BLOCKED_REASONS) {
                throw AiException(AiError.ContentBlocked)
            }
            throw AiException(AiError.EmptyResponse)
        }
        val usage = root["usageMetadata"]?.asObject()
        emit(StreamEvent.Delta(text))
        emit(
            StreamEvent.Completed(
                promptTokens = usage?.intOrNull("promptTokenCount"),
                completionTokens = usage?.intOrNull("candidatesTokenCount"),
            ),
        )
    }

    private fun buildHttpRequest(
        request: AiRequest,
        apiKey: String,
        stream: Boolean,
    ): Request {
        val endpoint = if (stream) "streamGenerateContent?alt=sse" else "generateContent"
        return Request.Builder()
            .url("$BASE_URL/models/${request.model.id}:$endpoint")
            .header("x-goog-api-key", apiKey)
            .header("Content-Type", JSON_CONTENT)
            .post(requestBody(request).toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()
    }

    private fun requestBody(request: AiRequest): JsonObject = buildJsonObject {
        val systemPrompt = request.systemPrompt.takeIf {
            it.isNotBlank() && request.model.capabilities.supportsSystemPrompt
        }
        if (systemPrompt != null) {
            putJsonObject("systemInstruction") {
                putJsonArray("parts") {
                    add(buildJsonObject { put("text", systemPrompt) })
                }
            }
        }
        putJsonArray("contents") {
            request.messages.mergeAdjacentRoles().forEach { message ->
                add(
                    buildJsonObject {
                        put(
                            "role",
                            if (message.role == MessageRole.ASSISTANT) "model" else "user",
                        )
                        putJsonArray("parts") {
                            if (message.content.isNotBlank()) {
                                add(buildJsonObject { put("text", message.content) })
                            }
                            message.images.forEach { image -> add(imagePart(image)) }
                        }
                    },
                )
            }
        }
        putJsonObject("generationConfig") {
            put("temperature", request.temperature.toDouble())
            put("maxOutputTokens", request.maxOutputTokens)
            put("candidateCount", 1)
        }
    }

    private fun imagePart(image: AiImage): JsonObject = buildJsonObject {
        putJsonObject("inlineData") {
            put("mimeType", image.mimeType)
            put("data", Base64.getEncoder().encodeToString(image.bytes))
        }
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

    private fun JsonObject.textOfParts(): String? {
        val parts = this["content"]?.asObject()?.get("parts")?.asArray() ?: return null
        val text = parts.mapNotNull { part ->
            part.asObject()?.get("text")?.asPrimitive()?.contentOrNull
        }.joinToString("")
        return text.ifEmpty { null }
    }

    private fun JsonElement.asObject(): JsonObject? = runCatching { jsonObject }.getOrNull()

    private fun JsonElement.asArray(): JsonArray? = runCatching { jsonArray }.getOrNull()

    private fun JsonElement.asPrimitive(): JsonPrimitive? = runCatching { jsonPrimitive }.getOrNull()

    private fun JsonObject.stringOrNull(key: String): String? =
        this[key]?.asPrimitive()?.contentOrNull

    private fun JsonObject.intOrNull(key: String): Int? =
        this[key]?.asPrimitive()?.intOrNull

    private companion object {
        const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta"
        const val JSON_CONTENT = "application/json"
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        val BLOCKED_REASONS = setOf("SAFETY", "RECITATION", "BLOCKLIST", "PROHIBITED_CONTENT")
    }
}

/**
 * Parses `GET /v1beta/models`. Kept as a pure function so it can be unit tested without
 * network access. Non-chat models (embeddings, images, TTS, Live) are filtered out.
 */
internal fun parseGeminiModels(json: Json, body: String): List<ModelInfo> {
    val root = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull()
        ?: return emptyList()
    val models = root["models"]?.let { element ->
        runCatching { element.jsonArray }.getOrNull()
    } ?: return emptyList()

    val excluded = listOf(
        "embedding",
        "image",
        "tts",
        "audio",
        "live",
        "veo",
        "imagen",
        "deep-research",
        "computer-use",
    )

    return models.mapNotNull { element ->
        val obj = runCatching { element.jsonObject }.getOrNull() ?: return@mapNotNull null
        val id = obj["name"]?.let { name ->
            runCatching { name.jsonPrimitive.contentOrNull }.getOrNull()
        }?.removePrefix("models/")?.takeIf { it.isNotBlank() } ?: return@mapNotNull null

        val methods = obj["supportedGenerationMethods"]?.let { methodsElement ->
            runCatching { methodsElement.jsonArray }.getOrNull()
        }?.mapNotNull { method ->
            runCatching { method.jsonPrimitive.contentOrNull }.getOrNull()
        }.orEmpty()
        if ("generateContent" !in methods) return@mapNotNull null
        if (excluded.any { id.contains(it, ignoreCase = true) }) return@mapNotNull null

        val displayName = obj["displayName"]?.let { name ->
            runCatching { name.jsonPrimitive.contentOrNull }.getOrNull()
        }?.takeIf { it.isNotBlank() } ?: id

        val inputTokenLimit = obj["inputTokenLimit"]?.let { limit ->
            runCatching { limit.jsonPrimitive.intOrNull }.getOrNull()
        }

        ModelInfo(
            id = id,
            providerId = ProviderIds.GEMINI,
            displayName = displayName,
            capabilities = ModelCapabilities(supportsImages = true),
            contextWindow = inputTokenLimit,
            isDefault = id == ProviderDefaults.GEMINI_MODEL,
        )
    }.distinctBy { it.id }
}
