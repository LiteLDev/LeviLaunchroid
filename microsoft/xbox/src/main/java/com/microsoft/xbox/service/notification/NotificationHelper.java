package com.microsoft.xbox.service.notification;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.RemoteMessage;

import org.jetbrains.annotations.Contract;

public class NotificationHelper {
    @NonNull
    @Contract("_, _ -> new")
    public static NotificationResult tryParseXboxLiveNotification(RemoteMessage remoteMessage, Context context) {
        return new NotificationResult(remoteMessage, context);
    }
}