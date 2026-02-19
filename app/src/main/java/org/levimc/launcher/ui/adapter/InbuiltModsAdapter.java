package org.levimc.launcher.ui.adapter;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import org.levimc.launcher.R;
import org.levimc.launcher.core.mods.inbuilt.manager.InbuiltModManager;
import org.levimc.launcher.core.mods.inbuilt.model.InbuiltMod;
import org.levimc.launcher.core.mods.inbuilt.model.ModIds;
import org.levimc.launcher.ui.animation.DynamicAnim;

import java.util.ArrayList;
import java.util.List;

public class InbuiltModsAdapter extends RecyclerView.Adapter<InbuiltModsAdapter.ViewHolder> {

    private List<InbuiltMod> mods = new ArrayList<>();
    private OnAddClickListener onAddClickListener;

    public interface OnAddClickListener {
        void onAddClick(InbuiltMod mod);
    }

    public void setOnAddClickListener(OnAddClickListener listener) {
        this.onAddClickListener = listener;
    }

    public void updateMods(List<InbuiltMod> mods) {
        this.mods = mods;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_inbuilt_mod, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        InbuiltMod mod = mods.get(position);
        Context context = holder.itemView.getContext();
        holder.name.setText(mod.getName());
        holder.description.setText(mod.getDescription());

        int iconRes = getModIcon(mod.getId());
        holder.icon.setImageResource(iconRes);
        holder.icon.setImageTintList(null); // Clear any tint
        holder.icon.setBackgroundTintList(null); // Clear background tint
        holder.icon.setBackground(null); // Force clear background

        holder.settingsButton.setOnClickListener(v -> showConfigDialog(context, mod));
        DynamicAnim.applyPressScale(holder.settingsButton);

        holder.addButton.setOnClickListener(v -> {
            if (onAddClickListener != null) {
                onAddClickListener.onAddClick(mod);
            }
        });
        DynamicAnim.applyPressScale(holder.addButton);
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

    private void showConfigDialog(Context context, InbuiltMod mod) {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_inbuilt_mod_config);

        TextView title = dialog.findViewById(R.id.config_title);
        SeekBar seekBarSize = dialog.findViewById(R.id.seekbar_button_size);
        TextView textSize = dialog.findViewById(R.id.text_button_size);
        SeekBar seekBarOpacity = dialog.findViewById(R.id.seekbar_button_opacity);
        TextView textOpacity = dialog.findViewById(R.id.text_button_opacity);
        LinearLayout lockContainer = dialog.findViewById(R.id.config_lock_container);
        Switch lockSwitch = dialog.findViewById(R.id.switch_lock_position);
        LinearLayout autoSprintContainer = dialog.findViewById(R.id.config_autosprint_container);
        Spinner spinnerAutoSprint = dialog.findViewById(R.id.spinner_autosprint_key);
        LinearLayout zoomContainer = dialog.findViewById(R.id.config_zoom_container);
        SeekBar seekBarZoom = dialog.findViewById(R.id.seekbar_zoom_level);
        TextView textZoom = dialog.findViewById(R.id.text_zoom_level);
        Button btnZoomKeybind = dialog.findViewById(R.id.btn_zoom_keybind);
        Button btnCancel = dialog.findViewById(R.id.btn_cancel);
        Button btnSave = dialog.findViewById(R.id.btn_save);

        InbuiltModManager manager = InbuiltModManager.getInstance(context);
        final int[] pendingZoomKeybind = {manager.getZoomKeybind()};

        title.setText(mod.getName());

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
            window.setAttributes(params);
        }

        DynamicAnim.animateDialogShow(dialog.findViewById(android.R.id.content));

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

        seekBarSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                textSize.setText(progress + "dp");
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        seekBarOpacity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                textOpacity.setText(progress + "%");
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        if (mod.getId().equals(ModIds.AUTO_SPRINT)) {
            autoSprintContainer.setVisibility(View.VISIBLE);
            String[] options = {
                context.getString(R.string.autosprint_key_ctrl),
                context.getString(R.string.autosprint_key_shift)
            };
            ArrayAdapter<String> adapter = new ArrayAdapter<>(context, R.layout.spinner_item_inbuilt, options);
            adapter.setDropDownViewResource(R.layout.spinner_dropdown_item_inbuilt);
            spinnerAutoSprint.setAdapter(adapter);

            int currentKey = manager.getAutoSprintKey();
            spinnerAutoSprint.setSelection(currentKey == KeyEvent.KEYCODE_SHIFT_LEFT ? 1 : 0);
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
                }
                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
            });

            btnZoomKeybind.setText(getKeyName(pendingZoomKeybind[0]));
            btnZoomKeybind.setOnClickListener(v -> showKeybindCaptureDialog(context, btnZoomKeybind, pendingZoomKeybind));
        } else {
            zoomContainer.setVisibility(View.GONE);
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        DynamicAnim.applyPressScale(btnCancel);

        btnSave.setOnClickListener(v -> {
            manager.setOverlayButtonSize(mod.getId(), seekBarSize.getProgress());
            manager.setOverlayOpacity(mod.getId(), seekBarOpacity.getProgress());
            manager.setOverlayLocked(mod.getId(), lockSwitch.isChecked());
            if (mod.getId().equals(ModIds.AUTO_SPRINT)) {
                int key = spinnerAutoSprint.getSelectedItemPosition() == 1 
                    ? KeyEvent.KEYCODE_SHIFT_LEFT 
                    : KeyEvent.KEYCODE_CTRL_LEFT;
                manager.setAutoSprintKey(key);
            }
            if (mod.getId().equals(ModIds.ZOOM)) {
                manager.setZoomLevel(seekBarZoom.getProgress());
                manager.setZoomKeybind(pendingZoomKeybind[0]);
            }
            dialog.dismiss();
        });
        DynamicAnim.applyPressScale(btnSave);

        dialog.show();
    }

    private void showKeybindCaptureDialog(Context context, Button keybindButton, int[] pendingKeybind) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(context.getString(R.string.zoom_keybind_label));
        builder.setMessage(context.getString(R.string.zoom_keybind_press));
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

    private String getKeyName(int keyCode) {
        String keyLabel = KeyEvent.keyCodeToString(keyCode);
        if (keyLabel.startsWith("KEYCODE_")) {
            keyLabel = keyLabel.substring(8);
        }
        return keyLabel;
    }

    @Override
    public int getItemCount() {
        return mods.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView name, description;
        ImageButton settingsButton;
        Button addButton;

        ViewHolder(View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.inbuilt_mod_icon);
            name = itemView.findViewById(R.id.inbuilt_mod_name);
            description = itemView.findViewById(R.id.inbuilt_mod_description);
            settingsButton = itemView.findViewById(R.id.inbuilt_mod_settings);
            addButton = itemView.findViewById(R.id.inbuilt_mod_add_button);
        }
    }
}
