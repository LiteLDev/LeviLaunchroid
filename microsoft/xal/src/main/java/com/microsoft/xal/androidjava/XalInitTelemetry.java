package com.microsoft.xal.androidjava;

import android.content.Context;
import android.content.pm.PackageManager;

import androidx.appcompat.app.AppCompatActivity;
import com.microsoft.applications.events.HttpClient;

/**
 * @author <a href="https://github.com/RadiantByte">RadiantByte</a>
 */

public class XalInitTelemetry extends AppCompatActivity {
    static void initOneDS() {
        System.loadLibrary("maesdk");
    }

    static void startHttpClient(Context context) throws PackageManager.NameNotFoundException {
        new HttpClient(context);
    }
}