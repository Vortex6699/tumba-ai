package com.tumba.ai.domain.ai

import com.tumba.ai.domain.model.MessageRole
import com.tumba.ai.domain.model.ModelInfo
import kotlinx.coroutines.flow.Flow

data class AiImage(
    val bytes: ByteArray,
    val mimeType: String,
)

data class AiMessage(
    val role: MessageRole,
    val content: String,
    val images: List<AiImage> = emptyList(),
)

data class AiRequest(
    val model: ModelInfo,
    val messages: List<AiMessage>,
    val systemPrompt: String,
    val temperature: Float,
    val maxOutputTokens: Int,
    val stream: Boolean,
)

/**
 * Only successful stream items are modeled here; failures are thrown as [AiException]
 * so cancellation and retry semantics stay unambiguous.
 */
sealed interface StreamEvent {
    data class Delta(val text: String) : StreamEvent
    data class Completed(
        val promptTokens: Int? = null,
        val completionTokens: Int? = null,
    ) : StreamEvent
}

data class ProviderCredential(
    val apiKey: String? = null,
    val baseUrl: String? = null,
)

data class ConnectionTest(
    val latencyMs: Long,
)

/**
 * The single abstraction every cloud/custom/local backend implements.
 * ChatViewModel never knows which provider it is talking to.
 */
interface AIProvider {
    val providerId: String

    fun listModels(): List<ModelInfo>

    /**
     * Optional live model discovery. Providers that cannot list models return no items.
     */
    suspend fun discoverModels(credential: ProviderCredential): List<ModelInfo> = emptyList()

    fun stream(request: AiRequest, credential: ProviderCredential): Flow<StreamEvent>

    suspend fun testConnection(model: ModelInfo, credential: ProviderCredential): ConnectionTest
}
