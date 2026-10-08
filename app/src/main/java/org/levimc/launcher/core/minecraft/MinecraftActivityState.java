package org.levimc.launcher.core.minecraft;

import android.app.Activity;
import android.content.Context;
import org.levimc.launcher.util.LauncherBackgroundController;

import java.lang.ref.WeakReference;

public final class MinecraftActivityState {
    private static volatile boolean running = false;
    private static volatile boolean resumed = false;
    private static WeakReference<Activity> currentActivityRef;

    private MinecraftActivityState() {}

    public static void onCreated(Activity activity) {
        running = true;
        currentActivityRef = new WeakReference<>(activity);
    }

    public static void onResumed() {
        resumed = true;
    }

    public static void onResumed(Activity activity) {
        resumed = true;
    }

    public static void onPaused() {
        resumed = false;
    }

    public static void onPaused(Activity activity) {
        resumed = false;
    }

    public static void onDestroyed() {
        running = false;
        resumed = false;
        currentActivityRef = null;
        LauncherBackgroundController.allowLauncherPlayback();
    }

    public static void onDestroyed(Activity activity) {
        running = false;
        resumed = false;
        currentActivityRef = null;
        LauncherBackgroundController.allowLauncherPlayback();
    }

    public static boolean isRunning() {
        return running;
    }

    public static boolean isRunning(Context context) {
        return running;
    }

    public static boolean isResumed() {
        return resumed;
    }

    public static boolean isResumed(Context context) {
        return resumed;
    }

    public static Activity getCurrentActivity() {
        return currentActivityRef != null ? currentActivityRef.get() : null;
    }
}
