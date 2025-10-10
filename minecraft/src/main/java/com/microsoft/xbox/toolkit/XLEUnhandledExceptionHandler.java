package com.microsoft.xbox.toolkit;

import org.jetbrains.annotations.NotNull;

import java.util.Date;


public class XLEUnhandledExceptionHandler implements Thread.UncaughtExceptionHandler {
    public static XLEUnhandledExceptionHandler Instance = new XLEUnhandledExceptionHandler();
    private final Thread.UncaughtExceptionHandler oldExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();

    public void uncaughtException(Thread thread, @NotNull Throwable th) {
        th.toString();
        if (th.getCause() != null) {
            printStackTrace("CAUSE STACK TRACE", th.getCause());
        }
        printStackTrace("MAIN THREAD STACK TRACE", th);
        this.oldExceptionHandler.uncaughtException(thread, th);
    }

    private void printStackTrace(String str, @NotNull Throwable th) {
        new Date();
        String str2 = "";
        for (StackTraceElement stackTraceElement : th.getStackTrace()) {
            str2 = str2 + String.format("\t%s\n", stackTraceElement.toString());
        }
    }
}
