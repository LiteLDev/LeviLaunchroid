package com.microsoft.applications.events;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.os.Build;
import android.provider.Settings;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

/**
 * @author <a href="https://github.com/RadiantByte">RadiantByte</a>
 */

public class HttpClient {
    private static final int MAX_HTTP_THREADS = 2;
    private ConnectivityCallback m_callback;
    private ConnectivityManager m_connectivityManager;
    private final Context m_context;
    private final ExecutorService m_executor;
    private PowerInfoReceiver m_power_receiver;

    public native void createClientInstance();

    public native void deleteClientInstance();

    public native void dispatchCallback(String str, int i, Object[] objArr, byte[] bArr);

    protected boolean hasConnectivityManager() {
        return true;
    }

    public native void onCostChange(boolean z);

    public native void onPowerChange(boolean z, boolean z2);

    public native void setCacheFilePath(String str);

    public native void setDeviceInfo(String str, String str2, String str3);

    public native void setSystemInfo(String str, String str2, String str3, String str4, String str5, String str6);

    static class FutureShim extends FutureTask<Boolean> {
        FutureShim(Request request) {
            super(request, true);
        }

        @Override
        public boolean cancel(boolean z) {
            return super.cancel(z);
        }
    }

    public HttpClient(Context context) throws PackageManager.NameNotFoundException {
        this.m_context = context;
        setCacheFilePath(System.getProperty("java.io.tmpdir"));
        setDeviceInfo(calculateID(context), Build.MANUFACTURER, Build.MODEL);
        calculateAndSetSystemInfo(context);
        this.m_executor = createExecutor();
        createClientInstance();
        if (hasConnectivityManager() && context.checkSelfPermission("android.permission.ACCESS_NETWORK_STATE") == 0) {
            try {
                ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService("connectivity");
                this.m_connectivityManager = connectivityManager;
                if (connectivityManager != null) {
                    boolean zIsActiveNetworkMetered = connectivityManager.isActiveNetworkMetered();
                    this.m_callback = new ConnectivityCallback(this, zIsActiveNetworkMetered);
                    onCostChange(zIsActiveNetworkMetered);
                    this.m_connectivityManager.registerDefaultNetworkCallback(this.m_callback);
                }
            } catch (Exception unused) {
            }
        }
        this.m_power_receiver = new PowerInfoReceiver(this);
        Intent intentRegisterReceiver = context.registerReceiver(this.m_power_receiver, new IntentFilter("android.intent.action.BATTERY_CHANGED"));
        if (intentRegisterReceiver != null) {
            this.m_power_receiver.onReceive(context, intentRegisterReceiver);
        }
    }

    private static String getLanguageTag(Locale locale) {
        return locale.toLanguageTag();
    }

    private static String getTimeZone() {
        String str = new SimpleDateFormat("Z", Locale.getDefault()).format(Calendar.getInstance(TimeZone.getTimeZone("GMT"), Locale.getDefault()).getTime());
        int length = str.length() - 2;
        return str.substring(0, length) + ':' + str.substring(length);
    }

    private void calculateAndSetSystemInfo(Context context) throws PackageManager.NameNotFoundException {
        PackageInfo packageInfo;
        String str;
        String packageName = context.getPackageName();
        try {
            packageInfo = context.getPackageManager().getPackageInfo(packageName, 0);
        } catch (PackageManager.NameNotFoundException unused) {
            packageInfo = null;
        }
        if (packageInfo != null && packageInfo.versionName != null) {
            str = packageInfo.versionName;
        } else {
            str = "";
        }
        String str2 = str;
        String languageTag = getLanguageTag(context.getResources().getConfiguration().locale);
        String timeZone = getTimeZone();
        String str3 = Build.VERSION.RELEASE;
        if (str3 == null) {
            str3 = "GECOS III";
        }
        String str4 = str3;
        setSystemInfo(String.format("A:%s", packageName), str2, languageTag, str4, String.format("%s %s", str4, Build.VERSION.INCREMENTAL), timeZone);
    }

    private String calculateID(Context context) {
        String string;
        try {
            string = Settings.Secure.getString(context.getContentResolver(), "android_id");
        } catch (Exception e) {
            string = e.toString();
        }
        if (string == null) {
            return "";
        }
        return "a:" + string;
    }

    protected ExecutorService createExecutor() {
        return Executors.newFixedThreadPool(2);
    }

    public void finalize() {
        ConnectivityCallback connectivityCallback = this.m_callback;
        if (connectivityCallback != null) {
            this.m_connectivityManager.unregisterNetworkCallback(connectivityCallback);
            this.m_callback = null;
        }
        this.m_context.unregisterReceiver(this.m_power_receiver);
        this.m_power_receiver = null;
        deleteClientInstance();
        this.m_executor.shutdown();
    }

    public URL newUrl(String str) throws MalformedURLException {
        return new URL(str);
    }

    public FutureTask<Boolean> createTask(String str, String str2, byte[] bArr, String str3, int[] iArr, byte[] bArr2) {
        try {
            return new FutureShim(new Request(this, str, str2, bArr, str3, iArr, bArr2));
        } catch (Exception unused) {
            return null;
        }
    }

    public void executeTask(FutureTask<Boolean> futureTask) {
        this.m_executor.execute(futureTask);
    }
}
