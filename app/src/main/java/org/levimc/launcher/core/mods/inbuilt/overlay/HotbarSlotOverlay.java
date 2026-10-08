package org.levimc.launcher.core.mods.inbuilt.overlay;

import android.app.Activity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageButton;

import org.levimc.launcher.R;
import org.levimc.launcher.core.mods.inbuilt.manager.InbuiltModManager;
import org.levimc.launcher.core.mods.inbuilt.model.ModIds;
import org.levimc.launcher.core.mods.inbuilt.nativemod.HotbarSlotMod;
import org.levimc.launcher.core.mods.inbuilt.nativemod.MoreButtonsMod;

public final class HotbarSlotOverlay extends BaseOverlayButton {
    private final int slot;
    private HotbarSlotDrawable drawable;
    private volatile boolean iconsEnabled;
    private volatile boolean countsEnabled;
    private boolean visualUpdatePosted;
    private final android.os.Handler visualHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable refreshVisual = () -> {
        synchronized (HotbarSlotOverlay.this) {
            visualUpdatePosted = false;
        }
        updateVisual();
    };
    private volatile boolean pressed;
    private volatile boolean keyDown;
    private volatile boolean renderVisible = false;
    private volatile int nativeX;
    private volatile int nativeY;
    private volatile int nativeWidth;
    private volatile int nativeHeight;
    private volatile int nativeSurfaceWidth;
    private volatile int nativeSurfaceHeight;
    private volatile int itemCount;

    public HotbarSlotOverlay(Activity activity, int slot) {
        super(activity);
        this.slot = slot;
        readConfiguration();
    }

    public static void preloadArtwork(android.content.Context context) {
        HotbarSlotDrawable.preload(context);
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
    protected long getShowDelayMillis() {
        View decor = activity.getWindow() != null ? activity.getWindow().getDecorView() : null;
        long windowDelay = decor != null && decor.getWindowToken() != null ? 0L : 500L;
        return windowDelay + (slot - 1) * 16L;
    }

    @Override
    protected void configureOverlayView(View view) {
        drawable = new HotbarSlotDrawable(activity, slot);
        if (view instanceof ImageButton) ((ImageButton) view).setImageDrawable(drawable);
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

    public void updateItemCount(int currentCount) {
        if (currentCount == itemCount && iconsAvailableForVisual == HotbarSlotMod.areItemIconsAvailable()) return;
        itemCount = currentCount;
        iconsAvailableForVisual = HotbarSlotMod.areItemIconsAvailable();
        postVisualUpdate();
    }

    private void postVisualUpdate() {
        synchronized (this) {
            if (visualUpdatePosted) return;
            visualUpdatePosted = true;
        }
        visualHandler.post(refreshVisual);
    }

    private volatile boolean iconsAvailableForVisual;

    public void setRenderVisible(boolean visible) {
        if (renderVisible == visible) return;
        renderVisible = visible;
        if (!visible && keyDown) {
            sendSlotKey(false);
            keyDown = false;
            pressed = false;
            updateVisual();
        }
        syncNativeState();
    }

    @Override
    public void hide() {
        visualHandler.removeCallbacks(refreshVisual);
        synchronized (this) {
            visualUpdatePosted = false;
        }
        if (keyDown) sendSlotKey(false);
        keyDown = false;
        pressed = false;
        renderVisible = false;
        HotbarSlotMod.clearSlot(slot - 1);
        super.hide();
        drawable = null;
    }

    @Override
    public void applyConfigurationChanges() {
        readConfiguration();
        if (!iconsEnabled && !countsEnabled) itemCount = 0;
        super.applyConfigurationChanges();
        updateVisual();
        syncNativeState();
    }

    private void readConfiguration() {
        InbuiltModManager manager = InbuiltModManager.getInstance(activity);
        iconsEnabled = manager.isHotbarItemIconsEnabled();
        countsEnabled = manager.isHotbarItemCountsEnabled();
    }

    private void syncNativeState() {
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
        if (android.os.Looper.myLooper() != android.os.Looper.getMainLooper()) {
            postVisualUpdate();
            return;
        }
        if (!(overlayView instanceof ImageButton) || drawable == null) return;
        drawable.update(pressed, iconsEnabled && HotbarSlotMod.areItemIconsAvailable() && itemCount > 0,
                countsEnabled, itemCount);
        ImageButton button = (ImageButton) overlayView;
        button.setScaleType(ImageButton.ScaleType.FIT_CENTER);
        button.setAlpha(getButtonOpacity());
    }
}
