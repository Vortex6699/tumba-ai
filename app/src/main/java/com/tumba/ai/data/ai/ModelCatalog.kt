package com.tumba.ai.data.ai

import com.tumba.ai.domain.model.ModelCapabilities
import com.tumba.ai.domain.model.ModelInfo
import com.tumba.ai.domain.model.ProviderDefaults
import com.tumba.ai.domain.model.ProviderKind

/**
 * Curated catalogue of built-in cloud models. Gemini "latest" aliases are used so a model
 * retirement does not break new installations; the Models screen can additionally sync the
 * live list from the provider.
 */
object ModelCatalog {

    val gemini: List<ModelInfo> = listOf(
        ModelInfo(
            id = ProviderDefaults.GEMINI_MODEL,
            providerId = "gemini",
            displayName = "Gemini Flash (latest)",
            capabilities = ModelCapabilities(supportsImages = true),
            contextWindow = 1_048_576,
            isDefault = true,
        ),
        ModelInfo(
            id = "gemini-pro-latest",
            providerId = "gemini",
            displayName = "Gemini Pro (latest)",
            capabilities = ModelCapabilities(supportsImages = true),
            contextWindow = 1_048_576,
        ),
        ModelInfo(
            id = "gemini-flash-lite-latest",
            providerId = "gemini",
            displayName = "Gemini Flash Lite (latest)",
            capabilities = ModelCapabilities(supportsImages = true),
            contextWindow = 1_048_576,
        ),
        ModelInfo(
            id = "gemini-2.5-pro",
            providerId = "gemini",
            displayName = "Gemini 2.5 Pro",
            capabilities = ModelCapabilities(supportsImages = true),
            contextWindow = 1_048_576,
        ),
        ModelInfo(
            id = "gemini-2.5-flash",
            providerId = "gemini",
            displayName = "Gemini 2.5 Flash",
            capabilities = ModelCapabilities(supportsImages = true),
            contextWindow = 1_048_576,
        ),
        ModelInfo(
            id = "gemini-2.5-flash-lite",
            providerId = "gemini",
            displayName = "Gemini 2.5 Flash Lite",
            capabilities = ModelCapabilities(supportsImages = true),
            contextWindow = 1_048_576,
        ),
    )

    val deepSeek: List<ModelInfo> = listOf(
        ModelInfo(
            id = "deepseek-chat",
            providerId = "deepseek",
            displayName = "DeepSeek Chat",
            capabilities = ModelCapabilities(supportsImages = false),
            contextWindow = 128_000,
            isDefault = true,
        ),
        ModelInfo(
            id = "deepseek-reasoner",
            providerId = "deepseek",
            displayName = "DeepSeek Reasoner",
            capabilities = ModelCapabilities(supportsImages = false),
            contextWindow = 128_000,
        ),
    )

    val allCloud: List<ModelInfo> = gemini + deepSeek

    fun find(providerId: String, modelId: String): ModelInfo? =
        allCloud.firstOrNull { it.providerId == providerId && it.id == modelId }

    fun modelsFor(providerId: String): List<ModelInfo> =
        allCloud.filter { it.providerId == providerId }

    fun localModel(modelId: String, displayName: String): ModelInfo = ModelInfo(
        id = modelId,
        providerId = "local",
        displayName = displayName,
        capabilities = ModelCapabilities(
            supportsImages = false,
            supportsStreaming = true,
            supportsSystemPrompt = true,
        ),
        contextWindow = null,
        kind = ProviderKind.LOCAL,
    )
}
