package com.microsoft.xbox.xle.app;

import android.annotation.SuppressLint;

import com.microsoft.xboxtcui.XboxTcuiSdk;

import org.jetbrains.annotations.Nullable;
import org.spongycastle.crypto.tls.CipherSuite;

import java.net.URI;

/**
 * 07.01.2021
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */

public class ImageUtil {
    public static final int LARGE_PHONE = 640;
    public static final int LARGE_TABLET = 800;
    public static final int MEDIUM_PHONE = 300;
    public static final int MEDIUM_TABLET = 424;
    public static final int SMALL = 200;
    public static final int TINY = 100;
    public static final String resizeFormatter = "&w=%d&h=%d&format=png";
    public static final String resizeFormatterSizeOnly = "&w=%d&h=%d";
    public static final String resizeFormatterWithPadding = "&mode=padding&w=%d&h=%d&format=png";

    public static URI getURI(URI uri, int i, int i2) {
        return formatURI(uri, i, i2);
    }

    public static URI getURI(String str, int i, int i2) {
        URI formatString = formatString(str, i, i2);
        return formatString == null ? createUri(str) : formatString;
    }

    public static URI getTiny(URI uri) {
        return formatURI(uri, 100, 100);
    }

    public static URI getTiny(String str) {
        URI formatString = formatString(str, 100, 100);
        return formatString == null ? createUri(str) : formatString;
    }

    public static URI getTiny3X4(URI uri) {
        return formatURI(uri, 85, 120);
    }

    public static URI getTiny3X4(String str) {
        return formatString(str, 85, 120);
    }

    public static URI getTiny4X3(URI uri) {
        return formatURI(uri, 120, 90);
    }

    public static URI getTiny4X3(String str) {
        return formatString(str, 120, 90);
    }

    public static URI getTiny2X1(URI uri) {
        return formatURI(uri, CipherSuite.TLS_RSA_WITH_SEED_CBC_SHA, 84);
    }

    public static URI getTiny2X1(String str) {
        return formatString(str, CipherSuite.TLS_RSA_WITH_SEED_CBC_SHA, 84);
    }

    public static URI getSmall(URI uri) {
        return formatURI(uri, 200, 200);
    }

    public static URI getSmall(String str) {
        URI formatString = formatString(str, 200, 200);
        return (formatString != null || str == null) ? formatString : createUri(str);
    }

    public static URI getSmall3X4(URI uri) {
        return formatURI(uri, 215, 294);
    }

    public static URI getSmall3X4(String str) {
        return formatString(str, 215, 294);
    }

    public static URI getSmall4X3(URI uri) {
        return formatURI(uri, 275, 216);
    }

    public static URI getSmall4X3(String str) {
        return formatString(str, 275, 216);
    }

    public static URI getSmall2X1(URI uri) {
        return formatURI(uri, 243, CipherSuite.TLS_DH_anon_WITH_CAMELLIA_256_CBC_SHA);
    }

    public static URI getSmall2X1(String str) {
        return formatString(str, 243, CipherSuite.TLS_DH_anon_WITH_CAMELLIA_256_CBC_SHA);
    }

    public static URI getMedium(URI uri) {
        if (XboxTcuiSdk.getIsTablet()) {
            return formatURI(uri, 424, 424);
        }
        return formatURI(uri, 300, 300);
    }

    public static URI getMedium(String str) {
        URI uri;
        if (XboxTcuiSdk.getIsTablet()) {
            uri = formatString(str, 424, 424);
        } else {
            uri = formatString(str, 300, 300);
        }
        return (uri != null || str == null) ? uri : createUri(str);
    }

    public static URI getMedium3X4(URI uri) {
        return formatURI(uri, 426, LARGE_PHONE);
    }

    public static URI getMedium3X4(String str) {
        return formatString(str, 426, LARGE_PHONE);
    }

    public static URI getMedium4X3(URI uri) {
        return formatURI(uri, 562, 316);
    }

    public static URI getMedium4X3(String str) {
        return formatString(str, 562, 316);
    }

    public static URI getMedium2X1(URI uri) {
        return formatURI(uri, 480, 270);
    }

    public static URI getMedium2X1(String str) {
        return formatString(str, 480, 270);
    }

    public static URI getLarge(URI uri) {
        if (XboxTcuiSdk.getIsTablet()) {
            return formatURI(uri, LARGE_TABLET, LARGE_TABLET);
        }
        return formatURI(uri, LARGE_PHONE, LARGE_PHONE);
    }

    public static URI getLarge(String str) {
        URI uri;
        if (XboxTcuiSdk.getIsTablet()) {
            uri = formatString(str, LARGE_TABLET, LARGE_TABLET);
        } else {
            uri = formatString(str, LARGE_PHONE, LARGE_PHONE);
        }
        return (uri != null || str == null) ? uri : createUri(str);
    }

    public static URI getLarge3X4(URI uri) {
        return formatURI(uri, 720, 1080);
    }

    public static URI getLarge3X4(String str) {
        return formatString(str, 720, 1080);
    }

    private static URI formatURI(URI uri, int i, int i2) {
        if (uri == null) {
            return null;
        }
        URI formatString = formatString(uri.toString(), i, i2);
        return formatString == null ? uri : formatString;
    }

    @SuppressLint("DefaultLocale")
    private static URI formatString(String str, int i, int i2) {
        if (str == null || !str.contains("images-eds")) {
            return null;
        }
        boolean contains = str.contains("&w=");
        boolean contains2 = str.contains("&h=");
        if (contains && contains2) {
            String replaceAll = str.replaceAll("w=[0-9]+", "w=" + i);
            return createUri(replaceAll.replaceAll("h=[0-9]+", "h=" + i2));
        } else if (contains) {
            return createUri(str.replaceAll("w=[0-9]+", "w=" + i) + "&h=" + i2);
        } else if (contains2) {
            return createUri(str.replaceAll("h=[0-9]+", "h=" + i2) + "&w=" + i);
        } else if (str.contains("format=")) {
            return createUri(str + String.format(resizeFormatterSizeOnly, Integer.valueOf(i), Integer.valueOf(i2)));
        } else {
            return createUri(str + String.format(resizeFormatter, Integer.valueOf(i), Integer.valueOf(i2)));
        }
    }

    private static URI createUri(String str) {
        if (str != null) {
            try {
                return URI.create(str);
            } catch (IllegalArgumentException unused) {
            }
        }
        return null;
    }

    public static URI getUri(String url, ImageType imgType) {
        if (imgType == null) {
            return getSmall(url);
        }
        switch (imgType) {
            case TINY:
                return getTiny(url);
            case TINY_3X4:
                return getTiny3X4(url);
            case TINY_4X3:
                return getTiny4X3(url);
            case SMALL:
                return getSmall(url);
            case SMALL_3X4:
                return getSmall3X4(url);
            case SMALL_4X3:
                return getSmall4X3(url);
            case MEDIUM:
                return getMedium(url);
            case MEDIUM_3X4:
                return getMedium3X4(url);
            case MEDIUM_4X3:
                return getMedium4X3(url);
            case LARGE:
                return getLarge(url);
            case LARGE_3X4:
                return getLarge3X4(url);
            default:
                return getSmall(url);
        }
    }

    public static URI getUri(URI url, ImageType imgType) {
        if (imgType == null) {
            return getSmall(url);
        }
        switch (imgType) {
            case TINY:
                return getTiny(url);
            case TINY_3X4:
                return getTiny3X4(url);
            case TINY_4X3:
                return getTiny4X3(url);
            case SMALL:
                return getSmall(url);
            case SMALL_3X4:
                return getSmall3X4(url);
            case SMALL_4X3:
                return getSmall4X3(url);
            case MEDIUM:
                return getMedium(url);
            case MEDIUM_3X4:
                return getMedium3X4(url);
            case MEDIUM_4X3:
                return getMedium4X3(url);
            case LARGE:
                return getLarge(url);
            case LARGE_3X4:
                return getLarge3X4(url);
            default:
                return getSmall(url);
        }
    }

    public enum ImageType {
        TINY,
        TINY_3X4,
        TINY_4X3,
        SMALL,
        SMALL_3X4,
        SMALL_4X3,
        MEDIUM,
        MEDIUM_3X4,
        MEDIUM_4X3,
        LARGE,
        LARGE_3X4;

        public static @Nullable ImageType fromString(String str) {
            try {
                return valueOf(str);
            } catch (IllegalArgumentException | NullPointerException unused) {
                return null;
            }
        }
    }
}
