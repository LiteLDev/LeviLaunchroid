package com.mojang.minecraftpe;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.microsoft.xbox.service.notification.NotificationHelper;
import com.microsoft.xbox.service.notification.NotificationResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class NotificationListenerService extends FirebaseMessagingService {
    private static final String APP_NAME = "minecraft-xbox-notifications";
    private static final String DEFAULT_SENDER_ID = "486187589451";
    private static final String PREFERENCES = "minecraft_xbox_notifications";
    private static final String TOKEN_KEY = "registration_token";
    private static final String PENDING_KEY = "pending_notifications";
    private static final String APP_ID_KEY = "firebase_app_id";
    private static final String API_KEY_KEY = "firebase_api_key";
    private static final String PROJECT_ID_KEY = "firebase_project_id";
    private static final String SENDER_ID_KEY = "firebase_sender_id";
    private static final String CONFIG_KEY = "firebase_config";
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Object TOKEN_LOCK = new Object();
    private static volatile Context applicationContext;
    private static volatile NotificationListenerService activeService;
    private static volatile String registrationToken = "";
    private static volatile String firebaseAppId = "";
    private static volatile String firebaseApiKey = "";
    private static volatile String firebaseProjectId = "";
    private static volatile String firebaseSenderId = DEFAULT_SENDER_ID;
    private static volatile boolean nativeReady;

    public native void nativePushNotificationReceived(int type, String title, String body, String payload);

    @Override
    public void onCreate() {
        super.onCreate();
        activeService = this;
        initialize(this);
    }

    @Override
    public void onDestroy() {
        if (activeService == this) {
            activeService = null;
        }
        super.onDestroy();
    }

    public static void initialize(Context context) {
        if (context == null) {
            return;
        }
        applicationContext = context.getApplicationContext();
        SharedPreferences preferences = preferences();
        if (firebaseAppId.isEmpty()) {
            firebaseAppId = preferences.getString(APP_ID_KEY, "");
            firebaseApiKey = preferences.getString(API_KEY_KEY, "");
            firebaseProjectId = preferences.getString(PROJECT_ID_KEY, "");
            firebaseSenderId = preferences.getString(SENDER_ID_KEY, DEFAULT_SENDER_ID);
            registrationToken = preferences.getString(TOKEN_KEY, "");
        }
        if (hasFirebaseConfig()) {
            retrieveDeviceToken();
        } else if (registrationToken.isEmpty()) {
        } else {
        }
    }

    public static void initialize(
            Context context,
            String appId,
            String apiKey,
            String projectId,
            String senderId
    ) {
        if (context == null) {
            return;
        }
        applicationContext = context.getApplicationContext();
        String resolvedAppId = clean(appId);
        String resolvedApiKey = clean(apiKey);
        String resolvedProjectId = clean(projectId);
        String resolvedSenderId = clean(senderId);
        if (resolvedSenderId.isEmpty()) {
            resolvedSenderId = DEFAULT_SENDER_ID;
        }
        SharedPreferences preferences = preferences();
        String newConfig = resolvedAppId + "|" + resolvedProjectId + "|" + resolvedSenderId + "|" + resolvedApiKey;
        String oldConfig = preferences.getString(CONFIG_KEY, "");
        if (!newConfig.equals(oldConfig)) {
            registrationToken = "";
            preferences.edit().remove(TOKEN_KEY).apply();
            deleteMinecraftFirebaseApp();
        } else if (registrationToken.isEmpty()) {
            registrationToken = preferences.getString(TOKEN_KEY, "");
        }
        firebaseAppId = resolvedAppId;
        firebaseApiKey = resolvedApiKey;
        firebaseProjectId = resolvedProjectId;
        firebaseSenderId = resolvedSenderId;
        preferences.edit()
                .putString(APP_ID_KEY, firebaseAppId)
                .putString(API_KEY_KEY, firebaseApiKey)
                .putString(PROJECT_ID_KEY, firebaseProjectId)
                .putString(SENDER_ID_KEY, firebaseSenderId)
                .putString(CONFIG_KEY, newConfig)
                .apply();
        if (hasFirebaseConfig()) {
            retrieveDeviceToken();
        } else {
        }
    }

    public static String getDeviceRegistrationToken() {
        if (applicationContext != null && registrationToken.isEmpty()) {
            registrationToken = preferences().getString(TOKEN_KEY, "");
        }
        if (registrationToken.isEmpty() && hasFirebaseConfig()) {
            retrieveDeviceToken();
        }
        return registrationToken;
    }

    public static void requestDeviceRegistrationToken(TokenCallback callback) {
        String token = getDeviceRegistrationToken();
        if (!token.isEmpty()) {
            callback.onToken(token);
            return;
        }
        EXECUTOR.execute(() -> callback.onToken(fetchDeviceToken(false)));
    }

    public static void setNativeReady(boolean ready) {
        nativeReady = ready;
        if (ready) {
            flushPendingNotifications();
        }
    }

    public static void onMinecraftForeground(Context context) {
        if (context != null) {
            applicationContext = context.getApplicationContext();
        }
        nativeReady = true;
        EXECUTOR.execute(NotificationListenerService::flushPendingNotifications);
    }

    public static void refreshDeviceRegistrationToken(Context context) {
        if (context != null) {
            applicationContext = context.getApplicationContext();
        }
        EXECUTOR.execute(() -> fetchDeviceToken(true));
    }

    private static void retrieveDeviceToken() {
        EXECUTOR.execute(() -> fetchDeviceToken(false));
    }

    @SuppressWarnings("deprecation")
    private static String fetchDeviceToken(boolean force) {
        synchronized (TOKEN_LOCK) {
            if (!force && !registrationToken.isEmpty()) {
                return registrationToken;
            }
            FirebaseApp app = getMinecraftFirebaseApp();
            if (app == null) {
                return "";
            }
            try {
                String token = FirebaseInstanceId.getInstance(app).getToken(firebaseSenderId, "FCM");
                if (token != null && !token.isEmpty()) {
                    registrationToken = token;
                    preferences().edit().putString(TOKEN_KEY, token).apply();
                    return token;
                }
            } catch (IOException | RuntimeException error) {
            }
            return "";
        }
    }

    private static FirebaseApp getMinecraftFirebaseApp() {
        if (applicationContext == null || !hasFirebaseConfig()) {
            return null;
        }
        try {
            return FirebaseApp.getInstance(APP_NAME);
        } catch (IllegalStateException ignored) {
        }
        try {
            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setApplicationId(firebaseAppId)
                    .setApiKey(firebaseApiKey)
                    .setProjectId(firebaseProjectId)
                    .setGcmSenderId(firebaseSenderId)
                    .build();
            FirebaseApp app = FirebaseApp.initializeApp(applicationContext, options, APP_NAME);
            return app;
        } catch (RuntimeException error) {
            return null;
        }
    }

    private static void deleteMinecraftFirebaseApp() {
        try {
            FirebaseApp.getInstance(APP_NAME).delete();
        } catch (IllegalStateException ignored) {
        }
    }

    private static boolean hasFirebaseConfig() {
        return !firebaseAppId.isEmpty()
                && !firebaseApiKey.isEmpty()
                && !firebaseProjectId.isEmpty()
                && !firebaseSenderId.isEmpty();
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        refreshDeviceRegistrationToken(this);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        super.onMessageReceived(message);
        applicationContext = getApplicationContext();
        activeService = this;
        NotificationResult result = NotificationHelper.tryParseXboxLiveNotification(message, this);
        String payload = result.data == null ? "" : result.data;
        int type = result.notificationType.ordinal();
        if (result.notificationType == NotificationResult.NotificationType.Unknown || payload.isEmpty()) {
            return;
        }
        String title = result.title;
        String body = result.body;
        if (result.notificationType == NotificationResult.NotificationType.Invite) {
            String[] inviteText = resolveInviteText(payload);
            if (title == null || title.isEmpty()) {
                title = inviteText[0];
            }
            if (body == null || body.isEmpty()) {
                body = inviteText[1];
            }
        }
        NotificationData notification = new NotificationData(
                type,
                title,
                body,
                payload
        );
        if (!dispatch(notification)) {
            enqueue(notification);
        }
    }


    private static String[] resolveInviteText(String payload) {
        String title = "Game invite";
        String sender = "Someone";
        String game = "Minecraft";
        try {
            JSONObject root = new JSONObject(payload);
            JSONObject inviteInfo = root.optJSONObject("inviteInfo");
            if (inviteInfo != null) {
                String value = inviteInfo.optString("senderModernGamertag", "");
                if (value.isEmpty()) {
                    value = inviteInfo.optString("senderUniqueModernGamertag", "");
                }
                if (value.isEmpty()) {
                    value = inviteInfo.optString("sender", "");
                }
                if (!value.isEmpty()) {
                    sender = value;
                }
            }
            JSONObject activityInfo = root.optJSONObject("activityInfo");
            if (activityInfo != null) {
                String value = activityInfo.optString("titleName", "");
                if (!value.isEmpty()) {
                    game = value;
                }
            }
        } catch (JSONException ignored) {
        }
        return new String[]{title, sender + " has invited you to play " + game};
    }

    private static boolean dispatch(NotificationData notification) {
        MainActivity activity = MainActivity.mInstance;
        if (!nativeReady && activity == null) {
            return false;
        }
        NotificationListenerService service = activeService;
        if (service == null) {
            service = new NotificationListenerService();
        }
        NotificationListenerService target = service;
        AtomicBoolean delivered = new AtomicBoolean(false);
        CountDownLatch completed = new CountDownLatch(1);
        Runnable delivery = () -> {
            try {
                target.nativePushNotificationReceived(
                        notification.type,
                        notification.title,
                        notification.body,
                        notification.payload
                );
                nativeReady = true;
                delivered.set(true);
            } catch (UnsatisfiedLinkError | RuntimeException error) {
                nativeReady = false;
            } finally {
                completed.countDown();
            }
        };
        if (Looper.myLooper() == Looper.getMainLooper()) {
            delivery.run();
        } else {
            Handler handler = activity != null
                    ? new Handler(activity.getMainLooper())
                    : new Handler(Looper.getMainLooper());
            handler.post(delivery);
            try {
                if (!completed.await(5L, TimeUnit.SECONDS)) {
                    return false;
                }
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return delivered.get();
    }

    private static synchronized void enqueue(NotificationData notification) {
        if (applicationContext == null) {
            return;
        }
        JSONArray pending = readPending();
        pending.put(notification.toJson());
        while (pending.length() > 16) {
            JSONArray reduced = new JSONArray();
            for (int index = pending.length() - 16; index < pending.length(); index++) {
                reduced.put(pending.opt(index));
            }
            pending = reduced;
        }
        preferences().edit().putString(PENDING_KEY, pending.toString()).apply();
    }

    private static synchronized void flushPendingNotifications() {
        if (!nativeReady || applicationContext == null) {
            return;
        }
        JSONArray pending = readPending();
        JSONArray remaining = new JSONArray();
        for (int index = 0; index < pending.length(); index++) {
            JSONObject object = pending.optJSONObject(index);
            if (object == null) {
                continue;
            }
            NotificationData notification = NotificationData.fromJson(object);
            if (!dispatch(notification)) {
                remaining.put(object);
                for (int rest = index + 1; rest < pending.length(); rest++) {
                    remaining.put(pending.opt(rest));
                }
                break;
            }
        }
        preferences().edit().putString(PENDING_KEY, remaining.toString()).apply();
    }

    private static JSONArray readPending() {
        try {
            return new JSONArray(preferences().getString(PENDING_KEY, "[]"));
        } catch (JSONException ignored) {
            return new JSONArray();
        }
    }

    private static SharedPreferences preferences() {
        return applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
    }

    public interface TokenCallback {
        void onToken(String token);
    }

    private static final class NotificationData {
        private final int type;
        private final String title;
        private final String body;
        private final String payload;

        private NotificationData(int type, String title, String body, String payload) {
            this.type = type;
            this.title = title == null ? "" : title;
            this.body = body == null ? "" : body;
            this.payload = payload == null ? "" : payload;
        }

        private JSONObject toJson() {
            JSONObject object = new JSONObject();
            try {
                object.put("type", type);
                object.put("title", title);
                object.put("body", body);
                object.put("payload", payload);
            } catch (JSONException ignored) {
            }
            return object;
        }

        private static NotificationData fromJson(JSONObject object) {
            return new NotificationData(
                    object.optInt("type", 2),
                    object.optString("title", ""),
                    object.optString("body", ""),
                    object.optString("payload", "")
            );
        }
    }
}
