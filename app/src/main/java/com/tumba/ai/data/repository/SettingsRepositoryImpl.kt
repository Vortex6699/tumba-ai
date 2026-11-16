package com.tumba.ai.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.tumba.ai.domain.model.ActiveModel
import com.tumba.ai.domain.model.AppLanguage
import com.tumba.ai.domain.model.AppSettings
import com.tumba.ai.domain.model.ProviderDefaults
import com.tumba.ai.domain.model.ThemeMode
import com.tumba.ai.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore("tumba_settings")

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : SettingsRepository {

    private object Keys {
        val theme = stringPreferencesKey("theme")
        val language = stringPreferencesKey("language")
        val systemPrompt = stringPreferencesKey("system_prompt")
        val contextMessages = intPreferencesKey("context_messages")
        val temperature = floatPreferencesKey("temperature")
        val streaming = booleanPreferencesKey("streaming")
        val activeProvider = stringPreferencesKey("active_provider")
        val activeModel = stringPreferencesKey("active_model")
    }

    override val settings: Flow<AppSettings> = context.settingsDataStore.data
        .catch { throwable ->
            if (throwable is IOException) emit(emptyPreferences()) else throw throwable
        }
        .map { prefs ->
            val activeProvider = prefs[Keys.activeProvider] ?: ProviderDefaults.GEMINI_PROVIDER
            val storedModel = prefs[Keys.activeModel] ?: ProviderDefaults.GEMINI_MODEL
            AppSettings(
                theme = ThemeMode.fromStorage(prefs[Keys.theme]),
                language = AppLanguage.fromStorage(prefs[Keys.language]),
                systemPrompt = prefs[Keys.systemPrompt].orEmpty(),
                contextMessages = (prefs[Keys.contextMessages]
                    ?: AppSettings.DEFAULT_CONTEXT_MESSAGES)
                    .coerceIn(AppSettings.MIN_CONTEXT_MESSAGES, AppSettings.MAX_CONTEXT_MESSAGES),
                temperature = (prefs[Keys.temperature] ?: AppSettings.DEFAULT_TEMPERATURE)
                    .coerceIn(AppSettings.MIN_TEMPERATURE, AppSettings.MAX_TEMPERATURE),
                streaming = prefs[Keys.streaming] ?: true,
                activeProviderId = activeProvider,
                activeModelId = if (activeProvider == ProviderDefaults.GEMINI_PROVIDER) {
                    ProviderDefaults.normalizeGeminiModel(storedModel)
                } else {
                    storedModel
                },
            )
        }

    override suspend fun current(): AppSettings = settings.first()

    override suspend fun setTheme(theme: ThemeMode) {
        context.settingsDataStore.edit { it[Keys.theme] = theme.name }
    }

    override suspend fun setLanguage(language: AppLanguage) {
        context.settingsDataStore.edit { it[Keys.language] = language.name }
    }

    override suspend fun setSystemPrompt(prompt: String) {
        context.settingsDataStore.edit { it[Keys.systemPrompt] = prompt }
    }

    override suspend fun setContextMessages(count: Int) {
        context.settingsDataStore.edit {
            it[Keys.contextMessages] = count.coerceIn(
                AppSettings.MIN_CONTEXT_MESSAGES,
                AppSettings.MAX_CONTEXT_MESSAGES,
            )
        }
    }

    override suspend fun setTemperature(value: Float) {
        context.settingsDataStore.edit {
            it[Keys.temperature] = value.coerceIn(
                AppSettings.MIN_TEMPERATURE,
                AppSettings.MAX_TEMPERATURE,
            )
        }
    }

    override suspend fun setStreaming(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.streaming] = enabled }
    }

    override suspend fun setActiveModel(model: ActiveModel) {
        context.settingsDataStore.edit {
            it[Keys.activeProvider] = model.providerId
            it[Keys.activeModel] = model.modelId
        }
    }
}
