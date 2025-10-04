package com.microsoft.xbox.toolkit.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.widget.ProgressBar;

/**
 * 07.01.2021
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */

public class FastProgressBar extends ProgressBar {
    private boolean isEnabled;
    private int visibility;

    @SuppressLint("WrongConstant")
    public FastProgressBar(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        setEnabled(true);
        setVisibility(0);
    }

    public synchronized void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        postInvalidateDelayed(33);
    }

    @SuppressLint("WrongConstant")
    public void setEnabled(boolean z) {
        if (this.isEnabled != z) {
            this.isEnabled = z;
            if (!z) {
                this.visibility = getVisibility();
                super.setVisibility(8);
                return;
            }
            super.setVisibility(this.visibility);
        }
    }

    public void setVisibility(int i) {
        if (this.isEnabled) {
            super.setVisibility(i);
        } else {
            this.visibility = i;
        }
    }
}
