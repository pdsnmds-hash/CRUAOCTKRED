package com.cruaoctkred.app.data.firebase.push

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import com.cruaoctkred.app.domain.repository.PushTokenRepository

class FirebasePushTokenRepository(
    private val appVersion: String,
    private val firestore: FirebaseFirestore?,
) : PushTokenRepository {
    override suspend fun registerCurrentToken(): Result<Unit> = runCatching {
        if (firestore == null) return@runCatching
        register(FirebaseMessaging.getInstance().token.await()).getOrThrow()
    }

    override suspend fun register(token: String): Result<Unit> = runCatching {
        val db = firestore ?: return@runCatching
        val payload = mapOf(
            "platform" to "android",
            "appVersion" to appVersion,
            "createdAt" to FieldValue.serverTimestamp(),
        )
        db.collection("pushTokens").document(token).set(payload).await()
    }
}
