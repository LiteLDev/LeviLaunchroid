package com.microsoft.xbox.xle.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;
import android.widget.LinearLayout;

import com.mojang.minecraftpe.R;
import com.microsoft.xbox.toolkit.JavaUtil;
import com.microsoft.xbox.toolkit.XLERValueHelper;
import com.microsoft.xbox.toolkit.ui.CustomTypefaceTextView;
import com.microsoft.xbox.toolkit.ui.XLEUniversalImageView;
import com.microsoft.xbox.xle.app.XLEUtil;

import java.net.URI;


public class ImageTitleSubtitleButton extends LinearLayout {
    private final XLEUniversalImageView iconImageView;
    private final CustomTypefaceTextView subtitleTextView;
    private final CustomTypefaceTextView titleTextView;

    public ImageTitleSubtitleButton(Context context) {
        this(context, null);
    }

    public ImageTitleSubtitleButton(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public ImageTitleSubtitleButton(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        LayoutInflater.from(context).inflate(R.layout.image_title_subtitle_button, this, true);
        this.iconImageView = findViewById(R.id.image_title_subtitle_button_image);
        this.titleTextView = findViewById(R.id.image_title_subtitle_button_title);
        this.subtitleTextView = findViewById(R.id.image_title_subtitle_button_subtitle);
        TypedArray obtainStyledAttributes = context.obtainStyledAttributes(attributeSet, XLERValueHelper.getStyleableRValueArray("ImageTitleSubtitleButton"));
        String string = obtainStyledAttributes.getString(XLERValueHelper.getStyleableRValue("ImageTitleSubtitleButton_image_uri"));
        String string2 = obtainStyledAttributes.getString(XLERValueHelper.getStyleableRValue("ImageTitleSubtitleButton_text_title"));
        String string3 = obtainStyledAttributes.getString(XLERValueHelper.getStyleableRValue("ImageTitleSubtitleButton_text_subtitle"));
        obtainStyledAttributes.recycle();
        setImageUri(string);
        XLEUtil.updateTextAndVisibilityIfNotNull(this.titleTextView, string2, 0);
        XLEUtil.updateTextAndVisibilityIfNotNull(this.subtitleTextView, string3, 0);
        setFocusable(true);
    }

    public void setImageUri(String str) {
        if (!JavaUtil.isNullOrEmpty(str)) {
            this.iconImageView.setImageURI2(URI.create(str));
        }
    }

    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo accessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(accessibilityNodeInfo);
        accessibilityNodeInfo.setClickable(true);
        accessibilityNodeInfo.setClassName(Button.class.getName());
    }
}
