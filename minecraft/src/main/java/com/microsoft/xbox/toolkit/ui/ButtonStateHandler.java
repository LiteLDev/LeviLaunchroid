package com.microsoft.xbox.toolkit.ui;

import android.graphics.drawable.Drawable;
import android.view.MotionEvent;

import org.jetbrains.annotations.NotNull;


public class ButtonStateHandler {
    protected boolean disabled = false;
    protected boolean pressed = false;
    private XLEBitmap.XLEBitmapDrawable disabledImage = null;
    private int disabledImageHandle = -1;
    private XLEBitmap.XLEBitmapDrawable enabledImage = null;
    private int enabledImageHandle = -1;
    private XLEBitmap.XLEBitmapDrawable pressedImage = null;
    private int pressedImageHandle = -1;
    private ButtonStateHandlerRunnable pressedStateRunnable = null;

    public void setDisabledImageHandle(int i) {
        this.disabledImageHandle = i;
    }

    public void setEnabledImageHandle(int i) {
        this.enabledImageHandle = i;
    }

    public void setPressedImageHandle(int i) {
        this.pressedImageHandle = i;
    }

    public boolean getDisabled() {
        return this.disabled;
    }

    public void setDisabled(boolean z) {
        this.disabled = z;
    }

    public void setEnabled(boolean z) {
        this.disabled = !z;
    }

    public boolean onTouch(@NotNull MotionEvent motionEvent) {
        boolean z;
        boolean z2 = this.pressed;
        if (motionEvent.getAction() == 0) {
            this.pressed = true;
        } else if (motionEvent.getAction() == 1) {
            this.pressed = false;
        } else if (motionEvent.getAction() == 3) {
            this.pressed = false;
        }
        ButtonStateHandlerRunnable buttonStateHandlerRunnable = this.pressedStateRunnable;
        if (!(buttonStateHandlerRunnable == null || z2 == (z = this.pressed))) {
            buttonStateHandlerRunnable.onPressStateChanged(z);
        }
        return false;
    }

    public boolean onSizeChanged(int i, int i2) {
        boolean z;
        if (this.disabledImage != null || this.disabledImageHandle == -1) {
            z = false;
        } else {
            this.disabledImage = TextureManager.Instance().loadScaledResourceDrawable(this.disabledImageHandle);
            z = true;
        }
        if (this.enabledImage == null && this.enabledImageHandle != -1) {
            this.enabledImage = TextureManager.Instance().loadScaledResourceDrawable(this.enabledImageHandle);
            z = true;
        }
        if (this.pressedImage != null || this.pressedImageHandle == -1) {
            return z;
        }
        this.pressedImage = TextureManager.Instance().loadScaledResourceDrawable(this.pressedImageHandle);
        return true;
    }

    public Drawable getImageDrawable() {
        XLEBitmap.XLEBitmapDrawable xLEBitmapDrawable;
        if (this.pressed && this.pressedImageHandle != -1) {
            XLEBitmap.XLEBitmapDrawable xLEBitmapDrawable2 = this.pressedImage;
            if (xLEBitmapDrawable2 == null) {
                return null;
            }
            return xLEBitmapDrawable2.getDrawable();
        } else if (this.disabled && this.disabledImageHandle != -1) {
            XLEBitmap.XLEBitmapDrawable xLEBitmapDrawable3 = this.disabledImage;
            if (xLEBitmapDrawable3 == null) {
                return null;
            }
            return xLEBitmapDrawable3.getDrawable();
        } else if (this.enabledImageHandle == -1 || (xLEBitmapDrawable = this.enabledImage) == null) {
            return null;
        } else {
            return xLEBitmapDrawable.getDrawable();
        }
    }

    public void setPressedStateRunnable(ButtonStateHandlerRunnable buttonStateHandlerRunnable) {
        this.pressedStateRunnable = buttonStateHandlerRunnable;
    }

    public interface ButtonStateHandlerRunnable {
        void onPressStateChanged(boolean z);
    }
}
