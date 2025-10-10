package com.microsoft.xbox.toolkit.ui;

import android.app.Dialog;
import android.content.Context;
import android.widget.TextView;

import com.microsoft.xbox.toolkit.XLERValueHelper;


public class BlockingScreen extends Dialog {
    public BlockingScreen(Context context) {
        super(context, XLERValueHelper.getStyleRValue("blocking_dialog_style"));
        requestWindowFeature(1);
    }

    public void show(Context context, CharSequence charSequence) {
        setCancelable(false);
        setOnCancelListener(null);
        setContentView(XLERValueHelper.getLayoutRValue("blocking_dialog"));
        setMessage(charSequence);
        show();
    }

    public void setMessage(CharSequence charSequence) {
        ((TextView) findViewById(XLERValueHelper.getIdRValue("blocking_dialog_status_text"))).setText(charSequence);
    }
}
