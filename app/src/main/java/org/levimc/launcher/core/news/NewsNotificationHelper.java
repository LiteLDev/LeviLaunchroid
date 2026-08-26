package org.levimc.launcher.core.news;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.messaging.FirebaseMessaging;

import org.levimc.launcher.R;
import org.levimc.launcher.ui.activities.NewsActivity;

public final class NewsNotificationHelper {
    public static final String TOPIC = "levilauncher-news";
    public static final String CHANNEL_ID = "launcher_news";

    private NewsNotificationHelper() {
    }

    public static void initialize(Context context) {
        createChannel(context);
        try {
            FirebaseMessaging.getInstance().subscribeToTopic(TOPIC);
        } catch (Exception ignored) {
        }
    }

    public static void show(Context context, String id, String title, String body, String url) {
        createChannel(context);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        Intent intent = new Intent(context, NewsActivity.class)
                .putExtra(NewsActivity.EXTRA_NEWS_ID, id)
                .putExtra(NewsActivity.EXTRA_NEWS_URL, url)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                id == null ? 0 : id.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification_leaf)
                .setColor(ContextCompat.getColor(context, R.color.primary))
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION);
        NotificationManagerCompat.from(context).notify(id == null ? 7001 : id.hashCode(), builder.build());
    }

    private static void createChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) return;
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.news_notification_channel),
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription(context.getString(R.string.news_notification_channel_description));
        manager.createNotificationChannel(channel);
    }
}
