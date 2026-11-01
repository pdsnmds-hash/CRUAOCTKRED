package com.cruaoctkred.app.ui.feed

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.cruaoctkred.app.domain.model.LoanCard
import com.cruaoctkred.app.feature.offers.home.HomeScreen
import com.cruaoctkred.app.feature.offers.home.HomeUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class HomeFeedOrderTest {
    @get:Rule val compose = createComposeRule()
    private val cards = mutableStateOf(('a'..'h').map { card(it.toString()) })

    private fun render() {
        compose.setContent {
            Box(Modifier.size(360.dp, 360.dp)) {
                HomeScreen(HomeUiState(loading = false, cards = cards.value),
                    onRefresh = {}, onDetail = {}, onSave = {}, onCompare = {}, onCta = {},
                    onStorefrontVisible = {}, modifier = Modifier.testTag("feed"),
                    showOfflineBanner = false, showFeedHeader = false,
                    offerCard = { card, _, _ -> Text(card.title, Modifier.height(100.dp).testTag("card-${card.id}")) })
            }
        }
    }

    private fun scrollPartwayDown() {
        compose.onNodeWithTag("feed").performScrollToIndex(1)
        compose.onNodeWithTag("feed").performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, 35f) }
        compose.waitForIdle()
    }

    @Test fun shuffleShowsNewFirstCardFromItsTop() {
        render()
        val firstTop = compose.onNodeWithTag("card-a").fetchSemanticsNode().boundsInRoot.top
        scrollPartwayDown()
        compose.runOnIdle { cards.value = cards.value.reversed() }
        compose.onNodeWithTag("card-h").assertIsDisplayed()
        assertEquals(firstTop, compose.onNodeWithTag("card-h").fetchSemanticsNode().boundsInRoot.top, 0.5f)
    }

    @Test fun contentUpdateWithoutReorderingPreservesScroll() {
        render()
        scrollPartwayDown()
        val before = compose.onNodeWithTag("card-b").fetchSemanticsNode().boundsInRoot.top
        compose.runOnIdle { cards.value = cards.value.map { it.copy(title = "Updated ${it.id}") } }
        assertEquals(before, compose.onNodeWithTag("card-b").fetchSemanticsNode().boundsInRoot.top, 0.5f)
    }

    private fun card(id: String) = LoanCard(id = id, title = id, rating = 5.0,
        firstCredit = "1000 грн", repeatCredit = "", rate = "0.01%", age = "18–70", term = "65–365 днів",
        decisionTime = "5 хв", website = "", phone = "", email = "", address = "", legalEntity = "",
        license = "", apr = "", aprCalculation = "", logoUrl = null, linkUrl = null, order = 0)
}
