package com.cruaoctkred.app.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import com.cruaoctkred.app.domain.model.AttributionSnapshot

class OfferLinkResolverTest {
    private val resolver = OfferLinkResolver()

    @Test fun rejectsMissingHttpAndMalformedLinks() {
        assertNull(resolver.resolveCardCta(null))
        assertNull(resolver.resolveCardCta("http://example.com"))
        assertNull(resolver.resolveCardCta("not-a-url"))
    }

    @Test fun enrichesQueryPreservesRoutingAndFragmentWithoutDuplicates() {
        val result = resolver.enrich(
            "https://partner.example/path?pth=abc&gclid=old&gclid=duplicate#apply",
            OfferLinkResolver.FALLBACK_TEMPLATE,
            AttributionSnapshot(campaign = "Tết 2026", gclid = "g 1"),
        ).orEmpty()
        assertTrue(result.startsWith("https://partner.example/path?"))
        assertTrue(result.contains("pth=abc"))
        assertTrue(result.contains("camp_name=T%E1%BA%BFt%202026"))
        assertTrue(result.contains("gclid=g%201"))
        assertEquals(1, Regex("(?:\\?|&)gclid=").findAll(result).count())
        assertTrue(result.endsWith("#apply"))
        assertTrue(result.contains("appsfid=0"))
    }

    @Test fun fillsEveryAttributionPlaceholderWithUrlEncodedValues() {
        val result = resolver.enrich("https://tracker.example/offer?targetid=keep", OfferLinkResolver.FALLBACK_TEMPLATE,
            AttributionSnapshot(campaign = "Кампанія 1", adset = "set&1", network = "network",
                mediaSource = "source", gclid = "g+1", campaignId = "123", creativeId = "456", appsFlyerId = "af-789"))!!
        val query = java.net.URI(result).rawQuery.split('&').associate {
            val pair = it.split('=', limit = 2)
            java.net.URLDecoder.decode(pair[0], "UTF-8") to java.net.URLDecoder.decode(pair[1], "UTF-8")
        }
        assertEquals(mapOf("targetid" to "keep", "camp_name" to "Кампанія 1", "adset_name" to "set&1",
            "network" to "network", "source" to "source", "gclid" to "g+1", "campaignid" to "123",
            "creative_id" to "456", "appsfid" to "af-789"), query)
    }

    @Test fun handlesUrlWithoutQuery() {
        val result = resolver.enrich("https://example.vn", "source={media_source}", AttributionSnapshot())
        assertEquals("https://example.vn/?source=0", result)
    }
}

