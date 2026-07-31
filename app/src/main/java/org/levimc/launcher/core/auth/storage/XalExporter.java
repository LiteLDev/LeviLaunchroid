package org.levimc.launcher.core.auth.storage;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.levimc.launcher.core.auth.MsftAccountStore;
import org.levimc.launcher.util.JsonIOUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class XalExporter {
    private static final String TAG = "XalExporter";
    private static final Gson GSON = new Gson();

    public static void exportActiveAccount(Context ctx) {
        try {
            MsftAccountStore.MsftAccount active = null;
            for (MsftAccountStore.MsftAccount acc : MsftAccountStore.list(ctx)) {
                if (acc.active) {
                    active = acc;
                    break;
                }
            }

            if (active == null || active.serializedAuthManager == null) {
                clearXalData(ctx);
                return;
            }

            JsonObject authJson = JsonParser.parseString(active.serializedAuthManager).getAsJsonObject();
            export(ctx, authJson, active.msUserId, active.xboxGamertag, active.xuid);

        } catch (Exception e) {
            Log.e(TAG, "Failed to export XAL data", e);
        }
    }

    private static void clearXalData(Context ctx) {
        File xalDir = new File(ctx.getApplicationContext().getFilesDir(), "xal");
        deleteDirectory(xalDir);
        SharedPreferences.Editor edit = ctx.getSharedPreferences("org.levimc.xal.crypto", Context.MODE_PRIVATE).edit();
        edit.clear();
        edit.apply();
    }

    private static boolean deleteDirectory(File dir) {
        if (dir == null || !dir.exists()) return false;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        return dir.delete();
    }

    public static void export(Context ctx, JsonObject authJson, String msaUserId, String gamertag, String xuid) {
        File root = new File(ctx.getApplicationContext().getFilesDir(), "xal");
        if (!root.exists()) root.mkdirs();

        String b64User = Base64.encodeToString(msaUserId.getBytes(StandardCharsets.UTF_8),
                Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP);
        File userDir = new File(root, b64User);
        if (!userDir.exists()) userDir.mkdirs();

        String tid = "1739947436";

        // DeviceIdentity.json
        if (authJson.has("deviceId") && !authJson.get("deviceId").isJsonNull()) {
            String deviceId = authJson.get("deviceId").getAsString();
            JsonObject di = new JsonObject();
            di.addProperty("Id", "{" + deviceId + "}");
            di.addProperty("Key", "Serialized to SharedPreferences");
            File diFile = new File(userDir, "Xal.Production.RETAIL.DeviceIdentity.json");
            JsonIOUtils.write(diFile, GSON.toJson(di));
        }

        // Default.json
        JsonObject def = new JsonObject();
        def.addProperty("default", msaUserId);
        File defFile = new File(userDir, "Xal." + tid + ".Production.Default.json");
        JsonIOUtils.write(defFile, GSON.toJson(def));

        // Msa.json
        if (authJson.has("msaToken")) {
            JsonObject msaJson = authJson.getAsJsonObject("msaToken");
            JsonObject rootMsa = new JsonObject();
            rootMsa.addProperty("user_id", msaUserId);
            rootMsa.addProperty("refresh_token", msaJson.has("refreshToken") && !msaJson.get("refreshToken").isJsonNull() ? msaJson.get("refreshToken").getAsString() : "");
            rootMsa.addProperty("foci", "");

            JsonObject at = new JsonObject();
            String tokenVal = msaJson.has("accessToken") ? msaJson.get("accessToken").getAsString() : "";
            at.addProperty("access_token", "t=" + tokenVal);
            long expireTimeMs = msaJson.has("expireTimeMs") ? msaJson.get("expireTimeMs").getAsLong() : System.currentTimeMillis();
            at.addProperty("xal_expires", formatDate(expireTimeMs));
            at.addProperty("scopes", "service::user.auth.xboxlive.com::mbi_ssl");

            JsonArray ats = new JsonArray();
            ats.add(at);
            rootMsa.add("access_tokens", ats);
            File msaFile = new File(userDir, "Xal." + tid + ".Production.Msa." + b64User + ".json");
            JsonIOUtils.write(msaFile, GSON.toJson(rootMsa));
        }

        // User.json (User + XSTS tokens)
        JsonObject uRoot = new JsonObject();
        uRoot.addProperty("deviceId", "{" + (authJson.has("deviceId") ? authJson.get("deviceId").getAsString() : "") + "}");
        JsonArray tokens = new JsonArray();

        // XSTS tokens
        if (authJson.has("xboxLiveXstsToken")) tokens.add(buildXstsEnvelope("Xtoken", "http://xboxlive.com", msaUserId, gamertag, xuid, authJson.getAsJsonObject("xboxLiveXstsToken")));
        if (authJson.has("playFabXstsToken")) tokens.add(buildXstsEnvelope("Xtoken", "https://b980a380.minecraft.playfabapi.com/", msaUserId, gamertag, xuid, authJson.getAsJsonObject("playFabXstsToken")));
        if (authJson.has("realmsXstsToken")) tokens.add(buildXstsEnvelope("Xtoken", "https://pocket.realms.minecraft.net/", msaUserId, gamertag, xuid, authJson.getAsJsonObject("realmsXstsToken")));
        
        // User token
        if (authJson.has("xblUserToken")) tokens.add(buildXstsEnvelope("Utoken", "http://auth.xboxlive.com", msaUserId, gamertag, xuid, authJson.getAsJsonObject("xblUserToken")));

        uRoot.add("tokens", tokens);
        File uFile = new File(userDir, "Xal." + tid + ".Production.RETAIL.User." + b64User + ".json");
        JsonIOUtils.write(uFile, GSON.toJson(uRoot));

        // Write KeyPair to SharedPreferences
        if (authJson.has("deviceKeyPair")) {
            JsonObject kp = authJson.getAsJsonObject("deviceKeyPair");
            if (kp.has("publicKey") && kp.has("privateKey")) {
                SharedPreferences.Editor edit = ctx.getSharedPreferences("org.levimc.xal.crypto", Context.MODE_PRIVATE).edit();
                edit.putString("id", "{" + (authJson.has("deviceId") ? authJson.get("deviceId").getAsString() : "") + "}");
                
                String pubB64 = kp.get("publicKey").getAsString();
                String privB64 = kp.get("privateKey").getAsString();
                
                try {
                    byte[] pubBytes = android.util.Base64.decode(pubB64, android.util.Base64.DEFAULT);
                    byte[] privBytes = android.util.Base64.decode(privB64, android.util.Base64.DEFAULT);
                    edit.putString("public", Base64.encodeToString(pubBytes, Base64.NO_WRAP | Base64.NO_PADDING | Base64.URL_SAFE));
                    edit.putString("private", Base64.encodeToString(privBytes, Base64.NO_WRAP | Base64.NO_PADDING | Base64.URL_SAFE));
                } catch (Exception e) {
                    edit.putString("public", pubB64);
                    edit.putString("private", privB64);
                }
                edit.apply();
            }
        }
    }

    private static JsonObject buildXstsEnvelope(String identityType, String relyingParty, String msaUserId, String gamertag, String xuid, JsonObject tokenObj) {
        long expireTimeMs = tokenObj.has("expireTimeMs") ? tokenObj.get("expireTimeMs").getAsLong() : System.currentTimeMillis();
        String tokenStr = tokenObj.has("token") ? tokenObj.get("token").getAsString() : "";
        String uhs = tokenObj.has("userHash") ? tokenObj.get("userHash").getAsString() : "";

        JsonObject xui = new JsonObject();
        xui.addProperty("uhs", uhs);
        xui.addProperty("gtg", gamertag);
        xui.addProperty("mgt", "");
        xui.addProperty("mgs", "");
        xui.addProperty("umg", "");
        xui.addProperty("xid", xuid);
        xui.addProperty("agg", "");
        xui.addProperty("prv", "");
        xui.addProperty("usr", "");
        xui.addProperty("uer", "");
        xui.addProperty("utr", "");

        JsonArray xuiArr = new JsonArray();
        xuiArr.add(xui);

        JsonObject displayClaims = new JsonObject();
        displayClaims.add("xui", xuiArr);

        JsonObject data = new JsonObject();
        data.addProperty("Token", tokenStr);
        data.addProperty("NotAfter", formatDate(expireTimeMs));
        data.addProperty("IssueInstant", formatDate(expireTimeMs - 8 * 60 * 60 * 1000L)); // Approx 8 hours before expiry
        data.addProperty("ClientAttested", false);
        data.add("DisplayClaims", displayClaims);

        JsonObject env = new JsonObject();
        env.addProperty("MsaUserId", msaUserId);
        env.addProperty("HasSignInDisplayClaims", true);
        env.addProperty("IdentityType", identityType);
        env.addProperty("Environment", "Production");
        env.addProperty("Sandbox", "RETAIL");
        env.addProperty("TokenType", "JWT");
        env.addProperty("RelyingParty", relyingParty);
        env.addProperty("SubRelyingParty", "");
        env.add("TokenData", data);

        return env;
    }

    private static String formatDate(long millis) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
        return sdf.format(new Date(millis));
    }
}
