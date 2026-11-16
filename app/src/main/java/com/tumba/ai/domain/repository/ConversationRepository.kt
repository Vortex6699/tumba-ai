package com.tumba.ai.domain.repository

import com.tumba.ai.domain.model.Attachment
import com.tumba.ai.domain.model.Conversation
import com.tumba.ai.domain.model.ConversationStats
import com.tumba.ai.domain.model.ConversationSummary
import com.tumba.ai.domain.model.Message
import com.tumba.ai.domain.model.MessageRole
import com.tumba.ai.domain.model.MessageStatus
import kotlinx.coroutines.flow.Flow
import java.io.OutputStream

data class NewAttachment(
    val path: String,
    val mimeType: String,
    val name: String?,
    val sizeBytes: Long,
)

interface ConversationRepository {
    fun observeConversations(): Flow<List<ConversationSummary>>

    fun searchConversations(query: String): Flow<List<ConversationSummary>>

    fun observeMessages(conversationId: Long): Flow<List<Message>>

    fun observeConversation(conversationId: Long): Flow<Conversation?>

    fun observeStats(): Flow<ConversationStats>

    suspend fun getConversation(conversationId: Long): Conversation?

    suspend fun getMessages(conversationId: Long): List<Message>

    suspend fun createConversation(title: String, providerId: String, modelId: String): Long

    suspend fun addMessage(
        conversationId: Long,
        role: MessageRole,
        content: String,
        status: MessageStatus,
        error: String? = null,
        attachments: List<NewAttachment> = emptyList(),
    ): Long

    suspend fun updateMessage(
        messageId: Long,
        content: String,
        status: MessageStatus,
        error: String? = null,
    )

    suspend fun deleteMessage(messageId: Long)

    suspend fun renameConversation(conversationId: Long, title: String)

    suspend fun touchConversation(conversationId: Long)

    suspend fun setPinned(conversationId: Long, pinned: Boolean)

    suspend fun deleteConversation(conversationId: Long)

    suspend fun deleteAllConversations()

    suspend fun getAttachments(messageId: Long): List<Attachment>

    suspend fun exportAll(output: OutputStream)
}
