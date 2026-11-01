package com.cruaoctkred.app.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EventNameTest {
    @Test fun keepsShortIdAndCapsLongId() {
        assertEquals("offer_click_money_veo", offerClickEventName("money-veo"))
        val long = offerClickEventName("a".repeat(100))
        assertTrue(long.length <= 45)
        assertEquals(long, offerClickEventName("a".repeat(100)))
    }
}

