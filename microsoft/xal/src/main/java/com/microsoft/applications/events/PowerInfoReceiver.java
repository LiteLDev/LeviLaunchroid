package com.microsoft.applications.events;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.pairip.VMRunner;

/**
 * @author <a href="https://github.com/RadiantByte">RadiantByte</a>
 */

class PowerInfoReceiver extends BroadcastReceiver {
    private final HttpClient m_parent;

    @Override
    public final void onReceive(Context context, Intent intent) {
        VMRunner.invoke("0zTQUt2KnCyB9wDP", new Object[]{this, context, intent});
    }

    PowerInfoReceiver(HttpClient httpClient) {
        this.m_parent = httpClient;
    }
}
