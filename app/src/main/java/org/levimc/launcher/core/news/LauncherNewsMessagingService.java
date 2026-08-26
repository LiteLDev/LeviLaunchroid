package org.levimc.launcher.core.news;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public final class LauncherNewsMessagingService extends FirebaseMessagingService {
    @Override
    public void onNewToken(String token) {
        FirebaseMessaging.getInstance().subscribeToTopic(NewsNotificationHelper.TOPIC);
    }

    @Override
    public void onMessageReceived(RemoteMessage message) {
        String id = message.getData().get("news_id");
        String title = message.getData().get("title");
        String body = message.getData().get("summary");
        String publishedAt = message.getData().get("published_at");
        String url = message.getData().get("url");

        if (message.getNotification() != null) {
            if (title == null) title = message.getNotification().getTitle();
            if (body == null) body = message.getNotification().getBody();
        }
        if (title == null || title.trim().isEmpty()) title = "LeviLauncher news";
        if (body == null) body = "Open LeviLauncher to read the latest news.";

        NewsState.recordPush(this, id, publishedAt);
        NewsRepository.refresh(this, null);
        NewsNotificationHelper.show(this, id, title, body, url);
    }
}
