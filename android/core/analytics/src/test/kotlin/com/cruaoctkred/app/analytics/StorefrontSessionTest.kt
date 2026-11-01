package com.cruaoctkred.app.analytics

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StorefrontSessionTest {
    @Test fun rollsAfterThirtyMinutesAndClaimsEventsOnce() {
        var now = 0L
        val session = StorefrontSession { now }
        val firstId = session.id
        assertTrue(session.claimHome())
        assertFalse(session.claimHome())
        assertTrue(session.claimFirstCta())
        assertFalse(session.claimFirstCta())
        session.onBackground()
        now += StorefrontSession.SESSION_TIMEOUT_MS
        session.onForeground()
        assertNotEquals(firstId, session.id)
        assertTrue(session.claimHome())
        assertTrue(session.claimFirstCta())
    }
}
