package com.tumba.ai.ui.util

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.tumba.ai.R
import com.tumba.ai.core.ProviderIds
import com.tumba.ai.domain.ai.AiError

fun providerName(context: Context, providerId: String?): String = when (providerId) {
    ProviderIds.GEMINI -> context.getString(R.string.models_provider_gemini)
    ProviderIds.DEEPSEEK -> context.getString(R.string.models_provider_deepseek)
    ProviderIds.LOCAL -> context.getString(R.string.models_provider_local)
    null -> context.getString(R.string.models_provider_custom)
    else -> if (ProviderIds.isCustom(providerId)) {
        context.getString(R.string.models_provider_custom)
    } else {
        providerId
    }
}

@Composable
fun providerDisplayName(providerId: String?): String =
    providerName(LocalContext.current, providerId)


@StringRes
fun providerDescriptionRes(providerId: String): Int? = when (providerId) {
    ProviderIds.GEMINI -> R.string.models_description_gemini
    ProviderIds.DEEPSEEK -> R.string.models_description_deepseek
    ProviderIds.LOCAL -> R.string.models_description_local
    else -> if (ProviderIds.isCustom(providerId)) R.string.models_description_custom else null
}

@Composable
fun aiErrorMessage(error: AiError, providerId: String? = null): String {
    val provider = providerDisplayName(providerId)
    return when (error) {
        AiError.MissingApiKey -> stringResource(R.string.error_missing_key, provider)
        AiError.InvalidApiKey -> stringResource(R.string.error_invalid_key)
        AiError.RateLimited -> stringResource(R.string.error_rate_limited)
        AiError.Network -> stringResource(R.string.error_network)
        AiError.Timeout -> stringResource(R.string.error_timeout)
        AiError.EmptyResponse -> stringResource(R.string.error_empty_response)
        AiError.ModelNotFound -> stringResource(R.string.error_model_not_found)
        AiError.ContextTooLong -> stringResource(R.string.error_context_too_long)
        AiError.ImagesNotSupported -> stringResource(R.string.error_images_not_supported)
        AiError.ContentBlocked -> stringResource(R.string.error_content_blocked)
        is AiError.LocalEngineUnavailable -> error.detail
            ?.takeIf { it.isNotBlank() }
            ?.let { stringResource(R.string.error_local_engine_detail, it) }
            ?: stringResource(R.string.error_local_engine)
        is AiError.Server -> stringResource(R.string.error_server, error.code)
        is AiError.Unknown -> stringResource(R.string.error_unknown)
    }
}

@Composable
fun contextWindowLabel(tokens: Int?): String? = tokens?.let {
    stringResource(R.string.models_context_size, compactTokens(it))
}

fun compactTokens(tokens: Int): String = when {
    tokens >= 1_000_000 -> "${tokens / 1_000_000}M"
    tokens >= 1_000 -> "${tokens / 1_000}K"
    else -> tokens.toString()
}
