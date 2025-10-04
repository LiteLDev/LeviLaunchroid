package com.mojang.minecraftpe;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.*;
import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

/**
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */
public class ImportService extends Service {
    static final int MSG_CORRELATION_CHECK = 672;
    static final int MSG_CORRELATION_RESPONSE = 837;
    final Messenger mMessenger = new Messenger(new IncomingHandler());

    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }

    @SuppressLint("HandlerLeak")
    class IncomingHandler extends Handler {
        IncomingHandler() {
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            if (msg.what == ImportService.MSG_CORRELATION_CHECK) {
                SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                String deviceId = defaultSharedPreferences.getString("deviceId", "?");
                String sessionnnId = defaultSharedPreferences.getString("LastDeviceSessionId", "");
                if (deviceId.equals("?")) {
                    return;
                }
                try {
                    long firstInstallTime = getPackageManager().getPackageInfo(getApplicationContext().getPackageName(), 0).firstInstallTime;
                    Bundle bundle = new Bundle();
                    bundle.putLong("time", firstInstallTime);
                    bundle.putString("deviceId", deviceId);
                    bundle.putString("sessionId", sessionnnId);
                    Message obtain = Message.obtain(null, ImportService.MSG_CORRELATION_RESPONSE);
                    obtain.setData(bundle);
                    msg.replyTo.send(obtain);
                    return;
                } catch (PackageManager.NameNotFoundException | RemoteException unused) {
                    return;
                }
            }
            super.handleMessage(msg);
        }
    }
}
