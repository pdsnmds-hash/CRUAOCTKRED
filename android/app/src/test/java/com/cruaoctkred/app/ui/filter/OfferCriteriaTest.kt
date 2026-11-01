package com.cruaoctkred.app.ui.filter

import com.cruaoctkred.app.domain.model.LoanCard
import com.cruaoctkred.app.domain.model.ValueRange
import org.junit.Assert.*
import org.junit.Test

class OfferCriteriaTest {
    private val card = LoanCard(
        id = "test", title = "Lender", rating = 4.8,
        firstCredit = "10 000", repeatCredit = "", rate = "0.1%",
        age = "", term = "", decisionTime = "", website = "", phone = "",
        email = "", address = "", legalEntity = "", license = "", apr = "",
        aprCalculation = "", logoUrl = null, linkUrl = null, order = 0,
        firstCreditRange = ValueRange(500, 10_000), termRange = ValueRange(7, 30),
    )

    @Test fun `empty conditions preserve offers with incomplete data`() {
        val unknown = card.copy(rate = "unknown", firstCreditRange = ValueRange(null, null),
            termRange = ValueRange(null, null))
        assertTrue(OfferCriteria().matches(unknown))
        assertEquals(0, OfferCriteria().activeCount)
    }

    @Test fun `requested amount and term must both be supported inclusively`() {
        assertTrue(OfferCriteria(500, 7).matches(card))
        assertTrue(OfferCriteria(10_000, 30).matches(card))
        assertFalse(OfferCriteria(499, 7).matches(card))
        assertFalse(OfferCriteria(10_001, 30).matches(card))
        assertFalse(OfferCriteria(500, 31).matches(card))
    }

    @Test fun `one sided ranges use known bounds and unknown or reversed ranges cannot match`() {
        assertTrue(OfferCriteria(amount = 10_000).matches(card.copy(firstCreditRange = ValueRange(null, 10_000))))
        assertTrue(OfferCriteria(amount = 50_000).matches(card.copy(firstCreditRange = ValueRange(500, null))))
        assertFalse(OfferCriteria(amount = 1_000).matches(card.copy(firstCreditRange = ValueRange(null, null))))
        assertFalse(OfferCriteria(amount = 1_000).matches(card.copy(firstCreditRange = ValueRange(2_000, 500))))
        assertFalse(OfferCriteria(days = 10).matches(card.copy(termRange = ValueRange(null, null))))
    }

    @Test fun `maximum rate includes boundary and supports decimal comma`() {
        assertTrue(OfferCriteria(maxRate = 0.1).matches(card))
        assertTrue(OfferCriteria(maxRate = 0.1).matches(card.copy(rate = " 0,10 % ")))
        assertFalse(OfferCriteria(maxRate = 0.09).matches(card))
        assertTrue(OfferCriteria(maxRate = 0.0).matches(card.copy(rate = "0%")))
        for (rate in listOf("from 0%", "0–1%", "unknown", "NaN%", "-1%", "101%")) {
            assertFalse(rate, OfferCriteria(maxRate = 100.0).matches(card.copy(rate = rate)))
        }
    }

    @Test fun `combined conditions intersect rather than substitute each other`() {
        val criteria = OfferCriteria(1_000, 14, 0.1)
        assertEquals(3, criteria.activeCount)
        assertTrue(criteria.matches(card))
        assertFalse(criteria.matches(card.copy(rate = "0.2%")))
        assertFalse(criteria.matches(card.copy(termRange = ValueRange(30, 60))))
    }

    @Test fun `slider limits cover catalog conditions and preserve a useful baseline`() {
        val limits = OfferFilterLimits.fromCards(listOf(card.copy(
            firstCreditRange = ValueRange(500, 32_000), termRange = ValueRange(1, 1_000), rate = "2,5%")))
        assertEquals(500L, limits.amountMin)
        assertEquals(32_000L, limits.amountMax)
        assertEquals(1_000L, limits.daysMax)
        assertEquals(3.0, limits.rateMax, 0.0)
    }

    @Test fun `slider limits tolerate empty catalogs unknown bounds and invalid rates`() {
        val defaults = OfferFilterLimits.fromCards(emptyList())
        assertEquals(20_000L, defaults.amountMax)
        assertEquals(365L, defaults.daysMax)
        assertEquals(1.0, defaults.rateMax, 0.0)
        val limits = OfferFilterLimits.fromCards(listOf(card.copy(
            firstCreditRange = ValueRange(60_000, null), termRange = ValueRange(600, null), rate = "NaN%")))
        assertEquals(60_000L, limits.amountMax)
        assertEquals(600L, limits.daysMax)
        assertEquals(1.0, limits.rateMax, 0.0)
    }

    @Test fun `hot and new categories use independent administrator flags and preserve order`() {
        val cards = listOf(card.copy(id = "both", order = 2, isHot = true, isNew = true),
            card.copy(id = "hot", order = 0, isHot = true), card.copy(id = "new", order = 1, isNew = true),
            card.copy(id = "ordinary", order = 3))
        assertEquals(listOf("hot", "both"), sortOfferCategory(cards, "hot").map { it.id })
        assertEquals(listOf("new", "both"), sortOfferCategory(cards, "new").map { it.id })
        assertEquals(4, sortOfferCategory(cards, "best_rate").size)
    }

    @Test fun `best rate compares numeric percentages and keeps unknown rates last`() {
        val cards = listOf(card.copy(id = "unknown", rate = "unknown"),
            card.copy(id = "high", rate = "10%"), card.copy(id = "low", rate = "0,1%"),
            card.copy(id = "middle", rate = "2%"))
        assertEquals(listOf("low", "middle", "high", "unknown"), sortOfferCategory(cards, "best_rate").map { it.id })
    }
}
