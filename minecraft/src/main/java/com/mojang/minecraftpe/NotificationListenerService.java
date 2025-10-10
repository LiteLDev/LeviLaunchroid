package com.mojang.minecraftpe;

public class NotificationListenerService {

    public native void nativePushNotificationReceived(int i, String str, String str2, String str3);

    public static String getDeviceRegistrationToken() {
        return "";
    }

    private static void retrieveDeviceToken() {
    }

}
