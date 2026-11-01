package com.cruaoctkred.app.ui.localization

import org.junit.Assert.assertEquals
import org.junit.Test

class OfferTextTest {
    @Test fun ukrainianUnitsFromAdmin() {
        assertEquals("65 - 365 днів", localizeOfferText("65 - 365 дней", "uk"))
        assertEquals("6 хв", localizeOfferText("6 min", "uk"))
        assertEquals("від 10 000 грн", localizeOfferText("От 10 000 PHP", "uk"))
        assertEquals("18–70 років", localizeOfferText("18–70 лет", "uk"))
    }
    @Test fun russianUnitsWhenSwitchingBack() {
        assertEquals("65–365 дней", localizeOfferText("65–365 днів", "ru"))
        assertEquals("5 мин", localizeOfferText("5 хв", "ru"))
        assertEquals("от 10 000 грн", localizeOfferText("від 10 000 PHP", "ru"))
    }
    @Test fun normalizesLegacyCurrencyWithoutChangingAmounts() {
        assertEquals("від 10 000 грн", localizeOfferText("От 10 000 PHP", "uk"))
        assertEquals("25 000 грн", localizeOfferText("25 000 ₱", "ru"))
        assertEquals("100 грн", localizeOfferText("100 UAH", "uk"))
    }
    @Test fun usesCorrectPluralForms() {
        assertEquals("1 день", localizeOfferText("1 day", "uk"))
        assertEquals("2 дні", localizeOfferText("2 days", "uk"))
        assertEquals("12 днів", localizeOfferText("12 days", "uk"))
        assertEquals("21 день", localizeOfferText("21 дней", "uk"))
        assertEquals("2 дня", localizeOfferText("2 дні", "ru"))
    }
    @Test fun preservesNumbersAndDoesNotReplaceWordFragments() {
        assertEquals("0.01%", localizeOfferText("0.01%", "uk"))
        assertEquals("365", localizeOfferText("365", "uk"))
        assertEquals("Отлично", localizeOfferText("Отлично", "uk"))
        assertEquals("65 днів", localizeOfferText("65 днів", "uk"))
    }
}
