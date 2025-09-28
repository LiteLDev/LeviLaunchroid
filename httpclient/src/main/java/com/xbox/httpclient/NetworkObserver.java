package com.xbox.httpclient;

import android.content.Context;
import android.net.*;
import android.os.Build;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 09.09.2025
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */
public class NetworkObserver {
    private static final String CONNECTIVITY_SERVICE = "connectivity";

    private static String s_lastCapabilities = "";
    private static String s_lastLinkProperties = "";

    private static final ConnectivityManager.NetworkCallback s_networkChangedCallback = new ConnectivityManager.NetworkCallback() {
        @Override
        public void onAvailable(Network network) {
            logNetworkStatus(network, "is available");
        }

        @Override
        public void onLost(Network network) {
            logNetworkStatus(network, "was lost");
        }

        @Override
        public void onUnavailable() {
            NetworkObserver.Log("No networks were available");
        }

        @Override
        public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
            handleCapabilitiesChange(network, networkCapabilities);
        }

        @Override
        public void onLinkPropertiesChanged(Network network, LinkProperties linkProperties) {
            handleLinkPropertiesChange(network, linkProperties);
        }

        private void handleCapabilitiesChange(Network network, NetworkCapabilities networkCapabilities) {
            final String capabilities = NetworkDetails.checkNetworkCapabilities(networkCapabilities);
            if (!capabilities.equals(s_lastCapabilities)) {
                s_lastCapabilities = capabilities;
                logNetworkStatus(network, "has capabilities: " + s_lastCapabilities);
            }
        }

        private void handleLinkPropertiesChange(Network network, LinkProperties linkProperties) {
            final String linkProps = NetworkDetails.checkLinkProperties(linkProperties);
            if (!linkProps.equals(s_lastLinkProperties)) {
                s_lastLinkProperties = linkProps;
                logNetworkStatus(network, "has link properties: " + s_lastLinkProperties);
            }
        }

        private void logNetworkStatus(@NotNull Network network, String status) {
            NetworkObserver.Log("Network ID " + network.hashCode() + " " + status);
        }
    };

    private static native void Log(String message);

    public static void Initialize(Context context) {
        final ConnectivityManager connectivityManager = getConnectivityManager(context);
        final NetworkRequest networkRequest = buildNetworkRequest();
        connectivityManager.registerNetworkCallback(networkRequest, s_networkChangedCallback);
    }

    public static void Cleanup(Context context) {
        final ConnectivityManager connectivityManager = getConnectivityManager(context);
        connectivityManager.unregisterNetworkCallback(s_networkChangedCallback);
    }

    private static ConnectivityManager getConnectivityManager(@NotNull Context context) {
        return (ConnectivityManager) context.getSystemService(CONNECTIVITY_SERVICE);
    }

    private static NetworkRequest buildNetworkRequest() {
        return new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();
    }

    static class NetworkDetails {
        @NotNull
        public static String getNetworkDetails(Network network, @NotNull ConnectivityManager connectivityManager) {
            final NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
            final LinkProperties linkProperties = connectivityManager.getLinkProperties(network);

            return String.format("Network %d:%n  Capabilities: %s%n  Link properties: %s",
                    network.hashCode(),
                    capabilities != null ? checkNetworkCapabilities(capabilities) : "Got null capabilities",
                    linkProperties != null ? checkLinkProperties(linkProperties) : "Got null link properties");
        }

        @NotNull
        public static String checkNetworkCapabilities(@NotNull NetworkCapabilities networkCapabilities) {
            Map<String, Boolean> capabilitiesMap = new LinkedHashMap<>();

            capabilitiesMap.put("isWifi", networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI));
            capabilitiesMap.put("isBluetooth", networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH));
            capabilitiesMap.put("isCellular", networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR));
            capabilitiesMap.put("isVpn", networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN));
            capabilitiesMap.put("isEthernet", networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
            capabilitiesMap.put("shouldHaveInternet", networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET));
            capabilitiesMap.put("isNotVpn", networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN));
            capabilitiesMap.put("internetWasValidated", networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED));

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                capabilitiesMap.put("isNotSuspended", networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_SUSPENDED));
            }

            return joinMap(capabilitiesMap);
        }

        @NotNull
        public static String checkLinkProperties(@NotNull LinkProperties linkProperties) {
            Map<String, Boolean> linkPropsMap = new LinkedHashMap<>();
            linkPropsMap.put("hasProxy", linkProperties.getHttpProxy() != null);
            return joinMap(linkPropsMap);
        }

        @NotNull
        private static String joinMap(@NotNull Map<String, Boolean> map) {
            StringBuilder result = new StringBuilder();
            for (Map.Entry<String, Boolean> entry : map.entrySet()) {
                if (!result.isEmpty()) {
                    result.append(", ");
                }
                result.append(entry.getKey()).append(": ").append(entry.getValue());
            }
            return result.toString();
        }
    }
}