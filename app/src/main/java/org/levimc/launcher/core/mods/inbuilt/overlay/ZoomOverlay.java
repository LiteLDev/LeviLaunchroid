package org.levimc.launcher.core.mods.inbuilt.overlay;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ImageButton;

import org.levimc.launcher.R;
import org.levimc.launcher.core.mods.inbuilt.manager.InbuiltModManager;
import org.levimc.launcher.core.mods.inbuilt.model.ModIds;
import org.levimc.launcher.core.mods.inbuilt.nativemod.ZoomMod;

public class ZoomOverlay extends BaseOverlayButton {
    private static final String TAG = "ZoomOverlay";
    private boolean isZooming = false;
    private boolean initialized = false;
    private final Handler handler = new Handler(Looper.getMainLooper());

    public ZoomOverlay(Activity activity) {
        super(activity);
    }

    @Override
    protected String getModId() {
        return ModIds.ZOOM;
    }

    @Override
    protected int getIconResource() {
        return R.drawable.ic_zoom;
    }

    @Override
    public void show(int startX, int startY) {
        if (!initialized) {
            initializeNative();
        }
        super.show(startX, startY);
    }

    private void initializeNative() {
        handler.postDelayed(() -> {
            if (ZoomMod.init()) {
                initialized = true;
                applyZoomLevel();
                Log.i(TAG, "Zoom native initialized successfully");
            } else {
                Log.e(TAG, "Failed to initialize zoom native");
            }
        }, 1000);
    }

    private void applyZoomLevel() {
        int zoomPercent = InbuiltModManager.getInstance(activity).getZoomLevel();
        long normalFov = 5360000000L;
        long maxZoomFov = 5310000000L;
        long zoomLevel = normalFov - (long)((normalFov - maxZoomFov) * zoomPercent / 100.0);
        ZoomMod.nativeSetZoomLevel(zoomLevel);
    }

    @Override
    protected void onButtonClick() {
        if (!initialized) {
            Log.w(TAG, "Zoom not initialized yet");
            return;
        }

        isZooming = !isZooming;

        if (isZooming) {
            applyZoomLevel();
            ZoomMod.nativeOnKeyDown();
            updateButtonState(true);
        } else {
            ZoomMod.nativeOnKeyUp();
            updateButtonState(false);
        }
    }

    private void updateButtonState(boolean active) {
        if (overlayView instanceof ImageButton) {
            ImageButton btn = (ImageButton) overlayView;
            btn.setAlpha(active ? 1.0f : 0.6f);
            btn.setBackgroundResource(active ? R.drawable.bg_overlay_button_active : R.drawable.bg_overlay_button);
        }
    }

    public void onScroll(float delta) {
        if (initialized && isZooming) {
            ZoomMod.nativeOnScroll(delta);
        }
    }

    @Override
    public void hide() {
        if (isZooming && initialized) {
            ZoomMod.nativeOnKeyUp();
            isZooming = false;
        }
        super.hide();
    }

    public boolean isZooming() {
        return isZooming;
    }
}
