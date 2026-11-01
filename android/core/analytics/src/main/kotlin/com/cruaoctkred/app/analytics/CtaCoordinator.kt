package com.cruaoctkred.app.analytics

import com.cruaoctkred.app.domain.model.*
import com.cruaoctkred.app.domain.repository.*
import com.cruaoctkred.app.domain.usecase.OfferLinkResolver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*

sealed interface CtaResult {
    data class Open(val url: String) : CtaResult
    data object NativeDetails : CtaResult
}

class CtaCoordinator(
    private val attributionRepository: AttributionRepository,
    private val resolver: OfferLinkResolver,
    private val onWebOpen: suspend (LoanCard, String, AttributionSnapshot) -> Unit,
    private val network: NetworkStatusRepository,
    routingRepository: OfferRoutingRepository,
    scope: CoroutineScope,
) {
    val routing: StateFlow<OfferRoutingContext> = combine(
        routingRepository.observe(), attributionRepository.installStatus, network.online,
    ) { setting, attribution, online -> OfferRoutingContext(setting, attribution, online) }
        .stateIn(scope, SharingStarted.Eagerly, OfferRoutingContext())

    fun allows(card: LoanCard): Boolean = routing.value.copy(online = network.isOnline()).allows(card)

    suspend fun open(card: LoanCard, placement: String): CtaResult {
        if (!allows(card)) return CtaResult.NativeDetails
        val attribution = attributionRepository.read()
        val template = attributionRepository.trackingTemplate()
        val enriched = resolver.enrich(card.linkUrl, template, attribution) ?: return CtaResult.NativeDetails
        // Configuration/connectivity can change while tracking settings are loading.
        if (!allows(card)) return CtaResult.NativeDetails
        onWebOpen(card, placement, attribution)
        return if (allows(card)) CtaResult.Open(enriched) else CtaResult.NativeDetails
    }
}
