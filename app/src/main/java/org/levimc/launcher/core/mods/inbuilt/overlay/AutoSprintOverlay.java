package org.levimc.launcher.core.mods.inbuilt.overlay;

import android.app.Activity;
import android.widget.ImageButton;

import org.levimc.launcher.R;

import org.levimc.launcher.core.mods.inbuilt.model.ModIds;

public class AutoSprintOverlay extends BaseOverlayButton {
    private boolean isActive = false;
    private int sprintKey;

    public AutoSprintOverlay(Activity activity, int sprintKey) {
        super(activity);
        this.sprintKey = sprintKey;
    }

    @Override
    protected String getModId() {
        return ModIds.AUTO_SPRINT;
    }

    @Override
    protected int getIconResource() {
        return R.drawable.ic_sprint;
    }

    @Override
    protected void onButtonClick() {
        isActive = !isActive;
        if (isActive) {
            sendKeyDown(sprintKey);
            updateButtonState(true);
        } else {
            sendKeyUp(sprintKey);
            updateButtonState(false);
        }
    }

    private void updateButtonState(boolean active) {
        if (overlayView != null) {
            ImageButton btn = overlayView.findViewById(R.id.mod_overlay_button);
            if (btn != null) {
                float userOpacity = getButtonOpacity();
                btn.setAlpha(userOpacity);
                btn.setBackgroundResource(active ? R.drawable.bg_overlay_button_active : R.drawable.bg_overlay_button);
            }
        }
    }

    @Override
    public void hide() {
        if (isActive) {
            sendKeyUp(sprintKey);
            isActive = false;
        }
        super.hide();
    }
}