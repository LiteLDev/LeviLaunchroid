package com.microsoft.xal.browser;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.Signature;
import android.net.Uri;
import android.util.Base64;

import androidx.browser.customtabs.CustomTabsService;
import androidx.core.os.EnvironmentCompat;

import com.microsoft.xal.logging.XalLogger;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * @author <a href="https://github.com/dreamguxiang">dreamguxiang</a>
 */

public class BrowserSelector {

    private static final Map<String, String> customTabsAllowedBrowsers;

    static {
        customTabsAllowedBrowsers = new HashMap<>();
        customTabsAllowedBrowsers.put("com.android.chrome", "OJGKRT0HGZNU+LGa8F7GViztV4g=");
        customTabsAllowedBrowsers.put("org.mozilla.firefox", "kg9Idqale0pqL0zK9l99Kc4m/yw=");
        customTabsAllowedBrowsers.put("com.microsoft.emmx", "P2QOJ59jvOpxCCrn6MfvotoBTK0=");
        customTabsAllowedBrowsers.put("com.sec.android.app.sbrowser", "nKUXDzgZGd/gRG/NqxixmhQ7MWM=");
    }

    public static BrowserSelectionResult selectBrowser(Context context, boolean inProcRequested) {
        XalLogger log = new XalLogger("BrowserSelector");

        try {
            BrowserSelectionResult.BrowserInfo userDefault = getUserDefaultBrowserInfo(context, log);
            boolean customTabsAllowed = false;
            String reason;

            if (inProcRequested) {
                reason = "inProcRequested";
            } else if (browserInfoImpliesNoUserDefault(userDefault)) {
                reason = "noDefault";
            } else {
                String pkg = userDefault.packageName;
                if (!browserSupportsCustomTabs(context, pkg)) {
                    log.Important("Default browser does not support Custom Tabs.");
                    reason = "CTNotSupported";
                } else if (!browserAllowedForCustomTabs(context, log, pkg)) {
                    log.Important("Default browser supports Custom Tabs but is not explicitly allowed.");
                    reason = "CTSupportedButNotAllowed";
                } else {
                    log.Important("Default browser supports Custom Tabs and is allowed.");
                    reason = "CTSupportedAndAllowed";
                    customTabsAllowed = true;
                }
            }

            return new BrowserSelectionResult(userDefault, reason, customTabsAllowed);
        } catch (Throwable t) {
            log.Error("BrowserSelector.selectBrowser() exception: " + t);
            return new BrowserSelectionResult(
                    new BrowserSelectionResult.BrowserInfo("none", 0,"none"),
                    "Exception",
                    false);
        } finally {
            log.close();
        }
    }

    private static BrowserSelectionResult.BrowserInfo getUserDefaultBrowserInfo(Context context, XalLogger log) {
        ResolveInfo resolved = context.getPackageManager()
                .resolveActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://microsoft.com")), PackageManager.MATCH_DEFAULT_ONLY);

        String pkgName = resolved == null ? null : resolved.activityInfo.packageName;
        if (pkgName == null) {
            log.Important("No default browser resolved.");
            return new BrowserSelectionResult.BrowserInfo("none", 0, "none");
        }

        if (pkgName.equals("android")) {
            log.Important("System resolved as default browser.");
            return new BrowserSelectionResult.BrowserInfo("android", 0, "none");
        }

        int versionCode = -1;
        String versionName;
        try {
            PackageInfo pkg = context.getPackageManager().getPackageInfo(pkgName, 0);
            versionCode = pkg.versionCode;
            versionName = pkg.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            log.Error("Error in getPackageInfo(): " + e);
            versionName = EnvironmentCompat.MEDIA_UNKNOWN;
        }

        log.Important("User default browser: " + pkgName);
        return new BrowserSelectionResult.BrowserInfo(pkgName, versionCode, versionName);
    }

    private static boolean browserInfoImpliesNoUserDefault(BrowserSelectionResult.BrowserInfo info) {
        return info.versionCode == 0 && "none".equals(info.versionName);
    }

    private static boolean browserAllowedForCustomTabs(Context context, XalLogger log, String pkgName)
            throws PackageManager.NameNotFoundException, NoSuchAlgorithmException {

        String expectedHash = customTabsAllowedBrowsers.get(pkgName);
        if (expectedHash == null) {
            return false;
        }

        PackageInfo pkg = context.getPackageManager().getPackageInfo(pkgName, PackageManager.GET_SIGNATURES);
        if (pkg.signatures == null) {
            log.Important("No signatures found for package: " + pkgName);
            return false;
        }

        for (Signature sig : pkg.signatures) {
            if (hashFromSignature(sig).equals(expectedHash)) {
                return true;
            }
        }
        return false;
    }

    private static boolean browserSupportsCustomTabs(Context context, String pkgName) {
        List<ResolveInfo> services =
                context.getPackageManager().queryIntentServices(
                        new Intent(CustomTabsService.ACTION_CUSTOM_TABS_CONNECTION), 0);

        if (services == null) return false;

        for (ResolveInfo info : services) {
            if (info.serviceInfo != null && pkgName.equals(info.serviceInfo.packageName)) {
                return true;
            }
        }
        return false;
    }

    private static String hashFromSignature(Signature sig) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA");
        digest.update(sig.toByteArray());
        return Base64.encodeToString(digest.digest(), Base64.NO_WRAP);
    }
}