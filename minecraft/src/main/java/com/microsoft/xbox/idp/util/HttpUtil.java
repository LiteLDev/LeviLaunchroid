package com.microsoft.xbox.idp.util;

import android.net.Uri;
import android.text.TextUtils;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;


public class HttpUtil {

    public static Uri.Builder getImageSizeUrlParams(@NotNull Uri.Builder builder, @NotNull ImageSize imageSize) {
        return builder.appendQueryParameter("w", Integer.toString(imageSize.w)).appendQueryParameter("h", Integer.toString(imageSize.h));
    }

    public static @NotNull String getEndpoint(@NotNull Uri uri) {
        return uri.getScheme() + "://" + uri.getEncodedAuthority();
    }

    public static @NotNull String getPathAndQuery(@NotNull Uri uri) {
        String encodedPath = uri.getEncodedPath();
        String encodedQuery = uri.getEncodedQuery();
        String encodedFragment = uri.getEncodedFragment();
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(encodedPath);
        if (!TextUtils.isEmpty(encodedQuery)) {
            stringBuffer.append("?");
            stringBuffer.append(encodedQuery);
        }
        if (!TextUtils.isEmpty(encodedFragment)) {
            stringBuffer.append("#");
            stringBuffer.append(encodedFragment);
        }
        return stringBuffer.toString();
    }

    @Contract("_, _ -> param1")
    public static @NotNull HttpCall appendCommonParameters(@NotNull HttpCall httpCall, String str) {
        httpCall.setXboxContractVersionHeaderValue(str);
        httpCall.setContentTypeHeaderValue("application/json");
        httpCall.setRetryAllowed(true);
        return httpCall;
    }

    public enum ImageSize {
        SMALL(64, 64),
        MEDIUM(208, 208),
        LARGE(424, 424);

        public final int h;
        public final int w;

        ImageSize(int i, int i2) {
            this.w = i;
            this.h = i2;
        }
    }
}
