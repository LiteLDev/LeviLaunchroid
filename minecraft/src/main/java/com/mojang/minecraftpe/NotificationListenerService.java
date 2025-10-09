package com.mojang.minecraftpe;

import android.os.Looper;
import android.util.Log;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.microsoft.xbox.service.notification.NotificationHelper;
import com.microsoft.xbox.service.notification.NotificationResult;
import org.jetbrains.annotations.NotNull;


public class NotificationListenerService extends FirebaseMessagingService {
    private static String sDeviceRegistrationToken = "";

    public NotificationListenerService() {
        retrieveDeviceToken();
    }

    public static String getDeviceRegistrationToken() {
        if (sDeviceRegistrationToken.isEmpty()) {
            retrieveDeviceToken();
        }
        return sDeviceRegistrationToken;
    }

    private static void retrieveDeviceToken() {
        try {
            if (Thread.currentThread().equals(Looper.getMainLooper().getThread())) {
            }

            FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
                try {
                    String result = task.isSuccessful() ? task.getResult() : "";
                    if (result == null || result.isEmpty()) {
                        return;
                    }
                    NotificationListenerService.sDeviceRegistrationToken = result;
                } catch (Exception e) {
                    Log.e("NotificationService", "Failed to process token: " + e.getMessage());
                    NotificationListenerService.sDeviceRegistrationToken = "dummy_token_firebase_error";
                }
            }).addOnFailureListener(e -> {
                Log.e("NotificationService", "Failed to get token: " + e.getMessage());
                NotificationListenerService.sDeviceRegistrationToken = "dummy_token_firebase_error";
            });
        } catch (Exception e) {
            Log.e("NotificationService", "Error retrieving device token: " + e.getMessage());
            NotificationListenerService.sDeviceRegistrationToken = "dummy_token_firebase_error";
        }
    }

    native void nativePushNotificationReceived(final int type, String title, String description, String data);

    @Override
    public void onMessageReceived(@NotNull RemoteMessage remoteMessage) {
        if (!remoteMessage.getData().get("type").startsWith("xbox")) {
            nativePushNotificationReceived(
                    NotificationResult.NotificationType.Unknown.ordinal(),
                    remoteMessage.getNotification().getTitle(),
                    remoteMessage.getNotification().getBody(),
                    remoteMessage.getData().get("")
            );
        } else {
            NotificationResult tryParseXboxLiveNotification = NotificationHelper.tryParseXboxLiveNotification(remoteMessage, this);
            nativePushNotificationReceived(
                    tryParseXboxLiveNotification.notificationType.ordinal(),
                    tryParseXboxLiveNotification.title,
                    tryParseXboxLiveNotification.body,
                    tryParseXboxLiveNotification.data
            );
        }
    }


    @Override
    public void onNewToken(@NotNull String token) {
        sDeviceRegistrationToken = token;
    }
}
