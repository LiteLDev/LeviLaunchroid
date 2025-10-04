package com.microsoft.xbox.toolkit.ui;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;

import org.jetbrains.annotations.NotNull;

import java.io.InputStream;

/**
 * 07.01.2021
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */

public class XLEBitmap {
    public static String ALLOCATION_TAG = "XLEBITMAP";
    private Bitmap bitmapSrc = null;

    private XLEBitmap(Bitmap bitmap) {
        this.bitmapSrc = bitmap;
    }

    public static XLEBitmap createBitmap(int i, int i2, Bitmap.Config config) {
        return createBitmap(Bitmap.createBitmap(i, i2, config));
    }

    public static XLEBitmap createScaledBitmap(@NotNull XLEBitmap xLEBitmap, int i, int i2, boolean z) {
        return createBitmap(Bitmap.createScaledBitmap(xLEBitmap.bitmapSrc, i, i2, z));
    }

    public static XLEBitmap decodeStream(InputStream inputStream, BitmapFactory.Options options) {
        return createBitmap(BitmapFactory.decodeStream(inputStream, null, options));
    }

    public static XLEBitmap decodeStream(InputStream inputStream) {
        return createBitmap(BitmapFactory.decodeStream(inputStream));
    }

    public static XLEBitmap decodeResource(Resources resources, int i) {
        return createBitmap(BitmapFactory.decodeResource(resources, i));
    }

    public static XLEBitmap decodeResource(Resources resources, int i, BitmapFactory.Options options) {
        return createBitmap(BitmapFactory.decodeResource(resources, i, options));
    }

    public static XLEBitmap createScaledBitmap8888(@NotNull XLEBitmap xLEBitmap, int i, int i2, boolean z) {
        return createBitmap(TextureResizer.createScaledBitmap8888(xLEBitmap.bitmapSrc, i, i2, z));
    }

    public static XLEBitmap createBitmap(Bitmap bitmap) {
        if (bitmap == null) {
            return null;
        }
        return new XLEBitmap(bitmap);
    }

    public void finalize() {
    }

    public int getByteCount() {
        return this.bitmapSrc.getRowBytes() * this.bitmapSrc.getHeight();
    }

    public Bitmap getBitmap() {
        return this.bitmapSrc;
    }

    public XLEBitmapDrawable getDrawable() {
        return new XLEBitmapDrawable(new BitmapDrawable(this.bitmapSrc));
    }

    public static class XLEBitmapDrawable {
        private final BitmapDrawable drawable;

        public XLEBitmapDrawable(BitmapDrawable bitmapDrawable) {
            this.drawable = bitmapDrawable;
        }

        public BitmapDrawable getDrawable() {
            return this.drawable;
        }
    }
}
