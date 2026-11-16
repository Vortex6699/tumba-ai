package com.tumba.ai.data.repository

import com.tumba.ai.core.DispatcherProvider
import com.tumba.ai.data.local.dao.AttachmentDao
import com.tumba.ai.data.local.dao.ConversationDao
import com.tumba.ai.data.local.dao.MessageDao
import com.tumba.ai.data.local.entity.AttachmentEntity
import com.tumba.ai.data.local.entity.ConversationEntity
import com.tumba.ai.data.local.entity.MessageEntity
import com.tumba.ai.data.local.model.ConversationSummaryRow
import com.tumba.ai.data.local.model.MessageWithAttachments
import com.tumba.ai.domain.model.Attachment
import com.tumba.ai.domain.model.Conversation
import com.tumba.ai.domain.model.ConversationStats
import com.tumba.ai.domain.model.ConversationSummary
import com.tumba.ai.domain.model.Message
import com.tumba.ai.domain.model.MessageRole
import com.tumba.ai.domain.model.MessageStatus
import com.tumba.ai.domain.repository.ConversationRepository
import com.tumba.ai.domain.repository.NewAttachment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.io.File
import java.io.OutputStream
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConversationRepositoryImpl @Inject constructor(
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao,
    private val attachmentDao: AttachmentDao,
    private val dispatchers: DispatcherProvider,
) : ConversationRepository {

    override fun observeConversations(): Flow<List<ConversationSummary>> =
        conversationDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override fun searchConversations(query: String): Flow<List<ConversationSummary>> {
        val trimmed = query.trim()
        return if (trimmed.isEmpty()) {
            observeConversations()
        } else {
            conversationDao.search(trimmed).map { rows -> rows.map { it.toDomain() } }
        }
    }

    override fun observeMessages(conversationId: Long): Flow<List<Message>> =
        messageDao.observeForConversation(conversationId).map { list -> list.map { it.toDomain() } }

    override fun observeConversation(conversationId: Long): Flow<Conversation?> =
        conversationDao.observeById(conversationId).map { it?.toDomain() }

    override fun observeStats(): Flow<ConversationStats> =
        conversationDao.observeStats().map { ConversationStats(it.conversations, it.messages) }

    override suspend fun getConversation(conversationId: Long): Conversation? =
        withContext(dispatchers.io) {
            conversationDao.getById(conversationId)?.toDomain()
        }

    override suspend fun getMessages(conversationId: Long): List<Message> =
        withContext(dispatchers.io) {
            messageDao.getForConversation(conversationId).map { it.toDomain() }
        }

    override suspend fun createConversation(
        title: String,
        providerId: String,
        modelId: String,
    ): Long = withContext(dispatchers.io) {
        val now = System.currentTimeMillis()
        conversationDao.insert(
            ConversationEntity(
                title = title,
                createdAt = now,
                updatedAt = now,
                providerId = providerId,
                modelId = modelId,
            ),
        )
    }

    override suspend fun addMessage(
        conversationId: Long,
        role: MessageRole,
        content: String,
        status: MessageStatus,
        error: String?,
        attachments: List<NewAttachment>,
    ): Long = withContext(dispatchers.io) {
        val now = System.currentTimeMillis()
        val messageId = messageDao.insert(
            MessageEntity(
                conversationId = conversationId,
                content = content,
                role = role.name,
                timestamp = now,
                status = status.name,
                error = error,
            ),
        )
        if (attachments.isNotEmpty()) {
            attachmentDao.insertAll(
                attachments.map { attachment ->
                    AttachmentEntity(
                        messageId = messageId,
                        path = attachment.path,
                        mimeType = attachment.mimeType,
                        name = attachment.name,
                        sizeBytes = attachment.sizeBytes,
                    )
                },
            )
        }
        conversationDao.touch(conversationId, now)
        messageId
    }

    override suspend fun updateMessage(
        messageId: Long,
        content: String,
        status: MessageStatus,
        error: String?,
    ) {
        withContext(dispatchers.io) {
            messageDao.update(messageId, content, status.name, error)
        }
    }

    override suspend fun deleteMessage(messageId: Long) {
        withContext(dispatchers.io) {
            val paths = attachmentDao.pathsForMessage(messageId)
            messageDao.delete(messageId)
            paths.forEach { path -> runCatching { File(path).delete() } }
        }
    }

    override suspend fun renameConversation(conversationId: Long, title: String) {
        withContext(dispatchers.io) {
            conversationDao.rename(conversationId, title, System.currentTimeMillis())
        }
    }

    override suspend fun touchConversation(conversationId: Long) {
        withContext(dispatchers.io) {
            conversationDao.touch(conversationId, System.currentTimeMillis())
        }
    }

    override suspend fun setPinned(conversationId: Long, pinned: Boolean) {
        withContext(dispatchers.io) { conversationDao.setPinned(conversationId, pinned) }
    }

    override suspend fun deleteConversation(conversationId: Long) {
        withContext(dispatchers.io) {
            val paths = attachmentDao.pathsForConversation(conversationId)
            conversationDao.delete(conversationId)
            paths.forEach { path -> runCatching { File(path).delete() } }
        }
    }

    override suspend fun deleteAllConversations() {
        withContext(dispatchers.io) {
            val paths = attachmentDao.allPaths()
            conversationDao.deleteAll()
            paths.forEach { path -> runCatching { File(path).delete() } }
        }
    }

    override suspend fun getAttachments(messageId: Long): List<Attachment> =
        withContext(dispatchers.io) {
            attachmentDao.forMessage(messageId).map { it.toDomain() }
        }

    override suspend fun exportAll(output: OutputStream) = withContext(dispatchers.io) {
        val conversations = conversationDao.getAllOnce()
        val payload = conversations.map { conversation ->
            conversation to messageDao.getForConversation(conversation.id)
        }
        val json = buildJsonObject {
            put("app", "TUMBA AI")
            put("formatVersion", 1)
            put("exportedAt", System.currentTimeMillis())
            putJsonArray("conversations") {
                payload.forEach { (conversation, messages) ->
                    add(
                        buildJsonObject {
                            put("id", conversation.id)
                            put("title", conversation.title)
                            put("createdAt", conversation.createdAt)
                            put("updatedAt", conversation.updatedAt)
                            put("providerId", conversation.providerId)
                            put("modelId", conversation.modelId)
                            putJsonArray("messages") {
                                messages.forEach { message ->
                                    add(message.toExportJson())
                                }
                            }
                        },
                    )
                }
            }
        }
        output.writer(Charsets.UTF_8).use { writer -> writer.write(json.toString()) }
    }

    private fun MessageWithAttachments.toExportJson() = buildJsonObject {
        put("id", message.id)
        put("role", message.role)
        put("content", message.content)
        put("timestamp", message.timestamp)
        put("status", message.status)
        putJsonArray("attachments") {
            attachments.forEach { attachment ->
                add(
                    buildJsonObject {
                        put("name", attachment.name)
                        put("mimeType", attachment.mimeType)
                        put("sizeBytes", attachment.sizeBytes)
                    },
                )
            }
        }
    }

    private fun ConversationSummaryRow.toDomain() = ConversationSummary(
        id = conversation.id,
        title = conversation.title,
        preview = preview,
        updatedAt = conversation.updatedAt,
        providerId = conversation.providerId,
        modelId = conversation.modelId,
        messageCount = messageCount,
        pinned = conversation.pinned,
    )

    private fun ConversationEntity.toDomain() = Conversation(
        id = id,
        title = title,
        createdAt = createdAt,
        updatedAt = updatedAt,
        providerId = providerId,
        modelId = modelId,
        pinned = pinned,
    )

    private fun MessageWithAttachments.toDomain() = Message(
        id = message.id,
        conversationId = message.conversationId,
        content = message.content,
        role = MessageRole.entries.firstOrNull { it.name == message.role } ?: MessageRole.USER,
        timestamp = message.timestamp,
        status = MessageStatus.entries.firstOrNull { it.name == message.status }
            ?: MessageStatus.COMPLETE,
        error = message.error,
        attachments = attachments.map { it.toDomain() },
    )

    private fun AttachmentEntity.toDomain() = Attachment(
        id = id,
        messageId = messageId,
        path = path,
        mimeType = mimeType,
        name = name,
        sizeBytes = sizeBytes,
    )
}
