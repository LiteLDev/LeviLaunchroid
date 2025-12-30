package org.levimc.launcher.core.mods.inbuilt.overlay;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
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
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import org.levimc.launcher.R;
import org.levimc.launcher.core.mods.inbuilt.manager.InbuiltModManager;

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
    private static final long LONG_PRESS_TIMEOUT = 500;
    private static final long SLIDER_HIDE_DELAY = 2000;
    private static final float DRAG_THRESHOLD = 10f;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isShowing = false;
    private boolean isHiding = false;
    private Runnable pendingShowRunnable;
    
    private View sliderOverlay;
    private WindowManager.LayoutParams sliderParams;
    private boolean isSliderShowing = false;
    private Runnable longPressRunnable;
    private Runnable sliderHideRunnable;
    private long lastSliderInteraction = 0;

    public BaseOverlayButton(Activity activity) {
        this.activity = activity;
        this.windowManager = (WindowManager) activity.getSystemService(Context.WINDOW_SERVICE);
    }

    protected int getButtonSizePx() {
        int sizeDp = InbuiltModManager.getInstance(activity).getOverlayButtonSize(getModId());
        float density = activity.getResources().getDisplayMetrics().density;
        return (int) (sizeDp * density);
    }

    protected float getButtonOpacity() {
        int opacity = InbuiltModManager.getInstance(activity).getOverlayOpacity(getModId());
        return opacity / 100f;
    }

    protected void applyOpacity() {
        if (overlayView != null) {
            overlayView.setAlpha(getButtonOpacity());
        }
    }

    protected abstract String getModId();

    public void show(int startX, int startY) {
        if (isShowing || isHiding) return;
        if (pendingShowRunnable != null) {
            handler.removeCallbacks(pendingShowRunnable);
        }
        pendingShowRunnable = () -> showInternal(startX, startY);
        handler.postDelayed(pendingShowRunnable, 500);
    }

    private void showInternal(int startX, int startY) {
        pendingShowRunnable = null;
        if (isShowing || isHiding || activity.isFinishing() || activity.isDestroyed()) return;
        
        try {
            overlayView = LayoutInflater.from(activity).inflate(R.layout.overlay_mod_button, null);
            ImageButton btn = (ImageButton) overlayView;
            btn.setImageResource(getIconResource());

            int buttonSize = getButtonSizePx();
            int padding = (int) (buttonSize * 0.22f);
            btn.setPadding(padding, padding, padding, padding);
            btn.setScaleType(ImageButton.ScaleType.FIT_CENTER);
            wmParams = new WindowManager.LayoutParams(
                buttonSize,
                buttonSize,
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
            applyOpacity();
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

        int buttonSize = getButtonSizePx();
        int padding = (int) (buttonSize * 0.22f);
        btn.setPadding(padding, padding, padding, padding);
        btn.setScaleType(ImageButton.ScaleType.FIT_CENTER);
        
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
            buttonSize,
            buttonSize
        );
        params.gravity = Gravity.TOP | Gravity.START;
        params.leftMargin = startX;
        params.topMargin = startY;

        btn.setOnTouchListener(this::handleTouchFallback);
        rootView.addView(overlayView, params);
        isShowing = true;
        wmParams = null;
        applyOpacity();
    }

    public void hide() {
        if (pendingShowRunnable != null) {
            handler.removeCallbacks(pendingShowRunnable);
            pendingShowRunnable = null;
        }
        if (!isShowing || overlayView == null) {
            isShowing = false;
            return;
        }
        isHiding = true;
        hideSliderOverlay();
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
        isHiding = false;
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
                
                if (longPressRunnable != null) {
                    handler.removeCallbacks(longPressRunnable);
                }
                longPressRunnable = () -> {
                    if (!isDragging && isShowing) {
                        showSliderOverlay();
                    }
                };
                handler.postDelayed(longPressRunnable, LONG_PRESS_TIMEOUT);
                return true;

            case MotionEvent.ACTION_MOVE:
                float dx = event.getRawX() - initialTouchX;
                float dy = event.getRawY() - initialTouchY;
                if (Math.abs(dx) > DRAG_THRESHOLD || Math.abs(dy) > DRAG_THRESHOLD) {
                    isDragging = true;
                    if (longPressRunnable != null) {
                        handler.removeCallbacks(longPressRunnable);
                    }
                }
                if (isDragging && windowManager != null && overlayView != null) {
                    wmParams.x = (int) (initialX + dx);
                    wmParams.y = (int) (initialY + dy);
                    windowManager.updateViewLayout(overlayView, wmParams);
                }
                return true;

            case MotionEvent.ACTION_UP:
                if (longPressRunnable != null) {
                    handler.removeCallbacks(longPressRunnable);
                }
                long elapsed = SystemClock.uptimeMillis() - touchDownTime;
                if (!isDragging && elapsed < TAP_TIMEOUT) {
                    handler.post(this::onButtonClick);
                }
                isDragging = false;
                v.getParent().requestDisallowInterceptTouchEvent(false);
                return true;

            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_OUTSIDE:
                if (longPressRunnable != null) {
                    handler.removeCallbacks(longPressRunnable);
                }
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
                
                if (longPressRunnable != null) {
                    handler.removeCallbacks(longPressRunnable);
                }
                longPressRunnable = () -> {
                    if (!isDragging && isShowing) {
                        showSliderOverlay();
                    }
                };
                handler.postDelayed(longPressRunnable, LONG_PRESS_TIMEOUT);
                return true;

            case MotionEvent.ACTION_MOVE:
                float dx = event.getRawX() - initialTouchX;
                float dy = event.getRawY() - initialTouchY;
                if (Math.abs(dx) > DRAG_THRESHOLD || Math.abs(dy) > DRAG_THRESHOLD) {
                    isDragging = true;
                    if (longPressRunnable != null) {
                        handler.removeCallbacks(longPressRunnable);
                    }
                }
                if (isDragging) {
                    params.leftMargin = (int) (initialX + dx);
                    params.topMargin = (int) (initialY + dy);
                    overlayView.setLayoutParams(params);
                }
                return true;

            case MotionEvent.ACTION_UP:
                if (longPressRunnable != null) {
                    handler.removeCallbacks(longPressRunnable);
                }
                long elapsed = SystemClock.uptimeMillis() - touchDownTime;
                if (!isDragging && elapsed < TAP_TIMEOUT) {
                    handler.post(this::onButtonClick);
                }
                isDragging = false;
                v.getParent().requestDisallowInterceptTouchEvent(false);
                return true;

            case MotionEvent.ACTION_CANCEL:
                if (longPressRunnable != null) {
                    handler.removeCallbacks(longPressRunnable);
                }
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

    private void showSliderOverlay() {
        if (isSliderShowing || activity.isFinishing() || activity.isDestroyed()) return;
        
        if (overlayView != null) {
            overlayView.setVisibility(View.INVISIBLE);
        }

        float density = activity.getResources().getDisplayMetrics().density;
        int width = (int) (220 * density);
        int height = (int) (120 * density);

        LinearLayout container = new LinearLayout(activity);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding((int)(12*density), (int)(12*density), (int)(12*density), (int)(12*density));
        
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#E0222222"));
        bg.setCornerRadius(12 * density);
        container.setBackground(bg);

        InbuiltModManager manager = InbuiltModManager.getInstance(activity);
        
        TextView sizeLabel = new TextView(activity);
        sizeLabel.setText("Size: " + manager.getOverlayButtonSize(getModId()) + "dp");
        sizeLabel.setTextColor(Color.WHITE);
        sizeLabel.setTextSize(12);
        container.addView(sizeLabel);

        SeekBar sizeSeek = new SeekBar(activity);
        sizeSeek.setMax(100);
        sizeSeek.setMin(24);
        sizeSeek.setProgress(manager.getOverlayButtonSize(getModId()));
        sizeSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    sizeLabel.setText("Size: " + progress + "dp");
                    manager.setOverlayButtonSize(getModId(), progress);
                    updateButtonSize(progress);
                    resetSliderHideTimer();
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                resetSliderHideTimer();
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                resetSliderHideTimer();
            }
        });
        container.addView(sizeSeek);

        TextView opacityLabel = new TextView(activity);
        opacityLabel.setText("Opacity: " + manager.getOverlayOpacity(getModId()) + "%");
        opacityLabel.setTextColor(Color.WHITE);
        opacityLabel.setTextSize(12);
        LinearLayout.LayoutParams opacityLabelParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        opacityLabelParams.topMargin = (int)(8 * density);
        container.addView(opacityLabel, opacityLabelParams);

        SeekBar opacitySeek = new SeekBar(activity);
        opacitySeek.setMax(100);
        opacitySeek.setMin(10);
        opacitySeek.setProgress(manager.getOverlayOpacity(getModId()));
        opacitySeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    opacityLabel.setText("Opacity: " + progress + "%");
                    manager.setOverlayOpacity(getModId(), progress);
                    if (overlayView != null) {
                        overlayView.setAlpha(progress / 100f);
                    }
                    resetSliderHideTimer();
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                resetSliderHideTimer();
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                resetSliderHideTimer();
            }
        });
        container.addView(opacitySeek);

        sliderOverlay = container;

        sliderParams = new WindowManager.LayoutParams(
            width,
            height,
            WindowManager.LayoutParams.TYPE_APPLICATION_PANEL,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        );
        sliderParams.gravity = Gravity.TOP | Gravity.START;
        
        if (wmParams != null) {
            sliderParams.x = wmParams.x;
            sliderParams.y = wmParams.y;
        } else {
            sliderParams.x = 50;
            sliderParams.y = 150;
        }
        sliderParams.token = activity.getWindow().getDecorView().getWindowToken();

        try {
            windowManager.addView(sliderOverlay, sliderParams);
            isSliderShowing = true;
            resetSliderHideTimer();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void hideSliderOverlay() {
        if (!isSliderShowing || sliderOverlay == null) return;
        
        if (sliderHideRunnable != null) {
            handler.removeCallbacks(sliderHideRunnable);
        }
        
        handler.post(() -> {
            try {
                if (sliderOverlay != null && windowManager != null) {
                    windowManager.removeView(sliderOverlay);
                }
            } catch (Exception ignored) {}
            sliderOverlay = null;
            isSliderShowing = false;
            
            if (overlayView != null && isShowing) {
                overlayView.setVisibility(View.VISIBLE);
            }
        });
    }

    private void resetSliderHideTimer() {
        lastSliderInteraction = SystemClock.uptimeMillis();
        if (sliderHideRunnable != null) {
            handler.removeCallbacks(sliderHideRunnable);
        }
        sliderHideRunnable = this::hideSliderOverlay;
        handler.postDelayed(sliderHideRunnable, SLIDER_HIDE_DELAY);
    }

    private void updateButtonSize(int sizeDp) {
        if (overlayView == null || !isShowing) return;
        
        float density = activity.getResources().getDisplayMetrics().density;
        int buttonSize = (int) (sizeDp * density);
        int padding = (int) (buttonSize * 0.22f);
        
        if (wmParams != null) {
            wmParams.width = buttonSize;
            wmParams.height = buttonSize;
            try {
                windowManager.updateViewLayout(overlayView, wmParams);
            } catch (Exception ignored) {}
        } else {
            ViewGroup.LayoutParams params = overlayView.getLayoutParams();
            if (params != null) {
                params.width = buttonSize;
                params.height = buttonSize;
                overlayView.setLayoutParams(params);
            }
        }
        
        if (overlayView instanceof ImageButton) {
            ImageButton btn = (ImageButton) overlayView;
            btn.setPadding(padding, padding, padding, padding);
        }
    }
}