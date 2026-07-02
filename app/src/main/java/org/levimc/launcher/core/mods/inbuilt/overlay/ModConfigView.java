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
        addSlider(context, container, context.getString(R.string.mod_config_overlay_button_size_dp), manager.getOverlayButtonSize(mod.getId()), 20, 100, accent, density, progress -> {
            manager.setOverlayButtonSize(mod.getId(), progress);
            onConfigChanged.run();
        });

        // Opacity slider
        addSlider(context, container, context.getString(R.string.mod_config_overlay_opacity_percent), manager.getOverlayOpacity(mod.getId()), 10, 100, accent, density, progress -> {
            manager.setOverlayOpacity(mod.getId(), progress);
            onConfigChanged.run();
        });

        // Lock Position switch
        if (!mod.getId().equals(ModIds.CHICK_PET)) {
            addToggle(context, container, context.getString(R.string.overlay_button_lock), manager.isOverlayLocked(mod.getId()), accent, density, isChecked -> {
                manager.setOverlayLocked(mod.getId(), isChecked);
                onConfigChanged.run();
            });
        }

        // Auto Sprint specific
        if (mod.getId().equals(ModIds.AUTO_SPRINT)) {
            addKeybindCapture(context, container, context.getString(R.string.mod_config_auto_sprint_keybind), manager.getAutoSprintKeybind(), accent, density, keyCode -> {
                manager.setAutoSprintKeybind(keyCode);
                onConfigChanged.run();
            });
        }

        // Virtual Cursor specific
        if (mod.getId().equals(ModIds.VIRTUAL_CURSOR)) {
            addSlider(context, container, context.getString(R.string.mod_config_cursor_sensitivity_percent), manager.getCursorSensitivity(), 10, 200, accent, density, progress -> {
                manager.setCursorSensitivity(progress);
                onConfigChanged.run();
            });
        }

        // Zoom specific
        if (mod.getId().equals(ModIds.ZOOM)) {
            addSlider(context, container, context.getString(R.string.mod_config_zoom_level_percent), manager.getZoomLevel(), 10, 100, accent, density, progress -> {
                manager.setZoomLevel(progress);
                onConfigChanged.run();
            });
            addKeybindCapture(context, container, context.getString(R.string.mod_config_zoom_keybind), manager.getZoomKeybind(), accent, density, keyCode -> {
                manager.setZoomKeybind(keyCode);
                onConfigChanged.run();
            });
        }
    }

    private static void renderExternalConfig(Context context, ViewGroup container, UnifiedMod mod, int accent, float density, Runnable onConfigChanged) {
        List<UnifiedMod.ConfigEntry> configs = mod.getConfigEntries();
        
        java.util.Map<String, java.util.List<View>> configViews = new java.util.HashMap<>();
        Runnable applyDependencies = () -> {
            for (UnifiedMod.ConfigEntry cfg : configs) {
                if (cfg.dependsOn != null && !cfg.dependsOn.isEmpty() && configViews.containsKey(cfg.key)) {
                    boolean isParentEnabled = false;
                    for (UnifiedMod.ConfigEntry parentCfg : configs) {
                        if (parentCfg.key.equals(cfg.dependsOn)) {
                            isParentEnabled = "true".equalsIgnoreCase(parentCfg.currentValue) || "1".equals(parentCfg.currentValue);
                            break;
                        }
                    }
                    for (View v : configViews.get(cfg.key)) {
                        v.setVisibility(isParentEnabled ? View.VISIBLE : View.GONE);
                    }
                }
            }
        };
        Runnable wrappedOnConfigChanged = () -> {
            applyDependencies.run();
            onConfigChanged.run();
        };

        for (UnifiedMod.ConfigEntry cfg : configs) {
            int startIndex = container.getChildCount();
            switch (cfg.type) {
                case TOGGLE:
                    boolean checked = "true".equalsIgnoreCase(cfg.currentValue) || "1".equals(cfg.currentValue);
                    addToggle(context, container, cfg.displayName, checked, accent, density, isChecked -> {
                        cfg.currentValue = isChecked ? "true" : "false";
                        ExternalModBridge.setExternalModConfig(mod.getId(), cfg.key, cfg.currentValue);
                        wrappedOnConfigChanged.run();
                    });
                    break;
                case SLIDER_INT: {
                    int min = parseIntSafe(cfg.minValue, 0);
                    int max = parseIntSafe(cfg.maxValue, 100);
                    int cur = parseIntSafe(cfg.currentValue, parseIntSafe(cfg.defaultValue, min));
                    addSlider(context, container, cfg.displayName, cur, min, max, accent, density, progress -> {
                        cfg.currentValue = String.valueOf(progress);
                        ExternalModBridge.setExternalModConfig(mod.getId(), cfg.key, cfg.currentValue);
                        wrappedOnConfigChanged.run();
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
                                wrappedOnConfigChanged.run();
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
                                wrappedOnConfigChanged.run();
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
                    LinearLayout headerRow = new LinearLayout(context);
                    headerRow.setOrientation(LinearLayout.HORIZONTAL);
                    headerRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
                    LinearLayout.LayoutParams headerParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    headerParams.topMargin = (int)(12 * density);
                    headerRow.setLayoutParams(headerParams);

                    TextView label = createLabel(context, cfg.displayName, density);
                    headerRow.addView(label);

                    View colorPreview = new View(context);
                    LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(
                        (int)(40 * density), (int)(20 * density));
                    
                    String currentHex = cfg.currentValue.isEmpty() ? cfg.defaultValue : cfg.currentValue;
                    int initialColor = Color.WHITE;
                    try { initialColor = Color.parseColor(currentHex); } catch (Exception ignored) {}
                    
                    android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
                    gd.setColor(initialColor);
                    gd.setCornerRadius(4 * density);
                    gd.setStroke((int)(1 * density), 0xFF555555);
                    colorPreview.setBackground(gd);
                    headerRow.addView(colorPreview, previewParams);
                    container.addView(headerRow);

                    LinearLayout slidersContainer = new LinearLayout(context);
                    slidersContainer.setOrientation(LinearLayout.VERTICAL);
                    slidersContainer.setVisibility(View.GONE);
                    slidersContainer.setPadding((int)(12 * density), (int)(8 * density), 0, (int)(8 * density));

                    final int[] currentColor = {initialColor};
                    
                    ValueChangeListener onSliderChange = val -> {
                        gd.setColor(currentColor[0]);
                        colorPreview.setBackground(gd);
                        cfg.currentValue = String.format("#%08X", currentColor[0]);
                        ExternalModBridge.setExternalModConfig(mod.getId(), cfg.key, cfg.currentValue);
                        wrappedOnConfigChanged.run();
                    };

                    int a = Color.alpha(initialColor);
                    int r = Color.red(initialColor);
                    int g = Color.green(initialColor);
                    int b = Color.blue(initialColor);

                    SeekBar alphaSlider = addColorSliderInline(context, slidersContainer, context.getString(R.string.mod_config_color_alpha_short), a, 0xFFFFFFFF, density, progress -> {
                        currentColor[0] = Color.argb(progress, Color.red(currentColor[0]), Color.green(currentColor[0]), Color.blue(currentColor[0]));
                        onSliderChange.onValueChanged(progress);
                    });
                    SeekBar redSlider = addColorSliderInline(context, slidersContainer, context.getString(R.string.mod_config_color_red_short), r, 0xFFFF4444, density, progress -> {
                        currentColor[0] = Color.argb(Color.alpha(currentColor[0]), progress, Color.green(currentColor[0]), Color.blue(currentColor[0]));
                        onSliderChange.onValueChanged(progress);
                    });
                    SeekBar greenSlider = addColorSliderInline(context, slidersContainer, context.getString(R.string.mod_config_color_green_short), g, 0xFF44FF44, density, progress -> {
                        currentColor[0] = Color.argb(Color.alpha(currentColor[0]), Color.red(currentColor[0]), progress, Color.blue(currentColor[0]));
                        onSliderChange.onValueChanged(progress);
                    });
                    SeekBar blueSlider = addColorSliderInline(context, slidersContainer, context.getString(R.string.mod_config_color_blue_short), b, 0xFF4444FF, density, progress -> {
                        currentColor[0] = Color.argb(Color.alpha(currentColor[0]), Color.red(currentColor[0]), Color.green(currentColor[0]), progress);
                        onSliderChange.onValueChanged(progress);
                    });

                    container.addView(slidersContainer, new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

                    headerRow.setOnClickListener(v -> {
                        boolean isVisible = slidersContainer.getVisibility() == View.VISIBLE;
                        slidersContainer.setVisibility(isVisible ? View.GONE : View.VISIBLE);
                    });
                    break;
                }
            }

            int endIndex = container.getChildCount();
            java.util.List<View> viewsAdded = new java.util.ArrayList<>();
            for (int i = startIndex; i < endIndex; i++) {
                viewsAdded.add(container.getChildAt(i));
            }
            configViews.put(cfg.key, viewsAdded);
        }
        applyDependencies.run();
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
        btn.setText(getKeyName(context, currentKey));
        btn.setBackgroundTintList(ColorStateList.valueOf(0xFF333333));
        btn.setTextColor(accent);
        
        btn.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.Base_Theme_FullScreen); // using this to match theme
            builder.setTitle(labelText);
            builder.setMessage(R.string.mod_config_press_any_key);
            builder.setCancelable(true);
            builder.setNegativeButton(R.string.cancel, null);
            AlertDialog dialog = builder.create();
            dialog.setOnKeyListener((dialogInterface, keyCode, event) -> {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (keyCode == KeyEvent.KEYCODE_BACK) {
                        dialog.dismiss();
                        return true;
                    }
                    btn.setText(getKeyName(context, keyCode));
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

    private static SeekBar addColorSliderInline(Context context, ViewGroup container, String labelText, int value, int tint, float density, ValueChangeListener listener) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        
        TextView tv = new TextView(context);
        tv.setText(labelText);
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(12);
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        tv.setLayoutParams(new LinearLayout.LayoutParams((int)(20 * density), LinearLayout.LayoutParams.WRAP_CONTENT));
        
        SeekBar sb = new SeekBar(context);
        sb.setMax(255);
        sb.setProgress(value);
        sb.setProgressTintList(ColorStateList.valueOf(tint));
        sb.setThumbTintList(ColorStateList.valueOf(tint));
        sb.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) listener.onValueChanged(progress);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        row.addView(tv);
        row.addView(sb);
        
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rowParams.topMargin = (int)(4 * density);
        container.addView(row, rowParams);
        return sb;
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

    private static String getKeyName(Context context, int keyCode) {
        String keyLabel = KeyEvent.keyCodeToString(keyCode);
        if (keyLabel != null && keyLabel.startsWith("KEYCODE_")) {
            keyLabel = keyLabel.substring(8);
        }
        return keyLabel != null ? keyLabel : context.getString(R.string.mod_config_key_unknown);
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
