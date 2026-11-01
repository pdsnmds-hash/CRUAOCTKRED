package com.cruaoctkred.app.data.firebase

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore

object FirestoreProvider {
    fun provide(
        context: Context,
        hasFirebaseConfig: Boolean,
        useEmulator: Boolean,
        emulatorHost: String = "10.0.2.2",
        emulatorPort: Int = 8080,
    ): FirebaseFirestore? {
        val appContext = context.applicationContext
        if (!(hasFirebaseConfig || useEmulator) || FirebaseApp.getApps(appContext).isEmpty()) return null
        return FirebaseFirestore.getInstance().also { database ->
            if (useEmulator) database.useEmulator(emulatorHost, emulatorPort)
        }
    }
}
