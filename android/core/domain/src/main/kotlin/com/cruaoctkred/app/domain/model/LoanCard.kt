package com.cruaoctkred.app.domain.model

/**
 * Numeric bounds behind a display string. [to] is optional: without it the detail screen
 * shows only the lower bound. Both null means the admin has not filled the range yet and
 * the matching text field is the only source.
 */
data class ValueRange(val from: Long?, val to: Long?) {
    val isEmpty: Boolean get() = from == null && to == null
}

data class LoanCard(
    val id: String,
    val title: String,
    val rating: Double,
    val firstCredit: String,
    val repeatCredit: String,
    val rate: String,
    val age: String,
    val term: String,
    val decisionTime: String,
    val website: String,
    val phone: String,
    val email: String,
    val address: String,
    val legalEntity: String,
    val license: String,
    val apr: String,
    val aprCalculation: String,
    val logoUrl: String?,
    val linkUrl: String?,
    val order: Int,
    val firstCreditRange: ValueRange = ValueRange(null, null),
    val repeatCreditRange: ValueRange = ValueRange(null, null),
    val termRange: ValueRange = ValueRange(null, null),
    val ageRange: ValueRange = ValueRange(null, null),
    val isHot: Boolean = false,
    val isNew: Boolean = false,
)

data class CardsContent(
    val cards: List<LoanCard>,
    val isOffline: Boolean = false,
)

data class RulesPage(
    val title: String,
    val content: String,
    val isOffline: Boolean = false,
)

data class AttributionSnapshot(
    val campaign: String = "",
    val adset: String = "",
    val network: String = "",
    val mediaSource: String = "",
    val gclid: String = "",
    val campaignId: String = "",
    val creativeId: String = "",
    val appsFlyerId: String = "",
) {
    fun valueFor(key: String): String = when (key) {
        "campaign" -> campaign
        "af_adset" -> adset
        "network" -> network
        "media_source" -> mediaSource
        "gclid" -> gclid
        "campaignid" -> campaignId
        "af_ad_id" -> creativeId
        "appsflyer_id" -> appsFlyerId
        else -> ""
    }.ifBlank { "0" }
}

sealed interface StreamResult<out T> {
    data object Loading : StreamResult<Nothing>
    data class Data<T>(val value: T) : StreamResult<T>
    data class Error<T>(val cause: Throwable, val cached: T? = null) : StreamResult<T>
}

