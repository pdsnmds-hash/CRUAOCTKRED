package com.cruaoctkred.app.feature.compare.fintech_1

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import com.cruaoctkred.app.domain.model.ValueRange

class ComparisonCalculationTest {
    @Test fun referenceScenario() {
        assertEquals(ComparisonCalculation(10_000, 10, 0, 10_000), calculateComparison(10_000, 10, "0%"))
        assertEquals(ComparisonCalculation(10_000, 10, 500, 10_500), calculateComparison(10_000, 10, "0,5%"))
        assertEquals(ComparisonCalculation(10_000, 10, 800, 10_800), calculateComparison(10_000, 10, "0.8%"))
    }

    @Test fun roundsInterestToWholeMoneyUnits() {
        assertEquals(1L, calculateComparison(100, 1, "0.5%")?.overpayment)
        assertEquals(0L, calculateComparison(100, 1, "0.49%")?.overpayment)
    }

    @Test fun rejectsAmbiguousRatesAndInvalidInputs() {
        listOf("", "до 0.5%", "0–1%", "-1%", "101%", "0.5% / месяц").forEach {
            assertNull(calculateComparison(10_000, 10, it))
        }
        assertNull(calculateComparison(null, 10, "0%"))
        assertNull(calculateComparison(10_000, null, "0%"))
        assertNull(calculateComparison(10_000, 0, "0%"))
        assertNull(calculateComparison(-1, 10, "0%"))
        assertNull(calculateComparison(Long.MAX_VALUE, 10, "100%"))
    }

    @Test fun sharedScenarioFitsBothOffers() {
        assertEquals(10_000L, commonValue(ValueRange(null, 18_000), ValueRange(null, 32_000), 10_000))
        assertEquals(65L, commonValue(ValueRange(65, 365), ValueRange(30, 365), 10))
        assertEquals(5_000L, commonValue(ValueRange(1_000, 5_000), ValueRange(3_000, 8_000), 10_000))
        assertNull(commonValue(ValueRange(1, 10), ValueRange(20, 30), 10))
        assertNull(commonValue(ValueRange(30, 10), ValueRange(null, null), 10))
    }
    @Test fun pickerOptionsStayWithinBothOffersLimits() {
        val range = commonRange(ValueRange(3_000, 18_000), ValueRange(5_000, 32_000))!!
        assertEquals(listOf(5_000L, 10_000L, 15_000L, 18_000L), scenarioChoices(range, 10_000, false))
        assertEquals(listOf(65L, 90L, 120L, 180L, 365L), scenarioChoices(65L..365L, 65, true))
    }

}
