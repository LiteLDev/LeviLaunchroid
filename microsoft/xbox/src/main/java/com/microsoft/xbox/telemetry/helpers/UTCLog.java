package com.microsoft.xbox.telemetry.helpers;

import android.util.Log;

/**
 * 07.01.2021
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */

public class UTCLog {
    static final String UTCLOGTAG = "UTCLOGGING";

    public static void log(String str, Object... objArr) {
        try {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            if (stackTrace.length > 3) {
                String methodName = stackTrace[3].getMethodName();
                Log.d(UTCLOGTAG, String.format(String.format("%s: ", methodName) + str, objArr));
                return;
            }
            Log.d(UTCLOGTAG, String.format(str, objArr));
        } catch (Exception e) {
            UTCError.trackException(e, "UTCLog.log");
            if (e.getMessage().equals("Format specifier: s")) {
                Log.e(UTCLOGTAG, e.getMessage());
            }
            Log.e(UTCLOGTAG, e.getMessage());
        }
    }
}
