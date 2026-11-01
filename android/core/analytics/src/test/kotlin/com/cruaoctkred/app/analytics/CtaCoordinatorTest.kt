package com.cruaoctkred.app.analytics

import com.cruaoctkred.app.domain.model.*
import com.cruaoctkred.app.domain.repository.*
import com.cruaoctkred.app.domain.usecase.OfferLinkResolver
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.junit.Assert.*
import org.junit.Test

class CtaCoordinatorTest {
    private class Attribution : AttributionRepository {
        override val installStatus = MutableStateFlow(InstallAttributionStatus.UNKNOWN)
        override fun saveInstallStatus(status: InstallAttributionStatus) { installStatus.value = retainFirstInstallStatus(installStatus.value, status) }
        override fun read() = AttributionSnapshot()
        override fun save(snapshot: AttributionSnapshot) = Unit
        var wait: CompletableDeferred<Unit>? = null
        override suspend fun trackingTemplate(): String { wait?.await(); return "source={media_source}" }
    }
    private class Network : NetworkStatusRepository {
        override val online = MutableStateFlow(true)
        override fun isOnline() = online.value
    }
    private val card = LoanCard("a", "Offer", 4.5, "", "", "1%", "", "", "", "", "", "", "", "", "", "", "", null, "https://offer.test/start", 0)

    @Test fun liveChangesAndAllPlacementsUseOneDecisionWithoutRelaunch() = runBlocking {
        val status = MutableStateFlow<OfferRoutingSetting>(OfferRoutingSetting.Unavailable)
        val attribution = Attribution()
        val network = Network()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        var clicks = 0
        val coordinator = CtaCoordinator(attribution, OfferLinkResolver(), { _, _, _ -> clicks++ }, network,
            object : OfferRoutingRepository { override fun observe() = status }, scope)
        try {
            for (placement in listOf("card", "home", "saved", "compare", "detail", "website")) {
                assertEquals(CtaResult.NativeDetails, coordinator.open(card, placement))
                status.value = OfferRoutingSetting.Available(false)
                assertTrue(coordinator.open(card, placement) is CtaResult.Open)
                status.value = OfferRoutingSetting.Available(true)
                assertEquals(CtaResult.NativeDetails, coordinator.open(card, placement))
                attribution.saveInstallStatus(InstallAttributionStatus.NON_ORGANIC)
                assertTrue(coordinator.open(card, placement) is CtaResult.Open)
                network.online.value = false
                assertEquals(CtaResult.NativeDetails, coordinator.open(card, placement))
                network.online.value = true
                assertEquals(CtaResult.NativeDetails, coordinator.open(card.copy(linkUrl = null), placement))
                assertEquals(CtaResult.NativeDetails, coordinator.open(card.copy(linkUrl = "http://offer.test"), placement))
                status.value = OfferRoutingSetting.Unavailable
                attribution.installStatus.value = InstallAttributionStatus.UNKNOWN
            }
            assertEquals(12, clicks)
        } finally { scope.cancel() }
    }
    @Test fun revocationWhileTrackingLoadsCannotOpenWebView() = runBlocking {
        val status = MutableStateFlow<OfferRoutingSetting>(OfferRoutingSetting.Available(false))
        val attr = Attribution().apply { wait = CompletableDeferred() }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val coordinator = CtaCoordinator(attr, OfferLinkResolver(), { _, _, _ -> fail("Must not track or open") }, Network(),
            object : OfferRoutingRepository { override fun observe() = status }, scope)
        try {
            val pending = async(start = CoroutineStart.UNDISPATCHED) { coordinator.open(card, "home") }
            status.value = OfferRoutingSetting.Unavailable
            attr.wait!!.complete(Unit)
            assertEquals(CtaResult.NativeDetails, pending.await())
        } finally { scope.cancel() }
    }
}
