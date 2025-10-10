package com.microsoft.xbox.xle.viewmodel;

import android.view.View;

import com.microsoft.xbox.toolkit.DialogManager;
import com.microsoft.xbox.toolkit.XLEAllocationTracker;
import com.microsoft.xbox.toolkit.XLEAssert;
import com.microsoft.xbox.toolkit.anim.XLEAnimation;
import com.microsoft.xbox.toolkit.ui.NavigationManager;
import com.microsoft.xbox.xle.app.XLEUtil;
import com.microsoft.xbox.xle.app.module.ScreenModuleLayout;
import com.microsoft.xboxtcui.XboxTcuiSdk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;


public abstract class AdapterBase {
    private static final HashMap<String, Integer> adapterCounter = new HashMap<>();
    public static String ALLOCATION_TAG = "ADAPTERBASE";
    private final ViewModelBase viewModel;
    private final ArrayList<ScreenModuleLayout> screenModules;
    protected boolean isActive;
    private boolean isStarted;

    public AdapterBase() {
        this(null);
    }

    public AdapterBase(ViewModelBase viewModelBase) {
        this.isActive = false;
        this.isStarted = false;
        this.screenModules = new ArrayList<>();
        this.viewModel = viewModelBase;
        XLEAllocationTracker.getInstance().debugIncrement(ALLOCATION_TAG, getClass().getSimpleName());
        XLEAllocationTracker.getInstance().debugPrintOverallocated(ALLOCATION_TAG);
    }

    public ArrayList<XLEAnimation> getAnimateIn(boolean z) {
        return null;
    }

    public ArrayList<XLEAnimation> getAnimateOut(boolean z) {
        return null;
    }

    public void invalidateViewOverride() {
    }

    public void onAnimateInCompleted() {
    }

    public void onAppBarButtonsAdded() {
    }

    @Deprecated
    public void onAppBarUpdated() {
    }

    public void setScreenState(int i) {
    }

    public abstract void updateViewOverride();

    public boolean getIsStarted() {
        return this.isStarted;
    }

    public void finalize() {
        XLEAllocationTracker.getInstance().debugDecrement(ALLOCATION_TAG, getClass().getSimpleName());
        XLEAllocationTracker.getInstance().debugPrintOverallocated(ALLOCATION_TAG);
    }

    public void updateView() {
        if (!NavigationManager.getInstance().isAnimating()) {
            updateViewOverride();
            Iterator<ScreenModuleLayout> it = this.screenModules.iterator();
            while (it.hasNext()) {
                it.next().updateView();
            }
        }
    }

    public void invalidateView() {
        if (!NavigationManager.getInstance().isAnimating()) {
            invalidateViewOverride();
            Iterator<ScreenModuleLayout> it = this.screenModules.iterator();
            while (it.hasNext()) {
                it.next().invalidateView();
            }
        }
    }

    public void forceUpdateViewImmediately() {
        XLEAssert.assertIsUIThread();
        updateViewOverride();
        Iterator<ScreenModuleLayout> it = this.screenModules.iterator();
        while (it.hasNext()) {
            it.next().updateView();
        }
    }

    public void onPause() {
        Iterator<ScreenModuleLayout> it = this.screenModules.iterator();
        while (it.hasNext()) {
            it.next().onPause();
        }
    }

    public void onApplicationPause() {
        Iterator<ScreenModuleLayout> it = this.screenModules.iterator();
        while (it.hasNext()) {
            it.next().onApplicationPause();
        }
    }

    public void onApplicationResume() {
        Iterator<ScreenModuleLayout> it = this.screenModules.iterator();
        while (it.hasNext()) {
            it.next().onApplicationResume();
        }
    }

    public void onResume() {
        Iterator<ScreenModuleLayout> it = this.screenModules.iterator();
        while (it.hasNext()) {
            it.next().onResume();
        }
    }

    public void onDestroy() {
        Iterator<ScreenModuleLayout> it = this.screenModules.iterator();
        while (it.hasNext()) {
            it.next().onDestroy();
        }
        this.screenModules.clear();
    }

    public void onStart() {
        this.isStarted = true;
        Iterator<ScreenModuleLayout> it = this.screenModules.iterator();
        while (it.hasNext()) {
            it.next().onStart();
        }
    }

    public void onStop() {
        this.isStarted = false;
        Iterator<ScreenModuleLayout> it = this.screenModules.iterator();
        while (it.hasNext()) {
            it.next().onStop();
        }
    }

    public void showKeyboard(View view, int i) {
        XLEUtil.showKeyboard(view, i);
    }

    public void onSetActive() {
        this.isActive = true;
        if (XboxTcuiSdk.getActivity() != null && this.isStarted) {
            updateView();
        }
    }

    public void onSetInactive() {
        this.isActive = false;
    }

    public View findViewById(int i) {
        ViewModelBase viewModelBase = this.viewModel;
        View findViewById = viewModelBase != null ? viewModelBase.findViewById(i) : null;
        return findViewById != null ? findViewById : XboxTcuiSdk.getActivity().findViewById(i);
    }

    public void findAndInitializeModuleById(int i, ViewModelBase viewModelBase) {
        View findViewById = findViewById(i);
        if (findViewById != null && (findViewById instanceof ScreenModuleLayout)) {
            ScreenModuleLayout screenModuleLayout = (ScreenModuleLayout) findViewById(i);
            screenModuleLayout.setViewModel(viewModelBase);
            this.screenModules.add(screenModuleLayout);
        }
    }

    public void setBlocking(boolean z, String str) {
        DialogManager.getInstance().setBlocking(z, str);
    }

    public void setCancelableBlocking(boolean z, String str, Runnable runnable) {
        DialogManager.getInstance().setCancelableBlocking(z, str, runnable);
    }
}
