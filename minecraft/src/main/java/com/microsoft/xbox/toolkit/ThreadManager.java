package com.microsoft.xbox.toolkit;

import android.os.Handler;


public class ThreadManager {
    public static Handler Handler;
    public static Thread UIThread;

    public static void UIThreadPost(Runnable runnable) {
        UIThreadPostDelayed(runnable, 0);
    }

    public static void UIThreadPostDelayed(Runnable runnable, long j) {
        Handler.postDelayed(runnable, j);
    }

    public static void UIThreadSend(final Runnable runnable) {
        if (UIThread == Thread.currentThread()) {
            runnable.run();
            return;
        }
        final Ready ready = new Ready();
        Handler.post(() -> {
            runnable.run();
            ready.setReady();
        });
        ready.waitForReady();
    }
}
