package org.levimc.launcher.core.mods.inbuilt.overlay;

import android.app.Activity;
import android.view.KeyEvent;

import org.levimc.launcher.R;

public class CameraPerspectiveOverlay extends BaseOverlayButton {
    public CameraPerspectiveOverlay(Activity activity) {
        super(activity);
    }

    @Override
    protected int getIconResource() {
        return R.drawable.ic_camera;
    }

    @Override
    protected void onButtonClick() {
        sendKey(KeyEvent.KEYCODE_F5);
    }
}