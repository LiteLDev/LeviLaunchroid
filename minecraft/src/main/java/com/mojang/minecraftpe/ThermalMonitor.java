package com.mojang.minecraftpe;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.PowerManager;
import androidx.annotation.NonNull;

/**
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */
public class ThermalMonitor extends BroadcastReceiver {
    private final Context mContext;
    private boolean mLowPowerModeEnabled = false;

    public ThermalMonitor(@NonNull Context context) {
        mContext = context;
        context.registerReceiver(this, new IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED));
        readPowerMode(context);
    }

    protected void finalize() {
        mContext.unregisterReceiver(this);
    }

    public boolean getLowPowerModeEnabled() {
        return mLowPowerModeEnabled;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        readPowerMode(context);
    }

    private void readPowerMode(@NonNull Context context) {
        mLowPowerModeEnabled = ((PowerManager) context.getSystemService(Context.POWER_SERVICE)).isPowerSaveMode();
    }
}
