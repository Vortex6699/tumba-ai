package com.tumba.ai.data.ai.providers

import com.tumba.ai.core.ProviderIds
import com.tumba.ai.data.ai.ModelCatalog
import com.tumba.ai.data.ai.requireApiKey
import com.tumba.ai.domain.ai.AIProvider
import com.tumba.ai.domain.ai.AiRequest
import com.tumba.ai.domain.ai.ConnectionTest
import com.tumba.ai.domain.ai.ProviderCredential
import com.tumba.ai.domain.ai.StreamEvent
import com.tumba.ai.domain.model.ModelInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DeepSeek exposes an OpenAI-compatible API. This adapter pins the endpoint and requires
 * a key, while all request/stream logic is shared with [OpenAICompatibleProvider].
 */
@Singleton
class DeepSeekProvider @Inject constructor(
    private val delegate: OpenAICompatibleProvider,
) : AIProvider {

    override val providerId: String = ProviderIds.DEEPSEEK

    override fun listModels(): List<ModelInfo> = ModelCatalog.deepSeek

    override fun stream(
        request: AiRequest,
        credential: ProviderCredential,
    ): Flow<StreamEvent> = flow {
        requireApiKey(credential.apiKey)
        delegate.stream(request, credential.copy(baseUrl = BASE_URL)).collect { emit(it) }
    }

    override suspend fun testConnection(
        model: ModelInfo,
        credential: ProviderCredential,
    ): ConnectionTest {
        requireApiKey(credential.apiKey)
        return delegate.testConnection(model, credential.copy(baseUrl = BASE_URL))
    }

    companion object {
        const val BASE_URL = "https://api.deepseek.com"
    }
}
