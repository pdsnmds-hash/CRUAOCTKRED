package com.cruaoctkred.app.feature.offers.home

import com.cruaoctkred.app.domain.model.CardsContent
import com.cruaoctkred.app.domain.model.StreamResult
import org.junit.Assert.*
import org.junit.Test

class RefreshTest {
    private val loaded = StreamResult.Data(CardsContent(emptyList()))
    @Test fun localeRefreshKeepsLoadedContent() {
        val result = retainVisibleCards(loaded, StreamResult.Loading)
        assertSame(loaded.value, (result as StreamResult.Data).value)
    }
    @Test fun serverCanStillPublishAnEmptyList() {
        val update = StreamResult.Data(CardsContent(emptyList()))
        assertSame(update, retainVisibleCards(loaded, update))
    }
    @Test fun failedRefreshRetainsCards() {
        val error = IllegalStateException("offline")
        val result = retainVisibleCards(loaded, StreamResult.Error(error)) as StreamResult.Error
        assertSame(loaded.value, result.cached)
        assertSame(error, result.cause)
    }
    @Test fun firstLaunchStillLoads() {
        assertEquals(StreamResult.Loading, retainVisibleCards(StreamResult.Loading, StreamResult.Loading))
    }
}
