package com.microsoft.xbox.service.notification;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.mojang.minecraftpe.NotificationListenerService;

public class NotificationFirebaseMessagingService extends FirebaseMessagingService {
    @Override
    public void onNewToken(String str) {
        NotificationListenerService.refreshDeviceRegistrationToken(this);
    }
}