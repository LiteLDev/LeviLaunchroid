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
        return isActive ? R.drawable.ic_sprint_enabled : R.drawable.ic_sprint_disabled;
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
                btn.setImageResource(active ? R.drawable.ic_sprint_enabled : R.drawable.ic_sprint_disabled);
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