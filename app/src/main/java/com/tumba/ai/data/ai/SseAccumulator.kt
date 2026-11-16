package com.tumba.ai.data.ai

/**
 * Minimal, allocation-friendly Server-Sent Events accumulator.
 *
 * Kept free of Android/OkHttp types so it can be unit tested on the JVM.
 */
class SseAccumulator {
    private val data = StringBuilder()

    /** Feeds one line; returns a complete event payload when a blank line terminates it. */
    fun consume(line: String): String? {
        when {
            line.isEmpty() -> return take()
            line.startsWith(":") -> Unit
            line.startsWith("data:") -> {
                if (data.isNotEmpty()) data.append('\n')
                data.append(line.substring(5).trimStart())
            }
        }
        return null
    }

    /** Flushes a trailing event that was not followed by a blank line. */
    fun finish(): String? = take()

    private fun take(): String? {
        if (data.isEmpty()) return null
        val payload = data.toString()
        data.clear()
        return payload
    }
}
