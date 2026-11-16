package com.tumba.ai.data.files

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.tumba.ai.core.DispatcherProvider
import com.tumba.ai.domain.ai.AiImage
import com.tumba.ai.domain.model.Attachment
import com.tumba.ai.domain.model.PendingAttachment
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

class AttachmentException(
    val reason: Reason,
    cause: Throwable? = null,
) : Exception(reason.name, cause) {
    enum class Reason { NOT_AN_IMAGE, TOO_LARGE, IO_ERROR }
}

/**
 * Copies picked images into app-private storage immediately. The provider request later
 * reads from that private copy, so no persisted content:// permissions are needed.
 */
@Singleton
class AttachmentStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dispatchers: DispatcherProvider,
) {

    private val directory: File
        get() = File(context.filesDir, "attachments").apply { mkdirs() }

    suspend fun importImage(uri: Uri): PendingAttachment = withContext(dispatchers.io) {
        val resolver = context.contentResolver
        val mime = resolver.getType(uri)
            ?: throw AttachmentException(AttachmentException.Reason.NOT_AN_IMAGE)
        if (!mime.startsWith("image/")) {
            throw AttachmentException(AttachmentException.Reason.NOT_AN_IMAGE)
        }

        val metadata = queryMetadata(uri)
        val declaredSize = metadata.size
        if (declaredSize != null && declaredSize > MAX_IMAGE_BYTES) {
            throw AttachmentException(AttachmentException.Reason.TOO_LARGE)
        }

        val extension = extensionFor(mime)
        val target = File(directory, "${UUID.randomUUID()}.$extension")
        try {
            resolver.openInputStream(uri)?.use { input ->
                target.outputStream().buffered(BUFFER_SIZE).use { output ->
                    val copied = input.copyTo(output, BUFFER_SIZE)
                    if (copied > MAX_IMAGE_BYTES) {
                        target.delete()
                        throw AttachmentException(AttachmentException.Reason.TOO_LARGE)
                    }
                }
            } ?: throw AttachmentException(AttachmentException.Reason.IO_ERROR)
        } catch (e: AttachmentException) {
            target.delete()
            throw e
        } catch (t: Throwable) {
            target.delete()
            throw AttachmentException(AttachmentException.Reason.IO_ERROR, t)
        }

        if (target.length() == 0L) {
            target.delete()
            throw AttachmentException(AttachmentException.Reason.IO_ERROR)
        }

        PendingAttachment(
            path = target.absolutePath,
            mimeType = mime,
            name = metadata.name,
            sizeBytes = target.length(),
        )
    }

    suspend fun imageFor(attachment: Attachment): AiImage? = withContext(dispatchers.io) {
        val file = File(attachment.path)
        if (!file.exists() || file.length() > MAX_IMAGE_BYTES) return@withContext null
        runCatching { AiImage(file.readBytes(), attachment.mimeType) }.getOrNull()
    }

    suspend fun delete(path: String) {
        withContext(dispatchers.io) {
            runCatching { File(path).delete() }
        }
    }

    private data class Metadata(val name: String?, val size: Long?)

    private fun queryMetadata(uri: Uri): Metadata {
        var name: String? = null
        var size: Long? = null
        runCatching {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIndex >= 0 && !cursor.isNull(nameIndex)) name = cursor.getString(nameIndex)
                    if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) size = cursor.getLong(sizeIndex)
                }
            }
        }
        return Metadata(name, size)
    }

    private fun extensionFor(mime: String): String = when (mime.lowercase()) {
        "image/png" -> "png"
        "image/webp" -> "webp"
        "image/gif" -> "gif"
        "image/heic", "image/heif" -> "heic"
        else -> "jpg"
    }

    private companion object {
        const val BUFFER_SIZE = 64 * 1024
        const val MAX_IMAGE_BYTES = 12L * 1024 * 1024
    }
}
