package com.tumba.ai.core

object ProviderIds {
    const val GEMINI = "gemini"
    const val DEEPSEEK = "deepseek"
    const val LOCAL = "local"

    private const val CUSTOM_PREFIX = "custom:"

    fun custom(id: String): String = CUSTOM_PREFIX + id

    fun isCustom(providerId: String): Boolean = providerId.startsWith(CUSTOM_PREFIX)

    fun customId(providerId: String): String? =
        if (isCustom(providerId)) providerId.removePrefix(CUSTOM_PREFIX) else null

    /** Built-in cloud providers always require a key; custom endpoints may be keyless. */
    fun requiresApiKey(providerId: String): Boolean =
        providerId == GEMINI || providerId == DEEPSEEK
}
