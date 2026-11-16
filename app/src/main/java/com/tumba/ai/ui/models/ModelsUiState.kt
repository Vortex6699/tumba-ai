package com.tumba.ai.ui.models

import androidx.annotation.StringRes
import com.tumba.ai.data.ai.local.LocalEngineStatus
import com.tumba.ai.domain.ai.AiError
import com.tumba.ai.domain.model.CustomProvider
import com.tumba.ai.domain.model.LocalModel
import com.tumba.ai.domain.model.ModelInfo
import com.tumba.ai.domain.repository.MemoryStatus
import com.tumba.ai.ui.util.UiNotice

data class ProviderSectionUi(
    val providerId: String,
    val nameOverride: String?,
    val isCustom: Boolean,
    val models: List<ModelInfo>,
    val keyConfigured: Boolean,
    @StringRes val descriptionRes: Int?,
    val requiresKey: Boolean,
)

sealed interface ConnectionState {
    data object Testing : ConnectionState
    data class Success(val latencyMs: Long) : ConnectionState
    data class Failure(val error: AiError) : ConnectionState
}

data class CustomProviderDraft(
    val name: String,
    val baseUrl: String,
    val modelId: String,
    val apiKey: String,
    val supportsImages: Boolean,
    val contextWindow: Int?,
)

data class LocalDownloadUi(
    val specId: String,
    val downloadedBytes: Long,
    val totalBytes: Long,
    val resuming: Boolean = false,
) {
    val fraction: Float
        get() = if (totalBytes <= 0L) 0f
        else (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)

    val percent: Int get() = (fraction * 100f).toInt()
}

data class ModelsUiState(
    val isLoading: Boolean = true,
    val activeModel: ModelInfo? = null,
    val activeProviderId: String = "",
    val sections: List<ProviderSectionUi> = emptyList(),
    val customProviders: List<CustomProvider> = emptyList(),
    val localModels: List<LocalModel> = emptyList(),
    val downloads: Map<String, LocalDownloadUi> = emptyMap(),
    val engineStatus: LocalEngineStatus = LocalEngineStatus.Unavailable(""),
    val memory: MemoryStatus = MemoryStatus(0L, 0L),
    val keys: Set<String> = emptySet(),
    val testStates: Map<String, ConnectionState> = emptyMap(),
    val remoteModels: Map<String, List<ModelInfo>> = emptyMap(),
    val syncingProviders: Set<String> = emptySet(),
    val importing: Boolean = false,
    val notice: UiNotice? = null,
    @StringRes val errorRes: Int? = null,
) {
    val storageUsed: Long get() = localModels.sumOf { it.sizeBytes }

    fun downloadFor(specId: String): LocalDownloadUi? = downloads[specId]

    fun isDownloaded(specId: String): Boolean = localModels.any { it.id == specId }

    fun testStateFor(model: ModelInfo): ConnectionState? =
        testStates["${model.providerId}:${model.id}"]

    fun isActive(model: ModelInfo): Boolean =
        activeModel?.providerId == model.providerId && activeModel.id == model.id
}
