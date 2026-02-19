package org.levimc.launcher.ui.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import org.levimc.launcher.R;
import org.levimc.launcher.ui.animation.DynamicAnim;

public class InstallProgressDialog extends Dialog {

    //private ProgressBar progressBar;

    public InstallProgressDialog(Context context) {
        super(context);
        setCancelable(false);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_install_progress);


        Window window = getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            WindowManager.LayoutParams params = window.getAttributes();
            params.width = WindowManager.LayoutParams.WRAP_CONTENT;
            params.height = WindowManager.LayoutParams.WRAP_CONTENT;
            params.dimAmount = 0.6f;
            window.setAttributes(params);
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        }

        View content = findViewById(android.R.id.content);
        if (content != null) {
            DynamicAnim.animateDialogShow(content);
        }
    }

    @Override
    public void dismiss() {
        Window window = getWindow();
        if (window != null) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams params = window.getAttributes();
            params.dimAmount = 0f;
            window.setAttributes(params);
        }
        View content = findViewById(android.R.id.content);
        if (content != null) {
            DynamicAnim.animateDialogDismiss(content, () -> InstallProgressDialog.super.dismiss());
        } else {
            super.dismiss();
        }
    }
}