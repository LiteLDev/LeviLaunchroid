package com.microsoft.xbox.idp.util;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.util.Log;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 07.01.2021
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */

public class XboxAppLinker {
    public static final String XBOXAPP_BETA_PACKAGE = "com.microsoft.xboxone.smartglass.beta";
    public static final String XBOXAPP_PACKAGE = "com.microsoft.xboxone.smartglass";
    private static final String AMAZON_STORE_URI = "amzn://apps/android?p=";
    private static final String AMAZON_TABLET_STORE_PACKAGE = "com.amazon.venezia";
    private static final String AMAZON_UNDERGROUND_PACKAGE = "com.amazon.mShop.android";
    private static final String OCULUS_STORE_WEB_URI = "oculus.store://link/products?referrer=manual&item_id=";
    private static final String OCULUS_XBOXAPP_APP_ID = "1193603937358048";
    private static final String PLAY_STORE_PACKAGE = "com.android.vending";
    private static final String PLAY_STORE_URI = "market://details?id=";
    private static final String PLAY_STORE_WEB_URI = "https://play.google.com/store/apps/details?id=";
    private static final String TAG = XboxAppLinker.class.getSimpleName();
    public static boolean betaAppInstalled;
    public static boolean mainAppInstalled;

    public static void launchXboxAppStorePage(@NotNull Context context) {
        context.startActivity(getXboxAppInAnyMarketIntent(context));
    }

    @SuppressLint("WrongConstant")
    public static boolean isInstalled(@NotNull Context context, String str) {
        try {
            context.getPackageManager().getPackageInfo(str, 1);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean isServiceInstalled(String str, @NotNull Context context, String str2) {
        try {
            context.getPackageManager().getServiceInfo(new ComponentName(str, str2), 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            Log.i(TAG, e.getClass().toString());
            Log.i(TAG, e.getMessage());
            return false;
        }
    }

    public static boolean xboxAppIsInstalled(Context context) {
        mainAppInstalled = isInstalled(context, XBOXAPP_PACKAGE);
        betaAppInstalled = isInstalled(context, XBOXAPP_BETA_PACKAGE);
        return mainAppInstalled || betaAppInstalled;
    }

    @SuppressLint("WrongConstant")
    public static @NotNull Intent getXboxAppInOculusMarketIntent(Context context) {
        Intent intent = new Intent("android.intent.action.VIEW", Uri.parse("oculus.store://link/products?referrer=manual&item_id=1193603937358048"));
        intent.setFlags(270565376);
        return intent;
    }

    @SuppressLint("WrongConstant")
    public static @NotNull Intent getXboxAppInAnyMarketIntent(Context context) {
        Intent xboxAppInMarketIntent = getXboxAppInMarketIntent(context, PLAY_STORE_URI, PLAY_STORE_PACKAGE);
        if (xboxAppInMarketIntent == null) {
            xboxAppInMarketIntent = getXboxAppInMarketIntent(context, AMAZON_STORE_URI, AMAZON_UNDERGROUND_PACKAGE);
        }
        if (xboxAppInMarketIntent == null) {
            xboxAppInMarketIntent = getXboxAppInMarketIntent(context, AMAZON_STORE_URI, AMAZON_TABLET_STORE_PACKAGE);
        }
        if (xboxAppInMarketIntent == null) {
            xboxAppInMarketIntent = new Intent("android.intent.action.VIEW", Uri.parse("https://play.google.com/store/apps/details?id=com.microsoft.xboxone.smartglass"));
        }
        xboxAppInMarketIntent.setFlags(270565376);
        return xboxAppInMarketIntent;
    }

    @SuppressLint("WrongConstant")
    public static @Nullable Intent getXboxAppInMarketIntent(@NotNull Context context, String str, String str2) {
        Intent intent = new Intent("android.intent.action.VIEW", Uri.parse(str + XBOXAPP_PACKAGE));
        for (ResolveInfo next : context.getPackageManager().queryIntentActivities(intent, 0)) {
            if (next.activityInfo.applicationInfo.packageName.equals(str2)) {
                ActivityInfo activityInfo = next.activityInfo;
                ComponentName componentName = new ComponentName(activityInfo.applicationInfo.packageName, activityInfo.name);
                intent.setFlags(270532608);
                intent.setComponent(componentName);
                return intent;
            }
        }
        return null;
    }

    public static Intent getAppIntent(@NotNull Context context, String str) {
        return context.getPackageManager().getLaunchIntentForPackage(str);
    }

    public static Intent getXboxAppLaunchIntent(Context context) {
        if (betaAppInstalled) {
            return context.getPackageManager().getLaunchIntentForPackage(XBOXAPP_BETA_PACKAGE);
        }
        return context.getPackageManager().getLaunchIntentForPackage(XBOXAPP_PACKAGE);
    }
}
