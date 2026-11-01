package com.cruaoctkred.app.domain.usecase

import java.security.MessageDigest

fun offerClickEventName(cardId: String): String {
    val normalized = cardId.lowercase().replace(Regex("[^a-z0-9_]"), "_").trim('_').ifBlank { "card" }
    val prefix = "offer_click_"
    if (prefix.length + normalized.length <= 45) return prefix + normalized
    val hash = MessageDigest.getInstance("SHA-256")
        .digest(cardId.toByteArray())
        .take(4)
        .joinToString("") { "%02x".format(it) }
    return prefix + normalized.take(45 - prefix.length - hash.length - 1) + "_" + hash
}

