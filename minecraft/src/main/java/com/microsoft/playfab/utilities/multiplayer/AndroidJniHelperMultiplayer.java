package com.microsoft.playfab.utilities.multiplayer;

import android.content.Context;
import com.microsoft.applications.events.Constants;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * @author <a href="https://github.com/RadiantByte">RadiantByte</a>
 */

public class AndroidJniHelperMultiplayer {
    public static String createUUID() {
        return UUID.randomUUID().toString();
    }

    public static void executeWebRequest(String str, String str2, String str3, HashMap<String, String> map, byte[] bArr, byte[] bArr2) {
        HttpRequestMultiplayer httpRequestMultiplayer = new HttpRequestMultiplayer();
        httpRequestMultiplayer.setHttpUrl(str2);
        httpRequestMultiplayer.setHttpHeader("User-Agent", str3);
        for (Map.Entry<String, String> entry : map.entrySet()) {
            httpRequestMultiplayer.setHttpHeader(entry.getKey(), entry.getValue());
        }
        httpRequestMultiplayer.setHttpMethodAndBody(str, map.get("Content-Type"), bArr);
        httpRequestMultiplayer.setContext(bArr2);
        httpRequestMultiplayer.doAsyncRequest();
    }

    public static String getDefaultLanguage() {
        Locale locale = Locale.getDefault();
        StringBuilder sb = new StringBuilder(locale.getLanguage());
        String country = locale.getCountry();
        if (!country.isEmpty()) {
            sb.append(Constants.CONTEXT_SCOPE_NONE).append(country);
        }
        return sb.toString();
    }

    public static byte[] getBufferFromFile(Context context, String str) throws IOException {
        try {
            InputStream inputStreamOpen = context.getAssets().open(str);
            byte[] bArr = new byte[inputStreamOpen.available()];
            inputStreamOpen.read(bArr);
            inputStreamOpen.close();
            return bArr;
        } catch (IOException unused) {
            return null;
        }
    }
}
