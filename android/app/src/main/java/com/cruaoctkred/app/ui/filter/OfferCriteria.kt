package com.cruaoctkred.app.ui.filter

import com.cruaoctkred.app.config.ProductConfig
import kotlin.math.ceil
import com.cruaoctkred.app.domain.model.LoanCard
import com.cruaoctkred.app.domain.model.ValueRange

/** Applied conditions; empty fields impose no restriction. */
data class OfferCriteria(
    val amount: Long? = null,
    val days: Long? = null,
    val maxRate: Double? = null,
) {
    val activeCount: Int get() = listOf(amount, days, maxRate).count { it != null }

    fun matches(card: LoanCard): Boolean {
        if (amount != null && !card.firstCreditRange.containsKnown(amount)) return false
        if (days != null && !card.termRange.containsKnown(days)) return false
        if (maxRate != null) {
            if (!maxRate.isFinite() || maxRate !in 0.0..100.0) return false
            val rate = offerRatePercent(card.rate) ?: return false
            if (rate > maxRate) return false
        }
        return true
    }
}

private val OFFER_RATE = Regex("""\s*(\d+(?:[.,]\d+)?)\s*%\s*""")
private val DECIMAL = Regex("""\d+(?:[.,]\d+)?""")

private fun parsePercent(text: String): Double? =
    text.trim().takeIf { DECIMAL.matches(it) }?.replace(',', '.')?.toDoubleOrNull()
        ?.takeIf { it.isFinite() && it in 0.0..100.0 }

// Missing bounds impose no restriction, but a wholly unknown or reversed range
// cannot establish that an offer supports the user's requested amount or term.
private fun ValueRange.containsKnown(value: Long): Boolean {
    val minimum = from
    val maximum = to
    return value > 0 && !isEmpty &&
        !(minimum != null && maximum != null && minimum > maximum) &&
        (minimum == null || value >= minimum) && (maximum == null || value <= maximum)
}

/** Slider limits cover the full catalog, independent of search and active filters. */
data class OfferFilterLimits(
    val amountMin: Long,
    val amountMax: Long,
    val daysMax: Long,
    val rateMax: Double,
) {
    companion object {
        fun fromCards(cards: List<LoanCard>): OfferFilterLimits {
            val amount = cards.flatMap { listOfNotNull(it.firstCreditRange.from, it.firstCreditRange.to) }
                .maxOrNull() ?: 0L
            val days = cards.flatMap { listOfNotNull(it.termRange.from, it.termRange.to) }.maxOrNull() ?: 0L
            val rate = cards.mapNotNull { offerRatePercent(it.rate) }.maxOrNull() ?: 0.0
            return OfferFilterLimits(
                amountMin = ProductConfig.MIN_AMOUNT,
                amountMax = maxOf(ProductConfig.MAX_AMOUNT, amount),
                daysMax = maxOf(ProductConfig.MAX_DAYS.toLong(), days),
                rateMax = ceil(maxOf(1.0, rate)).coerceAtMost(100.0),
            )
        }
    }
}

internal fun offerRatePercent(text: String): Double? =
    OFFER_RATE.matchEntire(text)?.groupValues?.get(1)?.let(::parsePercent)

/** Category flags are product-managed; rate ordering uses the numeric percentage. */
internal fun sortOfferCategory(cards: List<LoanCard>, category: String): List<LoanCard> = when (category) {
    "hot" -> cards.filter { it.isHot }.sortedBy { it.order }
    "new" -> cards.filter { it.isNew }.sortedBy { it.order }
    "best_rate" -> cards.sortedWith(compareBy<LoanCard> { offerRatePercent(it.rate) ?: Double.POSITIVE_INFINITY }.thenBy { it.order })
    else -> cards.sortedBy { it.order }
}
