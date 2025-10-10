package com.microsoft.xbox.telemetry.helpers;

import com.microsoft.xbox.telemetry.utc.model.UTCNames;
import com.microsoft.xbox.toolkit.XLEAssert;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;


public class UTCReportUser {
    public static CharSequence currentActivityTitle = "";
    public static String currentXUID = "";

    private static void verifyTrackedDefaults() {
        XLEAssert.assertFalse("Called trackPeopleHubView without set currentXUID", currentXUID.equals(""));
        XLEAssert.assertFalse("Called trackPeopleHubView without set activityTitle", currentActivityTitle.toString().equals(""));
    }

    public static @NotNull HashMap<String, Object> getAdditionalInfo(String str) {
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put(UTCDeepLink.TARGET_XUID_KEY, "x:" + str);
        return hashMap;
    }

    public static void trackReportView(final CharSequence charSequence, final String str) {
        UTCEventTracker.callTrackWrapper(() -> {
            UTCPageView.track(UTCNames.PageView.PeopleHub.ReportView, UTCReportUser.currentActivityTitle, UTCReportUser.getAdditionalInfo(str));
        });
    }

    public static void trackReportDialogOK(String str) {
        verifyTrackedDefaults();
        trackReportDialogOK(currentActivityTitle, currentXUID, str);
    }

    public static void trackReportDialogOK(final CharSequence charSequence, final String str, final String str2) {
        UTCEventTracker.callTrackWrapper(() -> {
            HashMap access$200 = UTCReportUser.getAdditionalInfo(str);
            access$200.put("reason", str2);
            UTCPageAction.track(UTCNames.PageAction.PeopleHub.ReportOK, charSequence, access$200);
        });
    }
}
