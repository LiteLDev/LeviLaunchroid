package com.microsoft.xbox.xle.app.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;

import com.microsoft.xbox.toolkit.BackgroundThreadWaitor;
import com.microsoft.xbox.toolkit.anim.MAAS;
import com.microsoft.xbox.toolkit.anim.MAASAnimation;
import com.microsoft.xbox.toolkit.anim.XLEAnimation;
import com.microsoft.xbox.toolkit.anim.XLEAnimationPackage;
import com.microsoft.xbox.toolkit.ui.ScreenLayout;
import com.microsoft.xbox.xle.anim.XLEMAASAnimationPackageNavigationManager;
import com.microsoft.xbox.xle.ui.XLERootView;
import com.microsoft.xbox.xle.viewmodel.ViewModelBase;
import com.microsoft.xboxtcui.XboxTcuiSdk;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;


public abstract class ActivityBase extends ScreenLayout {
    private final boolean showRightPane;
    private final boolean showUtilityBar;
    protected ViewModelBase viewModel;

    public ActivityBase() {
        this(0);
    }

    public ActivityBase(int i) {
        super(XboxTcuiSdk.getApplicationContext(), i);
        this.showUtilityBar = true;
        this.showRightPane = true;
    }

    public ActivityBase(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.showUtilityBar = true;
        this.showRightPane = true;
    }

    public int computeBottomMargin() {
        return 0;
    }

    public boolean delayAppbarAnimation() {
        return false;
    }

    public abstract String getActivityName();

    public String getRelativeId() {
        return null;
    }

    public boolean getShouldShowAppbar() {
        return false;
    }

    public abstract void onCreateContentView();

    public void setHeaderName(String str) {
    }

    public void onActivityResult(int i, int i2, Intent intent) {
        super.onActivityResult(i, i2, intent);
        ViewModelBase viewModelBase = this.viewModel;
        if (viewModelBase != null) {
            viewModelBase.onActivityResult(i, i2, intent);
        }
    }

    public void onStop() {
        if (getIsStarted()) {
            super.onStop();
            ViewModelBase viewModelBase = this.viewModel;
            if (viewModelBase != null) {
                viewModelBase.onSetInactive();
            }
            ViewModelBase viewModelBase2 = this.viewModel;
            if (viewModelBase2 != null) {
                viewModelBase2.onStop();
            }
        }
    }

    public void forceRefresh() {
        ViewModelBase viewModelBase = this.viewModel;
        if (viewModelBase != null) {
            viewModelBase.forceRefresh();
        }
    }

    public void onStart() {
        if (!getIsStarted()) {
            super.onStart();
            ViewModelBase viewModelBase = this.viewModel;
            if (viewModelBase != null) {
                viewModelBase.onStart();
            }
            ViewModelBase viewModelBase2 = this.viewModel;
            if (viewModelBase2 != null) {
                viewModelBase2.load();
            }
        }
        if (!delayAppbarAnimation()) {
            adjustBottomMargin(computeBottomMargin());
        }
    }

    public void onAnimateInStarted() {
        ViewModelBase viewModelBase = this.viewModel;
        if (viewModelBase != null) {
            viewModelBase.forceUpdateViewImmediately();
        }
    }

    public void onAnimateInCompleted() {
        if (this.viewModel != null) {
            final WeakReference weakReference = new WeakReference(this.viewModel);
            BackgroundThreadWaitor.getInstance().postRunnableAfterReady(() -> {
                ViewModelBase viewModelBase = (ViewModelBase) weakReference.get();
                if (viewModelBase != null) {
                    viewModelBase.forceUpdateViewImmediately();
                }
            });
        }
        ViewModelBase viewModelBase = this.viewModel;
        if (viewModelBase != null) {
            viewModelBase.onAnimateInCompleted();
        }
    }

    public void forceUpdateViewImmediately() {
        ViewModelBase viewModelBase = this.viewModel;
        if (viewModelBase != null) {
            viewModelBase.forceUpdateViewImmediately();
        }
    }

    public XLEAnimationPackage getAnimateOut(boolean z) {
        MAASAnimation animation;
        XLEAnimation compile;
        View childAt = getChildAt(0);
        if (childAt == null || (animation = MAAS.getInstance().getAnimation("Screen")) == null || (compile = ((XLEMAASAnimationPackageNavigationManager) animation).compile(MAAS.MAASAnimationType.ANIMATE_OUT, z, childAt)) == null) {
            return null;
        }
        XLEAnimationPackage xLEAnimationPackage = new XLEAnimationPackage();
        xLEAnimationPackage.add(compile);
        return xLEAnimationPackage;
    }

    public XLEAnimationPackage getAnimateIn(boolean z) {
        MAASAnimation animation;
        XLEAnimation compile;
        View childAt = getChildAt(0);
        if (childAt == null || (animation = MAAS.getInstance().getAnimation("Screen")) == null || (compile = ((XLEMAASAnimationPackageNavigationManager) animation).compile(MAAS.MAASAnimationType.ANIMATE_IN, z, childAt)) == null) {
            return null;
        }
        XLEAnimationPackage xLEAnimationPackage = new XLEAnimationPackage();
        xLEAnimationPackage.add(compile);
        return xLEAnimationPackage;
    }

    public void onRestoreInstanceState(Bundle bundle) {
        super.onRestoreInstanceState(bundle);
        ViewModelBase viewModelBase = this.viewModel;
        if (viewModelBase != null) {
            viewModelBase.onRestoreInstanceState(bundle);
        }
    }

    public boolean onBackButtonPressed() {
        ViewModelBase viewModelBase = this.viewModel;
        if (viewModelBase != null) {
            return viewModelBase.onBackButtonPressed();
        }
        return false;
    }

    public void onSetActive() {
        super.onSetActive();
        ViewModelBase viewModelBase = this.viewModel;
        if (viewModelBase != null) {
            viewModelBase.onSetActive();
        }
    }

    @SuppressLint("WrongConstant")
    public boolean dispatchPopulateAccessibilityEvent(@NotNull AccessibilityEvent accessibilityEvent) {
        if (accessibilityEvent.getEventType() != 8 || getXLERootView() == null || getXLERootView().getContentDescription() == null) {
            return super.dispatchPopulateAccessibilityEvent(accessibilityEvent);
        }
        accessibilityEvent.getText().clear();
        accessibilityEvent.getText().add(getXLERootView().getContentDescription());
        return true;
    }

    public void onSetInactive() {
        super.onSetInactive();
        ViewModelBase viewModelBase = this.viewModel;
        if (viewModelBase != null) {
            viewModelBase.onSetInactive();
        }
    }

    public void onPause() {
        super.onPause();
        ViewModelBase viewModelBase = this.viewModel;
        if (viewModelBase != null) {
            viewModelBase.onPause();
        }
    }

    public void onApplicationPause() {
        super.onApplicationPause();
        ViewModelBase viewModelBase = this.viewModel;
        if (viewModelBase != null) {
            viewModelBase.onApplicationPause();
        }
    }

    public void onApplicationResume() {
        super.onApplicationResume();
        ViewModelBase viewModelBase = this.viewModel;
        if (viewModelBase != null) {
            viewModelBase.onApplicationResume();
        }
    }

    public void onResume() {
        super.onResume();
        ViewModelBase viewModelBase = this.viewModel;
        if (viewModelBase != null) {
            viewModelBase.onResume();
        }
    }

    public void onDestroy() {
        ViewModelBase viewModelBase = this.viewModel;
        if (viewModelBase != null) {
            viewModelBase.onDestroy();
        }
        this.viewModel = null;
        super.onDestroy();
    }

    public void onTombstone() {
        ViewModelBase viewModelBase = this.viewModel;
        if (viewModelBase != null) {
            viewModelBase.onTombstone();
        }
        super.onTombstone();
    }

    public void onRehydrate() {
        super.onRehydrate();
        ViewModelBase viewModelBase = this.viewModel;
        if (viewModelBase != null) {
            viewModelBase.onRehydrate();
        }
    }

    public void onRehydrateOverride() {
        onCreateContentView();
    }

    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        ViewModelBase viewModelBase = this.viewModel;
        if (viewModelBase != null) {
            viewModelBase.onConfigurationChanged(configuration);
        }
    }

    private @Nullable XLERootView getXLERootView() {
        if (getChildAt(0) instanceof XLERootView) {
            return (XLERootView) getChildAt(0);
        }
        return null;
    }

    public void adjustBottomMargin(int i) {
        if (getXLERootView() != null) {
            getXLERootView().setBottomMargin(i);
        }
    }

    public void removeBottomMargin() {
        if (getXLERootView() != null) {
            getXLERootView().setBottomMargin(0);
        }
    }

    public void resetBottomMargin() {
        if (getXLERootView() != null) {
            adjustBottomMargin(computeBottomMargin());
        }
    }

    public String getName() {
        return getActivityName();
    }

    public void setScreenState(int i) {
        ViewModelBase viewModelBase = this.viewModel;
        if (viewModelBase != null) {
            viewModelBase.setScreenState(i);
        }
    }

    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        clearDisappearingChildren();
    }
}
