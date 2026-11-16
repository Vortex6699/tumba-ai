package com.tumba.ai.data.ai

import com.tumba.ai.core.ProviderIds
import com.tumba.ai.domain.ai.AIRepository
import com.tumba.ai.domain.ai.AiError
import com.tumba.ai.domain.ai.AiException
import com.tumba.ai.domain.ai.AiMessage
import com.tumba.ai.domain.ai.AiRequest
import com.tumba.ai.domain.ai.ConnectionTest
import com.tumba.ai.domain.ai.ProviderCredential
import com.tumba.ai.domain.ai.StreamEvent
import com.tumba.ai.domain.model.CustomProvider
import com.tumba.ai.domain.model.LocalModel
import com.tumba.ai.domain.model.LocalModelCatalog
import com.tumba.ai.domain.model.ModelCapabilities
import com.tumba.ai.domain.model.ModelInfo
import com.tumba.ai.domain.model.ProviderDefaults
import com.tumba.ai.domain.model.ProviderKind
import com.tumba.ai.domain.repository.ApiKeyRepository
import com.tumba.ai.domain.repository.CustomProviderRepository
import com.tumba.ai.domain.repository.LocalModelRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AIRepositoryImpl @Inject constructor(
    private val registry: ProviderRegistry,
    private val apiKeys: ApiKeyRepository,
    private val customProviders: CustomProviderRepository,
    private val localModels: LocalModelRepository,
) : AIRepository {

    override suspend fun availableModels(): List<ModelInfo> {
        val custom = customProviders.all().map { it.toModelInfo() }
        val local = localModels.models().map { it.toModelInfo() }
        return ModelCatalog.allCloud + custom + local
    }

    override suspend fun findModel(providerId: String, modelId: String): ModelInfo? {
        ModelCatalog.find(providerId, modelId)?.let { return it }
        val customId = ProviderIds.customId(providerId)
        if (customId != null) {
            return customProviders.get(customId)?.toModelInfo()
                ?.takeIf { it.id == modelId }
        }
        if (providerId == ProviderIds.LOCAL) {
            return localModels.getModel(modelId)?.toModelInfo()
        }
        if (providerId == ProviderIds.GEMINI && modelId.isNotBlank()) {
            // Discovered via models.list can outlive the built-in catalogue. Keeping a
            // generic descriptor lets the selection survive restarts until the next sync.
            return ModelInfo(
                id = modelId,
                providerId = ProviderIds.GEMINI,
                displayName = modelId,
                capabilities = ModelCapabilities(supportsImages = true),
                contextWindow = 1_048_576,
                isDefault = modelId == ProviderDefaults.GEMINI_MODEL,
            )
        }
        return null
    }

    override suspend fun discoverModels(providerId: String): List<ModelInfo> {
        val provider = registry.providerFor(providerId) ?: return emptyList()
        return provider.discoverModels(credentialFor(providerId))
    }

    override fun stream(
        model: ModelInfo,
        messages: List<AiMessage>,
        systemPrompt: String,
        temperature: Float,
        maxOutputTokens: Int,
        stream: Boolean,
    ): Flow<StreamEvent> = flow {
        val provider = registry.providerFor(model.providerId)
            ?: throw AiException(AiError.Unknown("No provider for ${model.providerId}"))
        val credential = credentialFor(model.providerId)
        val request = AiRequest(
            model = model,
            messages = messages,
            systemPrompt = systemPrompt,
            temperature = temperature,
            maxOutputTokens = maxOutputTokens,
            stream = stream,
        )
        provider.stream(request, credential).collect { event -> emit(event) }
    }

    override suspend fun testConnection(model: ModelInfo): ConnectionTest {
        val provider = registry.providerFor(model.providerId)
            ?: throw AiException(AiError.Unknown("No provider for ${model.providerId}"))
        return provider.testConnection(model, credentialFor(model.providerId))
    }

    private suspend fun credentialFor(providerId: String): ProviderCredential {
        if (providerId == ProviderIds.LOCAL) return ProviderCredential()
        val key = apiKeys.getKey(providerId)
        val baseUrl = ProviderIds.customId(providerId)?.let { id ->
            customProviders.get(id)?.baseUrl
        }
        return ProviderCredential(apiKey = key, baseUrl = baseUrl)
    }

    private fun CustomProvider.toModelInfo(): ModelInfo = ModelInfo(
        id = modelId,
        providerId = ProviderIds.custom(id),
        displayName = modelId,
        capabilities = ModelCapabilities(supportsImages = supportsImages),
        contextWindow = contextWindow,
        kind = ProviderKind.CLOUD,
    )

    private fun LocalModel.toModelInfo(): ModelInfo = ModelInfo(
        id = id,
        providerId = ProviderIds.LOCAL,
        displayName = displayName,
        capabilities = ModelCapabilities(supportsImages = false),
        contextWindow = LocalModelCatalog.find(id)?.contextWindow,
        kind = ProviderKind.LOCAL,
    )
}
