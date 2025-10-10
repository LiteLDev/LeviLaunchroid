package com.microsoft.xbox.toolkit.anim;

import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Transformation;

import org.jetbrains.annotations.NotNull;


public class HeightAnimation extends Animation {
    private final int toValue;
    private int fromValue;
    private View view;

    public HeightAnimation(int i, int i2) {
        this.fromValue = i;
        this.toValue = i2;
    }

    public boolean willChangeBounds() {
        return true;
    }

    public void setTargetView(@NotNull View view2) {
        this.view = view2;
        this.fromValue = view2.getHeight();
    }

    public void applyTransformation(float f, Transformation transformation) {
        int i = (int) (((float) (this.toValue - this.fromValue)) * f);
        this.view.getLayoutParams().height = this.fromValue + i;
        this.view.requestLayout();
    }
}
