package com.tumba.ai.data.ai.providers

import com.tumba.ai.core.ProviderIds
import com.tumba.ai.data.ai.local.LocalEngineStatus
import com.tumba.ai.data.ai.local.LocalInferenceEngine
import com.tumba.ai.domain.ai.AIProvider
import com.tumba.ai.domain.ai.AiError
import com.tumba.ai.domain.ai.AiException
import com.tumba.ai.domain.ai.AiRequest
import com.tumba.ai.domain.ai.ConnectionTest
import com.tumba.ai.domain.ai.ProviderCredential
import com.tumba.ai.domain.ai.StreamEvent
import com.tumba.ai.domain.model.ModelInfo
import com.tumba.ai.domain.repository.LocalModelRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Runs a GGUF model through the bundled llama.cpp engine. The engine receives the structured
 * conversation so the model's own chat template can be applied; images are rejected because
 * local vision models are not part of the catalogue.
 */
@Singleton
class LocalProvider @Inject constructor(
    private val engine: LocalInferenceEngine,
    private val localModels: LocalModelRepository,
) : AIProvider {

    override val providerId: String = ProviderIds.LOCAL

    override fun listModels(): List<ModelInfo> = emptyList()

    override fun stream(
        request: AiRequest,
        credential: ProviderCredential,
    ): Flow<StreamEvent> = flow {
        if (engine.status !is LocalEngineStatus.Ready) {
            throw AiException(AiError.LocalEngineUnavailable(engine.unavailableReason()))
        }
        if (request.messages.any { it.images.isNotEmpty() }) {
            throw AiException(AiError.ImagesNotSupported)
        }
        val local = localModels.getModel(request.model.id)
            ?: throw AiException(AiError.Unknown("Local model not found"))
        engine.generate(
            modelPath = local.path,
            systemPrompt = request.systemPrompt,
            messages = request.messages,
            maxTokens = request.maxOutputTokens,
            temperature = request.temperature,
            contextWindow = request.model.contextWindow,
        ).collect { token -> emit(StreamEvent.Delta(token)) }
        emit(StreamEvent.Completed())
    }

    override suspend fun testConnection(
        model: ModelInfo,
        credential: ProviderCredential,
    ): ConnectionTest {
        if (engine.status !is LocalEngineStatus.Ready) {
            throw AiException(AiError.LocalEngineUnavailable(engine.unavailableReason()))
        }
        val local = localModels.getModel(model.id)
            ?: throw AiException(AiError.Unknown("Local model not found"))
        if (local.path.isBlank()) {
            throw AiException(AiError.Unknown("The model file is missing"))
        }
        return ConnectionTest(latencyMs = 0L)
    }
}