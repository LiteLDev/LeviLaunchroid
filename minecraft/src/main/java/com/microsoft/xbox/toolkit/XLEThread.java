package com.microsoft.xbox.toolkit;


public class XLEThread extends Thread {
    public XLEThread(Runnable runnable, String str) {
        super(runnable, str);
        setUncaughtExceptionHandler(XLEUnhandledExceptionHandler.Instance);
    }
}
