package com.tumba.ai.data.local.model

import androidx.room.Embedded
import androidx.room.Relation
import com.tumba.ai.data.local.entity.AttachmentEntity
import com.tumba.ai.data.local.entity.ConversationEntity
import com.tumba.ai.data.local.entity.MessageEntity

data class MessageWithAttachments(
    @Embedded val message: MessageEntity,
    @Relation(parentColumn = "id", entityColumn = "messageId")
    val attachments: List<AttachmentEntity>,
)

data class ConversationSummaryRow(
    @Embedded val conversation: ConversationEntity,
    val messageCount: Int,
    val preview: String?,
)

data class StatsRow(
    val conversations: Int,
    val messages: Int,
)
