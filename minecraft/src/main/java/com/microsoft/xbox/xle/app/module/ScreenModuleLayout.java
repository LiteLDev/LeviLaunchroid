package com.microsoft.xbox.xle.app.module;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;

import com.microsoft.xbox.xle.viewmodel.ViewModelBase;
import com.microsoft.xboxtcui.XboxTcuiSdk;


public abstract class ScreenModuleLayout extends FrameLayout {
    public ScreenModuleLayout(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public abstract ViewModelBase getViewModel();

    public abstract void setViewModel(ViewModelBase viewModelBase);

    public void invalidateView() {
    }

    public void onApplicationPause() {
    }

    public void onApplicationResume() {
    }

    public void onDestroy() {
    }

    public void onPause() {
    }

    public void onResume() {
    }

    public void onStart() {
    }

    public void onStop() {
    }

    public abstract void updateView();

    public void setContentView(int i) {
        ((LayoutInflater) XboxTcuiSdk.getSystemService("layout_inflater")).inflate(i, this, true);
    }
}
