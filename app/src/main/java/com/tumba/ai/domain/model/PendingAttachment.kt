package com.tumba.ai.domain.model

data class PendingAttachment(
    val path: String,
    val mimeType: String,
    val name: String?,
    val sizeBytes: Long,
)
