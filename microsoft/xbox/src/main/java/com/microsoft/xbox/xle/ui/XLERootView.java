package com.microsoft.xbox.xle.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

/**
 * 07.01.2021
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */

public class XLERootView extends RelativeLayout {
    private static final int UNASSIGNED_ACTIVITY_BODY_ID = -1;
    private final boolean isTopLevel = false;
    private final long lastFps = 0;
    private final long lastMs = 0;
    private final boolean showTitleBar = true;
    private View activityBody;
    private int activityBodyIndex;
    private String headerName;
    private int origPaddingBottom;

    public XLERootView(Context context) {
        super(context);
        throw new UnsupportedOperationException();
    }

    public XLERootView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        /*TypedArray obtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.XLERootView);
        if (obtainStyledAttributes != null) {
            try {
                this.activityBodyIndex = obtainStyledAttributes.getResourceId(R.styleable.XLERootView_activityBody, -1);
                this.isTopLevel = obtainStyledAttributes.getBoolean(R.styleable.XLERootView_isTopLevel, false);
                this.showTitleBar = obtainStyledAttributes.getBoolean(R.styleable.XLERootView_showTitleBar, true);
                int i = obtainStyledAttributes.getInt(R.styleable.XLERootView_minScreenPercent, PKIFailureInfo.systemUnavail);
                if (i != Integer.MIN_VALUE) {
                    setMinimumWidth((Math.max(0, i) * SystemUtil.getScreenWidth()) / 100);
                }
                this.headerName = obtainStyledAttributes.getString(R.styleable.XLERootView_headerName);
            } finally {
                obtainStyledAttributes.recycle();
            }
        }*/
    }

    public boolean getIsTopLevel() {
        return this.isTopLevel;
    }

    public boolean getShowTitleBar() {
        return this.showTitleBar;
    }

    public String getHeaderName() {
        return this.headerName;
    }

    public void onFinishInflate() {
        super.onFinishInflate();
        initialize();
    }

    private void initialize() {
        int i = this.activityBodyIndex;
        if (i != -1) {
            this.activityBody = findViewById(i);
        } else {
            this.activityBody = this;
        }
        this.origPaddingBottom = getPaddingBottom();
        View view = this.activityBody;
        if (view != null && view != this) {
            ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
            RelativeLayout.LayoutParams layoutParams2 = new RelativeLayout.LayoutParams(layoutParams);
            layoutParams2.width = -1;
            layoutParams2.height = -1;
            layoutParams2.addRule(10);
            if (layoutParams instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) layoutParams;
                setPadding(getPaddingLeft() + marginLayoutParams.leftMargin, getPaddingTop() + marginLayoutParams.topMargin, getPaddingRight() + marginLayoutParams.rightMargin, this.origPaddingBottom + marginLayoutParams.bottomMargin);
                layoutParams2.setMargins(0, 0, 0, 0);
            }
            removeView(this.activityBody);
            addView(this.activityBody, layoutParams2);
        }
    }

    public void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
    }

    public void setBottomMargin(int i) {
        setPadding(getPaddingLeft(), getPaddingTop(), getPaddingRight(), this.origPaddingBottom + i);
    }
}
