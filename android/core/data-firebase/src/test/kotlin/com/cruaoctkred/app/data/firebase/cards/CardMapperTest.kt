package com.cruaoctkred.app.data.firebase.cards

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CardMapperTest {
    private val mapper = CardMapper(locale = "vi-VN", fallbackLocale = "vi")

    @Test fun followsCurrentProductLanguageWhenRemapped() {
        var language = "uk"
        val mapper = CardMapper("uk", "uk") { language }
        val raw = mapOf("translations" to mapOf(
            "uk" to mapOf("decisionTime" to "5 хв"),
            "ru" to mapOf("decisionTime" to "5 мин"),
        ))
        assertEquals("5 хв", mapper.map("offer", raw).decisionTime)
        language = "ru"
        assertEquals("5 мин", mapper.map("offer", raw).decisionTime)
    }

    @Test fun mapsDefensivelyAndUsesVietnameseTranslation() {
        val card = mapper.map("alpha", mapOf(
            "title" to "Base",
            "rating" to 9,
            "rate" to "0,05% / ngày",
            "order" to "wrong",
            "logoUrl" to "http://unsafe",
            "translations" to mapOf("vi-VN" to mapOf("title" to "Tên Việt")),
        ))
        assertEquals("Tên Việt", card.title)
        assertEquals(5.0, card.rating, 0.0)
        assertEquals("0,05%", card.rate)
        assertEquals(0, card.order)
        assertNull(card.logoUrl)
        assertEquals("", card.legalEntity)
    }

    @Test fun usesFallbackLocaleWhenPrimaryMissing() {
        val card = mapper.map("beta", mapOf(
            "title" to "Base",
            "translations" to mapOf("vi" to mapOf("title" to "Tên ngắn")),
        ))
        assertEquals("Tên ngắn", card.title)
    }

    @Test fun fallsBackToRawFieldWhenNoTranslationMatches() {
        val card = mapper.map("gamma", mapOf(
            "title" to "Raw Title",
            "translations" to mapOf("fr" to mapOf("title" to "Titre")),
        ))
        assertEquals("Raw Title", card.title)
    }

    @Test fun differentLocalePolicy() {
        val enMapper = CardMapper(locale = "en-US", fallbackLocale = "en")
        val card = enMapper.map("delta", mapOf(
            "title" to "Base",
            "translations" to mapOf(
                "vi-VN" to mapOf("title" to "Tên Việt"),
                "en-US" to mapOf("title" to "English Title"),
            ),
        ))
        assertEquals("English Title", card.title)
    }

    @Test fun differentLocaleUsesItsFallback() {
        val enMapper = CardMapper(locale = "en-US", fallbackLocale = "en")
        val card = enMapper.map("epsilon", mapOf(
            "title" to "Base",
            "translations" to mapOf(
                "vi-VN" to mapOf("title" to "Tên Việt"),
                "en" to mapOf("title" to "English Fallback"),
            ),
        ))
        assertEquals("English Fallback", card.title)
    }

    @Test fun differentLocaleFallsBackToRawWhenNoMatch() {
        val enMapper = CardMapper(locale = "en-US", fallbackLocale = "en")
        val card = enMapper.map("zeta", mapOf(
            "title" to "Base Only",
            "translations" to mapOf("vi-VN" to mapOf("title" to "Tên Việt")),
        ))
        assertEquals("Base Only", card.title)
    }

    @Test fun noTranslationsMapUsesRawFields() {
        val card = mapper.map("eta", mapOf("title" to "Direct"))
        assertEquals("Direct", card.title)
    }

    @Test fun categoryFlagsRequireExplicitBooleanTrue() {
        val flagged = mapper.map("marked", mapOf("isHot" to true, "isNew" to true))
        org.junit.Assert.assertTrue(flagged.isHot)
        org.junit.Assert.assertTrue(flagged.isNew)
        val legacy = mapper.map("legacy", emptyMap())
        org.junit.Assert.assertFalse(legacy.isHot)
        org.junit.Assert.assertFalse(legacy.isNew)
        val malformed = mapper.map("bad", mapOf("isHot" to "true", "isNew" to 1))
        org.junit.Assert.assertFalse(malformed.isHot)
        org.junit.Assert.assertFalse(malformed.isNew)
    }
}
