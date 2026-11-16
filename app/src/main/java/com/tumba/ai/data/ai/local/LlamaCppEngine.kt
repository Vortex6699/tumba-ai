package com.tumba.ai.data.ai.local

import android.content.Context
import android.os.Build
import com.tumba.ai.domain.ai.AiError
import com.tumba.ai.domain.ai.AiException
import com.tumba.ai.domain.ai.AiMessage
import com.tumba.ai.domain.model.MessageRole
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

sealed interface LocalEngineStatus {
    data object Ready : LocalEngineStatus
    data class Unavailable(val reason: String) : LocalEngineStatus
}

/**
 * Callback used by the native layer to stream tokens back to Kotlin. It must stay public and
 * keep its method names: the JNI bridge resolves `onToken` and `isCancelled` by name.
 */
interface TokenCallback {
    fun onToken(text: String)
    fun isCancelled(): Boolean
}

/**
 * Contract for an on-device inference backend. Model management (import, download, size,
 * memory checks) lives outside the engine so it works even when native code is missing.
 */
interface LocalInferenceEngine {
    val status: LocalEngineStatus

    fun generate(
        modelPath: String,
        systemPrompt: String,
        messages: List<AiMessage>,
        maxTokens: Int,
        temperature: Float,
        contextWindow: Int?,
    ): Flow<String>

    /** Releases the loaded model. Safe to call at any time. */
    fun unload()

    /** Human-readable reason when [status] is not Ready (empty otherwise). */
    fun unavailableReason(): String =
        (status as? LocalEngineStatus.Unavailable)?.reason.orEmpty()
}

/**
 * llama.cpp backend.
 *
 * The shared library `libtumba_llama.so` is built from `app/src/main/cpp` (see
 * `tools/build-native.ps1` / `tools/build-native.sh`) and lives in `jniLibs`, so the regular
 * Android build never needs the NDK. When the library is missing for the device ABI the
 * engine reports [LocalEngineStatus.Unavailable] with the exact reason (ABIs, whether the
 * file is inside the APK, linker message) and callers surface it — no tokens are ever
 * fabricated.
 */
@Singleton
class LlamaCppEngine @Inject constructor(
    @ApplicationContext private val context: Context,
) : LocalInferenceEngine {

    private val loadMutex = Mutex()
    private val generationMutex = Mutex()
    private var modelHandle: Long = 0L
    private var loadedPath: String? = null

    override val status: LocalEngineStatus = detect()

    val version: String? by lazy {
        runCatching { nativeVersion() }.getOrNull()
    }

    override fun generate(
        modelPath: String,
        systemPrompt: String,
        messages: List<AiMessage>,
        maxTokens: Int,
        temperature: Float,
        contextWindow: Int?,
    ): Flow<String> = channelFlow {
        if (status !is LocalEngineStatus.Ready) {
            throw AiException(AiError.LocalEngineUnavailable(unavailableReason()))
        }

        val roles = ArrayList<String>(messages.size + 1)
        val contents = ArrayList<String>(messages.size + 1)
        if (systemPrompt.isNotBlank()) {
            roles += ROLE_SYSTEM
            contents += systemPrompt.trim()
        }
        messages.forEach { message ->
            if (message.content.isBlank()) return@forEach
            roles += when (message.role) {
                MessageRole.ASSISTANT -> ROLE_ASSISTANT
                MessageRole.SYSTEM -> ROLE_SYSTEM
                MessageRole.USER -> ROLE_USER
            }
            contents += message.content.trim()
        }
        if (roles.isEmpty()) throw AiException(AiError.Unknown("Empty prompt"))

        val job = coroutineContext[Job]
        val callback = object : TokenCallback {
            override fun onToken(text: String) {
                trySend(text)
            }

            override fun isCancelled(): Boolean = job?.isActive != true || !isActive
        }

        val threads = (Runtime.getRuntime().availableProcessors() - 1).coerceIn(1, MAX_THREADS)
        // The generation mutex is held for the whole native call so unload() can never free
        // the model while llama.cpp is decoding it (see tryLock below).
        val generated = generationMutex.withLock {
            val handle = acquireModel(modelPath)
            withContext(Dispatchers.IO) {
                nativeGenerate(
                    handle = handle,
                    roles = roles.toTypedArray(),
                    contents = contents.toTypedArray(),
                    maxTokens = maxTokens,
                    temperature = temperature,
                    contextSize = contextWindow ?: DEFAULT_CONTEXT,
                    threadCount = threads,
                    callback = callback,
                )
            }
        }
        if (generated < 0) {
            throw AiException(AiError.Unknown("llama.cpp generation failed"))
        }
        close()
    }.buffer(Channel.UNLIMITED)

    override fun unload() {
        // Never free the model under a running decode: skip this trim, the next one retries.
        if (!generationMutex.tryLock()) return
        try {
            synchronized(this) {
                val handle = modelHandle
                modelHandle = 0L
                loadedPath = null
                if (handle != 0L) runCatching { nativeFreeModel(handle) }
            }
        } finally {
            generationMutex.unlock()
        }
    }

    private suspend fun acquireModel(path: String): Long = loadMutex.withLock {
        val current = modelHandle
        if (current != 0L && loadedPath == path) return@withLock current
        if (current != 0L) {
            runCatching { nativeFreeModel(current) }
            modelHandle = 0L
            loadedPath = null
        }
        val loaded = withContext(Dispatchers.IO) { nativeLoadModel(path) }
        if (loaded == 0L) {
            throw AiException(AiError.Unknown("The GGUF model could not be loaded"))
        }
        modelHandle = loaded
        loadedPath = path
        loaded
    }

    /**
     * Reads the ABI folders that actually contain the engine inside our APK. The manifest has
     * `extractNativeLibs=false`, so `nativeLibraryDir` is empty even on a healthy install and
     * cannot be used to decide whether the library is bundled.
     */
    private fun packagedAbis(libraryName: String): Set<String> = runCatching {
        val apk = File(context.applicationInfo.sourceDir)
        java.util.zip.ZipFile(apk).use { zip ->
            zip.entries().asSequence().mapNotNull { entry ->
                val entryName = entry.name
                val prefix = "lib/"
                if (entryName.startsWith(prefix) && entryName.endsWith("/$libraryName")) {
                    entryName.removePrefix(prefix).removeSuffix("/$libraryName")
                } else {
                    null
                }
            }.toSet()
        }
    }.getOrDefault(emptySet())

    private fun detect(): LocalEngineStatus {
        val deviceAbis = Build.SUPPORTED_ABIS.joinToString(", ")
        val libraryName = "lib$NATIVE_LIBRARY.so"

        return try {
            System.loadLibrary(NATIVE_LIBRARY)
            val loadedVersion = nativeVersion()
            if (loadedVersion.isBlank()) {
                LocalEngineStatus.Unavailable(
                    "$libraryName loaded but reported no version",
                )
            } else {
                LocalEngineStatus.Ready
            }
        } catch (t: Throwable) {
            val bundled = packagedAbis(libraryName)
            val reason = buildString {
                if (bundled.isEmpty()) {
                    append("$libraryName is not bundled in this APK (device ABI: $deviceAbis)")
                } else {
                    append(
                        "$libraryName is bundled for ${bundled.joinToString(", ")} " +
                            "but could not be loaded on device ABI $deviceAbis",
                    )
                }
                t.message?.takeIf { it.isNotBlank() }?.let { append(". ").append(it) }
            }
            LocalEngineStatus.Unavailable(reason)
        }
    }

    // --- native -------------------------------------------------------------------------

    private external fun nativeVersion(): String

    private external fun nativeLoadModel(path: String): Long

    private external fun nativeFreeModel(handle: Long)

    private external fun nativeGenerate(
        handle: Long,
        roles: Array<String>,
        contents: Array<String>,
        maxTokens: Int,
        temperature: Float,
        contextSize: Int,
        threadCount: Int,
        callback: TokenCallback,
    ): Int

    private companion object {
        const val NATIVE_LIBRARY = "tumba_llama"
        const val ROLE_SYSTEM = "system"
        const val ROLE_USER = "user"
        const val ROLE_ASSISTANT = "assistant"
        const val DEFAULT_CONTEXT = 4_096
        const val MAX_THREADS = 6
    }
}