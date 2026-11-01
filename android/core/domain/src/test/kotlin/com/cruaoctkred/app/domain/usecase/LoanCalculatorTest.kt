package com.cruaoctkred.app.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Test

class LoanCalculatorTest {
    private val calculator = LoanCalculator()

    @Test fun calculatesTemplateFormula() {
        val result = calculator.calculate(amount = 2_000_000, dailyRatePercent = 0.05, days = 30)
        assertEquals(30_000, result.overpayment)
        assertEquals(2_030_000, result.total)
        assertEquals(67_667, result.perDay)
    }

    @Test fun zeroDaysKeepsTotalAsPerDay() {
        val result = calculator.calculate(1_000_000, 0.05, 0)
        assertEquals(1_000_000, result.perDay)
    }
}

