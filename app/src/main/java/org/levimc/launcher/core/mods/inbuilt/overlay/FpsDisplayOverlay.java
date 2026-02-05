package org.levimc.launcher.core.mods.inbuilt.overlay;

import android.app.Activity;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
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
import org.levimc.launcher.core.mods.inbuilt.nativemod.FpsMod;

public class FpsDisplayOverlay {
    private static final String TAG = "FpsDisplayOverlay";
    private static final int UPDATE_INTERVAL = 250;
    private static final float DRAG_THRESHOLD = 10f;

    private final Activity activity;
    private final WindowManager windowManager;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private View overlayView;
    private TextView statsText;
    private WindowManager.LayoutParams wmParams;
    private boolean isShowing = false;
    private Runnable pendingShowRunnable;
    private boolean initialized = false;

    private float initialX, initialY;
    private float initialTouchX, initialTouchY;
    private boolean isDragging = false;
    private boolean isLocked = false;
    private long touchDownTime = 0;

    private final Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isShowing) return;
            updateDisplay();
            handler.postDelayed(this, UPDATE_INTERVAL);
        }
    };

    public FpsDisplayOverlay(Activity activity) {
        this.activity = activity;
        this.windowManager = (WindowManager) activity.getSystemService(Activity.WINDOW_SERVICE);
    }

    public void show(int startX, int startY) {
        if (isShowing) return;
        if (pendingShowRunnable != null) {
            handler.removeCallbacks(pendingShowRunnable);
        }
        pendingShowRunnable = () -> showInternal(startX, startY);
        handler.postDelayed(pendingShowRunnable, 500);
    }

    private void initializeNative() {
        handler.postDelayed(() -> {
            if (FpsMod.init()) {
                initialized = true;
                Log.i(TAG, "FPS native initialized successfully");
            } else {
                Log.e(TAG, "Failed to initialize FPS native");
            }
        }, 1000);
    }

    private void showInternal(int startX, int startY) {
        pendingShowRunnable = null;
        if (isShowing || activity.isFinishing() || activity.isDestroyed()) return;

        if (!initialized) {
            initializeNative();
        }

        try {
            overlayView = LayoutInflater.from(activity).inflate(R.layout.overlay_stats_display, null);
            statsText = overlayView.findViewById(R.id.stats_text);
            statsText.setText("FPS: --");

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
            updateLockState();
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
        statsText.setText("FPS: --");

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
        updateLockState();
    }

    private void updateDisplay() {
        if (statsText != null) {
            if (initialized && FpsMod.nativeIsInitialized()) {
                int fps = FpsMod.nativeGetFps();
                statsText.setText("FPS: " + fps);
            } else {
                statsText.setText("FPS: --");
            }
        }
    }

    private void applyOpacity() {
        if (overlayView != null) {
            int opacity = InbuiltModManager.getInstance(activity).getOverlayOpacity(ModIds.FPS_DISPLAY);
            overlayView.setAlpha(opacity / 100f);
        }
    }

    private void updateLockState() {
        isLocked = InbuiltModManager.getInstance(activity).isOverlayLocked(ModIds.FPS_DISPLAY);
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
                v.getParent().requestDisallowInterceptTouchEvent(!isLocked);
                return true;
            case MotionEvent.ACTION_MOVE:
                float dx = event.getRawX() - initialTouchX;
                float dy = event.getRawY() - initialTouchY;
                if (Math.abs(dx) > DRAG_THRESHOLD || Math.abs(dy) > DRAG_THRESHOLD) {
                    if (!isLocked) {
                        isDragging = true;
                    }
                }
                if (isDragging && !isLocked && windowManager != null && overlayView != null) {
                    wmParams.x = (int) (initialX + dx);
                    wmParams.y = (int) (initialY + dy);
                    windowManager.updateViewLayout(overlayView, wmParams);
                }
                return !isLocked || !isDragging;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (isDragging && !isLocked) {
                    savePosition(wmParams.x, wmParams.y);
                }
                isDragging = false;
                v.getParent().requestDisallowInterceptTouchEvent(false);
                return true;
        }
        return false;
    }

    private void savePosition(int x, int y) {
        InbuiltModManager.getInstance(activity).setOverlayPosition(ModIds.FPS_DISPLAY, x, y);
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
                v.getParent().requestDisallowInterceptTouchEvent(!isLocked);
                return true;
            case MotionEvent.ACTION_MOVE:
                float dx = event.getRawX() - initialTouchX;
                float dy = event.getRawY() - initialTouchY;
                if (Math.abs(dx) > DRAG_THRESHOLD || Math.abs(dy) > DRAG_THRESHOLD) {
                    if (!isLocked) {
                        isDragging = true;
                    }
                }
                if (isDragging && !isLocked) {
                    params.leftMargin = (int) (initialX + dx);
                    params.topMargin = (int) (initialY + dy);
                    overlayView.setLayoutParams(params);
                }
                return !isLocked || !isDragging;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (isDragging && !isLocked) {
                    savePosition((int) (initialX + (event.getRawX() - initialTouchX)), 
                                 (int) (initialY + (event.getRawY() - initialTouchY)));
                }
                isDragging = false;
                v.getParent().requestDisallowInterceptTouchEvent(false);
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
    }

    public void applyConfigurationChanges() {
        if (!isShowing || overlayView == null) return;
        applyOpacity();
        updateLockState();
    }
}
