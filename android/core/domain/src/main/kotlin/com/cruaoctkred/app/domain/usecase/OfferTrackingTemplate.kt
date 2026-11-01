package com.cruaoctkred.app.domain.usecase

import java.net.URLDecoder
import java.nio.charset.StandardCharsets

/** Existing settings/tracking.template contract, bounded and validated before applying it. */
object OfferTrackingTemplate {
    private val sources = setOf("campaign", "af_adset", "network", "media_source", "gclid", "campaignid", "af_ad_id", "appsflyer_id")
    private val namePattern = Regex("[A-Za-z][A-Za-z0-9_.-]{0,63}")
    private val placeholder = Regex("\\{([^{}]+)\\}")

    fun parse(template: String): List<Pair<String, String>>? = runCatching {
        require(template.length in 1..4096)
        val parts = template.split('&')
        require(parts.size in 1..32)
        val seen = mutableSetOf<String>()
        parts.map { pair ->
            val at = pair.indexOf('=')
            require(at > 0)
            fun decode(value: String) = URLDecoder.decode(value, StandardCharsets.UTF_8.name())
            val name = decode(pair.substring(0, at))
            val value = decode(pair.substring(at + 1))
            require(namePattern.matches(name) && seen.add(name))
            require(value.length in 1..1024 && value.none { it.code < 32 || it.code == 127 })
            require(placeholder.findAll(value).all { it.groupValues[1] in sources })
            require(placeholder.replace(value, "").none { it == '{' || it == '}' })
            name to value
        }
    }.getOrNull()

    fun safe(template: String?): String = template?.takeIf { parse(it) != null } ?: OfferLinkResolver.FALLBACK_TEMPLATE
}
