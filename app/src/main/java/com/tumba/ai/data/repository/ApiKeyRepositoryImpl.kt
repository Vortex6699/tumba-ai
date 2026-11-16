package com.tumba.ai.data.repository

import com.tumba.ai.core.DispatcherProvider
import com.tumba.ai.data.security.SecretStore
import com.tumba.ai.domain.repository.ApiKeyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiKeyRepositoryImpl @Inject constructor(
    private val store: SecretStore,
    private val dispatchers: DispatcherProvider,
) : ApiKeyRepository {

    private val configured = MutableStateFlow(store.aliases())

    override val configuredProviders: StateFlow<Set<String>> = configured.asStateFlow()

    override fun hasKey(providerId: String): Boolean = providerId in configured.value

    override suspend fun setKey(providerId: String, key: String) {
        withContext(dispatchers.io) {
            store.put(providerId, key.trim())
            configured.value = store.aliases()
        }
    }

    override suspend fun getKey(providerId: String): String? = withContext(dispatchers.io) {
        store.get(providerId)
    }

    override suspend fun removeKey(providerId: String) {
        withContext(dispatchers.io) {
            store.remove(providerId)
            configured.value = store.aliases()
        }
    }

    override suspend fun clearAll() {
        withContext(dispatchers.io) {
            store.clear()
            configured.value = emptySet()
        }
    }
}
