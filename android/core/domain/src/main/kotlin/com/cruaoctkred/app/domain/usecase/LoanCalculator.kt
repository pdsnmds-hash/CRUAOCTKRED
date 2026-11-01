package com.cruaoctkred.app.domain.usecase

import kotlin.math.roundToLong

data class LoanEstimate(
    val overpayment: Long,
    val total: Long,
    val perDay: Long,
)

class LoanCalculator {
    fun calculate(amount: Long, dailyRatePercent: Double, days: Int): LoanEstimate {
        val safeAmount = amount.coerceAtLeast(0)
        val safeRate = dailyRatePercent.coerceAtLeast(0.0)
        val safeDays = days.coerceAtLeast(0)
        val overpayment = (safeAmount * safeRate / 100.0 * safeDays).roundToLong()
        val total = safeAmount + overpayment
        return LoanEstimate(
            overpayment = overpayment,
            total = total,
            perDay = if (safeDays > 0) (total.toDouble() / safeDays).roundToLong() else total,
        )
    }
}

