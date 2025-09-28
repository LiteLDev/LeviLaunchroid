package com.microsoft.xbox.toolkit.anim;

/**
 * 07.01.2021
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */

public class BackEaseInterpolator extends XLEInterpolator {
    private final float amplitude;

    public BackEaseInterpolator(float f, EasingMode easingMode) {
        super(easingMode);
        amplitude = f;
    }

    @Override
    public float getInterpolationCore(float f) {
        float max = (float) Math.max(f, 0.0d);
        return (float) (((double) ((max * max) * max)) - (((double) (amplitude * max)) * Math.sin(((double) max) * 3.141592653589793d)));
    }
}