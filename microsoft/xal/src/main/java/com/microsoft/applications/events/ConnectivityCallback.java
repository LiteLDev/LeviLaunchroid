package com.microsoft.applications.events;

import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

/**
 * @author <a href="https://github.com/RadiantByte">RadiantByte</a>
 */

class ConnectivityCallback extends ConnectivityManager.NetworkCallback {
    private boolean m_metered;
    private final HttpClient m_parent;

    ConnectivityCallback(HttpClient httpClient, boolean z) {
        this.m_parent = httpClient;
        this.m_metered = z;
    }

    @Override
    public final void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
        boolean z = !networkCapabilities.hasCapability(11);
        if (z != this.m_metered) {
            this.m_metered = z;
            this.m_parent.onCostChange(z);
        }
    }
}
