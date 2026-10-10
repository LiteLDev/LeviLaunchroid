package com.microsoft.xal.androidjava;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.AtomicFile;
import android.util.Base64;

import org.jetbrains.annotations.NotNull;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;

public class Storage {
    @NotNull
    public static String getStoragePath(@NotNull Context context) {
        boolean takeover = false;
        try {
            SharedPreferences sp = context.getSharedPreferences("feature_settings", Context.MODE_PRIVATE);
            String json = sp.getString("settings_json", null);
            if (json != null) {
                JSONObject obj = new JSONObject(json);
                takeover = obj.optBoolean("launcherManagedMcLoginEnabled", false);
            }
        } catch (Throwable ignored) {
        }

        File xalRoot;
        if (takeover) {
            File internalXal = new File(context.getApplicationContext().getFilesDir(), "xal");

            if (!internalXal.exists()) internalXal.mkdirs();
            xalRoot = internalXal;

            String activeMsUserId = null;
            File accountsFile = new File(xalRoot, "Xal.Accounts.json");
            if (accountsFile.exists() || new File(accountsFile.getPath() + ".bak").exists()) {
                try (FileInputStream fis = new AtomicFile(accountsFile).openRead();
                     ByteArrayOutputStream body = new ByteArrayOutputStream()) {
                    byte[] buffer = new byte[8192];
                    int count;
                    while ((count = fis.read(buffer)) != -1) {
                        body.write(buffer, 0, count);
                    }
                    JSONArray arr = new JSONArray(body.toString(StandardCharsets.UTF_8.name()));
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject account = arr.optJSONObject(i);
                        if (account == null || !account.optBoolean("active", false)) continue;
                        String userId = account.optString("msUserId", "");
                        if (!userId.isEmpty()) {
                            activeMsUserId = userId;
                            break;
                        }
                    }
                } catch (Exception ignored) {}
            }

            if (activeMsUserId != null && !activeMsUserId.isEmpty()) {
                String b64 = Base64.encodeToString(
                        activeMsUserId.getBytes(StandardCharsets.UTF_8),
                       Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP
                );
                File userDir = new File(xalRoot, b64);
                if (!userDir.exists()) userDir.mkdirs();
                return userDir.getAbsolutePath();
            }
            return xalRoot.getAbsolutePath();
        }

        return context.getApplicationContext().getFilesDir().getPath();
    }
}
