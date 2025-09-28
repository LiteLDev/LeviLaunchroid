package com.microsoft.xbox.xle.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.microsoft.xboxtcui.R;
import com.microsoft.xbox.toolkit.XLERValueHelper;
import com.microsoft.xbox.toolkit.ui.CustomTypefaceTextView;
import com.microsoft.xbox.xle.app.XLEUtil;

/**
 * 07.01.2021
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */

public class IconFontSubTextButton extends LinearLayout {
    private final FrameLayout iconFrameLayout;
    private final CustomTypefaceTextView iconTextView;
    private final CustomTypefaceTextView subtitleTextView;
    private final CustomTypefaceTextView titleTextView;

    public IconFontSubTextButton(Context context) {
        this(context, null);
    }

    public IconFontSubTextButton(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public IconFontSubTextButton(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        LayoutInflater.from(context).inflate(R.layout.icon_font_subtext_button, this, true);
        this.iconTextView = findViewById(R.id.icon_font_subtext_icon);
        this.iconFrameLayout = findViewById(R.id.icon_font_subtext_btn_icon_bg);
        this.titleTextView = findViewById(R.id.icon_font_subtext_btn_title);
        this.subtitleTextView = findViewById(R.id.icon_font_subtext_btn_subtitle);
        TypedArray obtainStyledAttributes = context.obtainStyledAttributes(attributeSet, XLERValueHelper.getStyleableRValueArray("IconFontSubTextButton"));
        String string = obtainStyledAttributes.getString(XLERValueHelper.getStyleableRValue("IconFontSubTextButton_icon_uri"));
        String string2 = obtainStyledAttributes.getString(XLERValueHelper.getStyleableRValue("IconFontSubTextButton_text_title"));
        String string3 = obtainStyledAttributes.getString(XLERValueHelper.getStyleableRValue("IconFontSubTextButton_text_subtitle"));
        this.iconFrameLayout.setBackgroundColor(obtainStyledAttributes.getColor(XLERValueHelper.getStyleableRValue("IconFontSubTextButton_icon_bg"), 0));
        obtainStyledAttributes.recycle();
        XLEUtil.updateTextAndVisibilityIfNotNull(this.iconTextView, string, 0);
        XLEUtil.updateTextAndVisibilityIfNotNull(this.titleTextView, string2, 0);
        XLEUtil.updateTextAndVisibilityIfNotNull(this.subtitleTextView, string3, 0);
        setFocusable(true);
    }

    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo accessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(accessibilityNodeInfo);
        accessibilityNodeInfo.setClickable(true);
        accessibilityNodeInfo.setClassName(Button.class.getName());
    }
}
