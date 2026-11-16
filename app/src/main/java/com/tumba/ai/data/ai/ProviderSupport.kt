package com.tumba.ai.data.ai

import com.tumba.ai.data.network.ModelDiscoveryApi
import com.tumba.ai.domain.ai.AiError
import com.tumba.ai.domain.ai.AiException
import com.tumba.ai.domain.ai.AiMessage
import kotlinx.serialization.json.Json
import okhttp3.ResponseBody
import retrofit2.Response

/**
 * Shared health-check plumbing. Never includes the key in URLs or logs.
 */
internal suspend fun ModelDiscoveryApi.checkConnection(
    url: String,
    apiKeyHeader: String?,
    bearerToken: String?,
    json: Json,
) {
    retryIo {
        val response = get(url, bearerToken?.let { "Bearer $it" }, apiKeyHeader)
        if (!response.isSuccessful) {
            val detail = runCatching { response.errorBody()?.string() }.getOrNull()
            response.errorBody()?.close()
            response.body()?.close()
            throw AiException(aiErrorFor(response.code(), extractApiError(json, detail)))
        }
        response.body()?.close()
    }
}

internal fun requireApiKey(key: String?): String =
    key?.takeIf { it.isNotBlank() } ?: throw AiException(AiError.MissingApiKey)

internal fun baseUrlOrThrow(baseUrl: String?): String {
    val normalized = baseUrl?.trim()?.trimEnd('/')
    if (normalized.isNullOrBlank()) throw AiException(AiError.Unknown("Endpoint base URL is missing"))
    return normalized
}

/**
 * Some providers reject two consecutive messages with the same role. Folding them keeps
 * multi-turn context intact and avoids provider-specific 400 errors.
 */
internal fun List<AiMessage>.mergeAdjacentRoles(): List<AiMessage> {
    val merged = mutableListOf<AiMessage>()
    for (message in this) {
        if (message.content.isBlank() && message.images.isEmpty()) continue
        val last = merged.lastOrNull()
        if (last != null && last.role == message.role) {
            merged[merged.lastIndex] = last.copy(
                content = listOf(last.content, message.content)
                    .filter { it.isNotBlank() }
                    .joinToString("\n\n"),
                images = last.images + message.images,
            )
        } else {
            merged += message
        }
    }
    return merged
}
