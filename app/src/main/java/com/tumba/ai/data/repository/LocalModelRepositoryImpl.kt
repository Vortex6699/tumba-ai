package com.tumba.ai.data.repository

import android.app.ActivityManager
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.tumba.ai.core.DispatcherProvider
import com.tumba.ai.core.LocalModelException
import com.tumba.ai.core.LocalModelFailure
import com.tumba.ai.data.local.dao.LocalModelDao
import com.tumba.ai.data.local.entity.LocalModelEntity
import com.tumba.ai.domain.model.DownloadableModel
import com.tumba.ai.domain.model.LocalModel
import com.tumba.ai.domain.repository.LocalModelRepository
import com.tumba.ai.domain.repository.MemoryStatus
import com.tumba.ai.domain.repository.ModelDownloadState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalModelRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: LocalModelDao,
    private val dispatchers: DispatcherProvider,
    private val httpClient: OkHttpClient,
) : LocalModelRepository {

    private val modelsDir: File
        get() = File(context.filesDir, "models").apply { mkdirs() }

    override fun observeModels(): Flow<List<LocalModel>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun models(): List<LocalModel> = withContext(dispatchers.io) {
        dao.getAll().map { it.toDomain() }
    }

    override suspend fun getModel(id: String): LocalModel? = withContext(dispatchers.io) {
        dao.getById(id)?.toDomain()
    }

    override suspend fun importModel(uri: Uri, displayName: String?): LocalModel =
        withContext(dispatchers.io) {
            val resolver = context.contentResolver
            val metadata = queryMetadata(uri)
            val sourceName = metadata.name ?: "model.gguf"
            val declaredSize = metadata.size
            if (declaredSize != null && modelsDir.usableSpace < declaredSize + SAFETY_MARGIN) {
                throw LocalModelException(LocalModelFailure.NO_SPACE)
            }

            val id = UUID.randomUUID().toString()
            val target = File(modelsDir, "$id.gguf")
            try {
                resolver.openInputStream(uri)?.let { raw ->
                    BufferedInputStream(raw, BUFFER_SIZE).use { input ->
                        val magic = ByteArray(4)
                        var read = 0
                        while (read < magic.size) {
                            val count = input.read(magic, read, magic.size - read)
                            if (count <= 0) break
                            read += count
                        }
                        if (read != magic.size || String(magic, Charsets.US_ASCII) != "GGUF") {
                            throw LocalModelException(LocalModelFailure.INVALID_FILE)
                        }
                        target.outputStream().buffered(BUFFER_SIZE).use { output ->
                            output.write(magic)
                            input.copyTo(output, BUFFER_SIZE)
                        }
                    }
                } ?: throw LocalModelException(LocalModelFailure.IO_ERROR)

                if (target.length() <= 4L) throw LocalModelException(LocalModelFailure.INVALID_FILE)

                val display = displayName?.trim()?.takeIf { it.isNotEmpty() }
                    ?: sourceName.removeSuffix(".gguf").ifBlank { "Local model" }

                val entity = LocalModelEntity(
                    id = id,
                    displayName = display,
                    fileName = sourceName,
                    path = target.absolutePath,
                    sizeBytes = target.length(),
                    importedAt = System.currentTimeMillis(),
                    quantization = detectQuantization(sourceName),
                )
                dao.upsert(entity)
                entity.toDomain()
            } catch (e: LocalModelException) {
                target.delete()
                throw e
            } catch (t: Throwable) {
                target.delete()
                throw LocalModelException(LocalModelFailure.IO_ERROR, t)
            }
        }

    override fun downloadModel(spec: DownloadableModel): Flow<ModelDownloadState> = flow {
        val target = File(modelsDir, "${spec.id}.gguf")
        val partial = File(modelsDir, "${spec.id}.gguf.part")

        if (target.exists() && spec.sizeBytes > 0 && target.length() == spec.sizeBytes) {
            val recorded = dao.getById(spec.id)?.toDomain() ?: registerDownloaded(spec, target)
            emit(ModelDownloadState.Completed(recorded))
            return@flow
        }

        var offset = if (partial.exists()) partial.length() else 0L
        if (spec.sizeBytes > 0 && offset > spec.sizeBytes) {
            partial.delete()
            offset = 0L
        }
        // A previous attempt may have finished the transfer but failed before recording it.
        if (spec.sizeBytes > 0 && offset == spec.sizeBytes && hasGgufMagic(partial)) {
            if (target.exists()) target.delete()
            if (!partial.renameTo(target)) {
                partial.copyTo(target, overwrite = true)
                partial.delete()
            }
            emit(ModelDownloadState.Completed(registerDownloaded(spec, target)))
            return@flow
        }
        if (modelsDir.usableSpace < (spec.sizeBytes - offset).coerceAtLeast(0L) + SAFETY_MARGIN) {
            throw LocalModelException(LocalModelFailure.NO_SPACE)
        }

        var response: Response? = null
        var call: Call? = null
        var cancellation: DisposableHandle? = null
        try {
            // A truncated `.part` means the transfer was interrupted: resume with Range,
            // and fall back to a clean download when the server does not honour it.
            while (true) {
                val builder = Request.Builder().url(spec.url).get()
                if (offset > 0L) builder.header("Range", "bytes=$offset-")
                val request = builder.build()
                val current = httpClient.newCall(request)
                call = current
                cancellation = currentCoroutineContext()[Job]?.invokeOnCompletion { current.cancel() }
                val candidate = current.execute()
                if (candidate.code == 416 && offset > 0L) {
                    candidate.close()
                    partial.delete()
                    offset = 0L
                    continue
                }
                if (!candidate.isSuccessful) {
                    candidate.close()
                    throw LocalModelException(LocalModelFailure.IO_ERROR)
                }
                response = candidate
                break
            }

            val body = response.body ?: throw LocalModelException(LocalModelFailure.IO_ERROR)
            val append = response.code == 206
            if (!append) offset = 0L
            val total = if (body.contentLength() > 0L) {
                offset + body.contentLength()
            } else {
                spec.sizeBytes
            }
            var downloaded = offset
            var lastEmit = System.nanoTime()
            emit(ModelDownloadState.Progress(downloaded, total, resuming = offset > 0L))

            FileOutputStream(partial, append).use { output ->
                body.byteStream().use { input ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        downloaded += read
                        val now = System.nanoTime()
                        if (now - lastEmit >= PROGRESS_INTERVAL_NANOS) {
                            lastEmit = now
                            emit(ModelDownloadState.Progress(downloaded, total))
                        }
                    }
                }
                runCatching { output.fd.sync() }
            }
            emit(ModelDownloadState.Progress(downloaded, total))

            if (partial.length() <= 4L || !hasGgufMagic(partial)) {
                partial.delete()
                throw LocalModelException(LocalModelFailure.INVALID_FILE)
            }
            if (target.exists()) target.delete()
            if (!partial.renameTo(target)) {
                partial.copyTo(target, overwrite = true)
                partial.delete()
            }
            emit(ModelDownloadState.Completed(registerDownloaded(spec, target)))
        } finally {
            cancellation?.dispose()
            runCatching { response?.close() }
            runCatching { call?.cancel() }
        }
    }.flowOn(dispatchers.io)

    private suspend fun registerDownloaded(spec: DownloadableModel, file: File): LocalModel {
        if (file.length() <= 4L || !hasGgufMagic(file)) {
            file.delete()
            throw LocalModelException(LocalModelFailure.INVALID_FILE)
        }
        val entity = LocalModelEntity(
            id = spec.id,
            displayName = spec.displayName,
            fileName = spec.fileName,
            path = file.absolutePath,
            sizeBytes = file.length(),
            importedAt = System.currentTimeMillis(),
            quantization = spec.quantization,
        )
        dao.upsert(entity)
        return entity.toDomain()
    }

    private fun hasGgufMagic(file: File): Boolean {
        val magic = ByteArray(4)
        val read = FileInputStream(file).use { it.read(magic) }
        return read == magic.size && String(magic, Charsets.US_ASCII) == "GGUF"
    }

    override suspend fun deleteModel(id: String) {
        withContext(dispatchers.io) {
            val entity = dao.getById(id) ?: return@withContext
            runCatching { File(entity.path).delete() }
            dao.delete(id)
        }
    }

    override suspend fun storageUsed(): Long = withContext(dispatchers.io) { dao.totalSize() }

    override fun memoryStatus(): MemoryStatus {
        val manager = context.getSystemService(ActivityManager::class.java)
        val info = ActivityManager.MemoryInfo()
        manager?.getMemoryInfo(info)
        return MemoryStatus(
            availableBytes = info.availMem,
            totalBytes = info.totalMem,
        )
    }

    override fun estimateRuntimeBytes(modelSizeBytes: Long): Long =
        (modelSizeBytes * 1.15).toLong() + 256L * 1024 * 1024

    private data class Metadata(val name: String?, val size: Long?)

    private fun queryMetadata(uri: Uri): Metadata {
        var name: String? = null
        var size: Long? = null
        runCatching {
            context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
                null,
                null,
                null,
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIndex >= 0 && !cursor.isNull(nameIndex)) name = cursor.getString(nameIndex)
                    if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) size = cursor.getLong(sizeIndex)
                }
            }
        }
        if (name == null) name = uri.lastPathSegment?.substringAfterLast('/')
        return Metadata(name, size)
    }

    private fun detectQuantization(fileName: String): String? {
        val match = Regex("(?i)(IQ\\d+[A-Z_]*|Q\\d+[A-Z_]*|BF16|F16|F32)")
            .find(fileName)
        return match?.value?.uppercase()
    }

    private fun LocalModelEntity.toDomain() = LocalModel(
        id = id,
        displayName = displayName,
        fileName = fileName,
        path = path,
        sizeBytes = sizeBytes,
        importedAt = importedAt,
        quantization = quantization,
    )

    private companion object {
        const val BUFFER_SIZE = 64 * 1024
        const val SAFETY_MARGIN = 16L * 1024 * 1024
        const val PROGRESS_INTERVAL_NANOS = 200_000_000L
    }
}
