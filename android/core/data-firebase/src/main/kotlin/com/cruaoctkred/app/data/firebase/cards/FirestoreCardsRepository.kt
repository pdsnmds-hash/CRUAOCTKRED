package com.cruaoctkred.app.data.firebase.cards

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import com.cruaoctkred.app.domain.model.CardsContent
import com.cruaoctkred.app.domain.model.LoanCard
import com.cruaoctkred.app.domain.model.StreamResult
import com.cruaoctkred.app.domain.repository.CardsRepository

class FirestoreCardsRepository(
    private val firestore: FirebaseFirestore?,
    private val mapper: CardMapper,
) : CardsRepository {
    private var activeRegistration: ListenerRegistration? = null

    override fun observePublishedCards(): Flow<StreamResult<CardsContent>> {
        val db = firestore ?: return flowOf(StreamResult.Error(IllegalStateException("firebase-config-missing")))
        return callbackFlow {
            trySend(StreamResult.Loading)
            var latest: CardsContent? = null
            val registration = db.collection("cards")
                .whereEqualTo("status", "published")
                .orderBy("order", Query.Direction.ASCENDING)
                .limit(100)
                .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                    if (error != null) {
                        trySend(StreamResult.Error(error, latest?.copy(isOffline = true)))
                        return@addSnapshotListener
                    }
                    if (snapshot == null) return@addSnapshotListener
                    val cards = snapshot.documents.mapNotNull { document ->
                        runCatching { mapper.map(document.id, document.data.orEmpty()) }.getOrNull()
                    }
                    val content = CardsContent(cards, snapshot.metadata.isFromCache)
                    latest = content
                    trySend(StreamResult.Data(content))
                }
            activeRegistration = registration
            awaitClose {
                registration.remove()
                if (activeRegistration === registration) activeRegistration = null
            }
        }
    }

    override fun observeCard(id: String): Flow<StreamResult<LoanCard>> {
        val db = firestore ?: return flowOf(StreamResult.Error(IllegalStateException("firebase-config-missing")))
        return callbackFlow {
            trySend(StreamResult.Loading)
            var latest: LoanCard? = null
            val registration = db.collection("cards").document(id).addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(StreamResult.Error(error, latest))
                    return@addSnapshotListener
                }
                val data = snapshot?.data
                if (snapshot == null || !snapshot.exists() || data?.get("status") != "published") {
                    trySend(StreamResult.Error(NoSuchElementException("card-not-found"), latest))
                } else {
                    latest = runCatching { mapper.map(snapshot.id, data) }.getOrNull()
                    latest?.let { trySend(StreamResult.Data(it)) }
                        ?: trySend(StreamResult.Error(IllegalArgumentException("card-invalid")))
                }
            }
            awaitClose { registration.remove() }
        }
    }

    override suspend fun refresh() {
        activeRegistration?.remove()
        activeRegistration = null
    }
}
