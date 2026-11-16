package com.tumba.ai.domain.repository

import com.tumba.ai.domain.model.ActiveModel
import com.tumba.ai.domain.model.AppLanguage
import com.tumba.ai.domain.model.AppSettings
import com.tumba.ai.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<AppSettings>

    suspend fun current(): AppSettings

    suspend fun setTheme(theme: ThemeMode)

    suspend fun setLanguage(language: AppLanguage)

    suspend fun setSystemPrompt(prompt: String)

    suspend fun setContextMessages(count: Int)

    suspend fun setTemperature(value: Float)

    suspend fun setStreaming(enabled: Boolean)

    suspend fun setActiveModel(model: ActiveModel)
}
