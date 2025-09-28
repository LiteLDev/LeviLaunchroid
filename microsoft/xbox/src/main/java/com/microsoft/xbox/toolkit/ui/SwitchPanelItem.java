package com.microsoft.xbox.toolkit.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.microsoft.xbox.toolkit.XLERValueHelper;

/**
 * 07.01.2021
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */

public class SwitchPanelItem extends FrameLayout implements SwitchPanel.SwitchPanelChild {
    private final int INVALID_STATE_ID = -1;
    private final int state;

    public SwitchPanelItem(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        TypedArray obtainStyledAttributes = context.obtainStyledAttributes(attributeSet, XLERValueHelper.getStyleableRValueArray("SwitchPanelItem"));
        this.state = obtainStyledAttributes.getInteger(XLERValueHelper.getStyleableRValue("SwitchPanelItem_state"), INVALID_STATE_ID);
        obtainStyledAttributes.recycle();
        if (this.state >= 0) {
            setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            return;
        }
        throw new IllegalArgumentException("You must specify the state attribute in the xml, and the value must be positive.");
    }

    public int getState() {
        return this.state;
    }
}
