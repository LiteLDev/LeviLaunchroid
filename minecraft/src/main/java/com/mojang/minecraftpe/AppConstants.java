package com.mojang.minecraftpe;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

/**
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */
public class AppConstants {
    public static String ANDROID_BUILD;
    public static String ANDROID_VERSION;
    public static String APP_PACKAGE;
    public static int APP_VERSION;
    public static String APP_VERSION_NAME;
    public static String PHONE_MANUFACTURER;
    public static String PHONE_MODEL;
    private static AsyncTask<Void, Object, String> loadIdentifiersTask;

    public static void loadFromContext(Context context) {
        ANDROID_VERSION = Build.VERSION.RELEASE;
        ANDROID_BUILD = Build.DISPLAY;
        PHONE_MODEL = Build.MODEL;
        PHONE_MANUFACTURER = Build.MANUFACTURER;
        loadPackageData(context);
    }

    private static void loadPackageData(Context context) {
        if (context != null) {
            try {
                PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
                APP_PACKAGE = packageInfo.packageName;
                APP_VERSION = packageInfo.versionCode;
                APP_VERSION_NAME = packageInfo.versionName;
            } catch (PackageManager.NameNotFoundException e) {
            }
        }
    }
}
