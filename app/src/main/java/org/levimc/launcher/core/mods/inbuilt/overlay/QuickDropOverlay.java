package org.levimc.launcher.core.mods.inbuilt.overlay;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.widget.ImageButton;

import org.levimc.launcher.R;
import org.levimc.launcher.core.mods.inbuilt.model.ModIds;

public class QuickDropOverlay extends BaseOverlayButton {
    private static final long REPEAT_START_DELAY_MS = 300;
    private static final long REPEAT_INTERVAL_MS = 100;

    private final Handler repeatHandler = new Handler(Looper.getMainLooper());
    private boolean pressed;

    private final Runnable repeatDrop = new Runnable() {
        @Override
        public void run() {
            if (!pressed) return;
            sendKey(KeyEvent.KEYCODE_Q);
            repeatHandler.postDelayed(this, REPEAT_INTERVAL_MS);
        }
    };

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
    protected void onButtonPressStart() {
        pressed = true;
        setPressedIcon(true);
        repeatHandler.removeCallbacks(repeatDrop);
        sendKey(KeyEvent.KEYCODE_Q);
        repeatHandler.postDelayed(repeatDrop, REPEAT_START_DELAY_MS);
    }

    @Override
    protected void onButtonPressEnd() {
        stopRepeating();
        setPressedIcon(false);
    }

    @Override
    protected void onButtonClick() {
    }

    @Override
    public void hide() {
        stopRepeating();
        super.hide();
    }

    private void stopRepeating() {
        pressed = false;
        repeatHandler.removeCallbacks(repeatDrop);
    }

    private void setPressedIcon(boolean pressed) {
        if (overlayView instanceof ImageButton) {
            ((ImageButton) overlayView).setImageResource(
                    pressed ? R.drawable.ic_quick_drop_pressed : R.drawable.ic_quick_drop);
        }
    }
}
