package com.microsoft.applications.events;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

/**
 * @author <a href="https://github.com/RadiantByte">RadiantByte</a>
 */

class PowerInfoReceiver extends BroadcastReceiver {
    private final HttpClient m_parent;

    PowerInfoReceiver(HttpClient httpClient) {
        this.m_parent = httpClient;
    }

    @Override
    public final void onReceive(Context context, Intent intent) {
        int intExtra = intent.getIntExtra(NotificationCompat.CATEGORY_STATUS, -1);
        this.m_parent.onPowerChange(intExtra == 2 || intExtra == 5, Build.VERSION.SDK_INT >= 28 ? intent.getBooleanExtra("battery_low", false) : false);
    }
}
