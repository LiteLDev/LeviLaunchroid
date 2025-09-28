package com.microsoft.xbox.telemetry.helpers;

/**
 * 07.01.2021
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */

public class UTCEventTracker {

    public static void callTrackWrapper(UTCEventDelegate uTCEventDelegate) {
        try {
            uTCEventDelegate.call();
        } catch (Exception e) {
            UTCLog.log(e.getMessage());
        }
    }

    public static String callStringTrackWrapper(UTCStringEventDelegate uTCStringEventDelegate) {
        try {
            return uTCStringEventDelegate.call();
        } catch (Exception e) {
            UTCLog.log(e.getMessage());
            return null;
        }
    }

    public interface UTCEventDelegate {
        void call();
    }

    public interface UTCStringEventDelegate {
        String call();
    }
}
