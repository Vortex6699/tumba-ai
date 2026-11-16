package com.tumba.ai.domain.repository

import com.tumba.ai.domain.model.CustomProvider
import kotlinx.coroutines.flow.Flow

interface CustomProviderRepository {
    fun observeAll(): Flow<List<CustomProvider>>

    suspend fun all(): List<CustomProvider>

    suspend fun get(id: String): CustomProvider?

    suspend fun add(
        name: String,
        baseUrl: String,
        modelId: String,
        supportsImages: Boolean,
        contextWindow: Int?,
    ): CustomProvider

    suspend fun update(
        id: String,
        name: String,
        baseUrl: String,
        modelId: String,
        supportsImages: Boolean,
        contextWindow: Int?,
    )

    suspend fun delete(id: String)
}
