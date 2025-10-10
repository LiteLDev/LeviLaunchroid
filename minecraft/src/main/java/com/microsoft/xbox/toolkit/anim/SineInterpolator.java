package com.microsoft.xbox.toolkit.anim;


public class SineInterpolator extends XLEInterpolator {
    public SineInterpolator(EasingMode easingMode) {
        super(easingMode);
    }

    @Override
    public float getInterpolationCore(float f) {
        return (float) (1.0d - Math.sin((1.0d - ((double) f)) * 1.5707963267948966d));
    }
}