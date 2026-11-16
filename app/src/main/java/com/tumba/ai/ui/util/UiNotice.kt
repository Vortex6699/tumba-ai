package com.tumba.ai.ui.util

import androidx.annotation.StringRes

data class UiNotice(
    @StringRes val messageRes: Int,
    val argument: String? = null,
)
