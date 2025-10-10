package com.mojang.minecraftpe;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NetworkMonitor {
    private enum NetworkType {
        Cable, Wifi, Other
    }

    private static int[] TRANSPORT_TYPE_CABLE = new int[] { NetworkCapabilities.TRANSPORT_ETHERNET };
    private static int[] TRANSPORT_TYPE_WIFI = new int[] { NetworkCapabilities.TRANSPORT_WIFI };
    private static int[] TRANSPORT_TYPE_OTHER = new int[] { NetworkCapabilities.TRANSPORT_CELLULAR, NetworkCapabilities.TRANSPORT_BLUETOOTH }; // TODO: transport usb

    private Context context;
    private List<ConnectivityManager.NetworkCallback> callbacks = new ArrayList<>();
    private Set<Network> knownNetworks = new HashSet<>();
    private int cableAvailable, wifiAvailable, otherAvailable;

    public NetworkMonitor(Context context) {
        this.context = context;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            registerCallbacks();
        } else {
            nativeUpdateNetworkStatus(false, true, false);
        }
    }

    public void finish() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            this.unregisterCallbacks();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void registerCallbacks() {
        ConnectivityManager connMan = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        registerCallback(NetworkType.Cable, TRANSPORT_TYPE_CABLE);
        registerCallback(NetworkType.Wifi, TRANSPORT_TYPE_WIFI);
        registerCallback(NetworkType.Other, TRANSPORT_TYPE_OTHER);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void registerCallback(NetworkType type, int[] transportTypes) {
        ConnectivityManager connMan = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        ConnectivityManager.NetworkCallback callback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                if (!knownNetworks.contains(network)) {
                    knownNetworks.add(network);
                    setHasNetworkType(type, true);
                }
            }

            @Override
            public void onLost(@NonNull Network network) {
                if (knownNetworks.contains(network)) {
                    setHasNetworkType(type, false);
                    knownNetworks.remove(network);
                }
            }
        };
        this.callbacks.add(callback);
        for (int transportType : transportTypes) {
            NetworkRequest.Builder builder = new NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
            if (Build.VERSION.SDK_INT >= 23)
                builder.addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
            builder.addTransportType(transportType);
            connMan.registerNetworkCallback(builder.build(), callback);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void unregisterCallbacks() {
        ConnectivityManager connMan = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        for (ConnectivityManager.NetworkCallback callback : callbacks)
            connMan.unregisterNetworkCallback(callback);
    }

    private void setHasNetworkType(NetworkType type, boolean available) {
        if (type == NetworkType.Cable)
            cableAvailable += available ? 1 : -1;
        if (type == NetworkType.Wifi)
            wifiAvailable += available ? 1 : -1;
        if (type == NetworkType.Other)
            otherAvailable += available ? 1 : -1;
        nativeUpdateNetworkStatus(cableAvailable > 0, wifiAvailable > 0, otherAvailable > 0);
    }


    private native void nativeUpdateNetworkStatus(boolean cable, boolean wifi, boolean misc);
}
