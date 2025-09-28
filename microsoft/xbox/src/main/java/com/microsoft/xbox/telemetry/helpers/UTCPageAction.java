package com.microsoft.xbox.telemetry.helpers;

import com.microsoft.xbox.telemetry.utc.PageAction;

import java.util.HashMap;

/**
 * 07.01.2021
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */

public class UTCPageAction {
    public static void track(String str, CharSequence charSequence) {
        track(str, UTCPageView.getCurrentPage(), charSequence, new HashMap());
    }

    public static void track(String str, CharSequence charSequence, HashMap<String, Object> hashMap) {
        track(str, UTCPageView.getCurrentPage(), charSequence, hashMap);
    }

    public static void track(String str, String str2, CharSequence charSequence, HashMap<String, Object> hashMap) {
        if (charSequence != null) {
            try {
                hashMap.put("activityTitle", charSequence);
            } catch (Exception e) {
                UTCError.trackException(e, "UTCPageAction.track");
                UTCLog.log(e.getMessage());
                return;
            }
        }
        PageAction pageAction = new PageAction();
        pageAction.actionName = str;
        pageAction.pageName = str2;
        pageAction.additionalInfo = hashMap;
        UTCLog.log("pageActions:%s, onPage:%s, additionalInfo:%s", str, str2, hashMap);
        UTCTelemetry.LogEvent(pageAction);
    }
}
