package org.levimc.launcher.core.mods.inbuilt.overlay;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ImageButton;

import org.levimc.launcher.R;
import org.levimc.launcher.core.mods.inbuilt.model.ModIds;
import org.levimc.launcher.core.mods.inbuilt.nativemod.SnaplookMod;

public class SnaplookOverlay extends BaseOverlayButton {
    private static final String TAG = "SnaplookOverlay";
    private boolean isActive = false;
    private boolean initialized = false;
    private final Handler handler = new Handler(Looper.getMainLooper());

    public SnaplookOverlay(Activity activity) {
        super(activity);
    }

    @Override
    protected String getModId() {
        return ModIds.SNAPLOOK;
    }

    @Override
    protected int getIconResource() {
        return R.drawable.ic_snaplook;
    }

    @Override
    public void show(int startX, int startY) {
        if (!initialized) {
            initializeNative();
        }
        super.show(startX, startY);
    }

    public void initializeForKeyboard() {
        if (!initialized) {
            initializeNative();
        }
    }

    private void initializeNative() {
        handler.postDelayed(() -> {
            if (SnaplookMod.init()) {
                initialized = true;
                Log.i(TAG, "Snaplook native initialized successfully");
            } else {
                Log.e(TAG, "Failed to initialize snaplook native");
            }
        }, 1000);
    }

    @Override
    protected void onButtonClick() {
        if (!initialized) {
            Log.w(TAG, "Snaplook not initialized yet");
            return;
        }

        isActive = !isActive;

        if (isActive) {
            SnaplookMod.nativeOnKeyDown();
            updateButtonState(true);
        } else {
            SnaplookMod.nativeOnKeyUp();
            updateButtonState(false);
        }
    }

    public void onKeyDown() {
        if (!initialized) {
            Log.w(TAG, "Snaplook not initialized yet");
            return;
        }
        if (isActive) return;

        isActive = true;
        SnaplookMod.nativeOnKeyDown();
        updateButtonState(true);
    }

    public void onKeyUp() {
        if (!initialized || !isActive) return;

        isActive = false;
        SnaplookMod.nativeOnKeyUp();
        updateButtonState(false);
    }

    private void updateButtonState(boolean active) {
        if (overlayView instanceof ImageButton) {
            ImageButton btn = (ImageButton) overlayView;
            float userOpacity = getButtonOpacity();
            btn.setAlpha(userOpacity);
            btn.setBackgroundResource(active ? R.drawable.bg_overlay_button_active : R.drawable.bg_overlay_button);
        }
    }

    @Override
    public void hide() {
        if (isActive && initialized) {
            SnaplookMod.nativeOnKeyUp();
            isActive = false;
        }
        super.hide();
    }

    public boolean isActive() {
        return isActive;
    }

    public boolean isInitialized() {
        return initialized;
    }
}
