package com.cruaoctkred.app.data.firebase.pages

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import com.cruaoctkred.app.domain.model.RulesPage
import com.cruaoctkred.app.domain.model.StreamResult
import com.cruaoctkred.app.domain.repository.PagesRepository

class FirestorePagesRepository(private val firestore: FirebaseFirestore?) : PagesRepository {
    override fun observeRules(): Flow<StreamResult<RulesPage>> {
        val db = firestore ?: return flowOf(StreamResult.Error(IllegalStateException("firebase-config-missing")))
        return callbackFlow {
            trySend(StreamResult.Loading)
            var latest: RulesPage? = null
            val registration = db.collection("pages").document("rules").addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(StreamResult.Error(error, latest?.copy(isOffline = true)))
                    return@addSnapshotListener
                }
                val page = RulesPage(
                    title = snapshot?.getString("title").orEmpty(),
                    content = snapshot?.getString("content").orEmpty(),
                    isOffline = snapshot?.metadata?.isFromCache == true,
                )
                latest = page
                trySend(StreamResult.Data(page))
            }
            awaitClose { registration.remove() }
        }
    }
}
