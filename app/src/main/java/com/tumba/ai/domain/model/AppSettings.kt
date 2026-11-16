package com.tumba.ai.domain.model

enum class ThemeMode {
    SYSTEM,
    DARK,
    LIGHT,
    ;

    companion object {
        fun fromStorage(value: String?): ThemeMode =
            entries.firstOrNull { it.name == value } ?: SYSTEM
    }
}

enum class AppLanguage(val tag: String) {
    SYSTEM(""),
    ENGLISH("en"),
    UKRAINIAN("uk"),
    ;

    companion object {
        fun fromStorage(value: String?): AppLanguage =
            entries.firstOrNull { it.name == value } ?: SYSTEM

        fun fromTag(tag: String?): AppLanguage =
            entries.firstOrNull { it.tag.isNotEmpty() && it.tag == tag } ?: SYSTEM
    }
}

data class AppSettings(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val language: AppLanguage = AppLanguage.SYSTEM,
    val systemPrompt: String = "",
    val contextMessages: Int = DEFAULT_CONTEXT_MESSAGES,
    val temperature: Float = DEFAULT_TEMPERATURE,
    val streaming: Boolean = true,
    val activeProviderId: String = ProviderDefaults.GEMINI_PROVIDER,
    val activeModelId: String = ProviderDefaults.GEMINI_MODEL,
) {
    val activeModel: ActiveModel
        get() = ActiveModel(activeProviderId, activeModelId)

    companion object {
        const val DEFAULT_CONTEXT_MESSAGES = 20
        const val MIN_CONTEXT_MESSAGES = 4
        const val MAX_CONTEXT_MESSAGES = 50
        const val DEFAULT_TEMPERATURE = 0.7f
        const val MIN_TEMPERATURE = 0f
        const val MAX_TEMPERATURE = 1.5f
        const val MAX_OUTPUT_TOKENS = 4096
    }
}
