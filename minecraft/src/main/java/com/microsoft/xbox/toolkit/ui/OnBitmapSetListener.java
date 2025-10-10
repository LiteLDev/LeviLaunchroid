package com.microsoft.xbox.toolkit.ui;

import android.graphics.Bitmap;
import android.widget.ImageView;


public interface OnBitmapSetListener {
    void onAfterImageSet(ImageView imageView, Bitmap bitmap);

    void onBeforeImageSet(ImageView imageView, Bitmap bitmap);
}
