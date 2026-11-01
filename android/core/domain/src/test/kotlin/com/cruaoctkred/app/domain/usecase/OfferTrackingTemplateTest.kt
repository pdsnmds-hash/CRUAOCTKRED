package com.cruaoctkred.app.domain.usecase

import com.cruaoctkred.app.domain.model.AttributionSnapshot
import org.junit.Assert.*
import org.junit.Test
import java.net.URI
import java.net.URLDecoder

class OfferTrackingTemplateTest {
    @Test fun acceptsExistingAndEncodedCustomMapping() {
        assertEquals(8, OfferTrackingTemplate.parse(OfferLinkResolver.FALLBACK_TEMPLATE)!!.size)
        val template = "sub1=%7Bcampaign%7D&sub2=%7Baf_adset%7D&fixed=blue%20ua-3&combo=%7Bcampaign%7D-%7Baf_adset%7D"
        val url = OfferLinkResolver().enrich("https://example.com/go?sub1=old&targetid=keep#apply", template,
            AttributionSnapshot(campaign = "Кампанія #2 % & +", adset = "Ad group 1"))!!
        val uri = URI(url)
        val pairs = uri.rawQuery.split('&').associate { val p = it.split('=', limit = 2); p[0] to URLDecoder.decode(p[1], "UTF-8") }
        assertEquals("Кампанія #2 % & +", pairs["sub1"])
        assertEquals("Ad group 1", pairs["sub2"])
        assertEquals("blue ua-3", pairs["fixed"])
        assertEquals("Кампанія #2 % & +-Ad group 1", pairs["combo"])
        assertEquals("keep", pairs["targetid"])
        assertEquals("apply", uri.rawFragment)
        assertEquals(1, Regex("(?:\\?|&)sub1=").findAll(url).count())
    }
    @Test fun malformedOrMissingConfigurationUsesPreviousStandardTemplate() {
        val invalid = listOf(null, "", "x", "x={unknown}", "x={campaign", "x=a&x=b", "bad name=x", "x=%ZZ", "x=", "x=" + "a".repeat(4096))
        invalid.forEach { input ->
            assertEquals(OfferLinkResolver.FALLBACK_TEMPLATE, OfferTrackingTemplate.safe(input))
            if (input != null) assertNotNull(OfferLinkResolver().enrich("https://example.com", input, AttributionSnapshot()))
        }
    }
    @Test fun missingAttributionIsZeroAndInvalidOfferLinkStaysRejected() {
        assertEquals("https://example.com/?sub1=0", OfferLinkResolver().enrich("https://example.com", "sub1={campaign}", AttributionSnapshot()))
        assertNull(OfferLinkResolver().enrich("http://example.com", "sub1={campaign}", AttributionSnapshot()))
        assertNull(OfferTrackingTemplate.parse((1..33).joinToString("&") { "p$it=value" }))
    }
}
