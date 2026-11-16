package com.tumba.ai.data.ai

import com.tumba.ai.core.ProviderIds
import com.tumba.ai.data.ai.providers.DeepSeekProvider
import com.tumba.ai.data.ai.providers.GeminiProvider
import com.tumba.ai.data.ai.providers.LocalProvider
import com.tumba.ai.data.ai.providers.OpenAICompatibleProvider
import com.tumba.ai.domain.ai.AIProvider
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Maps a provider id to its implementation. Custom OpenAI-compatible endpoints all share
 * [OpenAICompatibleProvider]; the per-endpoint base URL travels with the credential.
 */
@Singleton
class ProviderRegistry @Inject constructor(
    private val gemini: GeminiProvider,
    private val deepSeek: DeepSeekProvider,
    private val openAiCompatible: OpenAICompatibleProvider,
    private val local: LocalProvider,
) {
    fun providerFor(providerId: String): AIProvider? = when {
        providerId == ProviderIds.GEMINI -> gemini
        providerId == ProviderIds.DEEPSEEK -> deepSeek
        providerId == ProviderIds.LOCAL -> local
        ProviderIds.isCustom(providerId) -> openAiCompatible
        else -> null
    }
}
