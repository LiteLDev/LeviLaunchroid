package org.levimc.launcher.core.mods.inbuilt.overlay;

import android.app.Activity;
import android.view.KeyEvent;
import android.widget.ImageButton;

import org.levimc.launcher.R;
import org.levimc.launcher.core.mods.inbuilt.model.ModIds;

public class ToggleHudOverlay extends BaseOverlayButton {
    private boolean isActive = false;

    public ToggleHudOverlay(Activity activity) {
        super(activity);
    }

    @Override
    protected String getModId() {
        return ModIds.TOGGLE_HUD;
    }

    @Override
    protected int getIconResource() {
        return isActive ? R.drawable.ic_toggle_hud_pressed : R.drawable.ic_toggle_hud_normal;
    }

    @Override
    protected void onButtonClick() {
        isActive = !isActive;
        sendKey(KeyEvent.KEYCODE_F1);
        updateButtonState();
    }

    private void updateButtonState() {
        if (overlayView instanceof ImageButton) {
            ImageButton btn = (ImageButton) overlayView;
            btn.setAlpha(getButtonOpacity());
            btn.setImageResource(isActive
                    ? R.drawable.ic_toggle_hud_pressed
                    : R.drawable.ic_toggle_hud_normal);
        }
    }
}
