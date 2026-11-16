package com.tumba.ai.data.ai

import com.tumba.ai.domain.ai.AiException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Executes HTTP calls for every provider. Streaming uses a blocking OkHttp read on the IO
 * dispatcher and cancels the call when the coroutine is cancelled, so "Stop" is immediate.
 *
 * Retries happen only if the stream failed before emitting any payload, which prevents
 * duplicated text after a partial answer.
 */
@Singleton
class AiHttpClient @Inject constructor(
    private val client: OkHttpClient,
    private val json: Json,
) {

    fun streamData(request: Request): Flow<String> = flow {
        var attempt = 0
        var payloadSeen = false
        while (true) {
            try {
                streamOnce(request).collect { payload ->
                    payloadSeen = true
                    emit(payload)
                }
                return@flow
            } catch (exception: AiException) {
                if (payloadSeen || !exception.isRetryable() || attempt >= MAX_RETRIES) throw exception
                attempt += 1
                delay(backoffMs(attempt))
            } catch (io: IOException) {
                val mapped = io.toAiException()
                if (payloadSeen || attempt >= MAX_RETRIES) throw mapped
                attempt += 1
                delay(backoffMs(attempt))
            }
        }
    }.flowOn(Dispatchers.IO)

    suspend fun execute(request: Request): String = withContext(Dispatchers.IO) {
        val call = client.newCall(request)
        val handle = currentCoroutineContext()[Job]?.invokeOnCompletion { call.cancel() }
        try {
            call.execute().use { response ->
                if (!response.isSuccessful) throw response.toAiException(json)
                response.body?.string()?.takeIf { it.isNotBlank() }
                    ?: throw AiException(com.tumba.ai.domain.ai.AiError.EmptyResponse)
            }
        } catch (io: IOException) {
            if (!currentCoroutineContext().isActive) throw CancellationException("Cancelled")
            throw io.toAiException()
        } finally {
            handle?.dispose()
        }
    }

    private fun streamOnce(request: Request): Flow<String> = flow {
        val call = client.newCall(request)
        val handle = currentCoroutineContext()[Job]?.invokeOnCompletion { call.cancel() }
        try {
            call.execute().use { response ->
                if (!response.isSuccessful) throw response.toAiException(json)
                val body = response.body ?: throw AiException(
                    com.tumba.ai.domain.ai.AiError.EmptyResponse,
                )
                val source = body.source()
                val accumulator = SseAccumulator()
                while (currentCoroutineContext().isActive) {
                    val line = source.readUtf8Line() ?: break
                    accumulator.consume(line)?.let { payload -> emit(payload) }
                }
                accumulator.finish()?.let { payload -> emit(payload) }
            }
        } catch (io: IOException) {
            if (!currentCoroutineContext().isActive) throw CancellationException("Cancelled")
            throw io.toAiException()
        } finally {
            handle?.dispose()
        }
    }.flowOn(Dispatchers.IO)

    private fun backoffMs(attempt: Int): Long =
        (BASE_BACKOFF_MS shl (attempt - 1)) + (0..250).random()

    private companion object {
        const val MAX_RETRIES = 2
        const val BASE_BACKOFF_MS = 800L
    }
}
