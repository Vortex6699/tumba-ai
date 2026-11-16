package com.tumba.ai.data.ai

import com.tumba.ai.domain.ai.AiError
import com.tumba.ai.domain.ai.AiException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Response
import java.io.IOException
import java.net.ConnectException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

internal val RETRYABLE_HTTP_CODES = setOf(408, 409, 425, 429, 500, 502, 503, 504, 529)

internal fun AiError.isRetryable(): Boolean = when (this) {
    AiError.Network, AiError.Timeout, AiError.RateLimited -> true
    is AiError.Server -> code in RETRYABLE_HTTP_CODES
    else -> false
}

internal fun AiException.isRetryable(): Boolean = error.isRetryable()

/**
 * Single place that translates an HTTP status + provider error envelope into a typed error.
 * The API key never appears in error messages because only the provider's message is used.
 */
internal fun aiErrorFor(code: Int, detail: String?): AiError = when {
    code == 401 || code == 403 -> AiError.InvalidApiKey
    code == 404 -> AiError.ModelNotFound
    code == 429 -> AiError.RateLimited
    code == 408 -> AiError.Timeout
    code == 400 && detail?.contains("api key", ignoreCase = true) == true -> AiError.InvalidApiKey
    code == 400 && detail?.contains("image", ignoreCase = true) == true -> AiError.ImagesNotSupported
    code == 400 && detail?.contains("context", ignoreCase = true) == true -> AiError.ContextTooLong
    code == 400 &&
        detail?.contains("model", ignoreCase = true) == true &&
        detail.contains("not found", ignoreCase = true) -> AiError.ModelNotFound
    else -> AiError.Server(code, detail)
}

internal fun Response.toAiException(json: Json): AiException {
    val body = runCatching { body?.string() }.getOrNull()
    return AiException(aiErrorFor(code, extractApiError(json, body)))
}

internal fun Throwable.toAiException(): AiException = when (this) {
    is AiException -> this
    is SocketTimeoutException -> AiException(AiError.Timeout, this)
    is UnknownHostException, is ConnectException, is SocketException -> AiException(AiError.Network, this)
    is IOException -> AiException(AiError.Network, this)
    else -> AiException(AiError.Unknown(message), this)
}

internal fun extractApiError(json: Json, body: String?): String? {
    if (body.isNullOrBlank()) return null
    return runCatching {
        val root = json.parseToJsonElement(body).jsonObject
        when (val error = root["error"]) {
            is JsonObject -> error["message"]?.jsonPrimitive?.contentOrNull
            is JsonPrimitive -> error.contentOrNull
            else -> null
        }
    }.getOrNull()
}

/**
 * Retries transient failures with linear backoff. Cancellation is always rethrown.
 */
internal suspend fun <T> retryIo(
    attempts: Int = 2,
    initialDelayMs: Long = 600L,
    block: suspend () -> T,
): T {
    var attempt = 0
    while (true) {
        try {
            return block()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            val retryable = when (throwable) {
                is AiException -> throwable.isRetryable()
                is IOException -> true
                else -> false
            }
            if (!retryable || attempt >= attempts) throw throwable
            attempt += 1
            delay(initialDelayMs * attempt)
        }
    }
}
