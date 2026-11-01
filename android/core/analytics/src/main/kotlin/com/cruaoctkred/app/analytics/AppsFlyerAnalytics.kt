package com.cruaoctkred.app.analytics

import android.content.Context
import android.app.Activity
import android.util.Log
import com.appsflyer.share.attribution.AppsFlyerRequestListener
import com.appsflyer.AppsFlyerLib
import com.appsflyer.share.AppsFlyerConversionListener
import com.cruaoctkred.app.domain.model.AttributionSnapshot
import com.cruaoctkred.app.domain.repository.AttributionRepository
import com.cruaoctkred.app.domain.usecase.offerClickEventName

class AppsFlyerAnalytics(
    private val context: Context,
    private val devKey: String,
    private val attributionRepository: AttributionRepository,
) {
    private val startupLock = Any()
    private var initialized = false
    private var sessionReady = false
    private var launcherCollected = false

    fun start() {
        if (devKey.isBlank()) return
        synchronized(startupLock) {
            if (initialized) return
            initialized = true
        }
        val listener = object : AppsFlyerConversionListener {
            override fun onConversionDataSuccess(data: MutableMap<String, Any>?) {
                val values = data.orEmpty()
                Log.i("ProductAppsFlyer", "Conversion data received")
                attributionRepository.saveInstallStatus(com.cruaoctkred.app.domain.model.InstallAttributionStatus.fromAppsFlyer(values["af_status"]?.toString()))
                fun value(vararg keys: String): String = keys.firstNotNullOfOrNull { values[it]?.toString()?.takeIf(String::isNotBlank) }.orEmpty()
                attributionRepository.save(
                    AttributionSnapshot(
                        campaign = value("campaign"),
                        adset = value("af_adset", "adset"),
                        network = value("network", "af_channel"),
                        mediaSource = value("media_source"),
                        gclid = value("gclid"),
                        campaignId = value("campaign_id"),
                        creativeId = value("af_ad_id", "creative_id"),
                        appsFlyerId = AppsFlyerLib.getInstance().getAppsFlyerUID(context).orEmpty(),
                    ),
                )
            }
            override fun onConversionDataFail(error: String?) { Log.w("ProductAppsFlyer", "Conversion data unavailable") }
        }
        AppsFlyerLib.getInstance().init(devKey, listener, context)
        AppsFlyerLib.getInstance().registerSessionReadyListener {
            synchronized(startupLock) {
                sessionReady = true
                startReadySession()
            }
        }
    }

    fun collectLauncher(activity: Activity) {
        if (devKey.isBlank()) return
        AppsFlyerLib.getInstance().collectDataFromLauncherActivity(activity)
        synchronized(startupLock) {
            launcherCollected = true
            startReadySession()
        }
    }

    // SDK 7 announces readiness for each foreground session. Wait for the launch intent too.
    private fun startReadySession() {
        if (!sessionReady || !launcherCollected) return
        sessionReady = false
        AppsFlyerLib.getInstance().start(object : AppsFlyerRequestListener {
            override fun onSuccess() { Log.i("ProductAppsFlyer", "Session accepted") }
            override fun onError(code: Int, error: String) {
                val httpCode = Regex("\\b[1-5][0-9]{2}\\b").find(error)?.value ?: "unknown"
                Log.w("ProductAppsFlyer", "Session rejected: SDK $code; HTTP $httpCode")
            }
        })
    }

    fun offerClick(cardId: String, cardTitle: String, placement: String) {
        if (devKey.isBlank()) return
        AppsFlyerLib.getInstance().logEvent(
            context,
            offerClickEventName(cardId),
            mapOf("card_id" to cardId, "card_title" to cardTitle, "placement" to placement),
        )
    }
}
