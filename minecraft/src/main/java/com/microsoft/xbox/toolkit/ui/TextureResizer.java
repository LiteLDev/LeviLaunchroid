package com.microsoft.xbox.toolkit.ui;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;

import org.jetbrains.annotations.NotNull;


public class TextureResizer {
    public static @NotNull Bitmap createScaledBitmap8888(@NotNull Bitmap bitmap, int i, int i2, boolean z) {
        Paint paint;
        Bitmap bitmap2;
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        float f = (float) width;
        float f2 = (float) height;
        Matrix matrix = new Matrix();
        matrix.setScale(((float) i) / f, ((float) i2) / f2);
        int i3 = width + 0;
        if (i3 <= bitmap.getWidth()) {
            int i4 = height + 0;
            if (i4 > bitmap.getHeight()) {
                throw new IllegalArgumentException("y + height must be <= bitmap.height()");
            } else if (!bitmap.isMutable() && width == bitmap.getWidth() && height == bitmap.getHeight() && matrix.isIdentity()) {
                return bitmap;
            } else {
                Canvas canvas = new Canvas();
                Rect rect = new Rect(0, 0, i3, i4);
                RectF rectF = new RectF(0.0f, 0.0f, f, f2);
                if (matrix.isIdentity()) {
                    bitmap2 = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                    paint = null;
                } else {
                    boolean z2 = bitmap.hasAlpha() || !matrix.rectStaysRect();
                    RectF rectF2 = new RectF();
                    matrix.mapRect(rectF2, rectF);
                    Bitmap createBitmap = Bitmap.createBitmap(Math.round(rectF2.width()), Math.round(rectF2.height()), Bitmap.Config.ARGB_8888);
                    if (z2) {
                        createBitmap.eraseColor(0);
                    }
                    canvas.translate(-rectF2.left, -rectF2.top);
                    canvas.concat(matrix);
                    Paint paint2 = new Paint();
                    paint2.setFilterBitmap(z);
                    if (!matrix.rectStaysRect()) {
                        paint2.setAntiAlias(true);
                    }
                    paint = paint2;
                    bitmap2 = createBitmap;
                }
                bitmap2.setDensity(bitmap.getDensity());
                canvas.setBitmap(bitmap2);
                canvas.drawBitmap(bitmap, rect, rectF, paint);
                return bitmap2;
            }
        } else {
            throw new IllegalArgumentException("x + width must be <= bitmap.width()");
        }
    }
}
