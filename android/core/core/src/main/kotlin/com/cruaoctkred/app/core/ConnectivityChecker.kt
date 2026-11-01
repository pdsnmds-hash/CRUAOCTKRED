package com.cruaoctkred.app.core

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

class ConnectivityChecker(context: Context) : com.cruaoctkred.app.domain.repository.NetworkStatusRepository {
    private val appContext = context.applicationContext

    private val mutableOnline = kotlinx.coroutines.flow.MutableStateFlow(isOnline())
    override val online: kotlinx.coroutines.flow.StateFlow<Boolean> = mutableOnline
    init {
        val manager = appContext.getSystemService(ConnectivityManager::class.java)
        manager.registerNetworkCallback(android.net.NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build(),
            object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: android.net.Network) { mutableOnline.value = isOnline() }
                override fun onLost(network: android.net.Network) { mutableOnline.value = isOnline() }
                override fun onCapabilitiesChanged(network: android.net.Network, capabilities: NetworkCapabilities) { mutableOnline.value = isOnline() }
            })
    }
    override fun isOnline(): Boolean {
        val manager = appContext.getSystemService(ConnectivityManager::class.java)
        val network = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
