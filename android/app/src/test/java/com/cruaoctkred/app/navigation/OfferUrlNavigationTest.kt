package com.cruaoctkred.app.navigation

import android.app.Application
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.cruaoctkred.app.domain.model.AttributionSnapshot
import com.cruaoctkred.app.domain.usecase.OfferLinkResolver
import com.cruaoctkred.app.feature.offerweb.OfferWebScreen
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.net.URI
import java.net.URLDecoder

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class OfferUrlNavigationTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun encodedAttributionSurvivesNavigationAndActualWebViewLoadUrl() {
        lateinit var navigation: NavHostController
        compose.setContent {
            navigation = rememberNavController()
            NavHost(navigation, startDestination = Routes.MAIN) {
                composable(Routes.MAIN) { Text("Start") }
                composable(Routes.WEB, arguments = listOf(navArgument("url") { type = NavType.StringType },
                    navArgument("cardId") { type = NavType.StringType })) { entry ->
                    // Same argument boundary as AppNavGraph: Navigation has already decoded the route.
                    OfferWebScreen(entry.arguments?.getString("url").orEmpty(), onClose = {})
                }
            }
        }
        val resolver = OfferLinkResolver()
        val campaigns = listOf("QA_TEST", "GoldAppKZ #2", "Кампанія №1", "50% discount", "A&B + C")
        campaigns.forEachIndexed { index, campaign ->
            val enriched = resolver.enrich(
                "https://tracker.example/offer?targetid=keep&existing=keep%2Bvalue&camp_name=old#apply",
                OfferLinkResolver.FALLBACK_TEMPLATE,
                AttributionSnapshot(campaign = campaign, adset = "Ad group 1", network = "Мережа & + % #"),
            )!!
            compose.runOnIdle { navigation.navigate(Routes.web("fixture-$index", Uri.encode(enriched))) }
            compose.waitForIdle()
            var loaded: String? = null
            compose.runOnIdle {
                val webView = findWebView(compose.activity.window.decorView)
                assertNotNull("Offer screen must create WebView", webView)
                loaded = shadowOf(webView!!).lastLoadedUrl
            }
            assertEquals("WebView must receive the exact enriched URL", enriched, loaded)
            assertEquals(loaded, resolver.resolveCardCta(loaded))
            val uri = URI(loaded!!)
            assertEquals("apply", uri.rawFragment)
            val query = uri.rawQuery.split('&').associate {
                val parts = it.split('=', limit = 2)
                URLDecoder.decode(parts[0], "UTF-8") to URLDecoder.decode(parts[1], "UTF-8")
            }
            assertEquals(campaign, query["camp_name"])
            assertEquals("Ad group 1", query["adset_name"])
            assertEquals("Мережа & + % #", query["network"])
            assertEquals("keep+value", query["existing"])
            assertEquals("keep", query["targetid"])
            assertEquals(1, Regex("(?:\\?|&)camp_name=").findAll(loaded!!).count())
        }
    }

    @Test fun negativeControlExtraDecodeBreaksTheDocumentedUrl() {
        val resolver = OfferLinkResolver()
        val enriched = resolver.enrich("https://tracker.example/offer#apply", OfferLinkResolver.FALLBACK_TEMPLATE,
            AttributionSnapshot(campaign = "GoldAppKZ #2", adset = "Ad group 1"))!!
        assertNotNull(resolver.resolveCardCta(enriched))
        assertNull(resolver.resolveCardCta(Uri.decode(enriched)))
    }

    private fun findWebView(view: View): WebView? {
        if (view is WebView) return view
        if (view is ViewGroup) for (index in 0 until view.childCount) {
            findWebView(view.getChildAt(index))?.let { return it }
        }
        return null
    }
}
