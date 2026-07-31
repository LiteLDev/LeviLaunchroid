package org.levimc.launcher.core.auth;

import android.content.Context;
import android.util.Pair;
import android.util.Log;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.lenni0451.commons.httpclient.HttpClient;
import net.raphimc.minecraftauth.bedrock.BedrockAuthManager;
import net.raphimc.minecraftauth.msa.model.MsaApplicationConfig;
import net.raphimc.minecraftauth.msa.request.MsaAuthCodeTokenRequest;
import net.raphimc.minecraftauth.msa.model.MsaToken;
import net.raphimc.minecraftauth.msa.data.MsaConstants;

public class MsftAuthManager {
    private static final String TAG = "MsftAuthManager";

    public static final String DEFAULT_CLIENT_ID = MsaConstants.BEDROCK_ANDROID_TITLE_ID;
    public static final String DEFAULT_SCOPE = MsaConstants.SCOPE_TITLE_AUTH;

    public static MsaApplicationConfig getAppConfig() {
        return new MsaApplicationConfig(DEFAULT_CLIENT_ID, DEFAULT_SCOPE, null, "https://login.live.com/oauth20_desktop.srf", net.raphimc.minecraftauth.msa.data.MsaEnvironment.LIVE);
    }

    public static String buildAuthorizeUrl(String state) {
        MsaApplicationConfig config = getAppConfig();
        return "https://login.live.com/oauth20_authorize.srf?" +
                "client_id=" + config.getClientId() +
                "&response_type=code" +
                "&scope=" + config.getScope().replace(" ", "%20") +
                "&redirect_uri=" + config.getRedirectUri() +
                "&state=" + state;
    }

    public static BedrockAuthManager loginWithCode(String code) throws Exception {
        HttpClient httpClient = new HttpClient();
        MsaApplicationConfig config = getAppConfig();
        MsaToken token = httpClient.executeAndHandle(new MsaAuthCodeTokenRequest(config, code));
        return BedrockAuthManager.create(httpClient, "1.21.110").login(token);
    }

    public static BedrockAuthManager refreshAndAuth(MsftAccountStore.MsftAccount account) throws Exception {
        if (account == null || account.serializedAuthManager == null) {
            throw new IllegalArgumentException("Account does not have valid serialized auth manager data.");
        }
        HttpClient httpClient = new HttpClient();
        JsonObject json = JsonParser.parseString(account.serializedAuthManager).getAsJsonObject();
        BedrockAuthManager authManager = BedrockAuthManager.fromJson(httpClient, "1.21.110", json);
        
        // This will force a refresh of the tokens if they are expired
        authManager.getMinecraftCertificateChain().getUpToDate();
        
        return authManager;
    }

    public static void saveAccount(Context ctx, BedrockAuthManager authManager) {
        try {
            String msaUserId = authManager.getXboxUserProfile().getUpToDate().getId();
            if (msaUserId == null) msaUserId = "unknown";
            
            String gamertag = authManager.getXboxUserProfile().getUpToDate().getSettings().get("Gamertag");
            String avatarUrl = authManager.getXboxUserProfile().getUpToDate().getSettings().get("AppDisplayPicRaw");
            
            String minecraftUsername = authManager.getMinecraftCertificateChain().getUpToDate().getIdentityDisplayName();
            String xuid = authManager.getMinecraftCertificateChain().getUpToDate().getIdentityXuid();
            
            try {
                authManager.getPlayFabXstsToken().getUpToDate();
                authManager.getRealmsXstsToken().getUpToDate();
                authManager.getXboxLiveXstsToken().getUpToDate();
            } catch (Exception ignored) {}
            
            String serialized = BedrockAuthManager.toJson(authManager).toString();
            MsftAccountStore.addOrUpdate(ctx, msaUserId, gamertag, minecraftUsername, xuid, avatarUrl, serialized);
        } catch (Exception e) {
            Log.e(TAG, "Failed to save account", e);
        }
    }
}
