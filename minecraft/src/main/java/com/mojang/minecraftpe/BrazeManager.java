package com.mojang.minecraftpe;

import android.os.Process;
import android.util.Log;

/**
 * 09.09.2025
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */
public class BrazeManager {
    private final MainActivity mActivity;

    public BrazeManager(MainActivity mainActivity) {
        mActivity = mainActivity;
    }

    public void setBrazeID(String str) {
    }

    public void enableBrazeSDK() {
    }

    public void disableBrazeSDK() {
    }

    public boolean isBrazeSDKDisabled() {
        return true;
    }

    public void requestImmediateDataFlush() {
    }

    public void configureBrazeAtRuntime() {
    }

    public void requestPushPermission() {
        if (mActivity.checkPermission("android.permission.POST_NOTIFICATIONS", Process.myPid(), Process.myUid()) != 0) {
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mActivity.suspendGameplayUpdates();
                    mActivity.requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 2);
                }
            });
        }
    }
}