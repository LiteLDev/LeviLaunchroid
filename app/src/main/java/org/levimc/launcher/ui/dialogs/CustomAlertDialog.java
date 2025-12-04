package org.levimc.launcher.ui.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import org.levimc.launcher.R;
import org.levimc.launcher.ui.animation.DynamicAnim;

public class CustomAlertDialog extends Dialog {

    private String mTitle;
    private String mMessage;
    private String mPositiveText;
    private String mNegativeText;
    private String mNeutralText;
    private View.OnClickListener mPositiveListener;
    private View.OnClickListener mNegativeListener;
    private View.OnClickListener mNeutralListener;

    public CustomAlertDialog(Context context) {
        super(context);
    }

    public CustomAlertDialog setTitleText(String title) {
        this.mTitle = title;
        return this;
    }

    public CustomAlertDialog setMessage(String message) {
        this.mMessage = message;
        return this;
    }

    public CustomAlertDialog setPositiveButton(String text, View.OnClickListener listener) {
        this.mPositiveText = text;
        this.mPositiveListener = listener;
        return this;
    }

    public CustomAlertDialog setNegativeButton(String text, View.OnClickListener listener) {
        this.mNegativeText = text;
        this.mNegativeListener = listener;
        return this;
    }

    public CustomAlertDialog setNeutralButton(String text, View.OnClickListener listener) {
        this.mNeutralText = text;
        this.mNeutralListener = listener;
        return this;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.alert_dialog_custom);
        setCanceledOnTouchOutside(false);

        TextView tvTitle = findViewById(R.id.tv_title);
        TextView tvMessage = findViewById(R.id.tv_message);
        Button btnPositive = findViewById(R.id.btn_positive);
        Button btnNegative = findViewById(R.id.btn_negative);
        Button btnNeutral = findViewById(R.id.btn_neutral);

        View spacingNegativeNeutral = findViewById(R.id.btn_spacing_neg_neu);
        View spacingNeutralPositive = findViewById(R.id.btn_spacing_neu_pos);

        tvTitle.setText(mTitle != null ? mTitle : "");
        tvMessage.setText(mMessage != null ? mMessage : "");

        if (mNegativeText != null) {
            btnNegative.setText(mNegativeText);
            btnNegative.setVisibility(View.VISIBLE);
            if (spacingNegativeNeutral != null) spacingNegativeNeutral.setVisibility(View.VISIBLE);
        } else {
            btnNegative.setVisibility(View.GONE);
            if (spacingNegativeNeutral != null) spacingNegativeNeutral.setVisibility(View.GONE);
        }

        if (mNeutralText != null) {
            btnNeutral.setText(mNeutralText);
            btnNeutral.setVisibility(View.VISIBLE);
            if (spacingNeutralPositive != null) spacingNeutralPositive.setVisibility(View.VISIBLE);
            if (spacingNegativeNeutral != null) spacingNegativeNeutral.setVisibility(View.VISIBLE);
        } else {
            btnNeutral.setVisibility(View.GONE);
            if (spacingNeutralPositive != null) spacingNeutralPositive.setVisibility(View.GONE);
        }

        if (mPositiveText != null) {
            btnPositive.setText(mPositiveText);
            btnPositive.setVisibility(View.VISIBLE);
        } else {
            btnPositive.setVisibility(View.GONE);
        }

        btnPositive.setOnClickListener(v -> {
            if (mPositiveListener != null) mPositiveListener.onClick(v);
            dismiss();
        });

        btnNegative.setOnClickListener(v -> {
            if (mNegativeListener != null) mNegativeListener.onClick(v);
            dismiss();
        });

        btnNeutral.setOnClickListener(v -> {
            if (mNeutralListener != null) mNeutralListener.onClick(v);
            dismiss();
        });

        Window window = getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        View content = findViewById(android.R.id.content);
        if (content != null) {
            DynamicAnim.animateDialogShow(content);
        }
        DynamicAnim.applyPressScale(btnPositive);
        DynamicAnim.applyPressScale(btnNegative);
        DynamicAnim.applyPressScale(btnNeutral);
    }

    @Override
    public void dismiss() {
        try {
            if (!isShowing()) {
                return;
            }
            Window window = getWindow();
            if (window == null || window.getDecorView().getParent() == null) {
                try {
                    super.dismiss();
                } catch (Exception ignored) {}
                return;
            }
            window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams params = window.getAttributes();
            params.dimAmount = 0f;
            window.setAttributes(params);

            View content = findViewById(android.R.id.content);
            if (content != null) {
                DynamicAnim.animateDialogDismiss(content, () -> {
                    try {
                        if (isShowing()) {
                            Window w = getWindow();
                            if (w != null && w.getDecorView().getParent() != null) {
                                CustomAlertDialog.super.dismiss();
                            }
                        }
                    } catch (Exception ignored) {}
                });
            } else {
                super.dismiss();
            }
        } catch (Exception ignored) {}
    }

    public void dismissImmediately() {
        try {
            if (isShowing()) {
                super.dismiss();
            }
        } catch (Exception ignored) {}
    }
}