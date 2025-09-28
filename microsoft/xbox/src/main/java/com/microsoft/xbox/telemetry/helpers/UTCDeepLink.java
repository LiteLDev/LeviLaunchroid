package com.microsoft.xbox.telemetry.helpers;

import com.microsoft.xbox.telemetry.utc.CommonData;
import com.microsoft.xbox.telemetry.utc.model.UTCNames;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

/**
 * 07.01.2021
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */

public class UTCDeepLink {
    public static final String CALLING_APP_KEY = "deepLinkCaller";
    public static final String DEEPLINK_KEY_NAME = "deepLinkId";
    public static final String INTENDED_ACTION_KEY = "intendedAction";
    public static final String TARGET_TITLE_KEY = "targetTitleId";
    public static final String TARGET_XUID_KEY = "targetXUID";

    private static @NotNull String generateCorrelationId() {
        return CommonData.getApplicationSession();
    }

    public static @NotNull HashMap<String, Object> getAdditionalInfo(String str) {
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put(DEEPLINK_KEY_NAME, generateCorrelationId());
        hashMap.put(CALLING_APP_KEY, str);
        return hashMap;
    }

    public static String trackUserProfileLink(final CharSequence charSequence, final String str, final String str2) {
        return UTCEventTracker.callStringTrackWrapper(() -> {
            HashMap access$000 = UTCDeepLink.getAdditionalInfo(str);
            access$000.put(UTCDeepLink.TARGET_XUID_KEY, "x:" + str2);
            UTCPageAction.track(UTCNames.PageAction.DeepLink.UserProfile, charSequence, access$000);
            return access$000.get(UTCDeepLink.DEEPLINK_KEY_NAME).toString();
        });
    }

    public static String trackGameHubLink(final CharSequence charSequence, final String str, final String str2) {
        return UTCEventTracker.callStringTrackWrapper(() -> {
            HashMap access$000 = UTCDeepLink.getAdditionalInfo(str);
            access$000.put(UTCDeepLink.TARGET_XUID_KEY, str2);
            UTCPageAction.track(UTCNames.PageAction.DeepLink.TitleHub, charSequence, access$000);
            return access$000.get(UTCDeepLink.DEEPLINK_KEY_NAME).toString();
        });
    }

    public static String trackGameHubAchievementsLink(final CharSequence charSequence, final String str, final String str2) {
        return UTCEventTracker.callStringTrackWrapper(() -> {
            HashMap access$000 = UTCDeepLink.getAdditionalInfo(str);
            access$000.put(UTCDeepLink.TARGET_TITLE_KEY, str2);
            UTCPageAction.track(UTCNames.PageAction.DeepLink.TitleHub, charSequence, access$000);
            return access$000.get(UTCDeepLink.DEEPLINK_KEY_NAME).toString();
        });
    }

    public static String trackUserSettingsLink(final CharSequence charSequence, final String str) {
        return UTCEventTracker.callStringTrackWrapper(() -> {
            HashMap access$000 = UTCDeepLink.getAdditionalInfo(str);
            UTCPageAction.track(UTCNames.PageAction.DeepLink.UserSettings, charSequence, access$000);
            return access$000.get(UTCDeepLink.DEEPLINK_KEY_NAME).toString();
        });
    }

    public static void trackUserSendToStore(final CharSequence charSequence, final String str, final String str2) {
        UTCEventTracker.callTrackWrapper(() -> {
            HashMap access$000 = UTCDeepLink.getAdditionalInfo(str);
            access$000.put(UTCDeepLink.INTENDED_ACTION_KEY, str2);
            UTCPageAction.track(UTCNames.PageAction.DeepLink.SendToStore, charSequence, access$000);
        });
    }

    public static String trackFriendSuggestionsLink(final CharSequence charSequence, final String str) {
        return UTCEventTracker.callStringTrackWrapper(() -> {
            HashMap access$000 = UTCDeepLink.getAdditionalInfo(str);
            UTCPageAction.track(UTCNames.PageAction.DeepLink.FriendSuggestions, charSequence, access$000);
            return access$000.get(UTCDeepLink.DEEPLINK_KEY_NAME).toString();
        });
    }
}
