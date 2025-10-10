package com.microsoft.xbox.toolkit.ui;

import android.view.View;
import android.widget.AdapterView;

import com.microsoft.xbox.toolkit.XLEAssert;


public class TouchUtil {
    public static View.OnClickListener createOnClickListener(View.OnClickListener onClickListener) {
        if (onClickListener == null) {
            return null;
        }
        XLEAssert.assertNotNull("Original listener is null.", onClickListener);
        return onClickListener;
    }

    public static AdapterView.OnItemClickListener createOnItemClickListener(AdapterView.OnItemClickListener onItemClickListener) {
        if (onItemClickListener == null) {
            return null;
        }
        XLEAssert.assertNotNull("Original listener is null.", onItemClickListener);
        return onItemClickListener;
    }

    public static View.OnLongClickListener createOnLongClickListener(View.OnLongClickListener onLongClickListener) {
        if (onLongClickListener == null) {
            return null;
        }
        XLEAssert.assertNotNull("Original listener is null.", onLongClickListener);
        return onLongClickListener;
    }
}
