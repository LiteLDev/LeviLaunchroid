package org.levimc.launcher.core.mods.inbuilt.overlay;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Space;

import androidx.appcompat.app.AlertDialog;

import org.levimc.launcher.R;
import org.levimc.launcher.core.mods.inbuilt.ExternalModBridge;
import org.levimc.launcher.core.mods.inbuilt.UnifiedMod;
import org.levimc.launcher.core.mods.inbuilt.manager.InbuiltModManager;
import org.levimc.launcher.core.mods.inbuilt.model.ModIds;

import java.util.List;

public class ModConfigView {

    public static void render(Context context, ViewGroup container, UnifiedMod mod, Runnable onConfigChanged) {
        container.removeAllViews();
        float density = context.getResources().getDisplayMetrics().density;
        int accent = 0xFF4AE0A0;

        if (mod.getSource() == UnifiedMod.Source.INBUILT) {
            renderInbuiltConfig(context, container, mod, accent, density, onConfigChanged);
        } else {
            renderExternalConfig(context, container, mod, accent, density, onConfigChanged);
        }
    }

    private static void renderInbuiltConfig(Context context, ViewGroup container, UnifiedMod mod, int accent, float density, Runnable onConfigChanged) {
        InbuiltModManager manager = InbuiltModManager.getInstance(context);

        // Size slider
        addSlider(context, container, "Overlay Button Size (dp)", manager.getOverlayButtonSize(mod.getId()), 20, 100, accent, density, progress -> {
            manager.setOverlayButtonSize(mod.getId(), progress);
            onConfigChanged.run();
        });

        // Opacity slider
        addSlider(context, container, "Overlay Opacity (%)", manager.getOverlayOpacity(mod.getId()), 10, 100, accent, density, progress -> {
            manager.setOverlayOpacity(mod.getId(), progress);
            onConfigChanged.run();
        });

        // Lock Position switch
        if (!mod.getId().equals(ModIds.CHICK_PET)) {
            addToggle(context, container, "Lock Position", manager.isOverlayLocked(mod.getId()), accent, density, isChecked -> {
                manager.setOverlayLocked(mod.getId(), isChecked);
                onConfigChanged.run();
            });
        }

        // Auto Sprint specific
        if (mod.getId().equals(ModIds.AUTO_SPRINT)) {
            addKeybindCapture(context, container, "Auto Sprint Keybind", manager.getAutoSprintKeybind(), accent, density, keyCode -> {
                manager.setAutoSprintKeybind(keyCode);
                onConfigChanged.run();
            });
        }

        // Virtual Cursor specific
        if (mod.getId().equals(ModIds.VIRTUAL_CURSOR)) {
            addSlider(context, container, "Cursor Sensitivity (%)", manager.getCursorSensitivity(), 10, 200, accent, density, progress -> {
                manager.setCursorSensitivity(progress);
                onConfigChanged.run();
            });
        }

        // Zoom specific
        if (mod.getId().equals(ModIds.ZOOM)) {
            addSlider(context, container, "Zoom Level (%)", manager.getZoomLevel(), 10, 100, accent, density, progress -> {
                manager.setZoomLevel(progress);
                onConfigChanged.run();
            });
            addKeybindCapture(context, container, "Zoom Keybind", manager.getZoomKeybind(), accent, density, keyCode -> {
                manager.setZoomKeybind(keyCode);
                onConfigChanged.run();
            });
        }
    }

    private static void renderExternalConfig(Context context, ViewGroup container, UnifiedMod mod, int accent, float density, Runnable onConfigChanged) {
        List<UnifiedMod.ConfigEntry> configs = mod.getConfigEntries();
        for (UnifiedMod.ConfigEntry cfg : configs) {
            switch (cfg.type) {
                case TOGGLE:
                    boolean checked = "true".equalsIgnoreCase(cfg.currentValue) || "1".equals(cfg.currentValue);
                    addToggle(context, container, cfg.displayName, checked, accent, density, isChecked -> {
                        cfg.currentValue = isChecked ? "true" : "false";
                        ExternalModBridge.setExternalModConfig(mod.getId(), cfg.key, cfg.currentValue);
                        onConfigChanged.run();
                    });
                    break;
                case SLIDER_INT: {
                    int min = parseIntSafe(cfg.minValue, 0);
                    int max = parseIntSafe(cfg.maxValue, 100);
                    int cur = parseIntSafe(cfg.currentValue, parseIntSafe(cfg.defaultValue, min));
                    addSlider(context, container, cfg.displayName, cur, min, max, accent, density, progress -> {
                        cfg.currentValue = String.valueOf(progress);
                        ExternalModBridge.setExternalModConfig(mod.getId(), cfg.key, cfg.currentValue);
                        onConfigChanged.run();
                    });
                    break;
                }
                case SLIDER_FLOAT: {
                    float fMin = parseFloatSafe(cfg.minValue, 0f);
                    float fMax = parseFloatSafe(cfg.maxValue, 1f);
                    float fCur = parseFloatSafe(cfg.currentValue, parseFloatSafe(cfg.defaultValue, fMin));
                    int steps = 100;
                    int curProgress = (int)((fCur - fMin) / (fMax - fMin) * steps);
                    
                    LinearLayout row = createRow(context, container, density);
                    TextView label = createLabel(context, cfg.displayName, density);
                    TextView valText = createValueText(context, String.format("%.2f", fCur), accent, density);
                    row.addView(label);
                    row.addView(valText);
                    container.addView(row);

                    SeekBar seekBar = new SeekBar(context);
                    seekBar.setMin(0);
                    seekBar.setMax(steps);
                    seekBar.setProgress(Math.max(0, Math.min(steps, curProgress)));
                    applyAccentToSeekBar(seekBar, accent);
                    
                    seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                        @Override
                        public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                            if (fromUser) {
                                float val = fMin + (fMax - fMin) * progress / (float) steps;
                                valText.setText(String.format("%.2f", val));
                                cfg.currentValue = String.valueOf(val);
                                ExternalModBridge.setExternalModConfig(mod.getId(), cfg.key, cfg.currentValue);
                                onConfigChanged.run();
                            }
                        }
                        @Override public void onStartTrackingTouch(SeekBar sb) {}
                        @Override public void onStopTrackingTouch(SeekBar sb) {}
                    });
                    addWithMargin(container, seekBar, density);
                    break;
                }
                case RADIO: {
                    LinearLayout row = createRow(context, container, density);
                    row.addView(createLabel(context, cfg.displayName, density));
                    container.addView(row);

                    RadioGroup radioGroup = new RadioGroup(context);
                    radioGroup.setOrientation(LinearLayout.VERTICAL);
                    
                    String[] options = cfg.minValue != null ? cfg.minValue.split(",") : new String[0];
                    int selectedIndex = parseIntSafe(cfg.currentValue, parseIntSafe(cfg.defaultValue, 0));

                    for (int i = 0; i < options.length; i++) {
                        RadioButton rb = new RadioButton(context);
                        rb.setText(options[i]);
                        rb.setTextColor(Color.WHITE);
                        rb.setTextSize(14);
                        int[][] states = {{android.R.attr.state_checked}, {}};
                        rb.setButtonTintList(new ColorStateList(states, new int[]{accent, 0xFFAAAAAA}));
                        if (i == selectedIndex) rb.setChecked(true);
                        
                        LinearLayout.LayoutParams rbParams = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                        rb.setLayoutParams(rbParams);
                        
                        int currentIndex = i;
                        rb.setOnCheckedChangeListener((btn, isChecked) -> {
                            if (isChecked) {
                                cfg.currentValue = String.valueOf(currentIndex);
                                ExternalModBridge.setExternalModConfig(mod.getId(), cfg.key, cfg.currentValue);
                                onConfigChanged.run();
                            }
                        });
                        radioGroup.addView(rb);
                    }
                    
                    LinearLayout.LayoutParams rgParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    rgParams.leftMargin = (int)(4 * density);
                    rgParams.topMargin = (int)(4 * density);
                    container.addView(radioGroup, rgParams);
                    break;
                }
                case COLOR: {
                    LinearLayout row = createRow(context, container, density);
                    row.addView(createLabel(context, cfg.displayName, density));
                    container.addView(row);

                    LinearLayout inputRow = new LinearLayout(context);
                    inputRow.setOrientation(LinearLayout.HORIZONTAL);
                    inputRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
                    
                    EditText hexInput = new EditText(context);
                    hexInput.setText(cfg.currentValue.isEmpty() ? cfg.defaultValue : cfg.currentValue);
                    hexInput.setTextColor(accent);
                    hexInput.setHint("#AARRGGBB");
                    hexInput.setHintTextColor(0xFF888888);
                    hexInput.setBackgroundTintList(ColorStateList.valueOf(accent));
                    
                    View colorPreview = new View(context);
                    LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams((int)(30*density), (int)(30*density));
                    previewParams.leftMargin = (int)(12*density);
                    
                    try { colorPreview.setBackgroundColor(Color.parseColor(hexInput.getText().toString())); } catch (Exception ignored) {}

                    hexInput.addTextChangedListener(new android.text.TextWatcher() {
                        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                        @Override public void afterTextChanged(android.text.Editable s) {
                            String hex = s.toString();
                            if (hex.length() >= 7 && hex.startsWith("#")) {
                                try {
                                    int color = Color.parseColor(hex);
                                    colorPreview.setBackgroundColor(color);
                                    cfg.currentValue = hex;
                                    ExternalModBridge.setExternalModConfig(mod.getId(), cfg.key, cfg.currentValue);
                                    onConfigChanged.run();
                                } catch (Exception ignored) {}
                            }
                        }
                    });
                    
                    inputRow.addView(hexInput, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
                    inputRow.addView(colorPreview, previewParams);
                    addWithMargin(container, inputRow, density);
                    break;
                }
            }
        }
    }

    private static void addSlider(Context context, ViewGroup container, String labelText, int cur, int min, int max, int accent, float density, ValueChangeListener listener) {
        LinearLayout row = createRow(context, container, density);
        TextView label = createLabel(context, labelText, density);
        TextView valText = createValueText(context, String.valueOf(cur), accent, density);
        row.addView(label);
        row.addView(valText);
        container.addView(row);

        SeekBar seekBar = new SeekBar(context);
        seekBar.setMin(min);
        seekBar.setMax(max);
        seekBar.setProgress(cur);
        applyAccentToSeekBar(seekBar, accent);
        
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                if (fromUser) {
                    valText.setText(String.valueOf(progress));
                    listener.onValueChanged(progress);
                }
            }
            @Override public void onStartTrackingTouch(SeekBar sb) {}
            @Override public void onStopTrackingTouch(SeekBar sb) {}
        });
        addWithMargin(container, seekBar, density);
    }

    private static void addToggle(Context context, ViewGroup container, String labelText, boolean isChecked, int accent, float density, ToggleChangeListener listener) {
        LinearLayout row = createRow(context, container, density);
        TextView label = createLabel(context, labelText, density);
        Switch toggle = new Switch(context);
        toggle.setChecked(isChecked);
        
        int[][] states = {{android.R.attr.state_checked}, {}};
        toggle.setThumbTintList(new ColorStateList(states, new int[]{accent, 0xFFAAAAAA}));
        int trackColor = Color.argb(100, Color.red(accent), Color.green(accent), Color.blue(accent));
        toggle.setTrackTintList(new ColorStateList(states, new int[]{trackColor, 0xFF555555}));
        
        toggle.setOnCheckedChangeListener((btn, checked) -> listener.onToggleChanged(checked));
        
        row.addView(label);
        row.addView(toggle);
        container.addView(row);
    }

    private static void addKeybindCapture(Context context, ViewGroup container, String labelText, int currentKey, int accent, float density, ValueChangeListener listener) {
        LinearLayout row = createRow(context, container, density);
        TextView label = createLabel(context, labelText, density);
        Button btn = new Button(context);
        btn.setText(getKeyName(currentKey));
        btn.setBackgroundTintList(ColorStateList.valueOf(0xFF333333));
        btn.setTextColor(accent);
        
        btn.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.Base_Theme_FullScreen); // using this to match theme
            builder.setTitle(labelText);
            builder.setMessage("Press any key to bind...");
            builder.setCancelable(true);
            builder.setNegativeButton("Cancel", null);
            AlertDialog dialog = builder.create();
            dialog.setOnKeyListener((dialogInterface, keyCode, event) -> {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (keyCode == KeyEvent.KEYCODE_BACK) {
                        dialog.dismiss();
                        return true;
                    }
                    btn.setText(getKeyName(keyCode));
                    listener.onValueChanged(keyCode);
                    dialog.dismiss();
                    return true;
                }
                return false;
            });
            dialog.show();
        });
        
        row.addView(label);
        row.addView(btn);
        container.addView(row);
    }

    private static LinearLayout createRow(Context context, ViewGroup container, float density) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = (int)(12 * density);
        row.setLayoutParams(params);
        return row;
    }

    private static TextView createLabel(Context context, String text, float density) {
        TextView label = new TextView(context);
        label.setText(text);
        label.setTextColor(Color.WHITE);
        label.setTextSize(14);
        label.setTypeface(null, android.graphics.Typeface.BOLD);
        label.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        return label;
    }

    private static TextView createValueText(Context context, String text, int accent, float density) {
        TextView val = new TextView(context);
        val.setText(text);
        val.setTextColor(accent);
        val.setTextSize(13);
        val.setTypeface(null, android.graphics.Typeface.BOLD);
        val.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        return val;
    }

    private static void applyAccentToSeekBar(SeekBar seekBar, int accent) {
        seekBar.setProgressTintList(ColorStateList.valueOf(accent));
        seekBar.setThumbTintList(ColorStateList.valueOf(accent));
    }

    private static void addWithMargin(ViewGroup container, View view, float density) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = (int)(6 * density);
        container.addView(view, params);
    }

    private static String getKeyName(int keyCode) {
        String keyLabel = KeyEvent.keyCodeToString(keyCode);
        if (keyLabel != null && keyLabel.startsWith("KEYCODE_")) {
            keyLabel = keyLabel.substring(8);
        }
        return keyLabel != null ? keyLabel : "UNKNOWN";
    }

    private static int parseIntSafe(String s, int fallback) {
        if (s == null || s.isEmpty()) return fallback;
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return fallback; }
    }

    private static float parseFloatSafe(String s, float fallback) {
        if (s == null || s.isEmpty()) return fallback;
        try { return Float.parseFloat(s); } catch (NumberFormatException e) { return fallback; }
    }

    interface ValueChangeListener { void onValueChanged(int val); }
    interface ToggleChangeListener { void onToggleChanged(boolean val); }
}
