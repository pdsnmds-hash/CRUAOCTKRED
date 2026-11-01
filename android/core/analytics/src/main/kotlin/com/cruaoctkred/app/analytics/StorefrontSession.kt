package com.cruaoctkred.app.analytics

import java.util.UUID

class StorefrontSession(
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {
    var id: String = UUID.randomUUID().toString()
        private set
    private var backgroundAt: Long? = null
    private var homeSent = false
    private var firstCtaSent = false

    fun onBackground() { backgroundAt = nowMillis() }

    fun onForeground() {
        val elapsed = backgroundAt?.let { nowMillis() - it } ?: return
        if (elapsed >= SESSION_TIMEOUT_MS) reset()
        backgroundAt = null
    }

    fun claimHome(): Boolean = if (homeSent) false else true.also { homeSent = true }
    fun claimFirstCta(): Boolean = if (firstCtaSent) false else true.also { firstCtaSent = true }

    private fun reset() {
        id = UUID.randomUUID().toString()
        homeSent = false
        firstCtaSent = false
    }

    companion object { const val SESSION_TIMEOUT_MS = 30 * 60 * 1000L }
}
