package com.cruaoctkred.app.reminders

import org.junit.Assert.*
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class PaymentReminderTest {
    private fun timestamp(value: String, zone: String = "UTC") = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ROOT).apply { timeZone = TimeZone.getTimeZone(zone) }.parse(value)!!.time
    @Test fun usesCalendarDaysAcrossMonthAndLeapYear() {
        val reminder = PaymentReminder("id", 123456, "2028-03-01", 30)
        assertEquals(timestamp("2028-02-28 09:00"), reminder.reminderTime(TimeZone.getTimeZone("UTC")))
    }
    @Test fun daylightSavingDoesNotChangeNineAmLocalTime() {
        val reminder = PaymentReminder("id", 100, "2026-03-09", 30)
        assertEquals(timestamp("2026-03-07 09:00", "America/New_York"), reminder.reminderTime(TimeZone.getTimeZone("America/New_York")))
    }
    @Test fun rejectsBadAmountDatesAndTerms() {
        for (raw in listOf("", "0", "-1", "1.234", "NaN", "1e3", "1000000001")) assertNull(raw, parsePaymentAmount(raw))
        assertEquals(123456L, parsePaymentAmount("1234,56"))
        assertEquals(1L, parsePaymentAmount("0.01"))
        assertNull(PaymentReminder("id", 100, "2026-02-30", 30).dueCalendar())
        assertFalse(PaymentReminder("id", 100, "2026-10-14", 30).valid(timestamp("2026-10-16 00:00")))
        assertFalse(PaymentReminder("id", 100, "2030-01-01", 0).valid(timestamp("2026-10-16 00:00")))
        assertFalse(PaymentReminder("id", 0, "2030-01-01", 30).valid(timestamp("2026-10-16 00:00")))
    }
    @Test fun paymentDayRemainsEligibleUntilMidnightAndThenExpires() {
        val reminder = PaymentReminder("id", 100, "2026-10-15", 30)
        assertFalse(reminder.expired(timestamp("2026-10-15 23:59"), TimeZone.getTimeZone("UTC")))
        assertTrue(reminder.expired(timestamp("2026-10-16 00:00"), TimeZone.getTimeZone("UTC")))
    }
}
