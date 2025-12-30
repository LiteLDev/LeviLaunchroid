package org.levimc.launcher.core.mods.inbuilt.overlay;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import org.levimc.launcher.R;
import org.levimc.launcher.core.mods.inbuilt.manager.InbuiltModManager;
import org.levimc.launcher.core.mods.inbuilt.model.InbuiltMod;
import org.levimc.launcher.core.mods.inbuilt.model.ModIds;

public class ModMenuButton {
    private static final String MOD_MENU_ID = "mod_menu";
    
    private final Activity activity;
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
    
    private ModMenuOverlay menuOverlay;
    
    public ModMenuButton(Activity activity) {
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
            buttonView = LayoutInflater.from(activity).inflate(R.layout.overlay_mod_menu_button, null);
            ImageButton btn = buttonView.findViewById(R.id.mod_menu_fab);
            
            float density = activity.getResources().getDisplayMetrics().density;
            int buttonSize = (int) (53 * density);
            
            wmParams = new WindowManager.LayoutParams(
                buttonSize,
                buttonSize,
                WindowManager.LayoutParams.TYPE_APPLICATION_PANEL,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                    | WindowManager.LayoutParams.FLAG_SPLIT_TOUCH,
                PixelFormat.TRANSLUCENT
            );
            wmParams.gravity = Gravity.TOP | Gravity.START;
            wmParams.x = startX;
            wmParams.y = startY;
            wmParams.token = activity.getWindow().getDecorView().getWindowToken();
            
            btn.setOnTouchListener(this::handleTouch);
            windowManager.addView(buttonView, wmParams);
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
        
        buttonView = LayoutInflater.from(activity).inflate(R.layout.overlay_mod_menu_button, null);
        ImageButton btn = buttonView.findViewById(R.id.mod_menu_fab);
        
        float density = activity.getResources().getDisplayMetrics().density;
        int buttonSize = (int) (48 * density);
        
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(buttonSize, buttonSize);
        params.gravity = Gravity.TOP | Gravity.START;
        params.leftMargin = startX;
        params.topMargin = startY;
        
        btn.setOnTouchListener(this::handleTouchFallback);
        rootView.addView(buttonView, params);
        isShowing = true;
        wmParams = null;
        applyOpacity();
    }
    
    private int getButtonSizePx() {
        float density = activity.getResources().getDisplayMetrics().density;
        return (int) (48 * density);
    }
    
    private void applyOpacity() {
        if (buttonView != null) {
            int opacity = InbuiltModManager.getInstance(activity).getOverlayOpacity(MOD_MENU_ID);
            buttonView.setAlpha(opacity / 100f);
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
                return true;
            case MotionEvent.ACTION_CANCEL:
                isDragging = false;
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
                return true;
            case MotionEvent.ACTION_CANCEL:
                isDragging = false;
                return true;
        }
        return false;
    }
    
    private void onButtonClick() {
        if (menuOverlay == null) {
            menuOverlay = new ModMenuOverlay(activity);
            menuOverlay.setCallback(new ModMenuOverlay.ModMenuCallback() {
                @Override
                public void onModToggled(String modId, boolean enabled) {
                    // ModMenuOverlay already calls InbuiltOverlayManager directly
                }
                @Override
                public void onModConfigRequested(InbuiltMod mod) {
                    showConfigDialog(mod);
                }
            });
        }
        
        if (menuOverlay.isShowing()) {
            menuOverlay.hide();
        } else {
            menuOverlay.show();
        }
    }
    
    private void showConfigDialog(InbuiltMod mod) {
        Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_inbuilt_mod_config);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                (int) (activity.getResources().getDisplayMetrics().widthPixels * 0.85),
                ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
        
        TextView title = dialog.findViewById(R.id.config_title);
        SeekBar seekBarSize = dialog.findViewById(R.id.seekbar_button_size);
        TextView textSize = dialog.findViewById(R.id.text_button_size);
        SeekBar seekBarOpacity = dialog.findViewById(R.id.seekbar_button_opacity);
        TextView textOpacity = dialog.findViewById(R.id.text_button_opacity);
        LinearLayout autoSprintContainer = dialog.findViewById(R.id.config_autosprint_container);
        Spinner spinnerAutoSprint = dialog.findViewById(R.id.spinner_autosprint_key);
        Button btnCancel = dialog.findViewById(R.id.btn_cancel);
        Button btnSave = dialog.findViewById(R.id.btn_save);
        
        InbuiltModManager manager = InbuiltModManager.getInstance(activity);
        title.setText(mod.getName());
        
        int currentSize = manager.getOverlayButtonSize(mod.getId());
        seekBarSize.setProgress(currentSize);
        textSize.setText(currentSize + "dp");
        
        int currentOpacity = manager.getOverlayOpacity(mod.getId());
        seekBarOpacity.setProgress(currentOpacity);
        textOpacity.setText(currentOpacity + "%");
        
        seekBarSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                textSize.setText(progress + "dp");
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        seekBarOpacity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                textOpacity.setText(progress + "%");
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        if (mod.getId().equals(ModIds.AUTO_SPRINT)) {
            autoSprintContainer.setVisibility(View.VISIBLE);
            String[] options = {
                activity.getString(R.string.autosprint_key_ctrl),
                activity.getString(R.string.autosprint_key_shift)
            };
            ArrayAdapter<String> adapter = new ArrayAdapter<>(activity, R.layout.spinner_item_inbuilt, options);
            adapter.setDropDownViewResource(R.layout.spinner_dropdown_item_inbuilt);
            spinnerAutoSprint.setAdapter(adapter);
            int currentKey = manager.getAutoSprintKey();
            spinnerAutoSprint.setSelection(currentKey == KeyEvent.KEYCODE_SHIFT_LEFT ? 1 : 0);
        } else {
            autoSprintContainer.setVisibility(View.GONE);
        }
        
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnSave.setOnClickListener(v -> {
            manager.setOverlayButtonSize(mod.getId(), seekBarSize.getProgress());
            manager.setOverlayOpacity(mod.getId(), seekBarOpacity.getProgress());
            if (mod.getId().equals(ModIds.AUTO_SPRINT)) {
                int key = spinnerAutoSprint.getSelectedItemPosition() == 1 
                    ? KeyEvent.KEYCODE_SHIFT_LEFT : KeyEvent.KEYCODE_CTRL_LEFT;
                manager.setAutoSprintKey(key);
            }
            dialog.dismiss();
        });
        
        dialog.show();
    }
    
    public void hide() {
        if (menuOverlay != null) {
            menuOverlay.hide();
            menuOverlay = null;
        }
        if (!isShowing || buttonView == null) return;
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
    
    public boolean isShowing() {
        return isShowing;
    }
    
    public ModMenuOverlay getMenuOverlay() {
        return menuOverlay;
    }
}
