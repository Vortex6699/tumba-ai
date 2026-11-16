package com.tumba.ai.domain.model

/**
 * Defaults that must stay valid as providers retire model IDs.
 *
 * Gemini aliases such as `gemini-flash-latest` always point at the current stable Flash
 * model, which avoids hard-coding a generation that later returns HTTP 404.
 */
object ProviderDefaults {
    const val GEMINI_PROVIDER = "gemini"
    const val GEMINI_MODEL = "gemini-flash-latest"

    val RETIRED_GEMINI_MODELS = setOf(
        "gemini-2.0-flash",
        "gemini-2.0-flash-lite",
    )

    fun normalizeGeminiModel(modelId: String): String =
        if (modelId in RETIRED_GEMINI_MODELS) GEMINI_MODEL else modelId
}
