package com.microsoft.xbox.toolkit.anim;

import android.view.View;
import android.view.animation.Interpolator;
import android.view.animation.LayoutAnimationController;
import android.widget.AbsListView;

import com.microsoft.xbox.toolkit.XLEAssert;


public class XLEAnimationAbsListView extends XLEAnimation {
    private LayoutAnimationController layoutAnimationController = null;
    private AbsListView layoutView = null;

    public XLEAnimationAbsListView(LayoutAnimationController layoutAnimationController2) {
        this.layoutAnimationController = layoutAnimationController2;
        XLEAssert.assertTrue(layoutAnimationController2 != null);
    }

    public void start() {
        this.layoutView.setLayoutAnimation(this.layoutAnimationController);
        if (this.endRunnable != null) {
            this.endRunnable.run();
        }
    }

    public void clear() {
        this.layoutView.setLayoutAnimationListener(null);
        this.layoutView.clearAnimation();
    }

    public void setInterpolator(Interpolator interpolator) {
        this.layoutAnimationController.setInterpolator(interpolator);
    }

    public void setTargetView(View view) {
        XLEAssert.assertNotNull(view);
        XLEAssert.assertTrue(view instanceof AbsListView);
        this.layoutView = (AbsListView) view;
    }
}
