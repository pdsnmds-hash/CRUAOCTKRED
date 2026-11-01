package com.cruaoctkred.app.feature.compare.fintech_1

import java.math.BigDecimal
import java.math.RoundingMode
import com.cruaoctkred.app.domain.model.ValueRange

internal data class ComparisonCalculation(val amount: Long, val days: Long, val overpayment: Long, val total: Long)

/** A single, shared scenario constrained by both offers' structured ranges. */
internal fun commonValue(a: ValueRange, b: ValueRange, suggested: Long): Long? {
    return commonRange(a, b)?.let { suggested.coerceIn(it) }
}

internal fun commonRange(a: ValueRange, b: ValueRange): LongRange? {
    val minimum = maxOf(1L, a.from ?: 1L, b.from ?: 1L)
    val maximum = minOf(a.to ?: Long.MAX_VALUE, b.to ?: Long.MAX_VALUE)
    return if (minimum > maximum) null else minimum..maximum
}

internal fun scenarioChoices(range: LongRange, current: Long, days: Boolean): List<Long> {
    val presets = if (days) listOf(10L, 15L, 20L, 30L, 45L, 60L, 90L, 120L, 180L, 365L)
        else listOf(1_000L, 3_000L, 5_000L, 10_000L, 15_000L, 20_000L, 30_000L, 50_000L, 100_000L)
    val boundaries = if (range.last == Long.MAX_VALUE) listOf(range.first) else listOf(range.first, range.last)
    return (presets + boundaries + current).filter { it in range }.distinct().sorted()
}

/** Daily-rate interpretation explicitly approved for this variant. No parsing of ranges or prose. */
internal fun calculateComparison(amount: Long?, days: Long?, rate: String): ComparisonCalculation? {
    if (amount == null || days == null || amount <= 0 || days <= 0) return null
    val match = Regex("""^\s*(\d+(?:[.,]\d+)?)\s*%\s*$""").matchEntire(rate) ?: return null
    val percent = match.groupValues[1].replace(',', '.').toBigDecimalOrNull() ?: return null
    if (percent > BigDecimal(100)) return null
    return try {
        val interest = BigDecimal.valueOf(amount).multiply(percent).multiply(BigDecimal.valueOf(days))
            .divide(BigDecimal(100)).setScale(0, RoundingMode.HALF_UP).longValueExact()
        ComparisonCalculation(amount, days, interest, Math.addExact(amount, interest))
    } catch (_: ArithmeticException) {
        null
    }
}
