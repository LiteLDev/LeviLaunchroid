package com.microsoft.xbox.toolkit.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.widget.ListView;


public class XLEListView extends ListView {
    public XLEListView(Context context) {
        super(context);
    }

    public XLEListView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public XLEListView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
    }

    public void dispatchDraw(Canvas canvas) {
        try {
            super.dispatchDraw(canvas);
        } catch (IndexOutOfBoundsException unused) {
        }
    }
}
