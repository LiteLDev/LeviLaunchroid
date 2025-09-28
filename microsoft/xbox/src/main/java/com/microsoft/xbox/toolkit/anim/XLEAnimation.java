package com.microsoft.xbox.toolkit.anim;

import android.view.View;
import android.view.animation.Interpolator;

import com.microsoft.xbox.toolkit.ThreadManager;

/**
 * 07.01.2021
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */

public abstract class XLEAnimation {
    protected Runnable endRunnable;

    public abstract void clear();

    public abstract void setInterpolator(Interpolator interpolator);

    public abstract void setTargetView(View view);

    public abstract void start();

    public void setOnAnimationEnd(final Runnable runnable) {
        if (runnable != null) {
            this.endRunnable = () -> ThreadManager.UIThreadPost(runnable);
        } else {
            this.endRunnable = null;
        }
    }
}
