package org.levimc.launcher.core.mods.inbuilt.overlay;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.levimc.launcher.R;
import org.levimc.launcher.core.mods.inbuilt.ExternalModBridge;

public class ExternalButtonOverlay extends BaseOverlayButton {
    private static final int KEYCAP_NORMAL_BG = 0xFF8B8B8B;
    private static final int KEYCAP_ACTIVE_BG = 0xFFC6C6C6;
    private static final int KEYCAP_BORDER = 0xFF373737;
    private static final int KEYCAP_TEXT = 0xFF373737;
    private static final int KEYCAP_ACTIVE_TEXT = 0xFF1F1F1F;
    private static final int ACCENT_NORMAL_BG = 0xCC24282C;
    private static final int ACCENT_ACTIVE_BG = 0xFF4AE0A0;
    private static final int ACCENT_BORDER = 0x994AE0A0;
    private static final int ACCENT_TEXT = Color.WHITE;
    private static final int ACCENT_ACTIVE_TEXT = Color.BLACK;
    private static final float MIN_WIDTH_SCALE = 0.6f;
    private static final float MAX_WIDTH_SCALE = 4.0f;
    private static final float MIN_HEIGHT_SCALE = 0.6f;
    private static final float MAX_HEIGHT_SCALE = 2.0f;

    private final ExternalModBridge.ExternalButton button;
    private TextView labelView;
    private ImageView iconView;
    private boolean iconVisible = false;
    private boolean active = false;
    private boolean hudEditorMode = false;

    public ExternalButtonOverlay(Activity activity, ExternalModBridge.ExternalButton button) {
        super(activity);
        this.button = button;
    }

    public String getButtonId() {
        return button.buttonId;
    }

    public String getModuleId() {
        return button.moduleId;
    }

    @Override
    protected String getModId() {
        return button.positionKey();
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.overlay_external_button;
    }

    @Override
    protected int getIconResource() {
        return 0;
    }

    @Override
    protected float getWidthScale() {
        if (button.widthScale > 0f) {
            return clamp(button.widthScale, MIN_WIDTH_SCALE, MAX_WIDTH_SCALE);
        }
        return autoWidthScaleForLabel(getLabelText());
    }

    @Override
    protected float getHeightScale() {
        if (button.heightScale > 0f) {
            return clamp(button.heightScale, MIN_HEIGHT_SCALE, MAX_HEIGHT_SCALE);
        }
        return 1.0f;
    }

    @Override
    protected void configureOverlayView(View view) {
        labelView = view.findViewById(R.id.external_overlay_label);
        iconView = view.findViewById(R.id.external_overlay_icon);
        view.setContentDescription(button.displayName);
        if (labelView != null) {
            labelView.setText(getLabelText());
            labelView.setContentDescription(button.displayName);
            labelView.setSingleLine(true);
            labelView.setEllipsize(TextUtils.TruncateAt.END);
            labelView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            updateLabelTextSize();
        }
        loadIcon();
        updateActiveVisual();
    }

    @Override
    public void setHudEditorMode(boolean active) {
        hudEditorMode = active;
        super.setHudEditorMode(active);
        if (!active) {
            updateActiveVisual();
        }
    }

    @Override
    public void applyConfigurationChanges() {
        super.applyConfigurationChanges();
        loadIcon();
        updateLabelTextSize();
        updateActiveVisual();
    }

    @Override
    protected void onButtonSizeChanged() {
        updateLabelTextSize();
    }

    @Override
    protected void onButtonPressStart() {
        if (button.behavior != ExternalModBridge.ExternalButton.BEHAVIOR_HOLD || active) return;
        active = true;
        if (button.androidKeyCode > 0) {
            sendKeyDown(button.androidKeyCode);
        }
        ExternalModBridge.dispatchExternalButtonEvent(
                button.buttonId, ExternalModBridge.ExternalButton.EVENT_DOWN, 1f);
        updateActiveVisual();
    }

    @Override
    protected void onButtonPressEnd() {
        if (button.behavior != ExternalModBridge.ExternalButton.BEHAVIOR_HOLD || !active) return;
        active = false;
        if (button.androidKeyCode > 0) {
            sendKeyUp(button.androidKeyCode);
        }
        ExternalModBridge.dispatchExternalButtonEvent(
                button.buttonId, ExternalModBridge.ExternalButton.EVENT_UP, 0f);
        updateActiveVisual();
    }

    @Override
    protected void onButtonClick() {
        if (button.behavior == ExternalModBridge.ExternalButton.BEHAVIOR_HOLD) return;

        if (button.behavior == ExternalModBridge.ExternalButton.BEHAVIOR_TOGGLE) {
            active = !active;
            if (button.androidKeyCode > 0) {
                if (active) {
                    sendKeyDown(button.androidKeyCode);
                } else {
                    sendKeyUp(button.androidKeyCode);
                }
            }
            ExternalModBridge.dispatchExternalButtonEvent(
                    button.buttonId,
                    ExternalModBridge.ExternalButton.EVENT_STATE_CHANGED,
                    active ? 1f : 0f);
            updateActiveVisual();
            return;
        }

        if (button.androidKeyCode > 0) {
            sendKey(button.androidKeyCode);
        }
        ExternalModBridge.dispatchExternalButtonEvent(
                button.buttonId, ExternalModBridge.ExternalButton.EVENT_CLICK, 1f);
    }

    public boolean onScroll(float delta) {
        if (!active) return false;
        if (button.behavior != ExternalModBridge.ExternalButton.BEHAVIOR_HOLD
                && button.behavior != ExternalModBridge.ExternalButton.BEHAVIOR_TOGGLE) {
            return false;
        }
        ExternalModBridge.dispatchExternalButtonEvent(
                button.buttonId, ExternalModBridge.ExternalButton.EVENT_SCROLL, delta);
        return true;
    }

    @Override
    public void hide() {
        if (active && button.behavior == ExternalModBridge.ExternalButton.BEHAVIOR_HOLD) {
            onButtonPressEnd();
        } else if (active && button.behavior == ExternalModBridge.ExternalButton.BEHAVIOR_TOGGLE) {
            if (button.androidKeyCode > 0) {
                sendKeyUp(button.androidKeyCode);
            }
            ExternalModBridge.dispatchExternalButtonEvent(
                    button.buttonId,
                    ExternalModBridge.ExternalButton.EVENT_STATE_CHANGED,
                    0f);
            active = false;
        }
        super.hide();
    }

    private String getLabelText() {
        if (button.label != null && !button.label.trim().isEmpty()) {
            return button.label.trim();
        }
        if (button.displayName != null && !button.displayName.trim().isEmpty()) {
            return button.displayName.trim().substring(0, 1).toUpperCase();
        }
        return "?";
    }

    private float autoWidthScaleForLabel(String text) {
        int length = Math.max(1, text.codePointCount(0, text.length()));
        if (length == 1) {
            return 1.0f;
        }
        if (length == 2) {
            return 1.25f;
        }
        if (length <= 4) {
            return 1.8f;
        }
        if (length <= 6) {
            return 2.2f;
        }
        if (length <= 8) {
            return 2.6f;
        }
        return 3.0f;
    }

    private float clamp(float value, float min, float max) {
        if (Float.isNaN(value) || Float.isInfinite(value)) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }

    private void updateActiveVisual() {
        if (overlayView == null) return;
        overlayView.setAlpha(getButtonOpacity());
        if (!hudEditorMode) {
            overlayView.setBackground(createButtonBackground(active));
        }
        if (labelView == null) {
            labelView = overlayView.findViewById(R.id.external_overlay_label);
        }
        if (labelView != null) {
            labelView.setTextColor(resolveTextColor(active));
        }
        if (iconView != null && iconVisible) {
            iconView.setColorFilter(resolveTextColor(active), android.graphics.PorterDuff.Mode.SRC_IN);
        }
        if (button.hasIcon && button.iconFormat == ExternalModBridge.ExternalButton.ICON_RESOURCE) {
            loadIcon();
        }
    }

    private void loadIcon() {
        iconVisible = false;
        if (iconView == null) {
            return;
        }
        iconView.setVisibility(View.GONE);
        iconView.setImageDrawable(null);
        if (!button.hasIcon) {
            updateLabelVisibility();
            return;
        }

        byte[] iconBytes = ExternalModBridge.getExternalButtonIconBytes(
                button.buttonId, getButtonWidthPx(), getButtonHeightPx());
        if (iconBytes == null || iconBytes.length == 0) {
            updateLabelVisibility();
            return;
        }

        if (button.iconFormat == ExternalModBridge.ExternalButton.ICON_RESOURCE) {
            String resNamesStr = new String(iconBytes, java.nio.charset.StandardCharsets.UTF_8);
            String[] resNames = resNamesStr.split(",");
            String resName = resNames[0];
            if (active && resNames.length > 1) {
                resName = resNames[1];
            }
            int resId = activity.getResources().getIdentifier(resName, "drawable", activity.getPackageName());
            if (resId != 0) {
                iconView.setImageResource(resId);
                iconView.setContentDescription(button.displayName);
                iconView.setVisibility(View.VISIBLE);
                iconVisible = true;
                updateLabelVisibility();
                return;
            }
        }

        Bitmap bitmap = BitmapFactory.decodeByteArray(iconBytes, 0, iconBytes.length);
        if (bitmap == null) {
            updateLabelVisibility();
            return;
        }

        iconView.setImageBitmap(bitmap);
        iconView.setContentDescription(button.displayName);
        iconView.setVisibility(View.VISIBLE);
        iconVisible = true;
        updateLabelVisibility();
    }

    private void updateLabelVisibility() {
        if (labelView == null) {
            return;
        }
        boolean hideLabel = iconVisible && button.hideLabelWhenIconPresent;
        labelView.setVisibility(hideLabel ? View.GONE : View.VISIBLE);
    }

    private GradientDrawable createButtonBackground(boolean activeState) {
        float density = activity.getResources().getDisplayMetrics().density;
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setColor(resolveBackgroundColor(activeState));
        drawable.setStroke(Math.max(1, Math.round(2f * density)), resolveBorderColor());
        drawable.setCornerRadius((button.stylePreset == ExternalModBridge.ExternalButton.STYLE_ACCENT ? 8f : 2f) * density);
        return drawable;
    }

    private int resolveBackgroundColor(boolean activeState) {
        int override = activeState ? button.activeBgColor : button.normalBgColor;
        if (override != 0) return override;
        if (button.stylePreset == ExternalModBridge.ExternalButton.STYLE_ACCENT) {
            return activeState ? ACCENT_ACTIVE_BG : ACCENT_NORMAL_BG;
        }
        return activeState ? KEYCAP_ACTIVE_BG : KEYCAP_NORMAL_BG;
    }

    private int resolveBorderColor() {
        if (button.borderColor != 0) return button.borderColor;
        if (button.stylePreset == ExternalModBridge.ExternalButton.STYLE_ACCENT) {
            return ACCENT_BORDER;
        }
        return KEYCAP_BORDER;
    }

    private int resolveTextColor(boolean activeState) {
        int override = activeState ? button.activeTextColor : button.textColor;
        if (override != 0) return override;
        if (button.stylePreset == ExternalModBridge.ExternalButton.STYLE_ACCENT) {
            return activeState ? ACCENT_ACTIVE_TEXT : ACCENT_TEXT;
        }
        return activeState ? KEYCAP_ACTIVE_TEXT : KEYCAP_TEXT;
    }

    private void updateLabelTextSize() {
        if (labelView == null) return;
        String text = getLabelText();
        int length = Math.max(1, text.codePointCount(0, text.length()));
        float buttonWidth = getButtonWidthPx();
        float buttonHeight = getButtonHeightPx();
        float density = activity.getResources().getDisplayMetrics().density;
        float estimatedTextUnits = length * 0.7f + 0.6f;
        float maxByWidth = buttonWidth / estimatedTextUnits;
        float maxByHeight = buttonHeight * 0.44f;
        float textPx = Math.max(9f * density, Math.min(24f * density, Math.min(maxByWidth, maxByHeight)));
        labelView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textPx);
    }
}
