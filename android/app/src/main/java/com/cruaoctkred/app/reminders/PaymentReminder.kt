package com.cruaoctkred.app.reminders

import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

enum class ReminderTermUnit { DAYS, MONTHS, PAYMENTS }

data class PaymentReminder(val id: String, val amountMinor: Long, val date: String, val termCount: Int, val delivered: Boolean = false, val termUnit: ReminderTermUnit = ReminderTermUnit.DAYS) {
    fun dueCalendar(zone: TimeZone = TimeZone.getDefault()): Calendar? = runCatching {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply { isLenient = false; timeZone = zone }
        require(date.matches(Regex("\\d{4}-\\d{2}-\\d{2}")))
        Calendar.getInstance(zone).apply { time = requireNotNull(format.parse(date)) }
    }.getOrNull()

    fun reminderTime(zone: TimeZone = TimeZone.getDefault()): Long? = dueCalendar(zone)?.apply {
        add(Calendar.DAY_OF_MONTH, -2)
        set(Calendar.HOUR_OF_DAY, 9)
    }?.timeInMillis

    fun expired(now: Long, zone: TimeZone = TimeZone.getDefault()): Boolean = dueCalendar(zone)?.apply { add(Calendar.DAY_OF_MONTH, 1) }?.timeInMillis?.let { now >= it } ?: true
    fun valid(now: Long): Boolean = amountMinor in 1..100_000_000_000L && termCount in 1..3650 && !expired(now)
}

fun parsePaymentAmount(raw: String): Long? = runCatching {
    val value = raw.trim().replace(',', '.')
    require(value.matches(Regex("\\d{1,10}(?:\\.\\d{1,2})?")))
    BigDecimal(value).movePointRight(2).longValueExact().takeIf { it in 1..100_000_000_000L }
}.getOrNull()
