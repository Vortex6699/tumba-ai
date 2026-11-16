package com.tumba.ai.core

import android.content.Context
import android.text.format.Formatter
import java.text.DateFormat
import java.util.Date

enum class LocalModelFailure {
    INVALID_FILE,
    NO_SPACE,
    IO_ERROR,
}

class LocalModelException(
    val failure: LocalModelFailure,
    cause: Throwable? = null,
) : Exception(failure.name, cause)

fun formatBytes(context: Context, bytes: Long): String =
    Formatter.formatShortFileSize(context, bytes)

fun formatDate(millis: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(millis))

fun formatTime(millis: Long): String =
    DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(millis))

fun String.oneLineSentence(maxLength: Int = 64): String {
    val normalized = trim().replace(Regex("\\s+"), " ")
    return if (normalized.length <= maxLength) normalized else normalized.take(maxLength).trimEnd() + "…"
}

fun String.toConversationTitle(): String = oneLineSentence(56).ifBlank { "New conversation" }
