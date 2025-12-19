package org.levimc.launcher.core.mods.inbuilt.overlay;

import android.app.Activity;
import android.view.KeyEvent;

import org.levimc.launcher.R;

import org.levimc.launcher.core.mods.inbuilt.model.ModIds;

public class QuickDropOverlay extends BaseOverlayButton {
    public QuickDropOverlay(Activity activity) {
        super(activity);
    }

    @Override
    protected String getModId() {
        return ModIds.QUICK_DROP;
    }

    @Override
    protected int getIconResource() {
        return R.drawable.ic_quick_drop;
    }

    @Override
    protected void onButtonClick() {
        sendKey(KeyEvent.KEYCODE_Q);
    }
}