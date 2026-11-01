package com.cruaoctkred.app.domain.model

import com.cruaoctkred.app.domain.usecase.OfferLinkResolver
import org.junit.Assert.*
import org.junit.Test

class OfferRoutingPolicyTest {
    @Test fun entireRoutingTable() {
        for (status in InstallAttributionStatus.entries) {
            for (enabled in listOf(false, true)) {
                for (online in listOf(false, true)) for (valid in listOf(false, true)) {
                    assertEquals("$enabled $status $online $valid", online && valid && (!enabled || status == InstallAttributionStatus.NON_ORGANIC),
                        OfferRoutingPolicy.allowsWebView(OfferRoutingSetting.Available(enabled), status, valid, online))
                }
            }
            assertFalse(OfferRoutingPolicy.allowsWebView(OfferRoutingSetting.Unavailable, status, true, true))
        }
    }
    @Test fun serverMetadataAndMissingDocumentAreDistinct() {
        assertEquals(OfferRoutingSetting.Available(false), routingSettingFromSnapshot(true, false, false, false, null))
        assertEquals(OfferRoutingSetting.Available(true), routingSettingFromSnapshot(true, true, false, false, true))
        assertEquals(OfferRoutingSetting.Available(false), routingSettingFromSnapshot(true, true, false, false, false))
        for (value in listOf(null, "true", 1)) assertEquals(OfferRoutingSetting.Unavailable, routingSettingFromSnapshot(true, true, false, false, value))
        assertEquals(OfferRoutingSetting.Unavailable, routingSettingFromSnapshot(true, false, true, false, null))
        assertEquals(OfferRoutingSetting.Unavailable, routingSettingFromSnapshot(true, true, false, true, false))
        assertEquals(OfferRoutingSetting.Unavailable, routingSettingFromSnapshot(false, false, false, false, false))
    }
    @Test fun unknownNeverClaimsInstallAndFirstDefinedStatusWins() {
        assertEquals(InstallAttributionStatus.UNKNOWN, InstallAttributionStatus.fromAppsFlyer(null))
        assertEquals(InstallAttributionStatus.UNKNOWN, InstallAttributionStatus.fromAppsFlyer("unexpected"))
        assertEquals(InstallAttributionStatus.ORGANIC, InstallAttributionStatus.fromAppsFlyer("Organic"))
        assertEquals(InstallAttributionStatus.NON_ORGANIC, InstallAttributionStatus.fromAppsFlyer("Non-organic"))
        for (first in listOf(InstallAttributionStatus.ORGANIC, InstallAttributionStatus.NON_ORGANIC)) {
            var saved = retainFirstInstallStatus(InstallAttributionStatus.UNKNOWN, InstallAttributionStatus.UNKNOWN)
            saved = retainFirstInstallStatus(saved, first)
            for (later in InstallAttributionStatus.entries) assertEquals(first, retainFirstInstallStatus(saved, later))
        }
    }
    @Test fun malformedAndUnsafeLinksAreRejected() {
        val resolver = OfferLinkResolver()
        for (link in listOf(null, "", " ", "http://offer.test", "javascript:alert(1)", "https:///", "https://a b.test", "https://user:pass@offer.test", "https://offer.test:99999")) assertNull(link, resolver.resolveCardCta(link))
        assertNotNull(resolver.resolveCardCta(" https://offer.test/path?a=1#form "))
    }
}
