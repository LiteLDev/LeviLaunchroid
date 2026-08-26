package org.levimc.launcher.core.mods.inbuilt.overlay;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageButton;

import org.levimc.launcher.R;
import org.levimc.launcher.core.mods.inbuilt.manager.InbuiltModManager;
import org.levimc.launcher.core.mods.inbuilt.model.ModIds;
import org.levimc.launcher.core.mods.inbuilt.nativemod.HotbarSlotMod;
import org.levimc.launcher.core.mods.inbuilt.nativemod.MoreButtonsMod;

public final class HotbarSlotOverlay extends BaseOverlayButton {
    private static final float PRESSED_Y = 8f;
    private static final float NUMBER_Y = -8f;
    private static final float ICON_WINDOW_LEFT = 93f;
    private static final float ICON_WINDOW_TOP = 93f;
    private static final float ICON_WINDOW_RIGHT = 419f;
    private static final float ICON_WINDOW_BOTTOM = 396f;

    private final int slot;
    private Bitmap normalBitmap;
    private Bitmap pressedBitmap;
    private boolean pressed;
    private boolean keyDown;
    private volatile boolean renderVisible = true;
    private volatile int nativeX;
    private volatile int nativeY;
    private volatile int nativeWidth;
    private volatile int nativeHeight;
    private volatile int nativeSurfaceWidth;
    private volatile int nativeSurfaceHeight;
    private volatile boolean hasItem;
    private volatile boolean lastBitmapItemState;

    public HotbarSlotOverlay(Activity activity, int slot) {
        super(activity);
        this.slot = slot;
        boolean enabled = InbuiltModManager.getInstance(activity).isHotbarItemIconsEnabled();
        HotbarSlotMod.setEnabled(enabled);
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
        return getModId();
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
    protected void onOverlayGeometryChanged(int x, int y, int width, int height) {
        nativeX = x;
        nativeY = y;
        nativeWidth = width;
        nativeHeight = height;
        View decor = activity.getWindow() != null ? activity.getWindow().getDecorView() : null;
        nativeSurfaceWidth = decor != null && decor.getWidth() > 0
                ? decor.getWidth()
                : activity.getResources().getDisplayMetrics().widthPixels;
        nativeSurfaceHeight = decor != null && decor.getHeight() > 0
                ? decor.getHeight()
                : activity.getResources().getDisplayMetrics().heightPixels;
        syncNativeState();
    }

    @Override
    protected void onButtonPressStart() {
        if (keyDown) return;
        keyDown = true;
        pressed = true;
        sendSlotKey(true);
        updateVisual();
        syncNativeState();
    }

    @Override
    protected void onButtonPressEnd() {
        if (!keyDown) return;
        sendSlotKey(false);
        keyDown = false;
        pressed = false;
        updateVisual();
        syncNativeState();
    }

    @Override
    protected void onButtonClick() {
    }

    @Override
    public void tick() {
        if (!itemIconsEnabled()) return;
        boolean current = HotbarSlotMod.hasItem(slot - 1);
        hasItem = current;
        if (current != lastBitmapItemState) {
            lastBitmapItemState = current;
            activity.runOnUiThread(() -> {
                rebuildBitmaps();
                updateVisual();
            });
        }
    }

    public void setRenderVisible(boolean visible) {
        renderVisible = visible;
        syncNativeState();
    }

    @Override
    public void hide() {
        if (keyDown) sendSlotKey(false);
        keyDown = false;
        pressed = false;
        renderVisible = false;
        HotbarSlotMod.clearSlot(slot - 1);
        super.hide();
        recycleBitmaps();
    }

    @Override
    public void applyConfigurationChanges() {
        boolean enabled = itemIconsEnabled();
        HotbarSlotMod.setEnabled(enabled);
        if (!enabled) {
            hasItem = false;
            lastBitmapItemState = false;
        }
        super.applyConfigurationChanges();
        rebuildBitmaps();
        updateVisual();
        syncNativeState();
    }

    private boolean itemIconsEnabled() {
        return InbuiltModManager.getInstance(activity).isHotbarItemIconsEnabled();
    }

    private void syncNativeState() {
        if (!itemIconsEnabled()) return;
        HotbarSlotMod.setSlotState(slot - 1, nativeX, nativeY, nativeWidth, nativeHeight,
                nativeSurfaceWidth, nativeSurfaceHeight, getButtonOpacity(), renderVisible, pressed);
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
        if (button.getDrawable() instanceof BitmapDrawable) {
            ((BitmapDrawable) button.getDrawable()).setFilterBitmap(false);
        }
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
        if (itemIconsEnabled() && hasItem) {
            Paint clear = new Paint();
            clear.setXfermode(new android.graphics.PorterDuffXfermode(PorterDuff.Mode.CLEAR));
            canvas.drawRect(ICON_WINDOW_LEFT, ICON_WINDOW_TOP, ICON_WINDOW_RIGHT, ICON_WINDOW_BOTTOM, clear);
            clear.setXfermode(null);
            return result;
        }

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(down ? Color.rgb(230, 230, 230) : Color.BLACK);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setTextSize(250f);
        Paint.FontMetrics fm = paint.getFontMetrics();
        float yOffset = NUMBER_Y + (down ? PRESSED_Y : 0f);
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
