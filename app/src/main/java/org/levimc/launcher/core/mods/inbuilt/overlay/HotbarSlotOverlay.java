package org.levimc.launcher.core.mods.inbuilt.overlay;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageButton;

import org.levimc.launcher.R;
import org.levimc.launcher.core.mods.inbuilt.model.ModIds;
import org.levimc.launcher.core.mods.inbuilt.nativemod.MoreButtonsMod;

public final class HotbarSlotOverlay extends BaseOverlayButton {
    private static final float PRESSED_Y = 8f;

    private final int slot;
    private Bitmap normalBitmap;
    private Bitmap pressedBitmap;
    private boolean pressed;
    private boolean keyDown;

    public HotbarSlotOverlay(Activity activity, int slot) {
        super(activity);
        this.slot = slot;
    }

    public int getSlot() {
        return slot;
    }

    @Override
    protected String getModId() {
        return ModIds.HOTBAR_SLOT + ":" + slot;
    }

    @Override
    public String getOverlayConfigKey() {
        return ModIds.HOTBAR_SLOT;
    }

    @Override
    protected int getIconResource() {
        return 0;
    }

    @Override
    protected void configureOverlayView(View view) {
        rebuildBitmaps();
        updateVisual();
        view.setContentDescription(activity.getString(R.string.hotbar_slot_content_description, slot));
    }

    @Override
    protected void onButtonPressStart() {
        if (keyDown) return;
        keyDown = true;
        pressed = true;
        sendSlotKey(true);
        updateVisual();
    }

    @Override
    protected void onButtonPressEnd() {
        if (!keyDown) return;
        sendSlotKey(false);
        keyDown = false;
        pressed = false;
        updateVisual();
    }

    @Override
    protected void onButtonClick() {
    }

    @Override
    public void hide() {
        if (keyDown) sendSlotKey(false);
        keyDown = false;
        pressed = false;
        super.hide();
        recycleBitmaps();
    }

    @Override
    public void applyConfigurationChanges() {
        super.applyConfigurationChanges();
        rebuildBitmaps();
        updateVisual();
    }

    private void sendSlotKey(boolean down) {
        int bedrockCode = '0' + slot;
        if (MoreButtonsMod.sendKey(bedrockCode, down)) return;
        int androidCode = KeyEvent.KEYCODE_1 + slot - 1;
        if (down) sendKeyDown(androidCode); else sendKeyUp(androidCode);
    }

    private void updateVisual() {
        if (!(overlayView instanceof ImageButton)) return;
        ImageButton button = (ImageButton) overlayView;
        button.setImageBitmap(pressed ? pressedBitmap : normalBitmap);
        button.setScaleType(ImageButton.ScaleType.FIT_CENTER);
        button.setAlpha(getButtonOpacity());
    }

    private void rebuildBitmaps() {
        recycleBitmaps();
        normalBitmap = createBitmap(false);
        pressedBitmap = createBitmap(true);
    }

    private Bitmap createBitmap(boolean down) {
        Bitmap base = BitmapFactory.decodeResource(activity.getResources(),
                down ? R.drawable.more_button_base_pressed : R.drawable.more_button_base_normal);
        if (base == null) return Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888);
        Bitmap result = base.copy(Bitmap.Config.ARGB_8888, true);
        if (base != result && !base.isRecycled()) base.recycle();

        Canvas canvas = new Canvas(result);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(down ? Color.rgb(230, 230, 230) : Color.BLACK);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setTextSize(250f);
        Paint.FontMetrics fm = paint.getFontMetrics();
        float yOffset = down ? PRESSED_Y : 0f;
        float y = 256f - (fm.ascent + fm.descent) / 2f + yOffset;
        canvas.drawText(Integer.toString(slot), 256f, y, paint);
        return result;
    }

    private void recycleBitmaps() {
        if (normalBitmap != null && !normalBitmap.isRecycled()) normalBitmap.recycle();
        if (pressedBitmap != null && !pressedBitmap.isRecycled()) pressedBitmap.recycle();
        normalBitmap = null;
        pressedBitmap = null;
    }
}
