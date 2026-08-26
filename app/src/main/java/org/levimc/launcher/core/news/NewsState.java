package org.levimc.launcher.core.news;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import java.util.HashSet;
import java.util.Set;

public final class NewsState {
    public static final String ACTION_NEWS_CHANGED = "org.levimc.launcher.action.NEWS_CHANGED";
    private static final String PREFS = "launcher_news_state";
    private static final String KEY_LAST_READ_AT = "last_read_at";
    private static final String KEY_PENDING_ID = "pending_id";
    private static final String KEY_PENDING_AT = "pending_at";

    private NewsState() {
    }

    public static int getUnreadCount(Context context, NewsFeed feed) {
        SharedPreferences prefs = prefs(context);
        String lastReadAt = prefs.getString(KEY_LAST_READ_AT, "");
        int count = 0;
        Set<String> feedIds = new HashSet<>();
        if (feed != null && feed.items != null) {
            for (NewsItem item : feed.items) {
                if (item == null || TextUtils.isEmpty(item.id)) continue;
                feedIds.add(item.id);
                if (isAfter(item.publishedAt, lastReadAt)) count++;
            }
        }

        String pendingId = prefs.getString(KEY_PENDING_ID, "");
        String pendingAt = prefs.getString(KEY_PENDING_AT, "");
        if (!TextUtils.isEmpty(pendingId) && !feedIds.contains(pendingId) && isAfter(pendingAt, lastReadAt)) {
            count++;
        }
        return count;
    }

    public static Set<String> getUnreadIds(Context context, NewsFeed feed) {
        String lastReadAt = prefs(context).getString(KEY_LAST_READ_AT, "");
        Set<String> unread = new HashSet<>();
        if (feed == null || feed.items == null) return unread;
        for (NewsItem item : feed.items) {
            if (item != null && !TextUtils.isEmpty(item.id) && isAfter(item.publishedAt, lastReadAt)) {
                unread.add(item.id);
            }
        }
        return unread;
    }

    public static void markAllRead(Context context, NewsFeed feed) {
        SharedPreferences preferences = prefs(context);
        boolean changed = getUnreadCount(context, feed) > 0
                || preferences.contains(KEY_PENDING_ID)
                || preferences.contains(KEY_PENDING_AT);
        String newestAt = preferences.getString(KEY_LAST_READ_AT, "");
        if (feed != null && feed.items != null) {
            for (NewsItem item : feed.items) {
                if (item != null && isAfter(item.publishedAt, newestAt)) newestAt = item.publishedAt;
            }
        }
        String pendingAt = preferences.getString(KEY_PENDING_AT, "");
        if (isAfter(pendingAt, newestAt)) newestAt = pendingAt;
        preferences.edit()
                .putString(KEY_LAST_READ_AT, newestAt)
                .remove(KEY_PENDING_ID)
                .remove(KEY_PENDING_AT)
                .apply();
        if (changed) notifyChanged(context);
    }

    public static void recordPush(Context context, String id, String publishedAt) {
        if (TextUtils.isEmpty(id)) return;
        prefs(context).edit()
                .putString(KEY_PENDING_ID, id)
                .putString(KEY_PENDING_AT, publishedAt == null ? "" : publishedAt)
                .apply();
        notifyChanged(context);
    }

    public static void notifyChanged(Context context) {
        android.content.Intent intent = new android.content.Intent(ACTION_NEWS_CHANGED);
        intent.setPackage(context.getPackageName());
        context.sendBroadcast(intent);
    }

    private static boolean isAfter(String value, String baseline) {
        if (TextUtils.isEmpty(value)) return false;
        return TextUtils.isEmpty(baseline) || value.compareTo(baseline) > 0;
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
