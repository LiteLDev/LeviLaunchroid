package com.microsoft.xbox.toolkit.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.microsoft.xbox.toolkit.BackgroundThreadWaitor;
import com.microsoft.xbox.toolkit.XLERValueHelper;

/**
 * 07.01.2021
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */

public class SwitchPanel extends LinearLayout {
    private static final int LAYOUT_BLOCK_TIMEOUT_MS = 150;
    private final int INVALID_STATE_ID = -1;
    private final int VALID_CONTENT_STATE = 0;
    private boolean active = false;
    private boolean blocking = false;
    private View newView = null;
    private final AnimatorListenerAdapter AnimateInListener = new AnimatorListenerAdapter() {
        public void onAnimationCancel(Animator animator) {
            SwitchPanel.this.onAnimateInEnd();
        }

        public void onAnimationEnd(Animator animator) {
            SwitchPanel.this.onAnimateInEnd();
        }

        public void onAnimationStart(Animator animator) {
            SwitchPanel.this.onAnimateInStart();
        }
    };
    private View oldView = null;
    private final AnimatorListenerAdapter AnimateOutListener = new AnimatorListenerAdapter() {
        public void onAnimationCancel(Animator animator) {
            SwitchPanel.this.onAnimateOutEnd();
        }

        public void onAnimationEnd(Animator animator) {
            SwitchPanel.this.onAnimateOutEnd();
        }

        public void onAnimationStart(Animator animator) {
            SwitchPanel.this.onAnimateOutStart();
        }
    };
    private int selectedState;
    private boolean shouldAnimate = true;

    public SwitchPanel(Context context) {
        super(context);
        throw new UnsupportedOperationException();
    }

    public SwitchPanel(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        TypedArray obtainStyledAttributes = context.obtainStyledAttributes(attributeSet, XLERValueHelper.getStyleableRValueArray("SwitchPanel"));
        this.selectedState = obtainStyledAttributes.getInteger(XLERValueHelper.getStyleableRValue("SwitchPanel_selectedState"), -1);
        obtainStyledAttributes.recycle();
        if (this.selectedState >= 0) {
            setLayoutParams(new RelativeLayout.LayoutParams(-1, -1));
            return;
        }
        throw new IllegalArgumentException("You must specify the selectedState attribute in the xml, and the value must be positive.");
    }

    public void onFinishInflate() {
        super.onFinishInflate();
        updateVisibility(-1, this.selectedState);
    }

    public void setActive(boolean z) {
        this.active = z;
    }

    public void setShouldAnimate(boolean z) {
        this.shouldAnimate = z;
    }

    public int getState() {
        return this.selectedState;
    }

    public void setState(int i) {
        if (i >= 0) {
            int i2 = this.selectedState;
            if (i2 != i) {
                this.selectedState = i;
                updateVisibility(i2, i);
                return;
            }
            return;
        }
        throw new IllegalArgumentException("New state must be a positive value.");
    }

    @SuppressLint("WrongConstant")
    private void updateVisibility(int i, int i2) {
        View view;
        int childCount = getChildCount();
        int i3 = 0;
        while (i3 < childCount) {
            View childAt = getChildAt(i3);
            if (childAt instanceof SwitchPanelChild) {
                int state = ((SwitchPanelChild) childAt).getState();
                if (state == i) {
                    this.oldView = childAt;
                } else if (state == i2) {
                    this.newView = childAt;
                } else {
                    childAt.setVisibility(8);
                }
                i3++;
            } else {
                throw new UnsupportedOperationException("All children of SwitchPanel must implement the SwitchPanelChild interface. All other types are not supported and should be removed.");
            }
        }
        if (!this.shouldAnimate || i2 != 0 || (view = this.newView) == null) {
            View view2 = this.oldView;
            if (view2 != null) {
                view2.setVisibility(8);
            }
            View view3 = this.newView;
            if (view3 != null) {
                view3.setAlpha(1.0f);
                this.newView.setVisibility(0);
            }
            requestLayout();
            return;
        }
        view.setAlpha(0.0f);
        this.newView.setVisibility(0);
        requestLayout();
        View view4 = this.oldView;
        if (view4 != null) {
            view4.animate().alpha(0.0f).setDuration(150).setListener(this.AnimateOutListener);
        }
        this.newView.animate().alpha(1.0f).setDuration(150).setListener(this.AnimateInListener);
    }

    public void setBlocking(boolean z) {
        if (this.blocking != z) {
            this.blocking = z;
            if (z) {
                BackgroundThreadWaitor.getInstance().setBlocking(BackgroundThreadWaitor.WaitType.ListLayout, 150);
            } else {
                BackgroundThreadWaitor.getInstance().clearBlocking(BackgroundThreadWaitor.WaitType.ListLayout);
            }
        }
    }

    @SuppressLint("WrongConstant")
    public void onAnimateInStart() {
        View view = this.newView;
        if (view != null) {
            view.setLayerType(2, null);
            setBlocking(true);
        }
    }

    @SuppressLint("WrongConstant")
    public void onAnimateInEnd() {
        setBlocking(false);
        View view = this.newView;
        if (view != null) {
            view.setLayerType(0, null);
        }
    }

    @SuppressLint("WrongConstant")
    public void onAnimateOutStart() {
        View view = this.oldView;
        if (view != null) {
            view.setLayerType(2, null);
            setBlocking(true);
        }
    }

    @SuppressLint("WrongConstant")
    public void onAnimateOutEnd() {
        View view = this.oldView;
        if (view != null) {
            view.setVisibility(8);
            this.oldView.setLayerType(0, null);
        }
    }

    public interface SwitchPanelChild {
        int getState();
    }
}
