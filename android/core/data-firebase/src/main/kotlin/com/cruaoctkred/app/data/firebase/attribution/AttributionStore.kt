package com.cruaoctkred.app.data.firebase.attribution

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import com.cruaoctkred.app.domain.model.AttributionSnapshot
import com.cruaoctkred.app.domain.repository.AttributionRepository

class AttributionStore(
    context: Context,
    private val firestore: FirebaseFirestore?,
) : AttributionRepository {
    private val preferences = context.getSharedPreferences("attribution_v1", Context.MODE_PRIVATE)

    private val status = kotlinx.coroutines.flow.MutableStateFlow(
        com.cruaoctkred.app.domain.model.InstallAttributionStatus.entries.firstOrNull { it.name == preferences.getString("install_status", null) }
            ?: com.cruaoctkred.app.domain.model.InstallAttributionStatus.UNKNOWN,
    )
    override val installStatus: kotlinx.coroutines.flow.StateFlow<com.cruaoctkred.app.domain.model.InstallAttributionStatus> = status

    @Synchronized
    override fun saveInstallStatus(value: com.cruaoctkred.app.domain.model.InstallAttributionStatus) {
        val next = com.cruaoctkred.app.domain.model.retainFirstInstallStatus(status.value, value)
        if (next == status.value) return
        if (preferences.edit().putString("install_status", next.name).commit()) status.value = next
    }

    override fun read() = AttributionSnapshot(
        campaign = preferences.getString("campaign", "").orEmpty(),
        adset = preferences.getString("adset", "").orEmpty(),
        network = preferences.getString("network", "").orEmpty(),
        mediaSource = preferences.getString("media_source", "").orEmpty(),
        gclid = preferences.getString("gclid", "").orEmpty(),
        campaignId = preferences.getString("campaign_id", "").orEmpty(),
        creativeId = preferences.getString("creative_id", "").orEmpty(),
        appsFlyerId = preferences.getString("appsflyer_id", "").orEmpty(),
    )

    override fun save(snapshot: AttributionSnapshot) {
        preferences.edit()
            .putString("campaign", snapshot.campaign)
            .putString("adset", snapshot.adset)
            .putString("network", snapshot.network)
            .putString("media_source", snapshot.mediaSource)
            .putString("gclid", snapshot.gclid)
            .putString("campaign_id", snapshot.campaignId)
            .putString("creative_id", snapshot.creativeId)
            .putString("appsflyer_id", snapshot.appsFlyerId)
            .apply()
    }

    override suspend fun trackingTemplate(): String = com.cruaoctkred.app.domain.usecase.OfferTrackingTemplate.safe(runCatching {
        firestore?.collection("settings")?.document("tracking")?.get()?.await()?.getString("template")
            ?.takeIf(String::isNotBlank)
    }.getOrNull())
}
