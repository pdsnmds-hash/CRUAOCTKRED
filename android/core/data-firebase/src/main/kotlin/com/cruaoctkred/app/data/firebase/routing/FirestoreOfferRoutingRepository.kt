package com.cruaoctkred.app.data.firebase.routing

import com.cruaoctkred.app.domain.model.OfferRoutingSetting
import com.cruaoctkred.app.domain.repository.OfferRoutingRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MetadataChanges
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class FirestoreOfferRoutingRepository(
    private val firestore: FirebaseFirestore?,
) : OfferRoutingRepository {
    override fun observe(): Flow<OfferRoutingSetting> = callbackFlow {
        if (firestore == null) {
            trySend(OfferRoutingSetting.Unavailable)
            close()
            return@callbackFlow
        }
        val registration = firestore.document(DOCUMENT).addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
            val setting = com.cruaoctkred.app.domain.model.routingSettingFromSnapshot(
                hasSnapshot = error == null && snapshot != null,
                exists = snapshot?.exists() == true,
                fromCache = snapshot?.metadata?.isFromCache != false,
                pendingWrites = snapshot?.metadata?.hasPendingWrites() != false,
                value = snapshot?.get(WEB_VIEW_FOR_ADS_ENABLED),
            )
            trySend(setting)
        }
        awaitClose { registration.remove() }
    }

    private companion object {
        const val DOCUMENT = "settings/offerRouting"
        const val WEB_VIEW_FOR_ADS_ENABLED = "webViewForAdsEnabled"
    }
}
