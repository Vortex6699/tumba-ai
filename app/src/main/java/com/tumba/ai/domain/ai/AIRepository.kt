package com.tumba.ai.domain.ai

import com.tumba.ai.domain.model.ModelInfo
import kotlinx.coroutines.flow.Flow

/**
 * The only entry point the presentation layer has into model inference.
 */
interface AIRepository {
    suspend fun availableModels(): List<ModelInfo>

    /**
     * Fetches the provider's current model list when supported (currently Gemini).
     */
    suspend fun discoverModels(providerId: String): List<ModelInfo>

    suspend fun findModel(providerId: String, modelId: String): ModelInfo?

    fun stream(
        model: ModelInfo,
        messages: List<AiMessage>,
        systemPrompt: String,
        temperature: Float,
        maxOutputTokens: Int,
        stream: Boolean,
    ): Flow<StreamEvent>

    suspend fun testConnection(model: ModelInfo): ConnectionTest
}
