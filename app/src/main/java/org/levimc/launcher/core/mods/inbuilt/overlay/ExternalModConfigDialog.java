package org.levimc.launcher.core.mods.inbuilt.overlay;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Button;

import org.levimc.launcher.R;
import org.levimc.launcher.core.mods.inbuilt.ExternalModBridge;
import org.levimc.launcher.core.mods.inbuilt.UnifiedMod;

import java.util.List;

public class ExternalModConfigDialog {

    public static void show(Context context, UnifiedMod mod) {
        List<UnifiedMod.ConfigEntry> configs = mod.getConfigEntries();
        if (configs.isEmpty()) return;

        Context themedContext = new android.view.ContextThemeWrapper(context, R.style.Base_Theme_FullScreen);
        Dialog dialog = new Dialog(themedContext);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        float density = context.getResources().getDisplayMetrics().density;
        int screenWidth = context.getResources().getDisplayMetrics().widthPixels;
        int maxWidth = (int) (380 * density);

        android.widget.ScrollView scrollView = new android.widget.ScrollView(themedContext);
        scrollView.setFillViewport(true);

        LinearLayout root = new LinearLayout(themedContext);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding((int)(20*density), (int)(20*density), (int)(20*density), (int)(20*density));
        root.setBackgroundColor(Color.parseColor("#1A1A1A"));

        // Title
        TextView title = new TextView(themedContext);
        title.setText(mod.getName());
        title.setTextColor(0xFF4AE0A0);
        title.setTextSize(18);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        root.addView(title);

        if (mod.getDescription() != null && !mod.getDescription().isEmpty()) {
            TextView desc = new TextView(themedContext);
            desc.setText(mod.getDescription());
            desc.setTextColor(0xFF888888);
            desc.setTextSize(12);
            LinearLayout.LayoutParams descParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            descParams.topMargin = (int)(4*density);
            root.addView(desc, descParams);
        }

        // Spacer
        android.view.View spacer = new android.view.View(themedContext);
        LinearLayout.LayoutParams spacerParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, (int)(12*density));
        root.addView(spacer, spacerParams);

        // Config entries
        for (UnifiedMod.ConfigEntry cfg : configs) {
            LinearLayout row = new LinearLayout(themedContext);
            row.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            rowParams.topMargin = (int)(12*density);

            TextView label = new TextView(themedContext);
            label.setText(cfg.displayName);
            label.setTextColor(Color.WHITE);
            label.setTextSize(14);
            label.setTypeface(null, android.graphics.Typeface.BOLD);
            row.addView(label);

            switch (cfg.type) {
                case TOGGLE: {
                    Switch toggle = new Switch(themedContext);
                    boolean checked = "true".equalsIgnoreCase(cfg.currentValue)
                            || "1".equals(cfg.currentValue);
                    toggle.setChecked(checked);
                    toggle.setOnCheckedChangeListener((btn, isChecked) -> {
                        cfg.currentValue = isChecked ? "true" : "false";
                        ExternalModBridge.setExternalModConfig(mod.getId(), cfg.key, cfg.currentValue);
                    });
                    int[][] states = {{android.R.attr.state_checked}, {}};
                    toggle.setThumbTintList(new android.content.res.ColorStateList(
                            states, new int[]{0xFF4AE0A0, 0xFFAAAAAA}));
                    int trackColor = Color.argb(100, 0x4A, 0xE0, 0xA0);
                    toggle.setTrackTintList(new android.content.res.ColorStateList(
                            states, new int[]{trackColor, 0xFF555555}));
                    LinearLayout.LayoutParams toggleParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    toggleParams.topMargin = (int)(4*density);
                    row.addView(toggle, toggleParams);
                    break;
                }
                case SLIDER_INT: {
                    int min = parseIntSafe(cfg.minValue, 0);
                    int max = parseIntSafe(cfg.maxValue, 100);
                    int cur = parseIntSafe(cfg.currentValue, parseIntSafe(cfg.defaultValue, min));

                    TextView valText = new TextView(themedContext);
                    valText.setText(String.valueOf(cur));
                    valText.setTextColor(0xFF4AE0A0);
                    valText.setTextSize(12);
                    LinearLayout.LayoutParams valParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    valParams.topMargin = (int)(2*density);
                    row.addView(valText, valParams);

                    SeekBar seekBar = new SeekBar(themedContext);
                    seekBar.setMin(min);
                    seekBar.setMax(max);
                    seekBar.setProgress(cur);
                    seekBar.setProgressTintList(android.content.res.ColorStateList.valueOf(0xFF4AE0A0));
                    seekBar.setThumbTintList(android.content.res.ColorStateList.valueOf(0xFF4AE0A0));
                    seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                        @Override
                        public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                            if (fromUser) {
                                valText.setText(String.valueOf(progress));
                                cfg.currentValue = String.valueOf(progress);
                                ExternalModBridge.setExternalModConfig(mod.getId(), cfg.key, cfg.currentValue);
                            }
                        }
                        @Override public void onStartTrackingTouch(SeekBar sb) {}
                        @Override public void onStopTrackingTouch(SeekBar sb) {}
                    });
                    LinearLayout.LayoutParams seekParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    seekParams.topMargin = (int)(4*density);
                    row.addView(seekBar, seekParams);
                    break;
                }
                case SLIDER_FLOAT: {
                    float fMin = parseFloatSafe(cfg.minValue, 0f);
                    float fMax = parseFloatSafe(cfg.maxValue, 1f);
                    float fCur = parseFloatSafe(cfg.currentValue, parseFloatSafe(cfg.defaultValue, fMin));
                    int steps = 100;

                    TextView valText = new TextView(themedContext);
                    valText.setText(String.format("%.2f", fCur));
                    valText.setTextColor(0xFF4AE0A0);
                    valText.setTextSize(12);
                    LinearLayout.LayoutParams valParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    valParams.topMargin = (int)(2*density);
                    row.addView(valText, valParams);

                    SeekBar seekBar = new SeekBar(themedContext);
                    seekBar.setMin(0);
                    seekBar.setMax(steps);
                    int initProgress = (int)((fCur - fMin) / (fMax - fMin) * steps);
                    seekBar.setProgress(Math.max(0, Math.min(steps, initProgress)));
                    seekBar.setProgressTintList(android.content.res.ColorStateList.valueOf(0xFF4AE0A0));
                    seekBar.setThumbTintList(android.content.res.ColorStateList.valueOf(0xFF4AE0A0));
                    final float finalFMin = fMin;
                    final float finalFMax = fMax;
                    seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                        @Override
                        public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                            if (fromUser) {
                                float val = finalFMin + (finalFMax - finalFMin) * progress / (float) steps;
                                valText.setText(String.format("%.2f", val));
                                cfg.currentValue = String.valueOf(val);
                                ExternalModBridge.setExternalModConfig(mod.getId(), cfg.key, cfg.currentValue);
                            }
                        }
                        @Override public void onStartTrackingTouch(SeekBar sb) {}
                        @Override public void onStopTrackingTouch(SeekBar sb) {}
                    });
                    LinearLayout.LayoutParams seekParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    seekParams.topMargin = (int)(4*density);
                    row.addView(seekBar, seekParams);
                    break;
                }
                case RADIO: {
                    android.widget.RadioGroup radioGroup = new android.widget.RadioGroup(themedContext);
                    radioGroup.setOrientation(LinearLayout.VERTICAL);

                    String[] options = cfg.minValue != null ? cfg.minValue.split(",") : new String[0];
                    int selectedIndex = parseIntSafe(cfg.currentValue, parseIntSafe(cfg.defaultValue, 0));

                    for (int i = 0; i < options.length; i++) {
                        android.widget.RadioButton rb = new android.widget.RadioButton(themedContext);
                        rb.setText(options[i]);
                        rb.setTextColor(Color.WHITE);
                        rb.setId(android.view.View.generateViewId());
                        int currentIndex = i;
                        if (i == selectedIndex) {
                            rb.setChecked(true);
                        }
                        
                        rb.setOnCheckedChangeListener((btn, isChecked) -> {
                            if (isChecked) {
                                cfg.currentValue = String.valueOf(currentIndex);
                                ExternalModBridge.setExternalModConfig(mod.getId(), cfg.key, cfg.currentValue);
                            }
                        });
                        
                        radioGroup.addView(rb);
                    }
                    
                    LinearLayout.LayoutParams rgParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    rgParams.topMargin = (int)(4*density);
                    row.addView(radioGroup, rgParams);
                    break;
                }
                case COLOR: {
                    android.widget.EditText hexInput = new android.widget.EditText(themedContext);
                    hexInput.setText(cfg.currentValue.isEmpty() ? cfg.defaultValue : cfg.currentValue);
                    hexInput.setTextColor(0xFF4AE0A0);
                    hexInput.setHint("#AARRGGBB");
                    hexInput.setHintTextColor(0xFF888888);
                    
                    android.view.View colorPreview = new android.view.View(themedContext);
                    LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(
                        (int)(30*density), (int)(30*density)
                    );
                    previewParams.topMargin = (int)(4*density);
                    
                    try {
                        colorPreview.setBackgroundColor(Color.parseColor(hexInput.getText().toString()));
                    } catch (Exception e) {}

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
                                } catch (Exception e) {}
                            }
                        }
                    });

                    row.addView(hexInput);
                    row.addView(colorPreview, previewParams);
                    break;
                }
            }
            root.addView(row, rowParams);
        }

        // Close button
        Button closeBtn = new Button(themedContext);
        closeBtn.setText(android.R.string.ok);
        closeBtn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF4AE0A0));
        closeBtn.setTextColor(Color.BLACK);
        LinearLayout.LayoutParams closeBtnParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        closeBtnParams.topMargin = (int)(20*density);
        closeBtn.setOnClickListener(v -> dialog.dismiss());
        root.addView(closeBtn, closeBtnParams);

        scrollView.addView(root);
        dialog.setContentView(scrollView);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            android.view.WindowManager.LayoutParams params = window.getAttributes();
            params.dimAmount = 0.6f;
            params.width = Math.min((int) (screenWidth * 0.9), maxWidth);
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            window.setAttributes(params);
        }

        dialog.show();
    }

    private static int parseIntSafe(String s, int fallback) {
        if (s == null || s.isEmpty()) return fallback;
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return fallback; }
    }

    private static float parseFloatSafe(String s, float fallback) {
        if (s == null || s.isEmpty()) return fallback;
        try { return Float.parseFloat(s); } catch (NumberFormatException e) { return fallback; }
    }
}
