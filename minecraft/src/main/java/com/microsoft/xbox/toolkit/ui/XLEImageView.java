package com.microsoft.xbox.toolkit.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.view.View;

import androidx.appcompat.widget.AppCompatImageView;


public class XLEImageView extends AppCompatImageView {
    public static final int IMAGE_ERROR = 2;
    public static final int IMAGE_FINAL = 0;
    public static final int IMAGE_LOADING = 1;
    public String TEST_loadingOrLoadedImageUrl;
    protected boolean isFinal;
    protected boolean shouldAnimate;

    public XLEImageView(Context context) {
        this(context, null, 0);
    }

    public XLEImageView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public XLEImageView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.shouldAnimate = true;
        this.isFinal = false;
        setSoundEffectsEnabled(false);
    }

    public boolean getShouldAnimate() {
        return this.shouldAnimate && !this.isFinal;
    }

    public void setShouldAnimate(boolean z) {
        this.shouldAnimate = z;
    }

    public void setImageBitmap(Bitmap bitmap) {
        if (bitmap != null) {
            super.setImageBitmap(bitmap);
        }
    }

    public void setImageSource(Bitmap bitmap, int i) {
        if (bitmap != null) {
            super.setImageBitmap(bitmap);
        }
    }

    public void setFinal(boolean z) {
        this.isFinal = z;
    }

    public void setOnClickListener(View.OnClickListener onClickListener) {
        super.setOnClickListener(TouchUtil.createOnClickListener(onClickListener));
    }
}
