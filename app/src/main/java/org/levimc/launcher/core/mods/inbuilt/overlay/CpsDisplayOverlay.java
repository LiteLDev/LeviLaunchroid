package org.levimc.launcher.core.mods.inbuilt.overlay;

import android.app.Activity;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.levimc.launcher.R;
import org.levimc.launcher.core.mods.inbuilt.manager.InbuiltModManager;
import org.levimc.launcher.core.mods.inbuilt.model.ModIds;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class CpsDisplayOverlay {
    private static final int UPDATE_INTERVAL = 100;
    private static final double CPS_WINDOW = 1.0;
    private static final float DRAG_THRESHOLD = 10f;

    private final Activity activity;
    private final WindowManager windowManager;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<Double> clickTimes = new ArrayList<>();

    private View overlayView;
    private TextView statsText;
    private WindowManager.LayoutParams wmParams;
    private boolean isShowing = false;
    private Runnable pendingShowRunnable;

    private float initialX, initialY;
    private float initialTouchX, initialTouchY;
    private boolean isDragging = false;
    private long touchDownTime = 0;

    private View.OnTouchListener gameInputListener;

    private final Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isShowing) return;
            updateDisplay();
            handler.postDelayed(this, UPDATE_INTERVAL);
        }
    };

    public CpsDisplayOverlay(Activity activity) {
        this.activity = activity;
        this.windowManager = (WindowManager) activity.getSystemService(Activity.WINDOW_SERVICE);
        setupGameInputListener();
    }

    private void setupGameInputListener() {
        View decorView = activity.getWindow().getDecorView();
        decorView.setOnTouchListener((v, event) -> {
            if (isShowing) {
                int action = event.getActionMasked();
                if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
                    registerClick();
                }
            }
            return false;
        });
    }

    public void registerClick() {
        synchronized (clickTimes) {
            clickTimes.add(getTime());
        }
    }

    private double getTime() {
        return System.nanoTime() / 1_000_000_000.0;
    }

    private int getCps() {
        synchronized (clickTimes) {
            double now = getTime();
            Iterator<Double> it = clickTimes.iterator();
            while (it.hasNext()) {
                if (now - it.next() > CPS_WINDOW) {
                    it.remove();
                }
            }
            return clickTimes.size();
        }
    }

    public void show(int startX, int startY) {
        if (isShowing) return;
        if (pendingShowRunnable != null) {
            handler.removeCallbacks(pendingShowRunnable);
        }
        pendingShowRunnable = () -> showInternal(startX, startY);
        handler.postDelayed(pendingShowRunnable, 500);
    }

    private void showInternal(int startX, int startY) {
        pendingShowRunnable = null;
        if (isShowing || activity.isFinishing() || activity.isDestroyed()) return;

        try {
            overlayView = LayoutInflater.from(activity).inflate(R.layout.overlay_stats_display, null);
            statsText = overlayView.findViewById(R.id.stats_text);
            statsText.setText("CPS: 0");

            wmParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_PANEL,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                    | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            );
            wmParams.gravity = Gravity.TOP | Gravity.START;
            wmParams.x = startX;
            wmParams.y = startY;
            wmParams.token = activity.getWindow().getDecorView().getWindowToken();

            overlayView.setOnTouchListener(this::handleTouch);
            windowManager.addView(overlayView, wmParams);
            isShowing = true;

            handler.post(updateRunnable);
            applyOpacity();
        } catch (Exception e) {
            showFallback(startX, startY);
        }
    }

    private void showFallback(int startX, int startY) {
        if (isShowing) return;
        ViewGroup rootView = activity.findViewById(android.R.id.content);
        if (rootView == null) return;

        overlayView = LayoutInflater.from(activity).inflate(R.layout.overlay_stats_display, null);
        statsText = overlayView.findViewById(R.id.stats_text);
        statsText.setText("CPS: 0");

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        );
        params.gravity = Gravity.TOP | Gravity.START;
        params.leftMargin = startX;
        params.topMargin = startY;

        overlayView.setOnTouchListener(this::handleTouchFallback);
        rootView.addView(overlayView, params);
        isShowing = true;
        wmParams = null;

        handler.post(updateRunnable);
        applyOpacity();
    }

    private void updateDisplay() {
        if (statsText != null) {
            statsText.setText("CPS: " + getCps());
        }
    }

    private void applyOpacity() {
        if (overlayView != null) {
            int opacity = InbuiltModManager.getInstance(activity).getOverlayOpacity(ModIds.CPS_DISPLAY);
            overlayView.setAlpha(opacity / 100f);
        }
    }

    private boolean handleTouch(View v, MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                initialX = wmParams.x;
                initialY = wmParams.y;
                initialTouchX = event.getRawX();
                initialTouchY = event.getRawY();
                isDragging = false;
                touchDownTime = SystemClock.uptimeMillis();
                return true;
            case MotionEvent.ACTION_MOVE:
                float dx = event.getRawX() - initialTouchX;
                float dy = event.getRawY() - initialTouchY;
                if (Math.abs(dx) > DRAG_THRESHOLD || Math.abs(dy) > DRAG_THRESHOLD) {
                    isDragging = true;
                }
                if (isDragging && windowManager != null && overlayView != null) {
                    wmParams.x = (int) (initialX + dx);
                    wmParams.y = (int) (initialY + dy);
                    windowManager.updateViewLayout(overlayView, wmParams);
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isDragging = false;
                return true;
        }
        return false;
    }

    private boolean handleTouchFallback(View v, MotionEvent event) {
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) overlayView.getLayoutParams();
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                initialX = params.leftMargin;
                initialY = params.topMargin;
                initialTouchX = event.getRawX();
                initialTouchY = event.getRawY();
                isDragging = false;
                touchDownTime = SystemClock.uptimeMillis();
                return true;
            case MotionEvent.ACTION_MOVE:
                float dx = event.getRawX() - initialTouchX;
                float dy = event.getRawY() - initialTouchY;
                if (Math.abs(dx) > DRAG_THRESHOLD || Math.abs(dy) > DRAG_THRESHOLD) {
                    isDragging = true;
                }
                if (isDragging) {
                    params.leftMargin = (int) (initialX + dx);
                    params.topMargin = (int) (initialY + dy);
                    overlayView.setLayoutParams(params);
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isDragging = false;
                return true;
        }
        return false;
    }

    public void hide() {
        if (pendingShowRunnable != null) {
            handler.removeCallbacks(pendingShowRunnable);
            pendingShowRunnable = null;
        }
        if (!isShowing) return;
        isShowing = false;
        handler.removeCallbacks(updateRunnable);
        try {
            if (wmParams != null && windowManager != null) {
                windowManager.removeView(overlayView);
            } else {
                ViewGroup rootView = activity.findViewById(android.R.id.content);
                if (rootView != null) {
                    rootView.removeView(overlayView);
                }
            }
        } catch (Exception ignored) {}
        overlayView = null;
        statsText = null;
        synchronized (clickTimes) {
            clickTimes.clear();
        }
    }
}
