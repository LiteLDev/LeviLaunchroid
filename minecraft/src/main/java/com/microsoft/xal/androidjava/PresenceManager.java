package com.microsoft.xal.androidjava;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.ProcessLifecycleOwner;

public class PresenceManager implements LifecycleObserver {

    private static boolean isAttached;
    private static volatile boolean leviKeepRunningInBackground = false;
    private boolean m_paused = false;

    private static native void pausePresence();

    private static native void resumePresence();

    public static void setLeviKeepRunningInBackground(boolean enabled) {
        leviKeepRunningInBackground = enabled;
    }

    static void attach() {
        if (isAttached) {
            return;
        }
        isAttached = true;
        ProcessLifecycleOwner.get().getLifecycle().addObserver(new PresenceManager());
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    synchronized void onForeground() {
        if (m_paused) {
            try {
                System.out.println("Resuming presence on paused app resume");
                resumePresence();
                m_paused = false;
            } catch (UnsatisfiedLinkError e) {
                System.out.println("Failed to resume presence: " + e.toString());
            }
        } else {
            System.out.println("Ignoring resume, not currently paused");
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    synchronized void onBackground() {
        if (leviKeepRunningInBackground) {
            System.out.println("Ignoring presence pause while Levi background mode is active");
            return;
        }
        if (m_paused) {
            System.out.println("Ignoring pause, already paused");
        } else {
            try {
                System.out.println("Pausing presence on app pause");
                pausePresence();
                m_paused = true;
            } catch (UnsatisfiedLinkError e) {
                System.out.println("Failed to pause presence: " + e.toString());
            }
        }
    }
}
