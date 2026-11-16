package com.tumba.ai.data.repository

import com.tumba.ai.core.DispatcherProvider
import com.tumba.ai.data.local.dao.CustomProviderDao
import com.tumba.ai.data.local.entity.CustomProviderEntity
import com.tumba.ai.domain.model.CustomProvider
import com.tumba.ai.domain.repository.CustomProviderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CustomProviderRepositoryImpl @Inject constructor(
    private val dao: CustomProviderDao,
    private val dispatchers: DispatcherProvider,
) : CustomProviderRepository {

    override fun observeAll(): Flow<List<CustomProvider>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun all(): List<CustomProvider> = withContext(dispatchers.io) {
        dao.getAll().map { it.toDomain() }
    }

    override suspend fun get(id: String): CustomProvider? = withContext(dispatchers.io) {
        dao.getById(id)?.toDomain()
    }

    override suspend fun add(
        name: String,
        baseUrl: String,
        modelId: String,
        supportsImages: Boolean,
        contextWindow: Int?,
    ): CustomProvider = withContext(dispatchers.io) {
        val entity = CustomProviderEntity(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            baseUrl = baseUrl.trim().trimEnd('/'),
            modelId = modelId.trim(),
            supportsImages = supportsImages,
            contextWindow = contextWindow,
            createdAt = System.currentTimeMillis(),
        )
        dao.upsert(entity)
        entity.toDomain()
    }

    override suspend fun update(
        id: String,
        name: String,
        baseUrl: String,
        modelId: String,
        supportsImages: Boolean,
        contextWindow: Int?,
    ) {
        withContext(dispatchers.io) {
            val existing = dao.getById(id) ?: return@withContext
            dao.upsert(
                existing.copy(
                    name = name.trim(),
                    baseUrl = baseUrl.trim().trimEnd('/'),
                    modelId = modelId.trim(),
                    supportsImages = supportsImages,
                    contextWindow = contextWindow,
                ),
            )
        }
    }

    override suspend fun delete(id: String) {
        withContext(dispatchers.io) { dao.delete(id) }
    }

    private fun CustomProviderEntity.toDomain(): CustomProvider = CustomProvider(
        id = id,
        name = name,
        baseUrl = baseUrl,
        modelId = modelId,
        supportsImages = supportsImages,
        contextWindow = contextWindow,
        createdAt = createdAt,
    )
}
