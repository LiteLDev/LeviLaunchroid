package com.microsoft.xbox.toolkit.anim;

import android.view.animation.Interpolator;

/**
 * 07.01.2021
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */

public class XLEInterpolator implements Interpolator {
    private final EasingMode easingMode;

    public XLEInterpolator(EasingMode easingMode2) {
        this.easingMode = easingMode2;
    }

    public float getInterpolationCore(float f) {
        return f;
    }

    public float getInterpolation(float normalizedTime) {
        if (normalizedTime < 0.0f || normalizedTime > 1.0f) {
            throw new IllegalArgumentException("should respect 0<=normalizedTime<=1");
        }
        switch (easingMode) {
            case EaseIn:
                return getInterpolationCore(normalizedTime);
            case EaseOut:
                return 1.0f - getInterpolationCore(1.0f - normalizedTime);
            case EaseInOut:
                if (((double) normalizedTime) < 0.5d) {
                    return getInterpolationCore(normalizedTime * 2.0f) / 2.0f;
                }
                return ((1.0f - getInterpolationCore(2.0f - (normalizedTime * 2.0f))) / 2.0f) + 0.5f;
            default:
                return normalizedTime;
        }
    }
}
