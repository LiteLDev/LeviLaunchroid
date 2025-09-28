package com.microsoft.xboxtcui;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;

import com.microsoft.xbox.telemetry.helpers.UTCDeepLink;
import com.microsoft.xbox.telemetry.utc.model.UTCNames;
import com.microsoft.xbox.toolkit.XLEAssert;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 02.10.2020
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */

public class XboxAppDeepLinker {
    public static final String ACTION_FIND_PEOPLE = "com.microsoft.xbox.action.ACTION_FIND_PEOPLE";
    private static final String ACTION_VIEW_ACHIEVEMENTS = "com.microsoft.xbox.action.ACTION_VIEW_ACHIEVEMENTS";
    private static final String ACTION_VIEW_GAME_PROFILE = "com.microsoft.xbox.action.ACTION_VIEW_GAME_PROFILE";
    private static final String ACTION_VIEW_SETTINGS = "com.microsoft.xbox.action.ACTION_VIEW_SETTINGS";
    private static final String ACTION_VIEW_USER_PROFILE = "com.microsoft.xbox.action.ACTION_VIEW_USER_PROFILE";
    private static final String AMAZON_FIRE_TV_MODEL_PREFIX = "AFT";
    private static final String AMAZON_MANUFACTURER = "Amazon";
    private static final String AMAZON_STORE_URI = "amzn://apps/android?p=";
    private static final String AMAZON_TABLET_STORE_PACKAGE = "com.amazon.venezia";
    private static final String AMAZON_UNDERGROUND_PACKAGE = "com.amazon.mShop.android";
    private static final String EXTRA_IS_XBOX360_GAME = "com.microsoft.xbox.extra.IS_XBOX360_GAME";
    private static final String EXTRA_TITLEID = "com.microsoft.xbox.extra.TITLEID";
    private static final String EXTRA_XUID = "com.microsoft.xbox.extra.XUID";
    private static final String OCULUS_STORE_WEB_URI = "oculus.store://link/products?referrer=manual&item_id=";
    private static final String OCULUS_XBOXAPP_APP_ID = "1193603937358048";
    private static final String PLAY_STORE_PACKAGE = "com.android.vending";
    private static final String PLAY_STORE_URI = "market://details?id=";
    private static final String PLAY_STORE_WEB_URI = "https://play.google.com/store/apps/details?id=";
    private static final String XBOXAPP_BETA_PACKAGE = "com.microsoft.xboxone.smartglass.beta";
    private static final String XBOXAPP_PACKAGE = "com.microsoft.xboxone.smartglass";
    private static boolean betaAppInstalled;
    private static boolean mainAppInstalled;

    private XboxAppDeepLinker() {
    }

    public static boolean appDeeplinkingSupported() {
        boolean isAmazonFireTv;
        isAmazonFireTv = Build.MANUFACTURER.equalsIgnoreCase(AMAZON_MANUFACTURER) && Build.MODEL.startsWith(AMAZON_FIRE_TV_MODEL_PREFIX);
        return !isAmazonFireTv;
    }

    public static boolean showUserProfile(Context context, String xuid) {
        if (!appDeeplinkingSupported()) {
            return false;
        }
        String packageName = context == null ? "" : context.getPackageName();
        if (xboxAppIsInstalled(context)) {
            String correlationId = UTCDeepLink.trackUserProfileLink(getActivityTitle(), packageName, xuid);
            Intent profileIntent = getXboxAppLaunchIntent(context);
            profileIntent.setAction(ACTION_VIEW_USER_PROFILE);
            profileIntent.putExtra(EXTRA_XUID, xuid);
            profileIntent.putExtra(UTCDeepLink.DEEPLINK_KEY_NAME, correlationId);
            profileIntent.putExtra(UTCDeepLink.CALLING_APP_KEY, packageName);
            context.startActivity(profileIntent);
        } else {
            UTCDeepLink.trackUserSendToStore(getActivityTitle(), packageName, UTCNames.PageAction.DeepLink.UserProfile);
            launchXboxAppStorePage(context);
        }
        return true;
    }

    public static boolean showTitleHub(Context context, String titleId) {
        if (!appDeeplinkingSupported()) {
            return false;
        }
        String packageName = context == null ? "" : context.getPackageName();
        if (xboxAppIsInstalled(context)) {
            String correlationId = UTCDeepLink.trackGameHubLink(getActivityTitle(), packageName, titleId);
            Intent gameProfileIntent = getXboxAppLaunchIntent(context);
            gameProfileIntent.setAction(ACTION_VIEW_GAME_PROFILE);
            gameProfileIntent.putExtra(EXTRA_TITLEID, titleId);
            gameProfileIntent.putExtra(UTCDeepLink.DEEPLINK_KEY_NAME, correlationId);
            gameProfileIntent.putExtra(UTCDeepLink.CALLING_APP_KEY, packageName);
            context.startActivity(gameProfileIntent);
        } else {
            UTCDeepLink.trackUserSendToStore(getActivityTitle(), packageName, UTCNames.PageAction.DeepLink.TitleHub);
            launchXboxAppStorePage(context);
        }
        return true;
    }

    public static boolean showTitleAchievements(Context context, String titleId) {
        if (!appDeeplinkingSupported()) {
            return false;
        }
        String packageName = context == null ? "" : context.getPackageName();
        if (xboxAppIsInstalled(context)) {
            String correlationId = UTCDeepLink.trackGameHubAchievementsLink(getActivityTitle(), packageName, titleId);
            Intent gameAchievementsIntent = getXboxAppLaunchIntent(context);
            gameAchievementsIntent.setAction(ACTION_VIEW_ACHIEVEMENTS);
            gameAchievementsIntent.putExtra(EXTRA_TITLEID, titleId);
            gameAchievementsIntent.putExtra(UTCDeepLink.DEEPLINK_KEY_NAME, correlationId);
            gameAchievementsIntent.putExtra(UTCDeepLink.CALLING_APP_KEY, packageName);
            context.startActivity(gameAchievementsIntent);
        } else {
            UTCDeepLink.trackUserSendToStore(getActivityTitle(), packageName, UTCNames.PageAction.DeepLink.TitleAchievements);
            launchXboxAppStorePage(context);
        }
        return true;
    }

    public static boolean showUserSettings(Context context) {
        if (!appDeeplinkingSupported()) {
            return false;
        }
        String packageName = context == null ? "" : context.getPackageName();
        if (xboxAppIsInstalled(context)) {
            String correlationId = UTCDeepLink.trackUserSettingsLink(getActivityTitle(), packageName);
            Intent settingsIntent = getXboxAppLaunchIntent(context);
            settingsIntent.setAction(ACTION_VIEW_SETTINGS);
            settingsIntent.putExtra(UTCDeepLink.DEEPLINK_KEY_NAME, correlationId);
            settingsIntent.putExtra(UTCDeepLink.CALLING_APP_KEY, packageName);
            context.startActivity(settingsIntent);
        } else {
            UTCDeepLink.trackUserSendToStore(getActivityTitle(), packageName, UTCNames.PageAction.DeepLink.UserSettings);
            launchXboxAppStorePage(context);
        }
        return true;
    }

    public static boolean showAddFriends(Context context) {
        if (!appDeeplinkingSupported()) {
            return false;
        }
        String packageName = context == null ? "" : context.getPackageName();
        if (xboxAppIsInstalled(context)) {
            String correlationId = UTCDeepLink.trackFriendSuggestionsLink(getActivityTitle(), packageName);
            Intent findPeopleIntent = getXboxAppLaunchIntent(context);
            findPeopleIntent.setAction(ACTION_FIND_PEOPLE);
            findPeopleIntent.putExtra(UTCDeepLink.DEEPLINK_KEY_NAME, correlationId);
            findPeopleIntent.putExtra(UTCDeepLink.CALLING_APP_KEY, packageName);
            context.startActivity(findPeopleIntent);
        } else {
            UTCDeepLink.trackUserSendToStore(getActivityTitle(), packageName, UTCNames.PageAction.DeepLink.FriendSuggestions);
            launchXboxAppStorePageInOculusStore(context);
        }
        return true;
    }

    @SuppressLint("WrongConstant")
    private static boolean xboxAppIsInstalled(@NotNull Context context) {
        try {
            context.getPackageManager().getPackageInfo("com.microsoft.xboxone.smartglass", 1);
            mainAppInstalled = true;
        } catch (PackageManager.NameNotFoundException e) {
            mainAppInstalled = false;
        }
        try {
            context.getPackageManager().getPackageInfo("com.microsoft.xboxone.smartglass.beta", 1);
            betaAppInstalled = true;
        } catch (PackageManager.NameNotFoundException e2) {
            betaAppInstalled = false;
        }
        return mainAppInstalled || betaAppInstalled;
    }

    private static Intent getXboxAppLaunchIntent(Context context) {
        XLEAssert.assertTrue(mainAppInstalled || betaAppInstalled);
        if (betaAppInstalled) {
            return context.getPackageManager().getLaunchIntentForPackage("com.microsoft.xboxone.smartglass.beta");
        }
        return context.getPackageManager().getLaunchIntentForPackage("com.microsoft.xboxone.smartglass");
    }

    @SuppressLint("WrongConstant")
    private static void launchXboxAppStorePage(Context context) {
        Intent storeIntent = getXboxAppInStoreIntent(context, PLAY_STORE_URI, PLAY_STORE_PACKAGE);
        if (storeIntent == null) {
            storeIntent = getXboxAppInStoreIntent(context, AMAZON_STORE_URI, AMAZON_UNDERGROUND_PACKAGE);
        }
        if (storeIntent == null) {
            storeIntent = getXboxAppInStoreIntent(context, AMAZON_STORE_URI, AMAZON_TABLET_STORE_PACKAGE);
        }
        if (storeIntent == null) {
            storeIntent = new Intent("android.intent.action.VIEW", Uri.parse("https://play.google.com/store/apps/details?id=com.microsoft.xboxone.smartglass"));
        }
        storeIntent.setFlags(270565376);
        context.startActivity(storeIntent);
    }

    @SuppressLint("WrongConstant")
    private static void launchXboxAppStorePageInOculusStore(@NotNull Context context) {
        Intent storeIntent = new Intent("android.intent.action.VIEW", Uri.parse("oculus.store://link/products?referrer=manual&item_id=1193603937358048"));
        storeIntent.setFlags(270565376);
        context.startActivity(storeIntent);
    }

    @SuppressLint("WrongConstant")
    @Nullable
    private static Intent getXboxAppInStoreIntent(@NotNull Context context, String storeUri, String expectedStorePackage) {
        Intent storeIntent = new Intent("android.intent.action.VIEW", Uri.parse(storeUri + "com.microsoft.xboxone.smartglass"));
        for (ResolveInfo app : context.getPackageManager().queryIntentActivities(storeIntent, 0)) {
            if (app.activityInfo.applicationInfo.packageName.equals(expectedStorePackage)) {
                ActivityInfo otherAppActivity = app.activityInfo;
                ComponentName componentName = new ComponentName(otherAppActivity.applicationInfo.packageName, otherAppActivity.name);
                storeIntent.setFlags(270532608);
                storeIntent.setComponent(componentName);
                return storeIntent;
            }
        }
        return null;
    }

    @NotNull
    @Contract(pure = true)
    private static String getActivityTitle() {
        return "DeepLink";
    }
}
