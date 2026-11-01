package com.cruaoctkred.app.domain.usecase

import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import com.cruaoctkred.app.domain.model.AttributionSnapshot

class OfferLinkResolver {
    fun resolveCardCta(link: String?): String? {
        if (link.isNullOrBlank()) return null
        return runCatching {
            val uri = URI(link.trim())
            if (!uri.scheme.equals("https", ignoreCase = true) || uri.host.isNullOrBlank() || uri.rawUserInfo != null || uri.port < -1 || uri.port > 65535) null else "https:" + uri.toASCIIString().substringAfter(":")
        }.getOrNull()
    }

    fun enrich(link: String?, template: String, attribution: AttributionSnapshot): String? {
        val valid = resolveCardCta(link) ?: return null
        return runCatching {
            val uri = URI(valid)
            val pairs = parseRawQuery(uri.rawQuery).toMutableList()
            OfferTrackingTemplate.parse(OfferTrackingTemplate.safe(template))!!.forEach { (name, valueTemplate) ->
                val value = PLACEHOLDER.replace(valueTemplate) { match ->
                    attribution.valueFor(match.groupValues[1])
                }
                val encodedName = encode(name)
                pairs.removeAll { decode(it.first) == name }
                pairs += encodedName to encode(value)
            }
            buildString {
                append(uri.scheme).append("://").append(uri.rawAuthority)
                append(uri.rawPath.ifEmpty { "/" })
                if (pairs.isNotEmpty()) {
                    append('?')
                    append(pairs.joinToString("&") { (key, value) -> "$key=$value" })
                }
                uri.rawFragment?.let { append('#').append(it) }
            }
        }.getOrNull()
    }

    private fun parseRawQuery(query: String?): List<Pair<String, String>> =
        query.orEmpty().split('&').filter { it.isNotBlank() }.map { pair ->
            val separator = pair.indexOf('=')
            if (separator < 0) pair to "" else pair.substring(0, separator) to pair.substring(separator + 1)
        }

    private fun encode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8.name()).replace("+", "%20")
    private fun decode(value: String): String = URLDecoder.decode(value, StandardCharsets.UTF_8.name())

    companion object {
        // Both braces are escaped for Android ICU Regex safety.
        private val PLACEHOLDER = Regex("\\{([^}]+)\\}")
        const val FALLBACK_TEMPLATE = "camp_name={campaign}&adset_name={af_adset}&network={network}&source={media_source}&gclid={gclid}&campaignid={campaignid}&creative_id={af_ad_id}&appsfid={appsflyer_id}"
    }
}

