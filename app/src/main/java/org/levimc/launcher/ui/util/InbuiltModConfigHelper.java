package org.levimc.launcher.ui.util;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import org.levimc.launcher.R;
import org.levimc.launcher.core.mods.inbuilt.manager.InbuiltModManager;
import org.levimc.launcher.core.mods.inbuilt.model.InbuiltMod;
import org.levimc.launcher.core.mods.inbuilt.model.ModIds;
import org.levimc.launcher.ui.animation.DynamicAnim;

public class InbuiltModConfigHelper {

    public interface OnConfigChangeListener {
        void onConfigChanged(String modId);
    }

    public static int getModIcon(String modId) {
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
            case ModIds.VIRTUAL_CURSOR -> R.drawable.ic_virtual_cursor;
            default -> R.drawable.ic_settings;
        };
    }

    public static void showConfigDialog(Context context, InbuiltMod mod, OnConfigChangeListener listener) {
        Context themedContext = new android.view.ContextThemeWrapper(context, R.style.Base_Theme_FullScreen);
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

            float density = context.getResources().getDisplayMetrics().density;
            int screenWidth = context.getResources().getDisplayMetrics().widthPixels;
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
        LinearLayout cursorContainer = dialog.findViewById(R.id.config_cursor_container);
        SeekBar seekBarCursor = dialog.findViewById(R.id.seekbar_cursor_sensitivity);
        TextView textCursor = dialog.findViewById(R.id.text_cursor_sensitivity);
        LinearLayout zoomContainer = dialog.findViewById(R.id.config_zoom_container);
        SeekBar seekBarZoom = dialog.findViewById(R.id.seekbar_zoom_level);
        TextView textZoom = dialog.findViewById(R.id.text_zoom_level);
        Button btnZoomKeybind = dialog.findViewById(R.id.btn_zoom_keybind);
        Button btnCancel = dialog.findViewById(R.id.btn_cancel);
        Button btnSave = dialog.findViewById(R.id.btn_save);

        InbuiltModManager manager = InbuiltModManager.getInstance(context);
        final int[] pendingZoomKeybind = {manager.getZoomKeybind()};
        final int[] pendingAutoSprintKeybind = {manager.getAutoSprintKeybind()};

        title.setText(mod.getName());

        View contentView = dialog.findViewById(android.R.id.content);
        if (contentView != null) {
            DynamicAnim.animateDialogShow(contentView);
        }

        int currentSize = manager.getOverlayButtonSize(mod.getId());
        seekBarSize.setProgress(currentSize);
        textSize.setText(currentSize + "dp");

        int currentOpacity = manager.getOverlayOpacity(mod.getId());
        if (seekBarOpacity != null) {
            seekBarOpacity.setProgress(currentOpacity);
        }
        if (textOpacity != null) {
            textOpacity.setText(currentOpacity + "%");
        }

        lockSwitch.setChecked(manager.isOverlayLocked(mod.getId()));

        if (mod.getId().equals(ModIds.CHICK_PET)) {
            lockContainer.setVisibility(View.GONE);
        }

        if (listener != null) {
            lockSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                manager.setOverlayLocked(mod.getId(), isChecked);
                listener.onConfigChanged(mod.getId());
            });
        }

        seekBarSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                textSize.setText(progress + "dp");
                if (fromUser && listener != null) {
                    manager.setOverlayButtonSize(mod.getId(), progress);
                    listener.onConfigChanged(mod.getId());
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        if (seekBarOpacity != null && textOpacity != null) {
            seekBarOpacity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    textOpacity.setText(progress + "%");
                    if (fromUser && listener != null) {
                        manager.setOverlayOpacity(mod.getId(), progress);
                        listener.onConfigChanged(mod.getId());
                    }
                }
                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
            });
        }

        if (mod.getId().equals(ModIds.AUTO_SPRINT)) {
            autoSprintContainer.setVisibility(View.VISIBLE);
            btnAutoSprintKeybind.setText(getKeyName(pendingAutoSprintKeybind[0]));
            btnAutoSprintKeybind.setOnClickListener(v -> showKeybindCaptureDialog(context, btnAutoSprintKeybind, pendingAutoSprintKeybind, false));
        } else {
            autoSprintContainer.setVisibility(View.GONE);
        }

        if (mod.getId().equals(ModIds.VIRTUAL_CURSOR)) {
            if (cursorContainer != null) {
                cursorContainer.setVisibility(View.VISIBLE);
                int currentSensitivity = manager.getCursorSensitivity();
                seekBarCursor.setProgress(currentSensitivity);
                textCursor.setText(currentSensitivity + "%");

                seekBarCursor.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        textCursor.setText(progress + "%");
                        if (fromUser && listener != null) {
                            manager.setCursorSensitivity(progress);
                            listener.onConfigChanged(mod.getId());
                        }
                    }
                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {}
                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {}
                });
            }
        } else {
            if (cursorContainer != null) {
                cursorContainer.setVisibility(View.GONE);
            }
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
                    if (fromUser && listener != null) {
                        manager.setZoomLevel(progress);
                        listener.onConfigChanged(mod.getId());
                    }
                }
                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
            });

            btnZoomKeybind.setText(getKeyName(pendingZoomKeybind[0]));
            btnZoomKeybind.setOnClickListener(v -> showKeybindCaptureDialog(context, btnZoomKeybind, pendingZoomKeybind, true));
        } else {
            zoomContainer.setVisibility(View.GONE);
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        DynamicAnim.applyPressScale(btnCancel);

        btnSave.setOnClickListener(v -> {
            manager.setOverlayButtonSize(mod.getId(), seekBarSize.getProgress());
            if (seekBarOpacity != null) {
                manager.setOverlayOpacity(mod.getId(), seekBarOpacity.getProgress());
            }
            manager.setOverlayLocked(mod.getId(), lockSwitch.isChecked());
            if (mod.getId().equals(ModIds.AUTO_SPRINT)) {
                manager.setAutoSprintKeybind(pendingAutoSprintKeybind[0]);
            }
            if (mod.getId().equals(ModIds.ZOOM)) {
                manager.setZoomLevel(seekBarZoom.getProgress());
                manager.setZoomKeybind(pendingZoomKeybind[0]);
            }
            if (mod.getId().equals(ModIds.VIRTUAL_CURSOR) && seekBarCursor != null) {
                manager.setCursorSensitivity(seekBarCursor.getProgress());
            }
            if (listener != null) {
                listener.onConfigChanged(mod.getId());
            }
            dialog.dismiss();
        });
        DynamicAnim.applyPressScale(btnSave);

        org.levimc.launcher.util.PersonalizationManager pm = new org.levimc.launcher.util.PersonalizationManager(context);
        int accent = pm.getAccentColor();
        if (accent != 0) {
            android.content.res.ColorStateList accentTint = android.content.res.ColorStateList.valueOf(accent);
            if (seekBarSize != null) {
                seekBarSize.setProgressTintList(accentTint);
                seekBarSize.setThumbTintList(accentTint);
            }
            if (seekBarOpacity != null) {
                seekBarOpacity.setProgressTintList(accentTint);
                seekBarOpacity.setThumbTintList(accentTint);
            }
            if (seekBarCursor != null) {
                seekBarCursor.setProgressTintList(accentTint);
                seekBarCursor.setThumbTintList(accentTint);
            }
            if (seekBarZoom != null) {
                seekBarZoom.setProgressTintList(accentTint);
                seekBarZoom.setThumbTintList(accentTint);
            }
            if (lockSwitch != null) {
                int[][] states = {{android.R.attr.state_checked}, {}};
                lockSwitch.setThumbTintList(new android.content.res.ColorStateList(states, new int[]{accent, 0xFFAAAAAA}));
                int trackChecked = android.graphics.Color.argb(100, android.graphics.Color.red(accent), android.graphics.Color.green(accent), android.graphics.Color.blue(accent));
                lockSwitch.setTrackTintList(new android.content.res.ColorStateList(states, new int[]{trackChecked, 0xFF555555}));
            }
            if (btnSave != null) {
                btnSave.setBackgroundTintList(accentTint);
            }
            if (title != null) {
                title.setTextColor(accent);
            }
        }

        dialog.show();
    }

    private static void showKeybindCaptureDialog(Context context, Button keybindButton, int[] pendingKeybind, boolean isZoom) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(context.getString(isZoom ? R.string.zoom_keybind_label : R.string.autosprint_keybind_label));
        builder.setMessage(context.getString(isZoom ? R.string.zoom_keybind_press : R.string.autosprint_keybind_press));
        builder.setCancelable(true);
        builder.setNegativeButton(context.getString(R.string.dialog_negative_cancel), null);

        AlertDialog captureDialog = builder.create();
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

    private static String getKeyName(int keyCode) {
        String keyLabel = KeyEvent.keyCodeToString(keyCode);
        if (keyLabel.startsWith("KEYCODE_")) {
            keyLabel = keyLabel.substring(8);
        }
        return keyLabel;
    }
}
