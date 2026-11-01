package com.cruaoctkred.app.domain.model

enum class InstallAttributionStatus {
    UNKNOWN,
    ORGANIC,
    NON_ORGANIC;

    companion object {
        fun fromAppsFlyer(value: String?): InstallAttributionStatus = when {
            value.equals("Non-organic", ignoreCase = true) -> NON_ORGANIC
            value.equals("Organic", ignoreCase = true) -> ORGANIC
            else -> UNKNOWN
        }
    }
}

sealed interface OfferRoutingSetting {
    data object Unavailable : OfferRoutingSetting
    data class Available(val webViewForAdsEnabled: Boolean) : OfferRoutingSetting
}

sealed interface OfferRoute {
    data object NativeDetails : OfferRoute
    data class WebView(val url: String) : OfferRoute
}

object OfferRoutingPolicy {
    fun allowsWebView(
        setting: OfferRoutingSetting,
        attribution: InstallAttributionStatus,
        hasValidHttpsLink: Boolean,
        isOnline: Boolean,
    ): Boolean {
        if (!hasValidHttpsLink || !isOnline) return false
        val available = setting as? OfferRoutingSetting.Available ?: return false
        return !available.webViewForAdsEnabled || attribution == InstallAttributionStatus.NON_ORGANIC
    }
}

/** Immutable inputs shared by rendering and every offer entry point. */
data class OfferRoutingContext(
    val setting: OfferRoutingSetting = OfferRoutingSetting.Unavailable,
    val attribution: InstallAttributionStatus = InstallAttributionStatus.UNKNOWN,
    val online: Boolean = false,
) {
    fun allows(card: LoanCard): Boolean = OfferRoutingPolicy.allowsWebView(
        setting, attribution,
        com.cruaoctkred.app.domain.usecase.OfferLinkResolver().resolveCardCta(card.linkUrl) != null,
        online,
    )
}

fun routingSettingFromSnapshot(hasSnapshot: Boolean, exists: Boolean, fromCache: Boolean, pendingWrites: Boolean, value: Any?): OfferRoutingSetting = when {
    !hasSnapshot || fromCache || pendingWrites -> OfferRoutingSetting.Unavailable
    !exists -> OfferRoutingSetting.Available(false)
    value !is Boolean -> OfferRoutingSetting.Unavailable
    else -> OfferRoutingSetting.Available(value)
}

fun retainFirstInstallStatus(current: InstallAttributionStatus, incoming: InstallAttributionStatus): InstallAttributionStatus =
    if (current == InstallAttributionStatus.UNKNOWN) incoming else current
