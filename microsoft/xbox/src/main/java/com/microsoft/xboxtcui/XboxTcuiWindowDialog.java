package com.microsoft.xboxtcui;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.widget.FrameLayout;

import com.microsoft.xbox.toolkit.XLEException;
import com.microsoft.xbox.toolkit.ui.ActivityParameters;
import com.microsoft.xbox.toolkit.ui.NavigationManager;
import com.microsoft.xbox.toolkit.ui.ScreenLayout;
import com.microsoft.xbox.toolkit.ui.XLEButton;

/**
 * 02.10.2020
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */

public class XboxTcuiWindowDialog extends Dialog {
    private final XboxTcuiWindow xboxTcuiWindow;
    private DetachedCallback detachedCallback;

    public XboxTcuiWindowDialog(Activity activity, Class<? extends ScreenLayout> screenClass, ActivityParameters params) {
        super(activity, R.style.TcuiDialog);
        xboxTcuiWindow = new XboxTcuiWindow(activity, screenClass, params);
    }

    public void setDetachedCallback(DetachedCallback callback) {
        detachedCallback = callback;
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(1);
        getWindow().setLayout(-1, -1);
        getWindow().setFlags(1024, 1024);
        xboxTcuiWindow.onCreate(savedInstanceState);
        setContentView(xboxTcuiWindow);
        addCloseButton();
        NavigationManager.getInstance().setOnNavigatedListener(new NavigationManager.OnNavigatedListener() {
            public void onPageNavigated(ScreenLayout from, ScreenLayout to) {
                if (to == null) {
                    XboxTcuiWindowDialog.this.dismiss();
                }
            }

            public void onPageRestarted(ScreenLayout screen) {
            }
        });
    }

    private void addCloseButton() {
        FrameLayout frameLayout = new FrameLayout(getContext());
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(-2, -2);
        layoutParams.gravity = 5;
        XLEButton closeButton = new XLEButton(getContext());
        closeButton.setPadding(60, 0, 0, 0);
        closeButton.setBackgroundResource(R.drawable.common_button_background);
        closeButton.setText(R.string.ic_Close);
        closeButton.setTextColor(-1);
        closeButton.setTextSize(2, 14.0f);
        closeButton.setTypeFace("fonts/SegXboxSymbol.ttf");
        closeButton.setContentDescription(getContext().getResources().getString(R.string.TextInput_Confirm));
        closeButton.setOnClickListener(v -> {
            try {
                NavigationManager.getInstance().PopAllScreens();
            } catch (XLEException e) {
                e.printStackTrace();
            }
        });
        closeButton.setOnKeyListener(NavigationManager.getInstance());
        frameLayout.addView(closeButton);
        addContentView(frameLayout, layoutParams);
    }

    public void onStart() {
        xboxTcuiWindow.onStart();
    }

    public void onStop() {
        xboxTcuiWindow.onStop();
    }

    public void onDetachedFromWindow() {
        if (detachedCallback != null) {
            detachedCallback.onDetachedFromWindow();
        }
        super.onDetachedFromWindow();
    }

    public interface DetachedCallback {
        void onDetachedFromWindow();
    }
}