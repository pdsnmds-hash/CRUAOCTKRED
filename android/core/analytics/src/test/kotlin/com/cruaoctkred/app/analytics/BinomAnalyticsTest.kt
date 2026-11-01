package com.cruaoctkred.app.analytics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import com.cruaoctkred.app.domain.model.AttributionSnapshot

class BinomAnalyticsTest {
    @Test
    fun replacesTrackerPlaceholdersWithoutDuplicatingTokens() {
        val result = buildBinomUrl(
            base = "https://tracker.example/click.php?targetid=abc&t1={t1}&t2={t2}&t10={t10}",
            applicationId = "com.cruaoctkred.app",
            designVersion = "jade-2026",
            sessionId = "session-1",
            cardId = "cashspace",
            events = mapOf("event2" to "1", "event3" to "1"),
            attribution = AttributionSnapshot(campaign = "Tết 2026", gclid = "g 1"),
        ).toString()

        assertTrue(result.contains("targetid=abc"))
        assertTrue(result.contains("t1=com.cruaoctkred.app"))
        assertTrue(result.contains("t4=cashspace"))
        assertTrue(result.contains("t5=T%E1%BA%BFt%202026"))
        assertTrue(result.contains("t10=g%201"))
        assertTrue(result.contains("event2=1"))
        assertTrue(result.contains("event3=1"))
        assertEquals(1, Regex("(?:\\?|&)t1=").findAll(result).count())
        assertEquals(1, Regex("(?:\\?|&)t2=").findAll(result).count())
        assertEquals(1, Regex("(?:\\?|&)t10=").findAll(result).count())
        assertFalse(result.contains("{t1}"))
    }

    @Test
    fun homeReplacesEveryPlaceholderAndKeepsTarget() {
        val url = buildBinomUrl(
            base = "https://tracker.example/click.php?targetid=abc&" + (1..10).joinToString("&") { "t$it={t$it}" },
            applicationId = "com.cruaoctkred.app", designVersion = "blue-ua-3",
            sessionId = "session", cardId = null, events = mapOf("event1" to "1"),
            attribution = AttributionSnapshot(),
        ).toString()
        assertTrue(url.contains("targetid=abc"))
        assertTrue(url.contains("t2=blue-ua-3"))
        assertTrue(url.contains("t4=0"))
        assertTrue(url.contains("event1=1"))
        assertFalse(url.contains("{"))
        assertFalse(url.contains("%7B", ignoreCase = true))
        (1..10).forEach { assertEquals(1, Regex("(?:\\?|&)t$it=").findAll(url).count()) }
    }

    @Test
    fun rejectsNonHttpsTrackerUrl() {
        assertNull(
            buildBinomUrl(
                base = "http://tracker.example/click",
                applicationId = "com.cruaoctkred.app",
                designVersion = "jade-2026",
                sessionId = "session-1",
                cardId = null,
                events = emptyMap(),
                attribution = AttributionSnapshot(),
            ),
        )
    }
}
