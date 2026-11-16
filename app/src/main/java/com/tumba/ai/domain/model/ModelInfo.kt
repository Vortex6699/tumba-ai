package com.tumba.ai.domain.model

enum class ProviderKind { CLOUD, LOCAL }

data class ModelCapabilities(
    val supportsImages: Boolean = false,
    val supportsStreaming: Boolean = true,
    val supportsSystemPrompt: Boolean = true,
)

data class ModelInfo(
    val id: String,
    val providerId: String,
    val displayName: String,
    val capabilities: ModelCapabilities = ModelCapabilities(),
    val contextWindow: Int? = null,
    val isDefault: Boolean = false,
    val kind: ProviderKind = ProviderKind.CLOUD,
)

/**
 * The exact pair the user selected. Kept in DataStore so the choice survives restarts.
 */
data class ActiveModel(
    val providerId: String,
    val modelId: String,
)
