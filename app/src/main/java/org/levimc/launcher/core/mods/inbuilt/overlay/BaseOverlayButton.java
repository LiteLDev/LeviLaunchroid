package org.levimc.launcher.core.mods.inbuilt.overlay;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import org.levimc.launcher.R;

public abstract class BaseOverlayButton {
    protected final Activity activity;
    protected View overlayView;
    protected WindowManager windowManager;
    protected WindowManager.LayoutParams wmParams;
    private float initialX, initialY;
    private float initialTouchX, initialTouchY;
    private boolean isDragging = false;
    private long touchDownTime = 0;
    private static final long TAP_TIMEOUT = 200;
    private static final float DRAG_THRESHOLD = 10f;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isShowing = false;

    public BaseOverlayButton(Activity activity) {
        this.activity = activity;
        this.windowManager = (WindowManager) activity.getSystemService(Context.WINDOW_SERVICE);
    }

    public void show(int startX, int startY) {
        if (isShowing) return;
        handler.postDelayed(() -> showInternal(startX, startY), 500);
    }

    private void showInternal(int startX, int startY) {
        if (isShowing || activity.isFinishing() || activity.isDestroyed()) return;
        
        try {
            overlayView = LayoutInflater.from(activity).inflate(R.layout.overlay_mod_button, null);
            ImageButton btn = (ImageButton) overlayView;
            btn.setImageResource(getIconResource());

            wmParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_PANEL,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                    | WindowManager.LayoutParams.FLAG_SPLIT_TOUCH
                    | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            );
            wmParams.gravity = Gravity.TOP | Gravity.START;
            wmParams.x = startX;
            wmParams.y = startY;
            wmParams.token = activity.getWindow().getDecorView().getWindowToken();

            btn.setOnTouchListener(this::handleTouch);
            windowManager.addView(overlayView, wmParams);
            isShowing = true;
        } catch (Exception e) {
            showFallback(startX, startY);
        }
    }

    private void showFallback(int startX, int startY) {
        if (isShowing) return;
        
        ViewGroup rootView = activity.findViewById(android.R.id.content);
        if (rootView == null) return;

        overlayView = LayoutInflater.from(activity).inflate(R.layout.overlay_mod_button, null);
        ImageButton btn = (ImageButton) overlayView;
        btn.setImageResource(getIconResource());

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        );
        params.gravity = Gravity.TOP | Gravity.START;
        params.leftMargin = startX;
        params.topMargin = startY;

        btn.setOnTouchListener(this::handleTouchFallback);
        rootView.addView(overlayView, params);
        isShowing = true;
        wmParams = null;
    }

    public void hide() {
        if (!isShowing || overlayView == null) return;
        handler.post(() -> {
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
            isShowing = false;
        });
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
                v.getParent().requestDisallowInterceptTouchEvent(true);
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
                long elapsed = SystemClock.uptimeMillis() - touchDownTime;
                if (!isDragging && elapsed < TAP_TIMEOUT) {
                    handler.post(this::onButtonClick);
                }
                isDragging = false;
                v.getParent().requestDisallowInterceptTouchEvent(false);
                return true;

            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_OUTSIDE:
                isDragging = false;
                v.getParent().requestDisallowInterceptTouchEvent(false);
                return false;
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
                v.getParent().requestDisallowInterceptTouchEvent(true);
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
                long elapsed = SystemClock.uptimeMillis() - touchDownTime;
                if (!isDragging && elapsed < TAP_TIMEOUT) {
                    handler.post(this::onButtonClick);
                }
                isDragging = false;
                v.getParent().requestDisallowInterceptTouchEvent(false);
                return true;

            case MotionEvent.ACTION_CANCEL:
                isDragging = false;
                v.getParent().requestDisallowInterceptTouchEvent(false);
                return true;
        }
        return false;
    }

    protected void sendKey(int keyCode) {
        handler.post(() -> {
            long time = SystemClock.uptimeMillis();
            KeyEvent down = new KeyEvent(time, time, KeyEvent.ACTION_DOWN, keyCode, 0, 0, -1, 0, 0, InputDevice.SOURCE_KEYBOARD);
            KeyEvent up = new KeyEvent(time, time + 10, KeyEvent.ACTION_UP, keyCode, 0, 0, -1, 0, 0, InputDevice.SOURCE_KEYBOARD);
            activity.dispatchKeyEvent(down);
            activity.dispatchKeyEvent(up);
        });
    }

    protected void sendKeyDown(int keyCode) {
        handler.post(() -> {
            long time = SystemClock.uptimeMillis();
            KeyEvent down = new KeyEvent(time, time, KeyEvent.ACTION_DOWN, keyCode, 0, 0, -1, 0, 0, InputDevice.SOURCE_KEYBOARD);
            activity.dispatchKeyEvent(down);
        });
    }

    protected void sendKeyUp(int keyCode) {
        handler.post(() -> {
            long time = SystemClock.uptimeMillis();
            KeyEvent up = new KeyEvent(time, time, KeyEvent.ACTION_UP, keyCode, 0, 0, -1, 0, 0, InputDevice.SOURCE_KEYBOARD);
            activity.dispatchKeyEvent(up);
        });
    }

    protected abstract int getIconResource();
    protected abstract void onButtonClick();
}