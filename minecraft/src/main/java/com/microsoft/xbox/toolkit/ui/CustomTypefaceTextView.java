package com.microsoft.xbox.toolkit.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;


@SuppressLint("AppCompatCustomView")
public class CustomTypefaceTextView extends TextView {
    public CustomTypefaceTextView(Context context, String str) {
        super(context);
        applyCustomTypeface(context, str);
    }

    public CustomTypefaceTextView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        /*if (!isInEditMode()) {
            TypedArray obtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.CustomTypeface);
            String string = obtainStyledAttributes.getString(R.styleable.CustomTypeface_typefaceSource);
            String string2 = obtainStyledAttributes.getString(R.styleable.CustomTypeface_uppercaseText);
            if (string2 != null) {
                setText(string2.toUpperCase());
            }
            applyCustomTypeface(context, string);
            obtainStyledAttributes.recycle();
        }*/
    }

    public CustomTypefaceTextView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        /*if (!isInEditMode()) {
            TypedArray obtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.CustomTypeface);
            applyCustomTypeface(context, obtainStyledAttributes.getString(R.styleable.CustomTypeface_typefaceSource));
            obtainStyledAttributes.recycle();
        }*/
    }

    private void applyCustomTypeface(Context context, String str) {
        if (str != null) {
            setTypeface(FontManager.Instance().getTypeface(getContext(), str));
        }
        setCursorVisible(false);
    }

    public void setOnClickListener(View.OnClickListener onClickListener) {
        throw new UnsupportedOperationException("If you want CustomTypefaceTextView to be clickable, use XLEButton instead.");
    }

    public void setClickable(boolean z) {
        if (z) {
            throw new UnsupportedOperationException("If you want CustomTypefaceTextView to be clickable, use XLEButton instead.");
        }
    }
}
