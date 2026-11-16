package com.tumba.ai.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tumba.ai.domain.model.ConversationSummary
import com.tumba.ai.domain.repository.ConversationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val conversationRepository: ConversationRepository,
) : ViewModel() {

    private val queryFlow = MutableStateFlow("")
    val searchQuery = queryFlow.asStateFlow()

    val conversations: StateFlow<List<ConversationSummary>> = queryFlow
        .debounce(220)
        .flatMapLatest { query -> conversationRepository.searchConversations(query) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    fun onQueryChange(query: String) {
        queryFlow.value = query
    }

    fun rename(conversationId: Long, title: String) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            conversationRepository.renameConversation(conversationId, trimmed)
        }
    }

    fun delete(conversationId: Long) {
        viewModelScope.launch {
            conversationRepository.deleteConversation(conversationId)
        }
    }

    fun setPinned(conversationId: Long, pinned: Boolean) {
        viewModelScope.launch {
            conversationRepository.setPinned(conversationId, pinned)
        }
    }
}
