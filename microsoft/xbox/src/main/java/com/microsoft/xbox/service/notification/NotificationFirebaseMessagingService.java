package com.microsoft.xbox.service.notification;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.microsoft.xbox.idp.interop.Interop;

public class NotificationFirebaseMessagingService extends FirebaseMessagingService {
    @Override
    public void onNewToken(String str) {
        Interop.NotificationRegisterCallback(str);
    }
}