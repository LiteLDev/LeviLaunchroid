package org.levimc.launcher.core.mods.inbuilt.overlay;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.levimc.launcher.R;
import org.levimc.launcher.core.mods.inbuilt.model.ModIds;

import java.util.ArrayList;
import java.util.List;

public class ModNotificationManager {
    private static final int DISPLAY_DURATION = 2500;
    private static final int SLIDE_DURATION = 200;
    private static final int NOTIFICATION_HEIGHT_DP = 70;
    private static final int NOTIFICATION_SPACING_DP = 8;
    private static final int BASE_MARGIN_DP = 16;
    private static final int BOTTOM_OFFSET_DP = 60;

    private final Activity activity;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final WindowManager windowManager;
    private final List<NotificationEntry> activeNotifications = new ArrayList<>();
    private final float density;

    private static class NotificationEntry {
        View view;
        WindowManager.LayoutParams params;
        Runnable dismissRunnable;
        int index;
    }

    public ModNotificationManager(Activity activity) {
        this.activity = activity;
        this.windowManager = (WindowManager) activity.getSystemService(Activity.WINDOW_SERVICE);
        this.density = activity.getResources().getDisplayMetrics().density;
    }

    public void show(String modName, String modId) {
        if (activity.isFinishing() || activity.isDestroyed()) return;
        handler.post(() -> showInternal(modName, modId));
    }

    private void showInternal(String modName, String modId) {
        if (activity.isFinishing() || activity.isDestroyed()) return;

        shiftNotificationsUp();

        try {
            View notificationView = LayoutInflater.from(activity).inflate(R.layout.overlay_mod_notification, null);
            setupViews(notificationView, modName, modId);

            int margin = (int) (BASE_MARGIN_DP * density);
            int bottomOffset = (int) (BOTTOM_OFFSET_DP * density);

            WindowManager.LayoutParams wmParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_PANEL,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
            );
            wmParams.gravity = Gravity.BOTTOM | Gravity.END;
            wmParams.x = margin;
            wmParams.y = margin + bottomOffset;
            wmParams.token = activity.getWindow().getDecorView().getWindowToken();

            NotificationEntry entry = new NotificationEntry();
            entry.view = notificationView;
            entry.params = wmParams;
            entry.index = 0;

            windowManager.addView(notificationView, wmParams);
            activeNotifications.add(0, entry);

            animateIn(notificationView);
            startProgressAnimation(notificationView);

            entry.dismissRunnable = () -> dismissNotification(entry);
            handler.postDelayed(entry.dismissRunnable, DISPLAY_DURATION);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void shiftNotificationsUp() {
        int shiftAmount = (int) ((NOTIFICATION_HEIGHT_DP + NOTIFICATION_SPACING_DP) * density);
        for (NotificationEntry entry : activeNotifications) {
            entry.index++;
            entry.params.y += shiftAmount;
            try {
                entry.view.animate()
                    .translationY(-shiftAmount)
                    .setDuration(150)
                    .setInterpolator(new DecelerateInterpolator())
                    .withEndAction(() -> {
                        entry.view.setTranslationY(0);
                        try {
                            windowManager.updateViewLayout(entry.view, entry.params);
                        } catch (Exception ignored) {}
                    })
                    .start();
            } catch (Exception ignored) {}
        }
    }

    private void setupViews(View notificationView, String modName, String modId) {
        ImageView icon = notificationView.findViewById(R.id.notification_icon);
        TextView title = notificationView.findViewById(R.id.notification_title);
        TextView message = notificationView.findViewById(R.id.notification_message);

        icon.setImageResource(getModIcon(modId));
        title.setText(modName);
        message.setText(activity.getString(R.string.mod_status_enabled));
    }

    private void animateIn(View view) {
        view.setTranslationX(300);
        view.setAlpha(0f);
        view.animate()
            .translationX(0)
            .alpha(1f)
            .setDuration(SLIDE_DURATION)
            .setInterpolator(new DecelerateInterpolator())
            .start();
    }

    private void dismissNotification(NotificationEntry entry) {
        if (!activeNotifications.contains(entry)) return;

        entry.view.animate()
            .translationX(300)
            .alpha(0f)
            .setDuration(SLIDE_DURATION)
            .setInterpolator(new AccelerateInterpolator())
            .withEndAction(() -> removeNotification(entry))
            .start();
    }

    private void removeNotification(NotificationEntry entry) {
        try {
            windowManager.removeView(entry.view);
        } catch (Exception ignored) {}
        activeNotifications.remove(entry);
    }

    private void startProgressAnimation(View notificationView) {
        ProgressBar progress = notificationView.findViewById(R.id.notification_progress);
        ObjectAnimator animator = ObjectAnimator.ofInt(progress, "progress", 100, 0);
        animator.setDuration(DISPLAY_DURATION);
        animator.setInterpolator(new AccelerateInterpolator(0.5f));
        animator.start();
    }

    private int getModIcon(String modId) {
        return switch (modId) {
            case ModIds.QUICK_DROP -> R.drawable.ic_quick_drop;
            case ModIds.CAMERA_PERSPECTIVE -> R.drawable.ic_camera;
            case ModIds.TOGGLE_HUD -> R.drawable.ic_hud;
            case ModIds.AUTO_SPRINT -> R.drawable.ic_sprint_disabled;
            case ModIds.CHICK_PET -> R.drawable.chick_idle_1;
            case ModIds.ZOOM -> R.drawable.ic_zoom_disabled;
            case ModIds.FPS_DISPLAY -> R.drawable.ic_fps;
            case ModIds.CPS_DISPLAY -> R.drawable.ic_cps;
            case ModIds.SNAPLOOK -> R.drawable.ic_snaplook_disabled;
            default -> R.drawable.ic_settings;
        };
    }

    public void hideAll() {
        handler.post(() -> {
            for (NotificationEntry entry : new ArrayList<>(activeNotifications)) {
                if (entry.dismissRunnable != null) {
                    handler.removeCallbacks(entry.dismissRunnable);
                }
                try {
                    windowManager.removeView(entry.view);
                } catch (Exception ignored) {}
            }
            activeNotifications.clear();
        });
    }
}
