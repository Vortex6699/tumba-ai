package com.tumba.ai.domain.model

data class LocalModel(
    val id: String,
    val displayName: String,
    val fileName: String,
    val path: String,
    val sizeBytes: Long,
    val importedAt: Long,
    val quantization: String? = null,
)

data class CustomProvider(
    val id: String,
    val name: String,
    val baseUrl: String,
    val modelId: String,
    val supportsImages: Boolean,
    val contextWindow: Int?,
    val createdAt: Long,
)
