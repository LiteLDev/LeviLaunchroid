package com.microsoft.xbox.toolkit.ui;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.AsyncTask;
import android.text.TextPaint;
import android.widget.ImageView;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;


public class XLETextTask extends AsyncTask<XLETextArg, Void, Bitmap> {
    private static final String TAG = XLETextTask.class.getSimpleName();
    private final WeakReference<ImageView> img;
    private final int imgHeight;
    private final int imgWidth;

    public XLETextTask(ImageView imageView) {
        this.img = new WeakReference<>(imageView);
        this.imgWidth = imageView.getWidth();
        this.imgHeight = imageView.getHeight();
    }

    public Bitmap doInBackground(@NotNull XLETextArg ... xLETextArgArr) {
        int i;
        int i2;
        if (xLETextArgArr.length <= 0) {
            return null;
        }
        XLETextArg xLETextArg = xLETextArgArr[0];
        XLETextArg.Params params = xLETextArg.getParams();
        String text = xLETextArg.getText();
        TextPaint textPaint = new TextPaint();
        textPaint.setTextSize(params.getTextSize());
        textPaint.setAntiAlias(true);
        textPaint.setColor(params.getColor());
        textPaint.setTypeface(params.getTypeface());
        int round = Math.round(textPaint.measureText(text));
        int round2 = Math.round(textPaint.descent() - textPaint.ascent());
        if (params.isAdjustForImageSize()) {
            i2 = Math.max(round, this.imgWidth);
            i = Math.max(round2, this.imgHeight);
        } else {
            i2 = round;
            i = round2;
        }
        if (params.hasTextAspectRatio()) {
            float floatValue = params.getTextAspectRatio();
            if (floatValue > 0.0f) {
                float f = (float) i;
                float f2 = ((float) i2) * floatValue;
                if (f > f2) {
                    i2 = (int) (f / floatValue);
                } else {
                    i = (int) f2;
                }
            }
        }
        Bitmap createBitmap = Bitmap.createBitmap(i2, i, Bitmap.Config.ARGB_8888);
        if (params.hasEraseColor()) {
            createBitmap.eraseColor(params.getEraseColor());
        }
        new Canvas(createBitmap).drawText(text, (float) ((Math.max(0, i2 - round) / 2)), (-textPaint.ascent()) + ((float) (Math.max(0, i - round2) / 2)), textPaint);
        return createBitmap;
    }

    public void onPostExecute(Bitmap bitmap) {
        ImageView imageView = this.img.get();
        if (imageView != null) {
            imageView.setImageBitmap(bitmap);
        }
    }
}
