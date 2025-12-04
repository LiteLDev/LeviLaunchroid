package org.levimc.launcher.ui.views;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import androidx.annotation.Nullable;

import java.util.Random;

public class AmoledGlowView extends View {

    private Paint greenPaint;
    private Paint cyanPaint;
    private float greenProgress = 0f;
    private float cyanProgress = 0f;
    private ValueAnimator greenAnimator;
    private ValueAnimator cyanAnimator;
    private final Random random = new Random();

    public AmoledGlowView(Context context) {
        super(context);
        init();
    }

    public AmoledGlowView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AmoledGlowView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        greenPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        cyanPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        setLayerType(LAYER_TYPE_SOFTWARE, null);
    }

    private boolean isDarkMode() {
        int nightMode = getContext().getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return nightMode == Configuration.UI_MODE_NIGHT_YES;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        startAnimations();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopAnimations();
    }

    private void startAnimations() {
        startGreenAnimation();
        startCyanAnimation();
    }

    private void startGreenAnimation() {
        if (greenAnimator != null && greenAnimator.isRunning()) return;

        long duration = 3000 + random.nextInt(3000);

        greenAnimator = ValueAnimator.ofFloat(0f, 1f);
        greenAnimator.setDuration(duration);
        greenAnimator.setRepeatCount(ValueAnimator.INFINITE);
        greenAnimator.setRepeatMode(ValueAnimator.REVERSE);
        greenAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        greenAnimator.addUpdateListener(animation -> {
            greenProgress = (float) animation.getAnimatedValue();
            invalidate();
        });
        greenAnimator.setCurrentFraction(random.nextFloat());
        greenAnimator.start();
    }

    private void startCyanAnimation() {
        if (cyanAnimator != null && cyanAnimator.isRunning()) return;

        long duration = 3500 + random.nextInt(2000);

        cyanAnimator = ValueAnimator.ofFloat(0f, 1f);
        cyanAnimator.setDuration(duration);
        cyanAnimator.setRepeatCount(ValueAnimator.INFINITE);
        cyanAnimator.setRepeatMode(ValueAnimator.REVERSE);
        cyanAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        cyanAnimator.addUpdateListener(animation -> {
            cyanProgress = (float) animation.getAnimatedValue();
            invalidate();
        });
        cyanAnimator.setCurrentFraction(random.nextFloat());
        cyanAnimator.start();
    }

    private void stopAnimations() {
        if (greenAnimator != null) {
            greenAnimator.cancel();
            greenAnimator = null;
        }
        if (cyanAnimator != null) {
            cyanAnimator.cancel();
            cyanAnimator = null;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();

        boolean darkMode = isDarkMode();

        float minScale = 0.75f;
        float maxScale = 1.0f;

        float baseRadius = Math.min(width, height) * 0.50f;

        float greenScale = minScale + (maxScale - minScale) * greenProgress;
        float cyanScale = minScale + (maxScale - minScale) * cyanProgress;

        float greenRadius = baseRadius * 1.2f * greenScale;
        float cyanRadius = baseRadius * cyanScale;

        float greenAlpha, cyanAlpha;
        if (darkMode) {
            greenAlpha = 0.16f + 0.08f * greenProgress;
            cyanAlpha = 0.08f + 0.06f * cyanProgress;
        } else {
            greenAlpha = 0.12f + 0.06f * greenProgress;
            cyanAlpha = 0.08f + 0.04f * cyanProgress;
        }


        int greenStart = Color.argb((int)(greenAlpha * 255), 0, 255, 127);
        int greenEnd = Color.argb(0, 0, 255, 127);

        RadialGradient greenGradient = new RadialGradient(
                0, 0,
                greenRadius,
                new int[]{greenStart, greenEnd},
                new float[]{0.0f, 1.0f},
                Shader.TileMode.CLAMP
        );
        greenPaint.setShader(greenGradient);
        canvas.drawCircle(0, 0, greenRadius, greenPaint);

        int cyanStart = Color.argb((int)(cyanAlpha * 255), 0, 255, 255);
        int cyanEnd = Color.argb(0, 0, 255, 255);

        RadialGradient cyanGradient = new RadialGradient(
                width, height,
                cyanRadius,
                new int[]{cyanStart, cyanEnd},
                new float[]{0.0f, 1.0f},
                Shader.TileMode.CLAMP
        );
        cyanPaint.setShader(cyanGradient);
        canvas.drawCircle(width, height, cyanRadius, cyanPaint);
    }
}
