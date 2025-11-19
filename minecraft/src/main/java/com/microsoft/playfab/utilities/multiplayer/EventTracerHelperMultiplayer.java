package com.microsoft.playfab.utilities.multiplayer;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import java.lang.reflect.Field;
import java.util.HashMap;

/**
 * @author <a href="https://github.com/RadiantByte">RadiantByte</a>
 */

public class EventTracerHelperMultiplayer {
    private static final String APP_NAME = "AppName";
    private static final String APP_VERSION = "AppVersion";
    private static final String DEVICE_MAKE = "DeviceMake";
    private static final String DEVICE_MODEL = "DeviceModel";
    private static final String LOG_TAG = "EventTracerHelper";
    private static final String OS_NAME = "OSName";
    private static final String OS_VERSION = "OSVersion";
    private static Context mContext;

    public static void setContext(Context context) {
        mContext = context;
    }

    public static String[] getPlayFabEventCommonFields(String str) {
        HashMap map = new HashMap();
        map.put(OS_NAME, GetAndroidName());
        map.put(OS_VERSION, Build.VERSION.RELEASE);
        map.put(DEVICE_MAKE, Build.MANUFACTURER);
        map.put(DEVICE_MODEL, Build.MODEL);
        Context context = mContext;
        int i = 0;
        if (context != null) {
            try {
                PackageInfo packageInfo = context.getPackageManager().getPackageInfo(mContext.getPackageName(), 0);
                map.put(APP_NAME, mContext.getResources().getString(packageInfo.applicationInfo.labelRes));
                map.put(APP_VERSION, packageInfo.versionName);
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
            }
        }
        if (!map.containsKey(APP_NAME) || !map.containsKey(APP_VERSION)) {
            map.put(APP_NAME, str);
            map.put(APP_VERSION, "1.0.0");
        }
        String[] strArr = new String[map.size() * 2];
        for (Object str2 : map.keySet()) {
            int i2 = i * 2;
            strArr[i2] = str2.toString();
            strArr[i2 + 1] = (String) map.get(str2);
            i++;
        }
        return strArr;
    }

    private static String GetAndroidName() {
        StringBuilder sb = new StringBuilder();
        try {
            Field[] fields = Build.VERSION_CODES.class.getFields();
            for (Field field : fields) {
                String fieldName = field.getName();
                int value;
                try {
                    value = field.getInt(null);
                } catch (Exception e) {
                    Log.e(LOG_TAG, e.getMessage(), e);
                    continue;
                }

                if (value == Build.VERSION.SDK_INT) {
                    sb.append("Android ").append(fieldName).append(", API ").append(value);
                    Log.i("PlayFab Service Name", sb.toString());
                    return sb.toString();
                }
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage(), e);
        }
        return sb.toString();
    }
}
