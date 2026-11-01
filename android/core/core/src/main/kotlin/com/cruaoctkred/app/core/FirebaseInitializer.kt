package com.cruaoctkred.app.core

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

data class EmulatorFirebaseConfig(
    val projectId: String,
    val applicationId: String,
    val apiKey: String,
)

object FirebaseInitializer {
    fun initialize(
        context: Context,
        hasFirebaseConfig: Boolean,
        useEmulator: Boolean,
        emulatorConfig: EmulatorFirebaseConfig? = null,
    ) {
        if (hasFirebaseConfig) {
            FirebaseApp.initializeApp(context)
        } else if (useEmulator && emulatorConfig != null) {
            FirebaseApp.initializeApp(
                context,
                FirebaseOptions.Builder()
                    .setProjectId(emulatorConfig.projectId)
                    .setApplicationId(emulatorConfig.applicationId)
                    .setApiKey(emulatorConfig.apiKey)
                    .build(),
            )
        }
    }
}
