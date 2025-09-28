package com.mojang.minecraftpe;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import androidx.core.app.NotificationCompat;
import org.jetbrains.annotations.NotNull;

/**
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */
public class BatteryMonitor extends BroadcastReceiver {
    private final Context mContext;
    public int mBatteryLevel = -1;
    public int mBatteryScale = -1;
    public int mBatteryStatus = -1;
    private int mBatteryTemperature = -1;

    public BatteryMonitor(@NotNull Context context) {
        mContext = context;
        context.registerReceiver(this, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }

    @Override
    public void finalize() {
        mContext.unregisterReceiver(this);
    }

    public int getBatteryLevel() {
        return mBatteryLevel;
    }

    public int getBatteryScale() {
        return mBatteryScale;
    }

    public int getBatteryStatus() {
        return mBatteryStatus;
    }

    public int getBatteryTemperature() {
        return mBatteryTemperature;
    }

    public void onReceive(Context context, @NotNull Intent intent) {
        mBatteryLevel = intent.getIntExtra("level", -1);
        mBatteryScale = intent.getIntExtra("scale", -1);
        mBatteryStatus = intent.getIntExtra("status", -1);
        mBatteryTemperature = intent.getIntExtra("temperature", -1);
    }
}
