package com.mojang.minecraftpe;

import com.google.firebase.messaging.FirebaseMessagingService;

public class NotificationListenerService extends FirebaseMessagingService {

    public native void nativePushNotificationReceived(int i, String str, String str2, String str3);

    public static String getDeviceRegistrationToken() {
        return "";
    }

    private static void retrieveDeviceToken() {
    }

}
