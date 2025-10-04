package com.microsoft.xbox.toolkit.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;
import android.widget.RelativeLayout;

/**
 * 07.01.2021
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */

public class XLEClickableLayout extends RelativeLayout {
    public XLEClickableLayout(Context context) {
        super(context);
        setSoundEffectsEnabled(false);
    }

    public XLEClickableLayout(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        setSoundEffectsEnabled(false);
    }

    public XLEClickableLayout(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        setSoundEffectsEnabled(false);
    }

    public void setOnClickListener(View.OnClickListener onClickListener) {
        super.setOnClickListener(TouchUtil.createOnClickListener(onClickListener));
    }

    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo accessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(accessibilityNodeInfo);
        accessibilityNodeInfo.setClickable(true);
        accessibilityNodeInfo.setClassName(Button.class.getName());
    }
}
