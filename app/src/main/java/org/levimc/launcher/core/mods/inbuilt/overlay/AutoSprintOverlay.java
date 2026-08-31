package org.levimc.launcher.core.mods.inbuilt.overlay;

import android.app.Activity;
import android.widget.ImageButton;

import org.levimc.launcher.R;
import org.levimc.launcher.core.mods.inbuilt.model.ModIds;
import org.levimc.launcher.core.mods.inbuilt.nativemod.AutoSprintMod;

public class AutoSprintOverlay extends BaseOverlayButton {
    private boolean isActive = false;

    public AutoSprintOverlay(Activity activity) {
        super(activity);
    }

    @Override
    protected String getModId() {
        return ModIds.AUTO_SPRINT;
    }

    @Override
    protected int getIconResource() {
        return isActive ? R.drawable.ic_sprint_pressed : R.drawable.ic_sprint_normal;
    }

    @Override
    protected void onButtonClick() {
        boolean nextState = !isActive;
        if (!AutoSprintMod.setEnabled(nextState)) {
            nextState = false;
        }
        isActive = nextState;
        updateButtonState(isActive);
    }

    private void updateButtonState(boolean active) {
        if (overlayView != null) {
            ImageButton btn = overlayView.findViewById(R.id.mod_overlay_button);
            if (btn != null) {
                btn.setAlpha(getButtonOpacity());
                btn.setImageResource(active ? R.drawable.ic_sprint_pressed : R.drawable.ic_sprint_normal);
            }
        }
    }

    @Override
    public void hide() {
        if (isActive) {
            AutoSprintMod.setEnabled(false);
            isActive = false;
            updateButtonState(false);
        }
        super.hide();
    }
}
