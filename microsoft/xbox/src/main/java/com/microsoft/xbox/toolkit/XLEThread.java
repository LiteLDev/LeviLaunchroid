package com.microsoft.xbox.toolkit;

/**
 * 07.01.2021
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */

public class XLEThread extends Thread {
    public XLEThread(Runnable runnable, String str) {
        super(runnable, str);
        setUncaughtExceptionHandler(XLEUnhandledExceptionHandler.Instance);
    }
}
