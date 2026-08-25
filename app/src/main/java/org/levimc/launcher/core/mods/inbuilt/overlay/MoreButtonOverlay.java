package org.levimc.launcher.core.mods.inbuilt.overlay;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.view.KeyEvent;
import android.widget.ImageButton;

import org.levimc.launcher.R;
import org.levimc.launcher.core.mods.inbuilt.MoreButtonsSvgBridge;
import org.levimc.launcher.core.mods.inbuilt.manager.MoreButtonsManager;
import org.levimc.launcher.core.mods.inbuilt.model.MoreButtonConfig;
import org.levimc.launcher.core.mods.inbuilt.nativemod.MoreButtonsMod;
import org.levimc.pojavcontrols.KeyMapper;

public final class MoreButtonOverlay extends BaseOverlayButton {
    private final String buttonId;
    private MoreButtonConfig config;
    private boolean active;
    private boolean keyDown;
    private Bitmap normalBitmap;
    private Bitmap pressedBitmap;

    public MoreButtonOverlay(Activity activity, MoreButtonConfig config) {
        super(activity);
        this.buttonId = config.id;
        this.config = config.copy();
    }

    public String getButtonId() {
        return buttonId;
    }

    @Override
    protected String getModId() {
        return config.overlayKey();
    }

    @Override
    protected int getIconResource() {
        return 0;
    }

    @Override
    protected void configureOverlayView(android.view.View view) {
        rebuildBitmaps();
        updateVisual();
        view.setContentDescription(config.name);
    }

    @Override
    protected void onButtonPressStart() {
        if (!MoreButtonConfig.isSupportedMapping(config.keyCode)) return;

        if (config.toggle) {
            active = !active;
            sendMappedInput(config.keyCode, active);
            updateVisual();
            return;
        }

        if (keyDown) return;
        keyDown = true;
        active = true;
        sendMappedInput(config.keyCode, true);
        updateVisual();
    }

    @Override
    protected void onButtonPressEnd() {
        if (config.toggle || !keyDown) return;
        sendMappedInput(config.keyCode, false);
        keyDown = false;
        active = false;
        updateVisual();
    }

    @Override
    protected void onButtonClick() {
    }

    @Override
    public void hide() {
        if ((keyDown || active) && MoreButtonConfig.isSupportedMapping(config.keyCode)) {
            sendMappedInput(config.keyCode, false);
        }
        keyDown = false;
        active = false;
        super.hide();
        recycleBitmaps();
    }

    @Override
    public void applyConfigurationChanges() {
        MoreButtonConfig latest = MoreButtonsManager.getInstance(activity).getButton(buttonId);
        if (latest != null) {
            if ((keyDown || active)
                    && (latest.keyCode != config.keyCode || latest.toggle != config.toggle)
                    && MoreButtonConfig.isSupportedMapping(config.keyCode)) {
                sendMappedInput(config.keyCode, false);
                keyDown = false;
                active = false;
            }
            config = latest.copy();
            rebuildBitmaps();
            updateVisual();
        }
        super.applyConfigurationChanges();
    }

    private void sendMappedInput(int mappingCode, boolean down) {
        int bedrockCode = KeyMapper.toBedrock(mappingCode);
        if (bedrockCode <= 0) return;
        if (MoreButtonsMod.sendKey(bedrockCode, down)) return;
        int androidCode = KeyMapper.toAndroidKeyCode(mappingCode);
        if (androidCode == KeyEvent.KEYCODE_UNKNOWN) return;
        if (down) sendKeyDown(androidCode); else sendKeyUp(androidCode);
    }

    private void updateVisual() {
        if (!(overlayView instanceof ImageButton)) return;
        ImageButton button = (ImageButton) overlayView;
        button.setImageBitmap(active ? pressedBitmap : normalBitmap);
        button.setScaleType(ImageButton.ScaleType.FIT_CENTER);
        button.setAlpha(getButtonOpacity());
        button.setContentDescription(config.name);
    }

    private void rebuildBitmaps() {
        recycleBitmaps();
        normalBitmap = createButtonBitmap(activity, config, false);
        pressedBitmap = createButtonBitmap(activity, config, true);
    }

    private void recycleBitmaps() {
        if (normalBitmap != null && !normalBitmap.isRecycled()) normalBitmap.recycle();
        if (pressedBitmap != null && !pressedBitmap.isRecycled()) pressedBitmap.recycle();
        normalBitmap = null;
        pressedBitmap = null;
    }

    public static Bitmap createButtonBitmap(Activity activity, MoreButtonConfig config, boolean pressed) {
        Bitmap base = BitmapFactory.decodeResource(activity.getResources(),
                pressed ? R.drawable.more_button_base_pressed : R.drawable.more_button_base_normal);
        if (base == null) return Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888);
        Bitmap output = base.copy(Bitmap.Config.ARGB_8888, true);
        if (base != output && !base.isRecycled()) base.recycle();

        boolean hasPressedSvg = config.pressedSvg != null && !config.pressedSvg.trim().isEmpty();
        String svg = pressed && hasPressedSvg ? config.pressedSvg : config.normalSvg;
        if (svg == null || svg.trim().isEmpty()) return output;

        boolean keepColors = pressed ? config.keepPressedColors : config.keepNormalColors;
        if (!keepColors) {
            svg = MoreButtonsManager.recolorSvg(svg, pressed ? "#E6E6E6" : "#000000");
        }

        int iconSize = Math.max(96, Math.min(410, Math.round(512f * config.iconScale / 100f)));
        byte[] png = MoreButtonsSvgBridge.renderSvg(svg, iconSize, iconSize);
        if (png == null || png.length == 0) return output;
        Bitmap icon = BitmapFactory.decodeByteArray(png, 0, png.length);
        if (icon == null) return output;

        int left = (512 - iconSize) / 2 + config.iconOffsetX;
        int top = (512 - iconSize) / 2 + config.iconOffsetY + (pressed ? 8 : 0);
        Canvas canvas = new Canvas(output);
        canvas.drawBitmap(icon, null, new Rect(left, top, left + iconSize, top + iconSize), null);
        icon.recycle();
        return output;
    }
}
