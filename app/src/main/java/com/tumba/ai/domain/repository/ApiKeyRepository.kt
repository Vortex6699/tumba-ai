package com.tumba.ai.domain.repository

import kotlinx.coroutines.flow.StateFlow

/**
 * API keys are addressed by provider id ("gemini", "deepseek", "custom:<uuid>").
 * Implementations must encrypt at rest; plaintext never touches disk or logs.
 */
interface ApiKeyRepository {
    val configuredProviders: StateFlow<Set<String>>

    fun hasKey(providerId: String): Boolean

    suspend fun setKey(providerId: String, key: String)

    suspend fun getKey(providerId: String): String?

    suspend fun removeKey(providerId: String)

    suspend fun clearAll()
}
