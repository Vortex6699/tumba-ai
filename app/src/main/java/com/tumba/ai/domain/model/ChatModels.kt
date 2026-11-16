package com.tumba.ai.domain.model

enum class MessageRole { USER, ASSISTANT, SYSTEM }

enum class MessageStatus { COMPLETE, STREAMING, ERROR, CANCELLED }

data class Attachment(
    val id: Long = 0L,
    val messageId: Long = 0L,
    val path: String,
    val mimeType: String,
    val name: String?,
    val sizeBytes: Long,
)

data class Message(
    val id: Long,
    val conversationId: Long,
    val content: String,
    val role: MessageRole,
    val timestamp: Long,
    val status: MessageStatus,
    val error: String? = null,
    val attachments: List<Attachment> = emptyList(),
) {
    val isStreaming: Boolean get() = status == MessageStatus.STREAMING
}

data class Conversation(
    val id: Long,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val providerId: String,
    val modelId: String,
    val pinned: Boolean = false,
)

data class ConversationSummary(
    val id: Long,
    val title: String,
    val preview: String?,
    val updatedAt: Long,
    val providerId: String,
    val modelId: String,
    val messageCount: Int,
    val pinned: Boolean = false,
)

data class ConversationStats(
    val conversations: Int = 0,
    val messages: Int = 0,
)
