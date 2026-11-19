package com.tumba.ai.ui.chat

import android.content.ContextWrapper
import androidx.lifecycle.SavedStateHandle
import com.tumba.ai.core.DispatcherProvider
import com.tumba.ai.core.ProviderIds
import com.tumba.ai.data.ai.local.LocalEngineStatus
import com.tumba.ai.data.ai.local.LocalInferenceEngine
import com.tumba.ai.data.files.AttachmentStore
import com.tumba.ai.domain.ai.AIRepository
import com.tumba.ai.domain.ai.AiError
import com.tumba.ai.domain.ai.AiException
import com.tumba.ai.domain.ai.AiMessage
import com.tumba.ai.domain.ai.ConnectionTest
import com.tumba.ai.domain.ai.StreamEvent
import com.tumba.ai.domain.model.ActiveModel
import com.tumba.ai.domain.model.AppLanguage
import com.tumba.ai.domain.model.AppSettings
import com.tumba.ai.domain.model.Attachment
import com.tumba.ai.domain.model.Conversation
import com.tumba.ai.domain.model.ConversationStats
import com.tumba.ai.domain.model.ConversationSummary
import com.tumba.ai.domain.model.Message
import com.tumba.ai.domain.model.MessageRole
import com.tumba.ai.domain.model.MessageStatus
import com.tumba.ai.domain.model.ModelCapabilities
import com.tumba.ai.domain.model.ModelInfo
import com.tumba.ai.domain.model.ProviderDefaults
import com.tumba.ai.domain.model.ThemeMode
import com.tumba.ai.domain.repository.ApiKeyRepository
import com.tumba.ai.domain.repository.ConversationRepository
import com.tumba.ai.domain.repository.NewAttachment
import com.tumba.ai.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.OutputStream
import java.util.concurrent.atomic.AtomicLong

@OptIn(ExperimentalCoroutinesApi::class)
class ChatInitializationTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var conversationRepo: FakeConversationRepository
    private lateinit var aiRepo: FakeAIRepository
    private lateinit var settingsRepo: FakeSettingsRepository
    private lateinit var apiKeys: FakeApiKeyRepository
    private lateinit var localEngine: FakeLocalInferenceEngine
    private lateinit var attachmentStore: AttachmentStore

    private val testModel = ModelInfo(
        id = ProviderDefaults.GEMINI_MODEL,
        providerId = ProviderIds.GEMINI,
        displayName = "Gemini Flash Latest",
        contextWindow = 32000,
        capabilities = ModelCapabilities(supportsImages = true),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        conversationRepo = FakeConversationRepository()
        aiRepo = FakeAIRepository(mutableListOf(testModel))
        settingsRepo = FakeSettingsRepository()
        apiKeys = FakeApiKeyRepository(setOf(ProviderIds.GEMINI))
        localEngine = FakeLocalInferenceEngine()
        attachmentStore = AttachmentStore(
            context = ContextWrapper(null),
            dispatchers = FakeDispatcherProvider(testDispatcher),
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(conversationId: Long? = null): ChatViewModel {
        val handle = if (conversationId != null) {
            SavedStateHandle(mapOf("conversationId" to conversationId))
        } else {
            SavedStateHandle(mapOf("conversationId" to -1L))
        }
        return ChatViewModel(
            savedStateHandle = handle,
            conversationRepository = conversationRepo,
            aiRepository = aiRepo,
            settingsRepository = settingsRepo,
            apiKeys = apiKeys,
            attachmentStore = attachmentStore,
            engine = localEngine,
        )
    }

    @Test
    fun testFirstMessageCreatesChatAndGeneratesResponse() = runTest(testDispatcher) {
        val vm = createViewModel()
        testScheduler.advanceUntilIdle()

        // 1. New chat state initially
        assertNull(vm.state.value.conversationId)
        assertTrue(vm.state.value.messages.isEmpty())
        assertFalse(vm.state.value.isStreaming)

        // 2. User enters "Привіт" and clicks send
        vm.onInputChange("Привіт")
        assertEquals("Привіт", vm.state.value.input)
        vm.onSend()

        // Input should be cleared immediately and streaming should start
        assertEquals("", vm.state.value.input)
        assertTrue(vm.state.value.isStreaming)

        testScheduler.advanceUntilIdle()

        // 3. Conversation must be created with title "Привіт"
        val convId = vm.state.value.conversationId
        assertNotNull(convId)
        assertTrue(convId!! > 0L)
        assertEquals("Привіт", vm.state.value.title)

        // 4. Messages must contain User message and completed Assistant message
        val messages = vm.state.value.messages
        assertEquals(2, messages.size)
        assertEquals(MessageRole.USER, messages[0].role)
        assertEquals("Привіт", messages[0].content)
        assertEquals(MessageStatus.COMPLETE, messages[0].status)

        assertEquals(MessageRole.ASSISTANT, messages[1].role)
        assertEquals("Echo: Привіт", messages[1].content)
        assertEquals(MessageStatus.COMPLETE, messages[1].status)

        assertFalse(vm.state.value.isStreaming)
        assertNull(vm.state.value.error)

        // 5. Saved in history
        val savedMessages = conversationRepo.getMessages(convId)
        assertEquals(2, savedMessages.size)
    }

    @Test
    fun testConsecutiveMessagesDoNotCreateDuplicateConversations() = runTest(testDispatcher) {
        val vm = createViewModel()
        testScheduler.advanceUntilIdle()

        // First message
        vm.onInputChange("Привіт")
        vm.onSend()
        testScheduler.advanceUntilIdle()

        val firstConvId = vm.state.value.conversationId
        assertNotNull(firstConvId)
        assertEquals(1, conversationRepo.conversationsCount())

        // Second message
        vm.onInputChange("Як справи?")
        vm.onSend()
        testScheduler.advanceUntilIdle()

        val secondConvId = vm.state.value.conversationId
        assertEquals(firstConvId, secondConvId)
        assertEquals(1, conversationRepo.conversationsCount())

        val messages = vm.state.value.messages
        assertEquals(4, messages.size)
        assertEquals("Привіт", messages[0].content)
        assertEquals("Echo: Привіт", messages[1].content)
        assertEquals("Як справи?", messages[2].content)
        assertEquals("Echo: Як справи?", messages[3].content)
    }

    @Test
    fun testTwoIdenticalMessagesSendCorrectly() = runTest(testDispatcher) {
        val vm = createViewModel()
        testScheduler.advanceUntilIdle()

        // First "Привіт"
        vm.onInputChange("Привіт")
        vm.onSend()
        testScheduler.advanceUntilIdle()

        val convId = vm.state.value.conversationId
        assertNotNull(convId)

        // Second identical "Привіт"
        vm.onInputChange("Привіт")
        vm.onSend()
        testScheduler.advanceUntilIdle()

        assertEquals(convId, vm.state.value.conversationId)
        val messages = vm.state.value.messages
        assertEquals(4, messages.size)
        assertEquals("Привіт", messages[0].content)
        assertEquals("Echo: Привіт", messages[1].content)
        assertEquals("Привіт", messages[2].content)
        assertEquals("Echo: Привіт", messages[3].content)
    }

    @Test
    fun testNetworkErrorPreservesUserMessageAndReportsError() = runTest(testDispatcher) {
        aiRepo.shouldFail = true
        val vm = createViewModel()
        testScheduler.advanceUntilIdle()

        vm.onInputChange("Привіт")
        vm.onSend()
        testScheduler.advanceUntilIdle()

        val convId = vm.state.value.conversationId
        assertNotNull(convId)

        // User message must still be in the chat
        val messages = vm.state.value.messages
        assertTrue(messages.any { it.role == MessageRole.USER && it.content == "Привіт" })

        // Error must be reported and streaming stopped
        assertFalse(vm.state.value.isStreaming)
        assertNotNull(vm.state.value.error)
    }

    @Test
    fun testSwitchingBetweenConversationsLoadsCorrectMessages() = runTest(testDispatcher) {
        val c1 = conversationRepo.createConversation("Chat 1", ProviderIds.GEMINI, testModel.id)
        conversationRepo.addMessage(c1, MessageRole.USER, "Message in 1", MessageStatus.COMPLETE)

        val c2 = conversationRepo.createConversation("Chat 2", ProviderIds.GEMINI, testModel.id)
        conversationRepo.addMessage(c2, MessageRole.USER, "Message in 2", MessageStatus.COMPLETE)

        val handle = SavedStateHandle(mapOf("conversationId" to c1))
        val vm = ChatViewModel(
            savedStateHandle = handle,
            conversationRepository = conversationRepo,
            aiRepository = aiRepo,
            settingsRepository = settingsRepo,
            apiKeys = apiKeys,
            attachmentStore = attachmentStore,
            engine = localEngine,
        )
        testScheduler.advanceUntilIdle()

        assertEquals(c1, vm.state.value.conversationId)
        assertEquals("Chat 1", vm.state.value.title)
        assertEquals("Message in 1", vm.state.value.messages.firstOrNull()?.content)

        // Switch to Chat 2 via SavedStateHandle
        handle["conversationId"] = c2
        testScheduler.advanceUntilIdle()

        assertEquals(c2, vm.state.value.conversationId)
        assertEquals("Chat 2", vm.state.value.title)
        assertEquals("Message in 2", vm.state.value.messages.firstOrNull()?.content)

        // Switch to New Chat (-1)
        handle["conversationId"] = -1L
        testScheduler.advanceUntilIdle()

        assertNull(vm.state.value.conversationId)
        assertTrue(vm.state.value.messages.isEmpty())
    }

    @Test
    fun testLocalModelExecution() = runTest(testDispatcher) {
        val localModel = ModelInfo(
            id = "qwen-local",
            providerId = ProviderIds.LOCAL,
            displayName = "Qwen 2.5 0.5B",
            contextWindow = 8192,
            capabilities = ModelCapabilities(supportsImages = false),
        )
        aiRepo.models += localModel
        settingsRepo.setActiveModel(ActiveModel(ProviderIds.LOCAL, localModel.id))

        val vm = createViewModel()
        testScheduler.advanceUntilIdle()

        vm.onInputChange("Тест локальної моделі")
        vm.onSend()
        testScheduler.advanceUntilIdle()

        val convId = vm.state.value.conversationId
        assertNotNull(convId)
        assertEquals(2, vm.state.value.messages.size)
        assertEquals("Echo: Тест локальної моделі", vm.state.value.messages[1].content)
        assertFalse(vm.state.value.isStreaming)
    }
}

// ---------------- Fake Implementations ----------------

private class FakeDispatcherProvider(
    dispatcher: CoroutineDispatcher,
) : DispatcherProvider {
    override val io: CoroutineDispatcher = dispatcher
    override val default: CoroutineDispatcher = dispatcher
}

private class FakeConversationRepository : ConversationRepository {
    private val convSeq = AtomicLong(1L)
    private val msgSeq = AtomicLong(1L)

    private val convMap = mutableMapOf<Long, Conversation>()
    private val messagesFlowMap = mutableMapOf<Long, MutableStateFlow<List<Message>>>()

    fun conversationsCount() = convMap.size

    override fun observeConversations(): Flow<List<ConversationSummary>> = flow {
        emit(convMap.values.map {
            ConversationSummary(
                id = it.id,
                title = it.title,
                preview = null,
                updatedAt = it.updatedAt,
                providerId = it.providerId,
                modelId = it.modelId,
                messageCount = messagesFlowMap[it.id]?.value?.size ?: 0,
                pinned = it.pinned,
            )
        })
    }

    override fun searchConversations(query: String): Flow<List<ConversationSummary>> = observeConversations()

    override fun observeMessages(conversationId: Long): Flow<List<Message>> {
        return messagesFlowMap.getOrPut(conversationId) { MutableStateFlow(emptyList()) }.asStateFlow()
    }

    override fun observeConversation(conversationId: Long): Flow<Conversation?> = flow {
        emit(convMap[conversationId])
    }

    override fun observeStats(): Flow<ConversationStats> = flow {
        emit(ConversationStats(convMap.size, messagesFlowMap.values.sumOf { it.value.size }))
    }

    override suspend fun getConversation(conversationId: Long): Conversation? = convMap[conversationId]

    override suspend fun getMessages(conversationId: Long): List<Message> {
        return messagesFlowMap[conversationId]?.value ?: emptyList()
    }

    override suspend fun createConversation(title: String, providerId: String, modelId: String): Long {
        val id = convSeq.getAndIncrement()
        val conv = Conversation(
            id = id,
            title = title,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            providerId = providerId,
            modelId = modelId,
        )
        convMap[id] = conv
        messagesFlowMap.getOrPut(id) { MutableStateFlow(emptyList()) }
        return id
    }

    override suspend fun addMessage(
        conversationId: Long,
        role: MessageRole,
        content: String,
        status: MessageStatus,
        error: String?,
        attachments: List<NewAttachment>,
    ): Long {
        val id = msgSeq.getAndIncrement()
        val msg = Message(
            id = id,
            conversationId = conversationId,
            content = content,
            role = role,
            timestamp = System.currentTimeMillis(),
            status = status,
            error = error,
        )
        val flow = messagesFlowMap.getOrPut(conversationId) { MutableStateFlow(emptyList()) }
        flow.value = flow.value + msg
        return id
    }

    override suspend fun updateMessage(
        messageId: Long,
        content: String,
        status: MessageStatus,
        error: String?,
    ) {
        messagesFlowMap.values.forEach { flow ->
            val list = flow.value
            val idx = list.indexOfFirst { it.id == messageId }
            if (idx >= 0) {
                val updated = list.toMutableList()
                updated[idx] = updated[idx].copy(content = content, status = status, error = error)
                flow.value = updated
            }
        }
    }

    override suspend fun deleteMessage(messageId: Long) {
        messagesFlowMap.values.forEach { flow ->
            flow.value = flow.value.filterNot { it.id == messageId }
        }
    }

    override suspend fun renameConversation(conversationId: Long, title: String) {
        convMap[conversationId]?.let {
            convMap[conversationId] = it.copy(title = title, updatedAt = System.currentTimeMillis())
        }
    }

    override suspend fun touchConversation(conversationId: Long) {
        convMap[conversationId]?.let {
            convMap[conversationId] = it.copy(updatedAt = System.currentTimeMillis())
        }
    }

    override suspend fun setPinned(conversationId: Long, pinned: Boolean) {
        convMap[conversationId]?.let {
            convMap[conversationId] = it.copy(pinned = pinned)
        }
    }

    override suspend fun deleteConversation(conversationId: Long) {
        convMap.remove(conversationId)
        messagesFlowMap.remove(conversationId)
    }

    override suspend fun deleteAllConversations() {
        convMap.clear()
        messagesFlowMap.clear()
    }

    override suspend fun getAttachments(messageId: Long): List<Attachment> = emptyList()

    override suspend fun exportAll(output: OutputStream) {}
}

private class FakeAIRepository(
    val models: MutableList<ModelInfo>,
    var shouldFail: Boolean = false,
) : AIRepository {
    override suspend fun availableModels(): List<ModelInfo> = models
    override suspend fun discoverModels(providerId: String): List<ModelInfo> = models.filter { it.providerId == providerId }
    override suspend fun findModel(providerId: String, modelId: String): ModelInfo? =
        models.firstOrNull { it.providerId == providerId && it.id == modelId }

    override fun stream(
        model: ModelInfo,
        messages: List<AiMessage>,
        systemPrompt: String,
        temperature: Float,
        maxOutputTokens: Int,
        stream: Boolean,
    ): Flow<StreamEvent> = flow {
        if (shouldFail) {
            throw AiException(AiError.Network)
        }
        val lastText = messages.lastOrNull { it.role == MessageRole.USER }?.content ?: ""
        emit(StreamEvent.Delta("Echo: "))
        emit(StreamEvent.Delta(lastText))
        emit(StreamEvent.Completed())
    }

    override suspend fun testConnection(model: ModelInfo): ConnectionTest = ConnectionTest(latencyMs = 100L)
}

private class FakeSettingsRepository : SettingsRepository {
    private val settingsFlow = MutableStateFlow(AppSettings())
    override val settings: Flow<AppSettings> = settingsFlow.asStateFlow()

    override suspend fun current(): AppSettings = settingsFlow.value
    override suspend fun setTheme(theme: ThemeMode) { settingsFlow.value = settingsFlow.value.copy(theme = theme) }
    override suspend fun setLanguage(language: AppLanguage) { settingsFlow.value = settingsFlow.value.copy(language = language) }
    override suspend fun setSystemPrompt(prompt: String) { settingsFlow.value = settingsFlow.value.copy(systemPrompt = prompt) }
    override suspend fun setContextMessages(count: Int) { settingsFlow.value = settingsFlow.value.copy(contextMessages = count) }
    override suspend fun setTemperature(value: Float) { settingsFlow.value = settingsFlow.value.copy(temperature = value) }
    override suspend fun setStreaming(enabled: Boolean) { settingsFlow.value = settingsFlow.value.copy(streaming = enabled) }
    override suspend fun setActiveModel(model: ActiveModel) {
        settingsFlow.value = settingsFlow.value.copy(
            activeProviderId = model.providerId,
            activeModelId = model.modelId,
        )
    }
}

private class FakeApiKeyRepository(configured: Set<String>) : ApiKeyRepository {
    private val flow = MutableStateFlow(configured)
    override val configuredProviders = flow.asStateFlow()

    override fun hasKey(providerId: String): Boolean = flow.value.contains(providerId)
    override suspend fun setKey(providerId: String, key: String) { flow.value = flow.value + providerId }
    override suspend fun getKey(providerId: String): String? = if (hasKey(providerId)) "dummy" else null
    override suspend fun removeKey(providerId: String) { flow.value = flow.value - providerId }
    override suspend fun clearAll() { flow.value = emptySet() }
}

private class FakeLocalInferenceEngine : LocalInferenceEngine {
    override val status: LocalEngineStatus = LocalEngineStatus.Ready

    override fun generate(
        modelPath: String,
        systemPrompt: String,
        messages: List<AiMessage>,
        maxTokens: Int,
        temperature: Float,
        contextWindow: Int?,
    ): Flow<String> = flow {
        emit("Local Echo")
    }

    override fun unload() {}
}
