package com.tumba.ai.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tumba.ai.R
import com.tumba.ai.domain.model.AppLanguage
import com.tumba.ai.domain.model.AppSettings
import com.tumba.ai.domain.model.ConversationStats
import com.tumba.ai.domain.model.ThemeMode
import com.tumba.ai.domain.repository.ApiKeyRepository
import com.tumba.ai.domain.repository.ConversationRepository
import com.tumba.ai.domain.repository.SettingsRepository
import com.tumba.ai.ui.util.UiNotice
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val keys: Set<String> = emptySet(),
    val stats: ConversationStats = ConversationStats(),
    val exporting: Boolean = false,
    val notice: UiNotice? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val apiKeys: ApiKeyRepository,
    private val conversationRepository: ConversationRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                settingsRepository.settings,
                apiKeys.configuredProviders,
                conversationRepository.observeStats(),
            ) { settings, keys, stats -> Triple(settings, keys, stats) }
                .collect { (settings, keys, stats) ->
                    _state.update { it.copy(settings = settings, keys = keys, stats = stats) }
                }
        }
    }

    fun setTheme(theme: ThemeMode) {
        viewModelScope.launch { settingsRepository.setTheme(theme) }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch { settingsRepository.setLanguage(language) }
    }

    fun setSystemPrompt(prompt: String) {
        viewModelScope.launch { settingsRepository.setSystemPrompt(prompt) }
    }

    fun setContextMessages(count: Int) {
        viewModelScope.launch { settingsRepository.setContextMessages(count) }
    }

    fun setTemperature(value: Float) {
        viewModelScope.launch { settingsRepository.setTemperature(value) }
    }

    fun setStreaming(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setStreaming(enabled) }
    }

    fun saveApiKey(providerId: String, key: String) {
        if (key.isBlank()) return
        viewModelScope.launch {
            apiKeys.setKey(providerId, key)
            notice(R.string.models_key_saved_toast)
        }
    }

    fun removeApiKey(providerId: String) {
        viewModelScope.launch {
            apiKeys.removeKey(providerId)
            notice(R.string.models_key_removed_toast)
        }
    }

    fun deleteAllConversations() {
        viewModelScope.launch {
            conversationRepository.deleteAllConversations()
            notice(R.string.settings_cleared)
        }
    }

    fun deleteAllApiKeys() {
        viewModelScope.launch {
            apiKeys.clearAll()
            notice(R.string.settings_keys_cleared)
        }
    }

    fun exportTo(uri: Uri) {
        if (_state.value.exporting) return
        _state.update { it.copy(exporting = true) }
        viewModelScope.launch {
            try {
                val stream = context.contentResolver.openOutputStream(uri)
                    ?: throw IOException("Cannot open export destination")
                stream.use { output -> conversationRepository.exportAll(output) }
                notice(R.string.settings_export_success)
            } catch (throwable: Throwable) {
                notice(R.string.settings_export_failed)
            } finally {
                _state.update { it.copy(exporting = false) }
            }
        }
    }

    fun clearNotice() {
        _state.update { it.copy(notice = null) }
    }

    private fun notice(messageRes: Int) {
        _state.update { it.copy(notice = UiNotice(messageRes)) }
    }
}
