package com.tumba.ai.ui.models

import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tumba.ai.R
import com.tumba.ai.core.LocalModelException
import com.tumba.ai.core.LocalModelFailure
import com.tumba.ai.core.ProviderIds
import com.tumba.ai.data.ai.local.LocalInferenceEngine
import com.tumba.ai.domain.ai.AIRepository
import com.tumba.ai.domain.ai.AiError
import com.tumba.ai.domain.ai.AiException
import com.tumba.ai.domain.model.ActiveModel
import com.tumba.ai.domain.model.AppSettings
import com.tumba.ai.domain.model.CustomProvider
import com.tumba.ai.domain.model.DownloadableModel
import com.tumba.ai.domain.model.LocalModel
import com.tumba.ai.domain.model.ModelInfo
import com.tumba.ai.domain.model.ProviderDefaults
import com.tumba.ai.domain.repository.ApiKeyRepository
import com.tumba.ai.domain.repository.CustomProviderRepository
import com.tumba.ai.domain.repository.LocalModelRepository
import com.tumba.ai.domain.repository.ModelDownloadState
import com.tumba.ai.domain.repository.SettingsRepository
import com.tumba.ai.ui.util.UiNotice
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ModelsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val aiRepository: AIRepository,
    private val customProviderRepository: CustomProviderRepository,
    private val localModelRepository: LocalModelRepository,
    private val apiKeys: ApiKeyRepository,
    private val engine: LocalInferenceEngine,
) : ViewModel() {

    private val _state = MutableStateFlow(
        ModelsUiState(engineStatus = engine.status),
    )
    val state = _state.asStateFlow()

    private var lastSnapshot: Snapshot? = null
    private var autoSyncAttempted = false

    init {
        viewModelScope.launch {
            combine(
                settingsRepository.settings,
                apiKeys.configuredProviders,
                customProviderRepository.observeAll(),
                localModelRepository.observeModels(),
            ) { settings, keys, customProviders, localModels ->
                Snapshot(settings, keys, customProviders, localModels)
            }.collect { snapshot ->
                lastSnapshot = snapshot
                applySnapshot(snapshot)
            }
        }
    }

    private suspend fun applySnapshot(snapshot: Snapshot) {
        val resolvedModel = aiRepository.findModel(
            snapshot.settings.activeProviderId,
            snapshot.settings.activeModelId,
        )
        val activeModel = resolvedModel?.let { model ->
            _state.value.remoteModels[model.providerId]
                ?.firstOrNull { it.id == model.id }
                ?: model
        }
        val allModels = aiRepository.availableModels()
        val grouped = allModels.filter { it.providerId != ProviderIds.LOCAL }
            .groupBy { it.providerId }

        val orderedProviderIds = buildList {
            add(ProviderIds.GEMINI)
            add(ProviderIds.DEEPSEEK)
            snapshot.customProviders.forEach { add(ProviderIds.custom(it.id)) }
        }.distinct()

        val sections = orderedProviderIds.mapNotNull { providerId ->
            val remote = _state.value.remoteModels[providerId].orEmpty()
            val models = (remote + grouped[providerId].orEmpty()).distinctBy { it.id }
            if (models.isEmpty()) return@mapNotNull null
            val customName = ProviderIds.customId(providerId)?.let { customId ->
                snapshot.customProviders.firstOrNull { it.id == customId }?.name
            }
            ProviderSectionUi(
                providerId = providerId,
                nameOverride = customName,
                isCustom = ProviderIds.isCustom(providerId),
                models = models,
                keyConfigured = snapshot.keys.contains(providerId),
                descriptionRes = com.tumba.ai.ui.util.providerDescriptionRes(providerId),
                requiresKey = ProviderIds.requiresApiKey(providerId),
            )
        }

        val memory = localModelRepository.memoryStatus()
        _state.update {
            it.copy(
                isLoading = false,
                activeModel = activeModel,
                activeProviderId = snapshot.settings.activeProviderId,
                sections = sections,
                customProviders = snapshot.customProviders,
                localModels = snapshot.localModels,
                keys = snapshot.keys,
                engineStatus = engine.status,
                memory = memory,
            )
        }

        if (!autoSyncAttempted && snapshot.keys.contains(ProviderIds.GEMINI)) {
            autoSyncAttempted = true
            syncModels(ProviderIds.GEMINI, notify = false)
        }
    }

    /**
     * Refreshes the provider's live model list. Gemini retires generations regularly, so the
     * hardcoded catalogue must never be the only source of selectable models.
     */
    fun syncModels(providerId: String = ProviderIds.GEMINI, notify: Boolean = true) {
        if (providerId != ProviderIds.GEMINI) return
        if (providerId in _state.value.syncingProviders) return
        _state.update {
            it.copy(syncingProviders = it.syncingProviders + providerId)
        }
        viewModelScope.launch {
            try {
                val models = aiRepository.discoverModels(providerId)
                if (models.isEmpty()) throw AiException(AiError.EmptyResponse)
                _state.update {
                    it.copy(remoteModels = it.remoteModels + (providerId to models))
                }
                lastSnapshot?.let { applySnapshot(it) }
                if (notify) notice(R.string.models_sync_done, models.size.toString())
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (throwable: Throwable) {
                if (notify) {
                    _state.update { it.copy(errorRes = R.string.models_sync_failed) }
                }
            } finally {
                _state.update {
                    it.copy(syncingProviders = it.syncingProviders - providerId)
                }
            }
        }
    }

    fun selectModel(model: ModelInfo) {
        viewModelScope.launch {
            settingsRepository.setActiveModel(ActiveModel(model.providerId, model.id))
            _state.update { it.copy(activeModel = model) }
        }
    }

    fun testConnection(model: ModelInfo) {
        val key = "${model.providerId}:${model.id}"
        _state.update {
            it.copy(testStates = it.testStates + (key to ConnectionState.Testing))
        }
        viewModelScope.launch {
            val result = try {
                ConnectionState.Success(aiRepository.testConnection(model).latencyMs)
            } catch (exception: AiException) {
                ConnectionState.Failure(exception.error)
            } catch (throwable: Throwable) {
                ConnectionState.Failure(AiError.Unknown(throwable.message))
            }
            _state.update {
                it.copy(testStates = it.testStates + (key to result))
            }
        }
    }

    fun saveApiKey(providerId: String, key: String) {
        if (key.isBlank()) return
        viewModelScope.launch {
            apiKeys.setKey(providerId, key)
            notice(R.string.models_key_saved_toast)
        }
    }

    fun removeApiKey(providerId: String) {
        viewModelScope.launch {
            apiKeys.removeKey(providerId)
            notice(R.string.models_key_removed_toast)
        }
    }

    fun addCustom(draft: CustomProviderDraft) {
        viewModelScope.launch {
            val provider = customProviderRepository.add(
                name = draft.name,
                baseUrl = draft.baseUrl,
                modelId = draft.modelId,
                supportsImages = draft.supportsImages,
                contextWindow = draft.contextWindow,
            )
            if (draft.apiKey.isNotBlank()) {
                apiKeys.setKey(ProviderIds.custom(provider.id), draft.apiKey)
            }
            notice(R.string.models_custom_saved)
        }
    }

    fun updateCustom(id: String, draft: CustomProviderDraft) {
        viewModelScope.launch {
            customProviderRepository.update(
                id = id,
                name = draft.name,
                baseUrl = draft.baseUrl,
                modelId = draft.modelId,
                supportsImages = draft.supportsImages,
                contextWindow = draft.contextWindow,
            )
            if (draft.apiKey.isNotBlank()) {
                apiKeys.setKey(ProviderIds.custom(id), draft.apiKey)
            }
            notice(R.string.models_custom_saved)
        }
    }

    fun deleteCustom(provider: CustomProvider) {
        viewModelScope.launch {
            val providerId = ProviderIds.custom(provider.id)
            customProviderRepository.delete(provider.id)
            apiKeys.removeKey(providerId)
            if (_state.value.activeProviderId == providerId) {
                settingsRepository.setActiveModel(
                    ActiveModel(ProviderIds.GEMINI, ProviderDefaults.GEMINI_MODEL),
                )
            }
        }
    }

    fun importLocalModel(uri: Uri) {
        if (_state.value.importing) return
        _state.update { it.copy(importing = true, errorRes = null) }
        viewModelScope.launch {
            try {
                localModelRepository.importModel(uri, null)
                notice(R.string.models_local_imported)
            } catch (failure: LocalModelException) {
                val messageRes = when (failure.failure) {
                    LocalModelFailure.INVALID_FILE -> R.string.models_local_invalid
                    LocalModelFailure.NO_SPACE -> R.string.error_no_space
                    LocalModelFailure.IO_ERROR -> R.string.error_import_failed
                }
                _state.update { it.copy(errorRes = messageRes) }
            } catch (throwable: Throwable) {
                _state.update { it.copy(errorRes = R.string.error_import_failed) }
            } finally {
                _state.update {
                    it.copy(importing = false, memory = localModelRepository.memoryStatus())
                }
            }
        }
    }

    private val downloadJobs = mutableMapOf<String, Job>()

    /**
     * Downloads a curated GGUF model. The transfer is resumable: cancelling keeps the
     * partial file and the next attempt continues from where it stopped.
     */
    fun downloadModel(spec: DownloadableModel) {
        if (downloadJobs.containsKey(spec.id)) return
        val job = viewModelScope.launch {
            try {
                localModelRepository.downloadModel(spec).collect { status ->
                    when (status) {
                        is ModelDownloadState.Progress -> _state.update { state ->
                            state.copy(
                                downloads = state.downloads + (
                                    spec.id to LocalDownloadUi(
                                        specId = spec.id,
                                        downloadedBytes = status.downloadedBytes,
                                        totalBytes = status.totalBytes,
                                        resuming = status.resuming,
                                    )
                                    ),
                            )
                        }

                        is ModelDownloadState.Completed -> {
                            _state.update { it.copy(downloads = it.downloads - spec.id) }
                            if (_state.value.activeModel == null) {
                                // First model on a fresh install: make it usable right away.
                                selectLocalModel(status.model)
                            }
                            notice(R.string.models_download_done, spec.displayName)
                        }
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: LocalModelException) {
                val messageRes = when (failure.failure) {
                    LocalModelFailure.INVALID_FILE -> R.string.models_local_invalid
                    LocalModelFailure.NO_SPACE -> R.string.error_no_space
                    LocalModelFailure.IO_ERROR -> R.string.models_download_failed
                }
                _state.update { it.copy(errorRes = messageRes) }
            } catch (throwable: Throwable) {
                _state.update { it.copy(errorRes = R.string.models_download_failed) }
            } finally {
                downloadJobs.remove(spec.id)
                _state.update {
                    it.copy(
                        downloads = it.downloads - spec.id,
                        memory = localModelRepository.memoryStatus(),
                    )
                }
            }
        }
        downloadJobs[spec.id] = job
    }

    fun cancelDownload(spec: DownloadableModel) {
        // Stop the transfer but keep the `.part` file: the next Download tap resumes with an
        // HTTP Range request instead of restarting a 0.5–2 GB transfer.
        downloadJobs.remove(spec.id)?.cancel()
        _state.update { it.copy(downloads = it.downloads - spec.id) }
    }

    fun selectLocalModel(model: LocalModel) {
        viewModelScope.launch {
            settingsRepository.setActiveModel(ActiveModel(ProviderIds.LOCAL, model.id))
        }
    }

    fun deleteLocalModel(model: LocalModel) {
        viewModelScope.launch {
            val wasActive = _state.value.activeModel?.let {
                it.providerId == ProviderIds.LOCAL && it.id == model.id
            } == true
            localModelRepository.deleteModel(model.id)
            if (wasActive) {
                settingsRepository.setActiveModel(
                    ActiveModel(ProviderIds.GEMINI, ProviderDefaults.GEMINI_MODEL),
                )
            }
            notice(R.string.models_local_deleted)
            _state.update { it.copy(memory = localModelRepository.memoryStatus()) }
        }
    }

    fun estimateRuntimeBytes(sizeBytes: Long): Long =
        localModelRepository.estimateRuntimeBytes(sizeBytes)

    fun clearNotice() {
        _state.update { it.copy(notice = null) }
    }

    fun clearError() {
        _state.update { it.copy(errorRes = null) }
    }

    private fun notice(@StringRes messageRes: Int, argument: String? = null) {
        _state.update { it.copy(notice = UiNotice(messageRes, argument)) }
    }

    private data class Snapshot(
        val settings: AppSettings,
        val keys: Set<String>,
        val customProviders: List<CustomProvider>,
        val localModels: List<LocalModel>,
    )
}
