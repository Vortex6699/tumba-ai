package com.tumba.ai.domain.ai

/**
 * Every failed model call is expressed as one of these cases so the UI can localize it
 * instead of showing raw provider payloads (which may contain request fragments).
 */
sealed class AiError {
    data object MissingApiKey : AiError()
    data object InvalidApiKey : AiError()
    data object RateLimited : AiError()
    data object Network : AiError()
    data object Timeout : AiError()
    data object EmptyResponse : AiError()
    data object ModelNotFound : AiError()
    data object ContextTooLong : AiError()
    data object ImagesNotSupported : AiError()
    data object ContentBlocked : AiError()
    /** [detail] carries the technical reason (missing ABI library, link error, ...). */
    data class LocalEngineUnavailable(val detail: String? = null) : AiError()
    data class Server(val code: Int, val detail: String? = null) : AiError()
    data class Unknown(val detail: String? = null) : AiError()
}

class AiException(
    val error: AiError,
    cause: Throwable? = null,
) : Exception(
    when (error) {
        is AiError.Server -> "Provider error ${error.code}"
        is AiError.Unknown -> "AI request failed"
        AiError.MissingApiKey -> "Missing API key"
        AiError.InvalidApiKey -> "Invalid API key"
        AiError.RateLimited -> "Rate limited"
        AiError.Network -> "Network error"
        AiError.Timeout -> "Request timed out"
        AiError.EmptyResponse -> "Empty response"
        AiError.ModelNotFound -> "Model not found"
        AiError.ContextTooLong -> "Context too long"
        AiError.ImagesNotSupported -> "Images not supported"
        AiError.ContentBlocked -> "Blocked by safety filters"
        is AiError.LocalEngineUnavailable -> "Local engine unavailable"
    },
    cause,
)
