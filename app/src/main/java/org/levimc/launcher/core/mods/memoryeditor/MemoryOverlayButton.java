package org.levimc.launcher.core.mods.memoryeditor;

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

public class MemoryOverlayButton {
    private final Activity activity;
    private final MemoryAddress memoryAddress;
    private View buttonView;
    private WindowManager windowManager;
    private WindowManager.LayoutParams wmParams;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isShowing = false;
    private float initialX, initialY, initialTouchX, initialTouchY;
    private boolean isDragging = false;
    private long touchDownTime = 0;
    private static final long TAP_TIMEOUT = 200;
    private static final float DRAG_THRESHOLD = 10f;
    private boolean isActive = false;

    public MemoryOverlayButton(Activity activity, MemoryAddress memoryAddress) {
        this.activity = activity;
        this.memoryAddress = memoryAddress;
        this.windowManager = (WindowManager) activity.getSystemService(android.content.Context.WINDOW_SERVICE);
    }

    public void show(int startX, int startY) {
        if (isShowing) return;
        handler.postDelayed(() -> showInternal(startX, startY), 500);
    }

    private void showInternal(int startX, int startY) {
        if (isShowing || activity.isFinishing() || activity.isDestroyed()) return;
        try {
            buttonView = LayoutInflater.from(activity).inflate(R.layout.overlay_memory_named_button, null);
            TextView btn = (TextView) buttonView;
            String name = memoryAddress.getOverlayName();
            btn.setText(name.isEmpty() ? "MEM" : name);
            updateButtonState(false);

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

            btn.setOnTouchListener(this::handleTouch);
            windowManager.addView(buttonView, wmParams);
            isShowing = true;
        } catch (Exception e) {
            showFallback(startX, startY);
        }
    }

    private void showFallback(int startX, int startY) {
        if (isShowing) return;
        ViewGroup rootView = activity.findViewById(android.R.id.content);
        if (rootView == null) return;

        buttonView = LayoutInflater.from(activity).inflate(R.layout.overlay_memory_named_button, null);
        TextView btn = (TextView) buttonView;
        String name = memoryAddress.getOverlayName();
        btn.setText(name.isEmpty() ? "MEM" : name);
        updateButtonState(false);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        );
        params.gravity = Gravity.TOP | Gravity.START;
        params.leftMargin = startX;
        params.topMargin = startY;

        btn.setOnTouchListener(this::handleTouchFallback);
        rootView.addView(buttonView, params);
        isShowing = true;
        wmParams = null;
    }

    public void hide() {
        if (!isShowing || buttonView == null) return;
        if (isActive && memoryAddress.isOverlayToggleable()) {
            restoreOriginalValue();
        }
        handler.post(() -> {
            try {
                if (wmParams != null && windowManager != null) {
                    windowManager.removeView(buttonView);
                } else {
                    ViewGroup rootView = activity.findViewById(android.R.id.content);
                    if (rootView != null) {
                        rootView.removeView(buttonView);
                    }
                }
            } catch (Exception ignored) {}
            buttonView = null;
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
                if (isDragging && windowManager != null && buttonView != null) {
                    wmParams.x = (int) (initialX + dx);
                    wmParams.y = (int) (initialY + dy);
                    windowManager.updateViewLayout(buttonView, wmParams);
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

    private boolean handleTouchFallback(View v, MotionEvent event) {
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) buttonView.getLayoutParams();
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
                    buttonView.setLayoutParams(params);
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

    private void onButtonClick() {
        if (memoryAddress.isOverlayToggleable()) {
            isActive = !isActive;
            if (isActive) {
                applyNewValue();
                updateButtonState(true);
            } else {
                restoreOriginalValue();
                updateButtonState(false);
            }
        } else {
            applyNewValue();
        }
    }

    private void applyNewValue() {
        String newValue = memoryAddress.getOverlayNewValue();
        if (!newValue.isEmpty()) {
            memoryAddress.writeValue(newValue);
        }
    }

    private void restoreOriginalValue() {
        String originalValue = memoryAddress.getOverlayOriginalValue();
        if (!originalValue.isEmpty()) {
            memoryAddress.writeValue(originalValue);
        }
    }

    private void updateButtonState(boolean active) {
        if (buttonView != null && buttonView instanceof TextView) {
            TextView btn = (TextView) buttonView;
            if (active) {
                btn.setBackgroundResource(R.drawable.bg_memory_overlay_button_active);
                btn.setTextColor(0xFF000000);
            } else {
                btn.setBackgroundResource(R.drawable.bg_memory_overlay_button);
                btn.setTextColor(0xFFFFFFFF);
            }
        }
    }

    public boolean isShowing() {
        return isShowing;
    }

    public MemoryAddress getMemoryAddress() {
        return memoryAddress;
    }
}
