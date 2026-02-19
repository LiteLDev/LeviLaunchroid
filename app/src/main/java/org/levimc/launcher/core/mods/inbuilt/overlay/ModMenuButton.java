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
import android.widget.Switch;
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
            int opacity = InbuiltModManager.getInstance(activity).getModMenuButtonOpacity();
            buttonView.setAlpha(opacity / 100f);
        }
    }

    private void applyButtonOpacity() {
        applyOpacity();
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
            menuOverlay.setCallback(new ModMenuOverlay.ModMenuButtonCallback() {
                @Override
                public void onModToggled(String modId, boolean enabled) {
                }
                @Override
                public void onModConfigRequested(InbuiltMod mod) {
                    showConfigDialog(mod);
                }
                @Override
                public void onButtonOpacityChanged(int opacity) {
                    applyButtonOpacity();
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
        Context themedContext = new android.view.ContextThemeWrapper(activity, R.style.Base_Theme_FullScreen);
        Dialog dialog = new Dialog(themedContext);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_inbuilt_mod_config);
        
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
            android.view.WindowManager.LayoutParams params = window.getAttributes();
            params.dimAmount = 0.6f;

            float density = activity.getResources().getDisplayMetrics().density;
            int screenWidth = activity.getResources().getDisplayMetrics().widthPixels;
            int maxWidth = (int) (380 * density);
            params.width = Math.min((int) (screenWidth * 0.9), maxWidth);
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            window.setAttributes(params);
        }
        
        TextView title = dialog.findViewById(R.id.config_title);
        SeekBar seekBarSize = dialog.findViewById(R.id.seekbar_button_size);
        TextView textSize = dialog.findViewById(R.id.text_button_size);
        SeekBar seekBarOpacity = dialog.findViewById(R.id.seekbar_button_opacity);
        TextView textOpacity = dialog.findViewById(R.id.text_button_opacity);
        LinearLayout lockContainer = dialog.findViewById(R.id.config_lock_container);
        Switch lockSwitch = dialog.findViewById(R.id.switch_lock_position);
        LinearLayout autoSprintContainer = dialog.findViewById(R.id.config_autosprint_container);
        Button btnAutoSprintKeybind = dialog.findViewById(R.id.btn_autosprint_keybind);
        LinearLayout zoomContainer = dialog.findViewById(R.id.config_zoom_container);
        SeekBar seekBarZoom = dialog.findViewById(R.id.seekbar_zoom_level);
        TextView textZoom = dialog.findViewById(R.id.text_zoom_level);
        Button btnCancel = dialog.findViewById(R.id.btn_cancel);
        Button btnSave = dialog.findViewById(R.id.btn_save);
        
        InbuiltModManager manager = InbuiltModManager.getInstance(activity);
        final int[] pendingAutoSprintKeybind = {manager.getAutoSprintKeybind()};
        
        title.setText(mod.getName());
        
        int currentSize = manager.getOverlayButtonSize(mod.getId());
        seekBarSize.setProgress(currentSize);
        textSize.setText(currentSize + "dp");
        
        int currentOpacity = manager.getOverlayOpacity(mod.getId());
        seekBarOpacity.setProgress(currentOpacity);
        textOpacity.setText(currentOpacity + "%");
        
        lockSwitch.setChecked(manager.isOverlayLocked(mod.getId()));

        if (mod.getId().equals(ModIds.CHICK_PET)) {
            lockContainer.setVisibility(View.GONE);
        }
        
        lockSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            manager.setOverlayLocked(mod.getId(), isChecked);
            applyConfigurationChanges(mod.getId());
        });
        
        seekBarSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                textSize.setText(progress + "dp");
                if (fromUser) {
                    manager.setOverlayButtonSize(mod.getId(), progress);
                    applyConfigurationChanges(mod.getId());
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        seekBarOpacity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                textOpacity.setText(progress + "%");
                if (fromUser) {
                    manager.setOverlayOpacity(mod.getId(), progress);
                    applyConfigurationChanges(mod.getId());
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        if (mod.getId().equals(ModIds.AUTO_SPRINT)) {
            autoSprintContainer.setVisibility(View.VISIBLE);
            btnAutoSprintKeybind.setText(getKeyName(pendingAutoSprintKeybind[0]));
            btnAutoSprintKeybind.setOnClickListener(v -> showKeybindCaptureDialog(activity, btnAutoSprintKeybind, pendingAutoSprintKeybind));
        } else {
            autoSprintContainer.setVisibility(View.GONE);
        }

        if (mod.getId().equals(ModIds.ZOOM)) {
            zoomContainer.setVisibility(View.VISIBLE);
            int currentZoom = manager.getZoomLevel();
            seekBarZoom.setProgress(currentZoom);
            textZoom.setText(currentZoom + "%");

            seekBarZoom.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    textZoom.setText(progress + "%");
                    if (fromUser) {
                        manager.setZoomLevel(progress);
                        applyConfigurationChanges(mod.getId());
                    }
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {}
            });
        } else {
            zoomContainer.setVisibility(View.GONE);
        }
        
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnSave.setOnClickListener(v -> {
            manager.setOverlayButtonSize(mod.getId(), seekBarSize.getProgress());
            manager.setOverlayOpacity(mod.getId(), seekBarOpacity.getProgress());
            manager.setOverlayLocked(mod.getId(), lockSwitch.isChecked());
            if (mod.getId().equals(ModIds.AUTO_SPRINT)) {
                manager.setAutoSprintKeybind(pendingAutoSprintKeybind[0]);
            }
            if (mod.getId().equals(ModIds.ZOOM)) {
                manager.setZoomLevel(seekBarZoom.getProgress());
            }
            dialog.dismiss();
        });
        
        dialog.show();
    }
    
    private void showKeybindCaptureDialog(Context context, Button keybindButton, int[] pendingKeybind) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(context);
        builder.setTitle(context.getString(R.string.autosprint_keybind_label));
        builder.setMessage(context.getString(R.string.autosprint_keybind_press));
        builder.setCancelable(true);
        builder.setNegativeButton(context.getString(R.string.dialog_negative_cancel), null);

        androidx.appcompat.app.AlertDialog captureDialog = builder.create();
        captureDialog.setOnKeyListener((dialogInterface, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    captureDialog.dismiss();
                    return true;
                }
                pendingKeybind[0] = keyCode;
                keybindButton.setText(getKeyName(keyCode));
                captureDialog.dismiss();
                return true;
            }
            return false;
        });
        captureDialog.show();
    }

    private String getKeyName(int keyCode) {
        String keyLabel = KeyEvent.keyCodeToString(keyCode);
        if (keyLabel.startsWith("KEYCODE_")) {
            keyLabel = keyLabel.substring(8);
        }
        return keyLabel;
    }
    
    private void applyConfigurationChanges(String modId) {
        InbuiltOverlayManager overlayManager = InbuiltOverlayManager.getInstance();
        if (overlayManager != null) {
            overlayManager.applyConfigurationChanges(modId);
        }
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
