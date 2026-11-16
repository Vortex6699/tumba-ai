package com.tumba.ai.domain.repository

import android.net.Uri
import com.tumba.ai.domain.model.DownloadableModel
import com.tumba.ai.domain.model.LocalModel
import kotlinx.coroutines.flow.Flow

data class MemoryStatus(
    val availableBytes: Long,
    val totalBytes: Long,
)

/** Progress of a resumable model download; [totalBytes] is 0 when the host omits a length. */
sealed interface ModelDownloadState {
    data class Progress(
        val downloadedBytes: Long,
        val totalBytes: Long,
        val resuming: Boolean = false,
    ) : ModelDownloadState

    data class Completed(val model: LocalModel) : ModelDownloadState
}

interface LocalModelRepository {
    fun observeModels(): Flow<List<LocalModel>>

    suspend fun models(): List<LocalModel>

    suspend fun getModel(id: String): LocalModel?

    /**
     * Copies a user-picked GGUF file into app-private storage, validating the magic header.
     * Throws [com.tumba.ai.core.LocalModelException] with a specific reason on failure.
     */
    suspend fun importModel(uri: Uri, displayName: String?): LocalModel

    /**
     * Streams a catalogue model into app-private storage. Cancelling the collector aborts
     * the HTTP call and keeps the partial file, so a later attempt resumes with a Range
     * request instead of starting over.
     */
    fun downloadModel(spec: DownloadableModel): Flow<ModelDownloadState>

    suspend fun deleteModel(id: String)

    suspend fun storageUsed(): Long

    fun memoryStatus(): MemoryStatus

    fun estimateRuntimeBytes(modelSizeBytes: Long): Long
}