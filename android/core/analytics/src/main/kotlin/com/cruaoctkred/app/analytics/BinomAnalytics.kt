package com.cruaoctkred.app.analytics

import android.util.Log
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.cruaoctkred.app.domain.model.AttributionSnapshot

class BinomAnalytics(
    private val applicationId: String,
    private val analyticsUrl: String,
    private val designVersion: String,
    private val session: StorefrontSession,
) {
    suspend fun home(attribution: AttributionSnapshot) {
        if (session.claimHome()) send(mapOf("event1" to "1"), null, attribution)
    }

    suspend fun cta(cardId: String, attribution: AttributionSnapshot) {
        val events = if (session.claimFirstCta()) mapOf("event2" to "1", "event3" to "1") else mapOf("event3" to "1")
        send(events, cardId, attribution)
    }

    private suspend fun send(events: Map<String, String>, cardId: String?, attribution: AttributionSnapshot) = withContext(Dispatchers.IO) {
        val base = analyticsUrl.takeIf { it.startsWith("https://") } ?: return@withContext
        val target = buildBinomUrl(
            base = base,
            applicationId = applicationId,
            designVersion = designVersion,
            sessionId = session.id,
            cardId = cardId,
            events = events,
            attribution = attribution,
        ) ?: return@withContext
        runCatching {
            (target.openConnection() as HttpURLConnection).run {
                connectTimeout = 5_000
                readTimeout = 5_000
                instanceFollowRedirects = false
                requestMethod = "GET"
                val status = responseCode
                Log.i("ProductBinom", "${events.keys.joinToString("+")}: HTTP $status")
                (if (status < 400) inputStream else errorStream)?.use { it.read() }
                disconnect()
            }
        }.onFailure { Log.w("BinomAnalytics", "Analytics request failed: ${it.javaClass.simpleName}") }
    }
}

internal fun buildBinomUrl(
    base: String,
    applicationId: String,
    designVersion: String,
    sessionId: String,
    cardId: String?,
    events: Map<String, String>,
    attribution: AttributionSnapshot,
): URL? = runCatching {
    // URL accepts Binom's literal `{t1}` placeholders; URI rejects them before
    // we get a chance to replace the template values.
    val tracker = URL(base)
    if (tracker.protocol != "https" || tracker.host.isBlank()) return null
    val generated = buildMap {
        put("t1", applicationId)
        put("t2", designVersion)
        put("t3", sessionId)
        put("t4", cardId ?: "0")
        put("t5", attribution.campaign.ifBlank { "0" })
        put("t6", attribution.adset.ifBlank { "0" })
        put("t7", attribution.mediaSource.ifBlank { "0" })
        put("t8", attribution.campaignId.ifBlank { "0" })
        put("t9", attribution.creativeId.ifBlank { "0" })
        put("t10", attribution.gclid.ifBlank { "0" })
        putAll(events)
    }
    val original = tracker.query.orEmpty()
        .split('&')
        .filter(String::isNotBlank)
        .filter { part -> decode(part.substringBefore('=')) !in generated.keys }
    val query = (original + generated.map { (key, value) -> "${encode(key)}=${encode(value)}" }).joinToString("&")
    URL(
        buildString {
            append(tracker.protocol).append("://").append(tracker.authority)
            append(tracker.path.ifEmpty { "/" })
            if (query.isNotEmpty()) append('?').append(query)
            tracker.ref?.let { append('#').append(it) }
        },
    )
}.getOrNull()

private fun encode(value: String) = URLEncoder.encode(value, StandardCharsets.UTF_8.name()).replace("+", "%20")
private fun decode(value: String) = URLDecoder.decode(value, StandardCharsets.UTF_8.name())
