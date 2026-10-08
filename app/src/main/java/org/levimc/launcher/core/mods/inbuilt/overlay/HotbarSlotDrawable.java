package org.levimc.launcher.core.mods.inbuilt.overlay;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;

import org.levimc.launcher.R;

final class HotbarSlotDrawable extends Drawable {
    private static Bitmap normalBase;
    private static Bitmap pressedBase;
    private static final Typeface FONT = Typeface.create(Typeface.DEFAULT, Typeface.BOLD);
    private final Paint bitmapPaint = new Paint();
    private final Paint numberPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint countPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint.FontMetrics numberMetrics = new Paint.FontMetrics();
    private final String number;
    private String countText = "";
    private boolean pressed;
    private boolean showIcon;
    private boolean showCount;
    private int count;
    private int alpha = 255;

    static synchronized void preload(Context context) {
        if (normalBase != null && pressedBase != null) return;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        if (normalBase == null) normalBase = BitmapFactory.decodeResource(context.getResources(),
                R.drawable.more_button_base_normal, options);
        if (pressedBase == null) pressedBase = BitmapFactory.decodeResource(context.getResources(),
                R.drawable.more_button_base_pressed, options);
    }

    HotbarSlotDrawable(Context context, int slot) {
        preload(context);
        number = Integer.toString(slot);
        numberPaint.setTypeface(FONT);
        numberPaint.setTextAlign(Paint.Align.CENTER);
        numberPaint.setTextSize(250f);
        numberPaint.getFontMetrics(numberMetrics);
        countPaint.setTypeface(FONT);
        countPaint.setTextAlign(Paint.Align.RIGHT);
        bitmapPaint.setFilterBitmap(false);
    }

    void update(boolean down, boolean icon, boolean countsEnabled, int itemCount) {
        if (pressed == down && showIcon == icon && showCount == countsEnabled && count == itemCount) return;
        pressed = down;
        showIcon = icon;
        showCount = countsEnabled;
        if (count != itemCount) {
            count = itemCount;
            countText = itemCount > 0 ? Integer.toString(itemCount) : "";
            countPaint.setTextSize(countText.length() >= 3 ? 72f : (countText.length() == 2 ? 88f : 96f));
        }
        invalidateSelf();
    }

    @Override
    public void draw(Canvas canvas) {
        Rect bounds = getBounds();
        if (bounds.isEmpty()) return;
        int save = canvas.save();
        canvas.translate(bounds.left, bounds.top);
        canvas.scale(bounds.width() / 512f, bounds.height() / 512f);
        Bitmap base = pressed ? pressedBase : normalBase;
        if (base != null) {
            int background = canvas.save();
            if (showIcon) canvas.clipOutRect(93f, 93f, 419f, 396f);
            canvas.drawBitmap(base, 0f, 0f, bitmapPaint);
            canvas.restoreToCount(background);
        }
        if (!showIcon) {
            numberPaint.setColor(pressed ? Color.rgb(230, 230, 230) : Color.BLACK);
            numberPaint.setAlpha(alpha);
            float y = 256f - (numberMetrics.ascent + numberMetrics.descent) / 2f - 8f + (pressed ? 8f : 0f);
            canvas.drawText(number, 256f, y, numberPaint);
        }
        if (showCount && count > 0) {
            float y = 365f + (pressed ? 8f : 0f);
            countPaint.setColor(Color.BLACK);
            countPaint.setAlpha(alpha);
            canvas.drawText(countText, 405f, y + 5f, countPaint);
            countPaint.setColor(Color.WHITE);
            countPaint.setAlpha(alpha);
            canvas.drawText(countText, 400f, y, countPaint);
        }
        canvas.restoreToCount(save);
    }

    @Override public int getIntrinsicWidth() { return 512; }
    @Override public int getIntrinsicHeight() { return 512; }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    @Override public void setAlpha(int value) {
        alpha = value;
        bitmapPaint.setAlpha(value);
        invalidateSelf();
    }
    @Override public void setColorFilter(ColorFilter filter) {
        bitmapPaint.setColorFilter(filter);
        numberPaint.setColorFilter(filter);
        countPaint.setColorFilter(filter);
        invalidateSelf();
    }
}
