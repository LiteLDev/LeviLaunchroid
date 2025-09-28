package com.microsoft.xboxtcui;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;

import com.microsoft.xbox.toolkit.XLEAssert;

/**
 * 02.10.2020
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */

public final class XboxTcuiSdk {
    private static Activity activity;
    private static Context applicationContext;
    private static AssetManager assetManager;
    private static ContentResolver contentResolver;
    private static Resources resources;

    public static synchronized void sdkInitialize(Activity activity2) {
        synchronized (XboxTcuiSdk.class) {
            XLEAssert.assertNotNull(activity2);
            activity = activity2;
        }
    }

    public static Activity getActivity() {
        XLEAssert.assertNotNull(activity);
        return activity;
    }

    public static Context getApplicationContext() {
        XLEAssert.assertNotNull(activity);
        if (applicationContext == null) {
            applicationContext = activity.getApplicationContext();
        }
        return applicationContext;
    }

    public static Resources getResources() {
        XLEAssert.assertNotNull(activity);
        if (resources == null) {
            resources = activity.getResources();
        }
        return resources;
    }

    public static Object getSystemService(String name) {
        XLEAssert.assertNotNull(activity);
        return activity.getSystemService(name);
    }

    public static ContentResolver getContentResolver() {
        XLEAssert.assertNotNull(activity);
        if (contentResolver == null) {
            contentResolver = activity.getContentResolver();
        }
        return contentResolver;
    }

    public static AssetManager getAssetManager() {
        XLEAssert.assertNotNull(activity);
        if (assetManager == null) {
            assetManager = activity.getAssets();
        }
        return assetManager;
    }

    public static boolean getIsTablet() {
        return false;
    }
}