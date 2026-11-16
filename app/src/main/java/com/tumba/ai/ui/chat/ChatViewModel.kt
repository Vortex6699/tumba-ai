package com.tumba.ai.ui.chat

import android.net.Uri
import android.os.SystemClock
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tumba.ai.R
import com.tumba.ai.core.ProviderIds
import com.tumba.ai.core.toConversationTitle
import com.tumba.ai.data.ai.local.LocalEngineStatus
import com.tumba.ai.data.ai.local.LocalInferenceEngine
import com.tumba.ai.data.files.AttachmentException
import com.tumba.ai.data.files.AttachmentStore
import com.tumba.ai.domain.ai.AIRepository
import com.tumba.ai.domain.ai.AiError
import com.tumba.ai.domain.ai.AiException
import com.tumba.ai.domain.ai.AiMessage
import com.tumba.ai.domain.ai.StreamEvent
import com.tumba.ai.domain.model.AppSettings
import com.tumba.ai.domain.model.Conversation
import com.tumba.ai.domain.model.Message
import com.tumba.ai.domain.model.MessageRole
import com.tumba.ai.domain.model.MessageStatus
import com.tumba.ai.domain.model.ModelInfo
import com.tumba.ai.domain.model.PendingAttachment
import com.tumba.ai.domain.repository.ApiKeyRepository
import com.tumba.ai.domain.repository.ConversationRepository
import com.tumba.ai.domain.repository.NewAttachment
import com.tumba.ai.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val conversationRepository: ConversationRepository,
    private val aiRepository: AIRepository,
    private val settingsRepository: SettingsRepository,
    private val apiKeys: ApiKeyRepository,
    private val attachmentStore: AttachmentStore,
    private val engine: LocalInferenceEngine,
) : ViewModel() {

    private val initialConversationId: Long? =
        (savedStateHandle.get<Long>(ARG_CONVERSATION_ID) ?: -1L).takeIf { it > 0L }

    private val conversationIdFlow = MutableStateFlow(initialConversationId)

    private val _state = MutableStateFlow(
        ChatUiState(
            conversationId = initialConversationId,
            isLoading = initialConversationId != null,
        ),
    )
    val state = _state.asStateFlow()

    private var generationJob: kotlinx.coroutines.Job? = null

    init {
        observeMessages()
        observeConversation()
        observeSettings()
        recoverInterruptedStreams()
    }

    fun onInputChange(value: String) {
        _state.update { it.copy(input = value) }
    }

    fun onSuggestionSelected(text: String) {
        _state.update { it.copy(input = text) }
    }

    fun onAddAttachment(uri: Uri) {
        val model = _state.value.activeModel
        if (model != null && !model.capabilities.supportsImages) {
            _state.update {
                it.copy(error = AiError.ImagesNotSupported, errorProviderId = model.providerId)
            }
            return
        }
        viewModelScope.launch {
            try {
                val attachment = attachmentStore.importImage(uri)
                val current = _state.value.attachments
                val totalBytes = current.sumOf { it.sizeBytes } + attachment.sizeBytes
                if (current.size >= MAX_ATTACHMENTS || totalBytes > MAX_TOTAL_ATTACHMENT_BYTES) {
                    attachmentStore.delete(attachment.path)
                    _state.update { it.copy(noticeRes = R.string.chat_attachment_limit) }
                    return@launch
                }
                _state.update { it.copy(attachments = it.attachments + attachment) }
            } catch (failure: AttachmentException) {
                val messageRes = when (failure.reason) {
                    AttachmentException.Reason.NOT_AN_IMAGE -> R.string.chat_image_only
                    AttachmentException.Reason.TOO_LARGE -> R.string.error_image_too_large
                    AttachmentException.Reason.IO_ERROR -> R.string.error_file_read
                }
                _state.update { it.copy(noticeRes = messageRes) }
            }
        }
    }

    fun onRemoveAttachment(attachment: PendingAttachment) {
        _state.update {
            it.copy(attachments = it.attachments.filterNot { item -> item.path == attachment.path })
        }
        viewModelScope.launch { attachmentStore.delete(attachment.path) }
    }

    fun onSend() {
        val snapshot = _state.value
        if (snapshot.isStreaming) return
        val text = snapshot.input.trim()
        val attachments = snapshot.attachments
        if (text.isEmpty() && attachments.isEmpty()) return
        val model = snapshot.activeModel
        if (model == null) {
            _state.update { it.copy(error = AiError.Unknown("No model")) }
            return
        }
        if (attachments.isNotEmpty() && !model.capabilities.supportsImages) {
            _state.update {
                it.copy(error = AiError.ImagesNotSupported, errorProviderId = model.providerId)
            }
            return
        }
        if (!credentialsReady(model)) return

        _state.update { it.copy(input = "", attachments = emptyList(), error = null, noticeRes = null) }

        generationJob = viewModelScope.launch {
            _state.update { it.copy(isStreaming = true) }
            try {
                val conversationId = _state.value.conversationId ?: createConversation(text, model)
                conversationRepository.addMessage(
                    conversationId = conversationId,
                    role = MessageRole.USER,
                    content = text,
                    status = MessageStatus.COMPLETE,
                    attachments = attachments.map {
                        NewAttachment(it.path, it.mimeType, it.name, it.sizeBytes)
                    },
                )
                executeGeneration(conversationId, model)
            } catch (cancelled: CancellationException) {
                withContext(NonCancellable) { _state.update { it.copy(isStreaming = false) } }
                throw cancelled
            } catch (throwable: Throwable) {
                _state.update {
                    it.copy(
                        isStreaming = false,
                        error = (throwable as? AiException)?.error
                            ?: AiError.Unknown(throwable.message),
                        errorProviderId = model.providerId,
                    )
                }
            }
        }
    }

    fun onStopGeneration() {
        generationJob?.cancel()
    }

    fun onRegenerate() {
        val snapshot = _state.value
        if (snapshot.isStreaming) return
        val conversationId = snapshot.conversationId ?: return
        val model = snapshot.activeModel ?: return
        if (!credentialsReady(model)) return
        if (snapshot.messages.lastOrNull()?.role != MessageRole.ASSISTANT) return
        val lastAssistant = snapshot.messages.last()

        generationJob = viewModelScope.launch {
            try {
                conversationRepository.deleteMessage(lastAssistant.id)
                executeGeneration(conversationId, model)
            } catch (cancelled: CancellationException) {
                withContext(NonCancellable) { _state.update { it.copy(isStreaming = false) } }
                throw cancelled
            } catch (throwable: Throwable) {
                _state.update {
                    it.copy(
                        isStreaming = false,
                        error = (throwable as? AiException)?.error
                            ?: AiError.Unknown(throwable.message),
                        errorProviderId = model.providerId,
                    )
                }
            }
        }
    }

    fun onRetryFailed() {
        val last = _state.value.messages.lastOrNull()
        if (last != null && last.role == MessageRole.ASSISTANT) {
            onRegenerate()
        } else {
            _state.update { it.copy(error = null) }
        }
    }

    fun onRename(title: String) {
        val conversationId = _state.value.conversationId ?: return
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            conversationRepository.renameConversation(conversationId, trimmed)
            _state.update { it.copy(title = trimmed) }
        }
    }

    fun onDeleteConversation(onDeleted: () -> Unit) {
        val conversationId = _state.value.conversationId ?: return
        generationJob?.cancel()
        viewModelScope.launch {
            conversationRepository.deleteConversation(conversationId)
            conversationIdFlow.value = null
            savedStateHandle.remove<Long>(ARG_CONVERSATION_ID)
            _state.update {
                it.copy(
                    conversationId = null,
                    title = "",
                    messages = emptyList(),
                    isStreaming = false,
                    streamingMessageId = null,
                    streamingText = "",
                )
            }
            onDeleted()
        }
    }

    fun onDismissError() {
        _state.update { it.copy(error = null) }
    }

    fun onNoticeShown() {
        _state.update { it.copy(noticeRes = null) }
    }

    private fun observeMessages() {
        viewModelScope.launch {
            conversationIdFlow
                .flatMapLatest { conversationId ->
                    if (conversationId == null) {
                        flowOf(emptyList<Message>())
                    } else {
                        conversationRepository.observeMessages(conversationId)
                    }
                }
                .catch { throwable ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = AiError.Unknown(throwable.message),
                        )
                    }
                }
                .collect { messages ->
                    _state.update { it.copy(messages = messages, isLoading = false) }
                }
        }
    }

    private fun observeConversation() {
        viewModelScope.launch {
            conversationIdFlow
                .flatMapLatest { conversationId ->
                    if (conversationId == null) {
                        flowOf<Conversation?>(null)
                    } else {
                        conversationRepository.observeConversation(conversationId)
                    }
                }
                .collect { conversation ->
                    if (conversation != null) {
                        _state.update { it.copy(title = conversation.title) }
                    }
                }
        }
    }

    private fun observeSettings() {
        viewModelScope.launch {
            combine(
                settingsRepository.settings,
                apiKeys.configuredProviders,
            ) { settings, keys -> settings to keys }
                .collect { (settings, keys) ->
                    val model = aiRepository.findModel(
                        settings.activeProviderId,
                        settings.activeModelId,
                    )
                    val hasKey = model == null ||
                        !ProviderIds.requiresApiKey(model.providerId) ||
                        keys.contains(model.providerId)
                    _state.update {
                        it.copy(settings = settings, activeModel = model, hasApiKey = hasKey)
                    }
                }
        }
    }

    private fun recoverInterruptedStreams() {
        val conversationId = initialConversationId ?: return
        viewModelScope.launch {
            runCatching {
                conversationRepository.getMessages(conversationId)
                    .filter { it.status == MessageStatus.STREAMING }
                    .forEach { message ->
                        conversationRepository.updateMessage(
                            messageId = message.id,
                            content = message.content,
                            status = MessageStatus.CANCELLED,
                        )
                    }
            }
        }
    }

    private fun credentialsReady(model: ModelInfo): Boolean {
        if (model.providerId == ProviderIds.LOCAL) {
            if (engine.status !is LocalEngineStatus.Ready) {
                val reason = (engine.status as? LocalEngineStatus.Unavailable)?.reason
                _state.update {
                    it.copy(
                        error = AiError.LocalEngineUnavailable(reason),
                        errorProviderId = model.providerId,
                    )
                }
                return false
            }
            return true
        }
        if (ProviderIds.requiresApiKey(model.providerId) && !apiKeys.hasKey(model.providerId)) {
            _state.update {
                it.copy(error = AiError.MissingApiKey, errorProviderId = model.providerId)
            }
            return false
        }
        return true
    }

    private suspend fun createConversation(text: String, model: ModelInfo): Long {
        val title = if (text.isBlank()) "Image" else text.toConversationTitle()
        val conversationId = conversationRepository.createConversation(
            title = title,
            providerId = model.providerId,
            modelId = model.id,
        )
        savedStateHandle[ARG_CONVERSATION_ID] = conversationId
        conversationIdFlow.value = conversationId
        _state.update {
            it.copy(conversationId = conversationId, title = title, isLoading = false)
        }
        return conversationId
    }

    /**
     * Adds the assistant placeholder, streams into it and finalizes its persisted status.
     * Cancellation and provider failures always leave a consistent row behind.
     */
    private suspend fun executeGeneration(conversationId: Long, model: ModelInfo) {
        val buffer = StringBuilder()
        var assistantId: Long? = null
        try {
            val settings = settingsRepository.current()
            val currentAssistantId = conversationRepository.addMessage(
                conversationId = conversationId,
                role = MessageRole.ASSISTANT,
                content = "",
                status = MessageStatus.STREAMING,
            )
            assistantId = currentAssistantId
            _state.update {
                it.copy(
                    isStreaming = true,
                    streamingMessageId = currentAssistantId,
                    streamingText = "",
                    error = null,
                )
            }

            val context = buildContext(
                conversationId = conversationId,
                assistantId = currentAssistantId,
                limit = settings.contextMessages,
                allowImages = model.capabilities.supportsImages,
            )
            if (context.isEmpty()) throw AiException(AiError.Unknown("Empty context"))

            var lastPersist = 0L
            aiRepository.stream(
                model = model,
                messages = context,
                systemPrompt = settings.systemPrompt,
                temperature = settings.temperature,
                maxOutputTokens = AppSettings.MAX_OUTPUT_TOKENS,
                stream = settings.streaming,
            ).collect { event ->
                when (event) {
                    is StreamEvent.Delta -> {
                        buffer.append(event.text)
                        _state.update { it.copy(streamingText = buffer.toString()) }
                        val now = SystemClock.elapsedRealtime()
                        if (now - lastPersist >= PERSIST_INTERVAL_MS) {
                            lastPersist = now
                            conversationRepository.updateMessage(
                                messageId = currentAssistantId,
                                content = buffer.toString(),
                                status = MessageStatus.STREAMING,
                            )
                        }
                    }

                    is StreamEvent.Completed -> Unit
                }
            }

            if (buffer.isEmpty()) throw AiException(AiError.EmptyResponse)
            conversationRepository.updateMessage(
                messageId = currentAssistantId,
                content = buffer.toString(),
                status = MessageStatus.COMPLETE,
            )
            conversationRepository.touchConversation(conversationId)
            _state.update {
                it.copy(isStreaming = false, streamingMessageId = null, streamingText = "")
            }
        } catch (cancelled: CancellationException) {
            withContext(NonCancellable) {
                assistantId?.let { id ->
                    conversationRepository.updateMessage(
                        messageId = id,
                        content = buffer.toString(),
                        status = MessageStatus.CANCELLED,
                    )
                }
                _state.update {
                    it.copy(isStreaming = false, streamingMessageId = null, streamingText = "")
                }
            }
            throw cancelled
        } catch (throwable: Throwable) {
            val error = (throwable as? AiException)?.error
                ?: AiError.Unknown(throwable.message)
            withContext(NonCancellable) {
                assistantId?.let { id ->
                    conversationRepository.updateMessage(
                        messageId = id,
                        content = buffer.toString(),
                        status = MessageStatus.ERROR,
                        error = errorCode(error),
                    )
                }
                _state.update {
                    it.copy(
                        isStreaming = false,
                        streamingMessageId = null,
                        streamingText = "",
                        error = error,
                        errorProviderId = model.providerId,
                    )
                }
            }
        }
    }

    private suspend fun buildContext(
        conversationId: Long,
        assistantId: Long,
        limit: Int,
        allowImages: Boolean,
    ): List<AiMessage> {
        val history = conversationRepository.getMessages(conversationId)
            .filter { it.id != assistantId }
            .filter { it.role == MessageRole.USER || it.status == MessageStatus.COMPLETE }
            .filter { it.content.isNotBlank() || it.attachments.isNotEmpty() }

        val window = history.takeLast(limit).dropWhile { it.role == MessageRole.ASSISTANT }
        val lastUserIndex = window.indexOfLast { it.role == MessageRole.USER }

        return window.mapIndexed { index, message ->
            val images = if (allowImages && index == lastUserIndex) {
                message.attachments.mapNotNull { attachmentStore.imageFor(it) }
            } else {
                emptyList()
            }
            AiMessage(role = message.role, content = message.content, images = images)
        }
    }

    private fun errorCode(error: AiError): String = when (error) {
        is AiError.Server -> "server_${error.code}"
        else -> error::class.simpleName ?: "unknown"
    }

    private companion object {
        const val ARG_CONVERSATION_ID = "conversationId"
        const val PERSIST_INTERVAL_MS = 450L
        const val MAX_ATTACHMENTS = 4
        const val MAX_TOTAL_ATTACHMENT_BYTES = 12L * 1024 * 1024
    }
}
