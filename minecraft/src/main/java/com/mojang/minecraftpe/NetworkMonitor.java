package com.mojang.minecraftpe;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.HashSet;

public class NetworkMonitor {
    private static final int NETWORK_CATEGORY_ETHERNET = 0;
    private static final int NETWORK_CATEGORY_OTHER = 2;
    private static final int NETWORK_CATEGORY_WIFI = 1;
    private final HashMap<Integer, HashSet<Network>> mAvailableNetworksPerCategory;
    private final Context mContext;

    public NetworkMonitor(Context context) {
        mContext = context;
        HashMap<Integer, HashSet<Network>> hashMap = new HashMap<>();
        mAvailableNetworksPerCategory = hashMap;
        hashMap.put(0, new HashSet<>());
        mAvailableNetworksPerCategory.put(1, new HashSet<>());
        mAvailableNetworksPerCategory.put(2, new HashSet<>());
        _addNetworkCallbacksForTransport(3, NETWORK_CATEGORY_ETHERNET);
        _addNetworkCallbacksForTransport(1, NETWORK_CATEGORY_WIFI);
        _addNetworkCallbacksForTransport(0, NETWORK_CATEGORY_OTHER);
        _addNetworkCallbacksForTransport(2, NETWORK_CATEGORY_OTHER);
        if (Build.VERSION.SDK_INT >= 31) {
            _addNetworkCallbacksForTransport(8, NETWORK_CATEGORY_OTHER);
        }
    }

    private native void nativeUpdateNetworkStatus(boolean isEthernetConnected, boolean isWifiConnected, boolean isOtherConnected);

    private void _addNetworkCallbacksForTransport(int transport, final int networkCategory) {
        ((ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE)).registerNetworkCallback(_createNetworkRequestForTransport(transport), new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                mAvailableNetworksPerCategory.get(networkCategory).add(network);
                _updateStatus();
            }

            @Override
            public void onLost(@NonNull Network network) {
                mAvailableNetworksPerCategory.get(networkCategory).remove(network);
                _updateStatus();
            }
        });
    }

    private NetworkRequest _createNetworkRequestForTransport(int transport) {
        NetworkRequest.Builder builder = new NetworkRequest.Builder();
        builder.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        if (Build.VERSION.SDK_INT >= 23) {
            builder.addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
        }
        builder.addTransportType(transport);
        return builder.build();
    }

    public void _updateStatus() {
        nativeUpdateNetworkStatus(!mAvailableNetworksPerCategory.get(0).isEmpty(), !mAvailableNetworksPerCategory.get(1).isEmpty(), !mAvailableNetworksPerCategory.get(2).isEmpty());
    }
}