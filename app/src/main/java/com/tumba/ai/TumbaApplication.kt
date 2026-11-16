package com.tumba.ai

import android.app.Application
import android.content.ComponentCallbacks2
import android.content.res.Configuration
import com.tumba.ai.data.ai.local.LocalInferenceEngine
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class TumbaApplication : Application(), ComponentCallbacks2 {

    @Inject
    lateinit var localEngine: LocalInferenceEngine

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= TRIM_MEMORY_RUNNING_LOW) {
            // A loaded GGUF model holds hundreds of megabytes of mapped weights. Drop it
            // before the system starts killing the process; the next message reloads it.
            localEngine.unload()
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        localEngine.unload()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
    }

    override fun onTerminate() {
        localEngine.unload()
        super.onTerminate()
    }
}