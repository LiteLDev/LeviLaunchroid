package com.microsoft.xbox.idp.interop;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.iid.FirebaseInstanceId;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

@SuppressWarnings("JavaJniMissingFunction")
public class Interop {
    private static final String DNET_SCOPE = "open-user.auth.dnet.xboxlive.com";
    private static final String PACKAGE_NAME_TO_REMOVE = "com.microsoft.onlineid.sample";
    private static final String POLICY = "mbi_ssl";
    private static final String PROD_SCOPE = "open-user.auth.xboxlive.com";
    private static final String TAG = "Interop";
    private static Context s_context;

    public static native boolean deinitializeInterop();

    public static native boolean initializeInterop(Context context);

    private static native void notificiation_registration_callback(String str);

    @NonNull
    public static String getSystemProxy() {
        String property;
        String property2 = System.getProperty("http.proxyHost");
        if (property2 == null || (property = System.getProperty("http.proxyPort")) == null) {
            return "";
        }
        String str = "http://" + property2 + ":" + property;
        Log.i(TAG, str);
        return str;
    }

    @NonNull
    public static String getLocale() {
        String locale = Locale.getDefault().toString();
        Log.i(TAG, "locale is: " + locale);
        return locale;
    }

    @NonNull
    public static String ReadConfigFile(@NonNull Context context) {
        s_context = context;
        InputStream openRawResource = context.getResources().openRawResource(context.getResources().getIdentifier("xboxservices", "raw", context.getPackageName()));
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] bArr = new byte[1024];
        while (true) {
            try {
                int read = openRawResource.read(bArr);
                if (read == -1) {
                    break;
                }
                byteArrayOutputStream.write(bArr, 0, read);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            byteArrayOutputStream.close();
            openRawResource.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return byteArrayOutputStream.toString();
    }

    @NonNull
    public static String GetLocalStoragePath(@NonNull Context context) {
        return context.getFilesDir().getPath();
    }

    public static Context getApplicationContext() {
        return s_context;
    }

    public static void NotificationRegisterCallback(String str) {
        Log.i(TAG, "NotificationRegisterCallback, token:" + str);
        notificiation_registration_callback(str);
    }

    public static void RegisterWithGNS(Context context) {
        Log.i("XSAPI.Android", "trying to register..");
        try {
            FirebaseInstanceId.getInstance().getInstanceId().addOnSuccessListener(instanceIdResult -> {
                Log.d(Interop.TAG, "Got Firebase id:" + instanceIdResult.getId());
                Log.d(Interop.TAG, "Got Firebase token:" + instanceIdResult.getToken());
                Interop.NotificationRegisterCallback(instanceIdResult.getToken());
            }).addOnFailureListener(e -> Log.d(Interop.TAG, "Gettting Firebase token failed, message:" + e.getMessage()));
        } catch (Exception e) {
            Log.e(TAG, "Gettting Firebase instance failed, message:" + e.getMessage());
        }
    }

    public enum AuthFlowScreenStatus {
        NO_ERROR(0),
        ERROR_USER_CANCEL(1),
        PROVIDER_ERROR(2);

        private final int id;

        AuthFlowScreenStatus(int i) {
            this.id = i;
        }

        public int getId() {
            return this.id;
        }
    }

    public enum ErrorType {
        BAN(0),
        CREATION(1),
        OFFLINE(2),
        CATCHALL(3);

        private final int id;

        ErrorType(int i) {
            this.id = i;
        }

        public int getId() {
            return this.id;
        }
    }

    public enum ErrorStatus {
        TRY_AGAIN(0),
        CLOSE(1);

        private final int id;

        ErrorStatus(int i) {
            this.id = i;
        }

        public int getId() {
            return this.id;
        }
    }

    public interface ErrorCallback {
        void onError(int i, int i2, String str);
    }

    public interface EventInitializationCallback extends ErrorCallback {
        void onSuccess();
    }
}