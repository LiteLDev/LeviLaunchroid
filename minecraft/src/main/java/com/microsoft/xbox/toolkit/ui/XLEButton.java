package com.microsoft.xbox.toolkit.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

import androidx.appcompat.widget.AppCompatButton;
import com.microsoft.xbox.toolkit.XLERValueHelper;


public class XLEButton extends AppCompatButton {
    protected boolean disableSound;
    protected ButtonStateHandler stateHandler;
    private boolean alwaysClickable;
    private int disabledTextColor;
    private int enabledTextColor;

    public XLEButton(Context context) {
        super(context);
        this.stateHandler = new ButtonStateHandler();
        this.disableSound = false;
        setSoundEffectsEnabled(false);
    }

    public XLEButton(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public XLEButton(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.stateHandler = new ButtonStateHandler();
        this.disableSound = false;
        if (!isInEditMode()) {
            setSoundEffectsEnabled(false);
            TypedArray obtainStyledAttributes = context.obtainStyledAttributes(attributeSet, XLERValueHelper.getStyleableRValueArray("XLEButton"));
            try {
                this.stateHandler.setDisabled(obtainStyledAttributes.getBoolean(XLERValueHelper.getStyleableRValue("XLEButton_disabled"), false));
                this.stateHandler.setDisabledImageHandle(obtainStyledAttributes.getResourceId(XLERValueHelper.getStyleableRValue("XLEButton_disabledImage"), -1));
                this.stateHandler.setEnabledImageHandle(obtainStyledAttributes.getResourceId(XLERValueHelper.getStyleableRValue("XLEButton_enabledImage"), -1));
                this.stateHandler.setPressedImageHandle(obtainStyledAttributes.getResourceId(XLERValueHelper.getStyleableRValue("XLEButton_pressedImage"), -1));
                this.disableSound = obtainStyledAttributes.getBoolean(XLERValueHelper.getStyleableRValue("XLEButton_disableSound"), false);
                setLayoutParams(new FrameLayout.LayoutParams(-2, -2));
                obtainStyledAttributes = context.obtainStyledAttributes(attributeSet, XLERValueHelper.getStyleableRValueArray("CustomTypeface"));
                String string = obtainStyledAttributes.getString(XLERValueHelper.getStyleableRValue("CustomTypeface_typefaceSource"));
                if (string != null && string.length() > 0) {
                    applyCustomTypeface(context, string);
                }
                this.enabledTextColor = getCurrentTextColor();
                this.disabledTextColor = obtainStyledAttributes.getColor(XLERValueHelper.getStyleableRValue("XLEButton_disabledTextColor"), this.enabledTextColor);
                boolean z = obtainStyledAttributes.getBoolean(XLERValueHelper.getStyleableRValue("XLEButton_alwaysClickable"), false);
                this.alwaysClickable = z;
                if (z) {
                    super.setEnabled(true);
                    super.setClickable(true);
                }
                obtainStyledAttributes.recycle();
            } finally {
                obtainStyledAttributes.recycle();
            }
        }
    }

    private void applyCustomTypeface(Context context, String str) {
        if (str != null) {
            setTypeface(FontManager.Instance().getTypeface(getContext(), str));
        }
    }

    public void setTypeFace(String str) {
        applyCustomTypeface(getContext(), str);
    }

    public void setEnabled(boolean z) {
        if (!this.alwaysClickable) {
            super.setEnabled(z);
        }
        if (this.stateHandler == null) {
            this.stateHandler = new ButtonStateHandler();
        }
        this.stateHandler.setEnabled(z);
        updateImage();
        updateTextColor();
    }

    @SuppressLint({"MissingSuperCall", "ClickableViewAccessibility"})
    public void onFinishInflate() {
        updateImage();
        setOnTouchListener((view, motionEvent) -> {
            boolean onTouch = XLEButton.this.stateHandler.onTouch(motionEvent);
            XLEButton.this.updateImage();
            return onTouch;
        });
    }

    public void setOnClickListener(View.OnClickListener onClickListener) {
        if (this.disableSound) {
            super.setOnClickListener(onClickListener);
        } else {
            super.setOnClickListener(TouchUtil.createOnClickListener(onClickListener));
        }
    }

    public void setOnLongClickListener(View.OnLongClickListener onLongClickListener) {
        if (this.disableSound) {
            super.setOnLongClickListener(onLongClickListener);
        } else {
            super.setOnLongClickListener(TouchUtil.createOnLongClickListener(onLongClickListener));
        }
    }

    public void setPressedStateRunnable(ButtonStateHandler.ButtonStateHandlerRunnable buttonStateHandlerRunnable) {
        this.stateHandler.setPressedStateRunnable(buttonStateHandlerRunnable);
    }

    public void onSizeChanged(int i, int i2, int i3, int i4) {
        super.onSizeChanged(i, i2, i3, i4);
        if (hasSize() && this.stateHandler.onSizeChanged(getWidth(), getHeight())) {
            updateImage();
        }
    }

    private boolean hasSize() {
        return getWidth() > 0 && getHeight() > 0;
    }

    public void updateImage() {
        if (this.stateHandler.getImageDrawable() != null) {
            setBackgroundDrawable(this.stateHandler.getImageDrawable());
        }
    }

    public void updateTextColor() {
        if (this.enabledTextColor != this.disabledTextColor) {
            setTextColor(this.stateHandler.getDisabled() ? this.disabledTextColor : this.enabledTextColor);
        }
    }
}
