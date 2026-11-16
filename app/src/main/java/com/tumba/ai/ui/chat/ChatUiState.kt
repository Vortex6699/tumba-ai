package com.tumba.ai.ui.chat

import com.tumba.ai.core.ProviderIds
import com.tumba.ai.domain.ai.AiError
import com.tumba.ai.domain.model.AppSettings
import com.tumba.ai.domain.model.Message
import com.tumba.ai.domain.model.ModelInfo
import com.tumba.ai.domain.model.PendingAttachment

data class ChatUiState(
    val conversationId: Long? = null,
    val title: String = "",
    val messages: List<Message> = emptyList(),
    val input: String = "",
    val attachments: List<PendingAttachment> = emptyList(),
    val isStreaming: Boolean = false,
    val isLoading: Boolean = false,
    val activeModel: ModelInfo? = null,
    val settings: AppSettings = AppSettings(),
    val hasApiKey: Boolean = true,
    val streamingMessageId: Long? = null,
    val streamingText: String = "",
    val error: AiError? = null,
    val errorProviderId: String? = null,
    val noticeRes: Int? = null,
) {
    val canSend: Boolean
        get() = !isStreaming && (input.isNotBlank() || attachments.isNotEmpty())

    val needsSetup: Boolean
        get() {
            val model = activeModel ?: return false
            return ProviderIds.requiresApiKey(model.providerId) && !hasApiKey
        }

    val supportsImages: Boolean
        get() = activeModel?.capabilities?.supportsImages == true

    val isNewConversation: Boolean
        get() = conversationId == null && messages.isEmpty()
}
