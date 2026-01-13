package org.levimc.launcher.util;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class MinecraftUriHandler {

    public static final String SCHEME = "minecraft";
    
    public static final String ACTION_HOW_TO_PLAY = "showHowToPlayScreen";
    public static final String ACTION_ADD_EXTERNAL_SERVER = "addExternalServer";
    public static final String ACTION_SHOW_STORE_OFFER = "showStoreOffer";
    public static final String ACTION_SHOW_OFFER_COLLECTION = "showOfferCollection";
    public static final String ACTION_OPEN_STORE = "openStore";
    public static final String ACTION_SHOW_MINECOIN_OFFERS = "showMineCoinOffers";
    public static final String ACTION_OPEN_MARKETPLACE_INVENTORY = "openMarketplaceInventory";
    public static final String ACTION_OPEN_CSB_PDP_SCREEN = "openCsbPDPScreen";
    public static final String ACTION_OPEN_SERVERS_TAB = "openServersTab";
    public static final String ACTION_SHOW_DRESSING_ROOM_OFFER = "showDressingRoomOffer";
    public static final String ACTION_SHOW_PROFILE_SCREEN = "showProfileScreen";
    public static final String ACTION_JOIN_GATHERING = "joinGathering";
    public static final String ACTION_ACCEPT_REALM_INVITE = "acceptRealmInvite";
    public static final String ACTION_CONNECT_TO_REALM = "connectToRealm";
    public static final String ACTION_SLASH_COMMAND = "slashcommand";
    public static final String ACTION_IMPORT = "import";
    public static final String ACTION_IMPORT_PACK = "importpack";
    public static final String ACTION_IMPORT_ADDON = "importaddon";
    public static final String ACTION_IMPORT_TEMPLATE = "importtemplate";
    public static final String ACTION_LOAD_WORLD = "load";
    public static final String ACTION_CONNECT = "connect";

    public static class MinecraftUri {
        public final String action;
        public final String rawUri;
        public final Uri uri;
        
        @Nullable public String serverName;
        @Nullable public String serverIp;
        public int serverPort = 19132;
        @Nullable public String storeOfferId;
        @Nullable public String collectionId;
        @Nullable public String inventoryTab;
        @Nullable public String csbTab;
        @Nullable public String dressingRoomOfferId;
        @Nullable public String gatheringId;
        @Nullable public String realmInviteId;
        @Nullable public String realmId;
        @Nullable public String slashCommand;
        @Nullable public String importPath;
        @Nullable public String localLevelId;
        @Nullable public String localWorldName;
        
        public MinecraftUri(String action, String rawUri, Uri uri) {
            this.action = action;
            this.rawUri = rawUri;
            this.uri = uri;
        }
    }

    @Nullable
    public static MinecraftUri parse(@Nullable Uri uri) {
        if (uri == null) return null;
        String scheme = uri.getScheme();
        if (!SCHEME.equals(scheme)) return null;
        if (uri.isOpaque()) return parseOpaqueUri(uri);
        
        String host = uri.getHost();
        String path = uri.getPath();
        MinecraftUri result;
        
        if (host != null && !host.isEmpty() && !host.equals("?")) {
            result = new MinecraftUri(host, uri.toString(), uri);
        } else if (path != null && !path.isEmpty()) {
            result = new MinecraftUri(path.replace("/", ""), uri.toString(), uri);
        } else {
            String action = getFirstQueryParam(uri);
            result = new MinecraftUri(action != null ? action : "", uri.toString(), uri);
        }
        
        parseParameters(result, uri);
        return result;
    }

    @Nullable
    private static MinecraftUri parseOpaqueUri(@NonNull Uri uri) {
        String ssp = uri.getSchemeSpecificPart();
        if (ssp == null || ssp.isEmpty()) return new MinecraftUri("", uri.toString(), uri);
        if (ssp.startsWith("?")) ssp = ssp.substring(1);
        
        String action = "";
        String value = "";
        int eqIndex = ssp.indexOf('=');
        if (eqIndex > 0) {
            action = ssp.substring(0, eqIndex);
            int ampIndex = ssp.indexOf('&', eqIndex);
            value = ampIndex > 0 ? ssp.substring(eqIndex + 1, ampIndex) : ssp.substring(eqIndex + 1);
        } else {
            action = ssp;
        }
        
        MinecraftUri result = new MinecraftUri(action, uri.toString(), uri);
        parseOpaqueParameters(result, action, value, ssp);
        return result;
    }

    private static void parseOpaqueParameters(MinecraftUri result, String action, String value, String ssp) {
        switch (action) {
            case ACTION_ADD_EXTERNAL_SERVER:
                parseAddExternalServerValue(result, value);
                break;
            case ACTION_SHOW_STORE_OFFER:
                result.storeOfferId = value;
                break;
            case ACTION_SHOW_OFFER_COLLECTION:
                result.collectionId = value;
                break;
            case ACTION_OPEN_MARKETPLACE_INVENTORY:
                result.inventoryTab = value;
                break;
            case ACTION_OPEN_CSB_PDP_SCREEN:
                result.csbTab = value;
                break;
            case ACTION_SLASH_COMMAND:
                result.slashCommand = Uri.decode(value);
                break;
            case ACTION_IMPORT:
            case ACTION_IMPORT_PACK:
            case ACTION_IMPORT_ADDON:
            case ACTION_IMPORT_TEMPLATE:
                result.importPath = Uri.decode(value);
                break;
            case ACTION_LOAD_WORLD:
                result.localLevelId = Uri.decode(value);
                break;
            case ACTION_CONNECT:
                parseConnectFromSsp(result, ssp);
                break;
        }
    }

    private static void parseAddExternalServerValue(MinecraftUri result, String value) {
        if (value != null && value.contains("|")) {
            String[] parts = value.split("\\|");
            if (parts.length >= 2) {
                result.serverName = Uri.decode(parts[0]);
                String addressPart = parts[1];
                if (addressPart.contains(":")) {
                    String[] addressParts = addressPart.split(":");
                    result.serverIp = addressParts[0];
                    try {
                        result.serverPort = Integer.parseInt(addressParts[1]);
                    } catch (NumberFormatException e) {
                        result.serverPort = 19132;
                    }
                } else {
                    result.serverIp = addressPart;
                }
            }
        }
    }

    private static void parseConnectFromSsp(MinecraftUri result, String ssp) {
        String[] params = ssp.split("&");
        for (String param : params) {
            String[] kv = param.split("=");
            if (kv.length == 2) {
                String key = kv[0];
                String val = Uri.decode(kv[1]);
                switch (key) {
                    case "localLevelId":
                        result.localLevelId = val;
                        break;
                    case "localWorld":
                        result.localWorldName = val;
                        break;
                    case "serverUrl":
                        result.serverIp = val;
                        break;
                    case "serverPort":
                        try {
                            result.serverPort = Integer.parseInt(val);
                        } catch (NumberFormatException e) {
                            result.serverPort = 19132;
                        }
                        break;
                }
            }
        }
    }

    private static void parseParameters(MinecraftUri result, Uri uri) {
        if (result == null || uri == null || uri.isOpaque()) return;
        try {
            switch (result.action) {
                case ACTION_ADD_EXTERNAL_SERVER:
                    parseAddExternalServer(result, uri);
                    break;
                case ACTION_SHOW_STORE_OFFER:
                    result.storeOfferId = uri.getQueryParameter(ACTION_SHOW_STORE_OFFER);
                    break;
                case ACTION_SHOW_OFFER_COLLECTION:
                    result.collectionId = uri.getQueryParameter(ACTION_SHOW_OFFER_COLLECTION);
                    break;
                case ACTION_OPEN_MARKETPLACE_INVENTORY:
                    result.inventoryTab = uri.getQueryParameter(ACTION_OPEN_MARKETPLACE_INVENTORY);
                    break;
                case ACTION_OPEN_CSB_PDP_SCREEN:
                    result.csbTab = uri.getQueryParameter(ACTION_OPEN_CSB_PDP_SCREEN);
                    break;
                case ACTION_SHOW_DRESSING_ROOM_OFFER:
                    result.dressingRoomOfferId = uri.getQueryParameter("offerID");
                    break;
                case ACTION_JOIN_GATHERING:
                    result.gatheringId = uri.getQueryParameter("gatheringId");
                    break;
                case ACTION_ACCEPT_REALM_INVITE:
                    result.realmInviteId = uri.getQueryParameter("inviteID");
                    break;
                case ACTION_CONNECT_TO_REALM:
                    result.realmId = uri.getQueryParameter("realmId");
                    result.realmInviteId = uri.getQueryParameter("inviteID");
                    break;
                case ACTION_SLASH_COMMAND:
                    result.slashCommand = uri.getQueryParameter(ACTION_SLASH_COMMAND);
                    break;
                case ACTION_IMPORT:
                    result.importPath = uri.getQueryParameter(ACTION_IMPORT);
                    break;
                case ACTION_IMPORT_PACK:
                    result.importPath = uri.getQueryParameter(ACTION_IMPORT_PACK);
                    break;
                case ACTION_IMPORT_ADDON:
                    result.importPath = uri.getQueryParameter(ACTION_IMPORT_ADDON);
                    break;
                case ACTION_IMPORT_TEMPLATE:
                    result.importPath = uri.getQueryParameter(ACTION_IMPORT_TEMPLATE);
                    break;
                case ACTION_LOAD_WORLD:
                    result.localLevelId = uri.getQueryParameter(ACTION_LOAD_WORLD);
                    break;
                case ACTION_CONNECT:
                    parseConnect(result, uri);
                    break;
            }
        } catch (UnsupportedOperationException ignored) {
        }
    }

    private static void parseAddExternalServer(MinecraftUri result, Uri uri) {
        if (uri.isOpaque()) return;
        try {
            String serverParam = uri.getQueryParameter(ACTION_ADD_EXTERNAL_SERVER);
            parseAddExternalServerValue(result, serverParam);
        } catch (UnsupportedOperationException ignored) {
        }
    }

    private static void parseConnect(MinecraftUri result, Uri uri) {
        if (uri.isOpaque()) return;
        try {
            result.localLevelId = uri.getQueryParameter("localLevelId");
            result.localWorldName = uri.getQueryParameter("localWorld");
            result.serverIp = uri.getQueryParameter("serverUrl");
            String portStr = uri.getQueryParameter("serverPort");
            if (portStr != null) {
                try {
                    result.serverPort = Integer.parseInt(portStr);
                } catch (NumberFormatException e) {
                    result.serverPort = 19132;
                }
            }
        } catch (UnsupportedOperationException ignored) {
        }
    }

    @Nullable
    private static String getFirstQueryParam(Uri uri) {
        String query = uri.getEncodedQuery();
        if (query == null || query.isEmpty()) return null;
        int eqIndex = query.indexOf('=');
        return eqIndex > 0 ? query.substring(0, eqIndex) : query;
    }

    @NonNull
    public static Uri buildShowHowToPlay() {
        return Uri.parse(SCHEME + "://?" + ACTION_HOW_TO_PLAY + "=1");
    }

    @NonNull
    public static Uri buildAddExternalServer(@NonNull String name, @NonNull String ip, int port) {
        return Uri.parse(SCHEME + "://?" + ACTION_ADD_EXTERNAL_SERVER + "=" + Uri.encode(name) + "|" + ip + ":" + port);
    }

    @NonNull
    public static Uri buildOpenStore() {
        return Uri.parse(SCHEME + "://" + ACTION_OPEN_STORE);
    }

    @NonNull
    public static Uri buildShowMinecoinOffers() {
        return Uri.parse(SCHEME + "://?" + ACTION_SHOW_MINECOIN_OFFERS + "=1");
    }

    @NonNull
    public static Uri buildOpenCsbPdpScreen(@NonNull String tab) {
        return Uri.parse(SCHEME + "://?" + ACTION_OPEN_CSB_PDP_SCREEN + "=" + tab);
    }

    @NonNull
    public static Uri buildOpenServersTab() {
        return Uri.parse(SCHEME + "://" + ACTION_OPEN_SERVERS_TAB);
    }

    @NonNull
    public static Uri buildShowProfileScreen() {
        return Uri.parse(SCHEME + "://" + ACTION_SHOW_PROFILE_SCREEN);
    }

    @NonNull
    public static Uri buildAcceptRealmInvite(@NonNull String inviteCode) {
        return Uri.parse(SCHEME + "://" + ACTION_ACCEPT_REALM_INVITE + "?inviteID=" + inviteCode);
    }

    @NonNull
    public static Uri buildSlashCommand(@NonNull String command) {
        return Uri.parse(SCHEME + "://?" + ACTION_SLASH_COMMAND + "=" + Uri.encode(command));
    }

    @NonNull
    public static Uri buildConnectLocalWorld(@NonNull String worldName) {
        return Uri.parse(SCHEME + "://" + ACTION_CONNECT + "/?localWorld=" + Uri.encode(worldName));
    }

    @NonNull
    public static Uri buildConnectServer(@NonNull String serverUrl, int port) {
        return Uri.parse(SCHEME + "://" + ACTION_CONNECT + "/?serverUrl=" + serverUrl + "&serverPort=" + port);
    }

    public static boolean isMinecraftUri(@Nullable Uri uri) {
        return uri != null && SCHEME.equals(uri.getScheme());
    }
}
