package com.microsoft.xbox.xle.viewmodel;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;

import com.microsoft.xbox.service.model.UpdateData;
import com.microsoft.xbox.service.model.UpdateType;
import com.microsoft.xbox.toolkit.AsyncResult;
import com.microsoft.xbox.toolkit.DialogManager;
import com.microsoft.xbox.toolkit.ThreadManager;
import com.microsoft.xbox.toolkit.XLEAssert;
import com.microsoft.xbox.toolkit.XLEErrorCode;
import com.microsoft.xbox.toolkit.XLEException;
import com.microsoft.xbox.toolkit.XLEObserver;
import com.microsoft.xbox.toolkit.anim.XLEAnimation;
import com.microsoft.xbox.toolkit.anim.XLEAnimationPackage;
import com.microsoft.xbox.toolkit.ui.ActivityParameters;
import com.microsoft.xbox.toolkit.ui.NavigationManager;
import com.microsoft.xbox.toolkit.ui.ScreenLayout;
import com.microsoft.xbox.xle.app.XLEUtil;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;


public abstract class ViewModelBase implements XLEObserver<UpdateData> {
    public static final String TAG_PAGE_LOADING_TIME = "performance_measure_page_loadingtime";
    protected static int LAUNCH_TIME_OUT = 5000;
    private final ScreenLayout screen;
    private final HashMap<UpdateType, XLEException> updateExceptions;
    protected int LifetimeInMinutes;
    protected AdapterBase adapter;
    protected boolean isActive;
    protected boolean isForeground;
    protected boolean isLaunching;
    protected Runnable launchTimeoutHandler;
    protected int listIndex;
    protected int offset;
    private NavigationData nextScreenData;
    private boolean onlyProcessExceptionsAndShowToastsWhenActive;
    private ViewModelBase parent;
    private boolean shouldHideScreen;
    private boolean showNoNetworkPopup;
    private EnumSet<UpdateType> updateTypesToCheck;
    private boolean updating;

    public ViewModelBase(ScreenLayout screenLayout) {
        this(screenLayout, true, false);
    }

    public ViewModelBase() {
        this(null, true, false);
    }

    public ViewModelBase(boolean z, boolean z2) {
        this(null, z, z2);
    }

    public ViewModelBase(ScreenLayout screenLayout, boolean z, boolean z2) {
        this.LifetimeInMinutes = 60;
        this.updateExceptions = new HashMap<>();
        this.showNoNetworkPopup = true;
        this.onlyProcessExceptionsAndShowToastsWhenActive = false;
        this.nextScreenData = null;
        this.updating = false;
        this.isLaunching = false;
        this.screen = screenLayout;
        this.showNoNetworkPopup = z;
        this.onlyProcessExceptionsAndShowToastsWhenActive = z2;
    }

    public void TEST_induceGoBack() {
    }

    public String getBlockingStatusText() {
        return null;
    }

    public boolean isBlockingBusy() {
        return false;
    }

    public abstract boolean isBusy();

    public abstract void load(boolean z);

    /* access modifiers changed from: protected */
    public void logOut(boolean z) {
    }

    public void onActivityResult(int i, int i2, Intent intent) {
    }

    public boolean onBackButtonPressed() {
        return false;
    }

    public void onChildViewModelChanged(ViewModelBase viewModelBase) {
    }

    public void onConfigurationChanged(Configuration configuration) {
    }

    public abstract void onRehydrate();

    public void onRestoreInstanceState(Bundle bundle) {
    }

    public void onSaveInstanceState(Bundle bundle) {
    }

    public abstract void onStartOverride();

    public abstract void onStopOverride();

    public boolean shouldDismissTopNoFatalAlert() {
        return true;
    }

    public boolean shouldRefreshAsPivotHeader() {
        return false;
    }

    public void showMustActDialog(String str, String str2, String str3, Runnable runnable, boolean z) {
    }

    public void updateOverride(AsyncResult<UpdateData> asyncResult) {
    }

    public boolean updateWithoutAdapter() {
        return false;
    }

    public View findViewById(int i) {
        ScreenLayout screenLayout = this.screen;
        if (screenLayout != null) {
            return screenLayout.xleFindViewId(i);
        }
        return null;
    }

    public ScreenLayout getScreen() {
        return this.screen;
    }

    public ViewModelBase getParent() {
        return this.parent;
    }

    public void setParent(ViewModelBase viewModelBase) {
        this.parent = viewModelBase;
    }

    public AdapterBase getAdapter() {
        return this.adapter;
    }

    public void updateAdapter() {
        updateAdapter(true);
    }

    public void updateAdapter(boolean z) {
        AdapterBase adapterBase = this.adapter;
        if (adapterBase != null) {
            adapterBase.updateView();
        }
        ViewModelBase viewModelBase = this.parent;
        if (viewModelBase != null && z) {
            viewModelBase.onChildViewModelChanged(this);
        }
    }

    public void onStart() {
        this.isForeground = true;
        onStartOverride();
        AdapterBase adapterBase = this.adapter;
        if (adapterBase != null) {
            adapterBase.onStart();
        }
    }

    public boolean getShouldHideScreen() {
        return this.shouldHideScreen;
    }

    public void setShouldHideScreen(boolean z) {
        this.shouldHideScreen = z;
    }

    public void setListPosition(int i, int i2) {
        this.listIndex = i;
        this.offset = i2;
    }

    public int getAndResetListPosition() {
        int i = this.listIndex;
        this.listIndex = 0;
        return i;
    }

    public int getAndResetListOffset() {
        int i = this.offset;
        this.offset = 0;
        return i;
    }

    public void onStop() {
        this.isForeground = false;
        AdapterBase adapterBase = this.adapter;
        if (adapterBase != null) {
            adapterBase.onStop();
        }
        DialogManager.getInstance().dismissBlocking();
        if (shouldDismissTopNoFatalAlert()) {
            DialogManager.getInstance().dismissTopNonFatalAlert();
        }
        DialogManager.getInstance().dismissToast();
        onStopOverride();
    }

    public void onPause() {
        cancelLaunchTimeout();
        AdapterBase adapterBase = this.adapter;
        if (adapterBase != null) {
            adapterBase.onPause();
        }
    }

    public void onApplicationPause() {
        AdapterBase adapterBase = this.adapter;
        if (adapterBase != null) {
            adapterBase.onApplicationPause();
        }
    }

    public void onApplicationResume() {
        AdapterBase adapterBase = this.adapter;
        if (adapterBase != null) {
            adapterBase.onApplicationResume();
        }
    }

    public void onResume() {
        AdapterBase adapterBase = this.adapter;
        if (adapterBase != null) {
            adapterBase.onResume();
            this.adapter.updateView();
        }
    }

    public void onDestroy() {
        AdapterBase adapterBase = this.adapter;
        if (adapterBase != null) {
            adapterBase.onDestroy();
        }
        this.adapter = null;
    }

    public void onTombstone() {
        AdapterBase adapterBase = this.adapter;
        if (adapterBase != null) {
            adapterBase.onDestroy();
        }
        this.adapter = null;
    }

    public void forceUpdateViewImmediately() {
        AdapterBase adapterBase = this.adapter;
        if (adapterBase != null) {
            adapterBase.forceUpdateViewImmediately();
        }
    }

    public void load() {
        load(XLEGlobalData.getInstance().CheckDrainShouldRefresh(getClass()));
    }

    public void forceRefresh() {
        load(true);
        AdapterBase adapterBase = this.adapter;
        if (adapterBase != null) {
            adapterBase.updateView();
        }
    }

    public final void update(@NotNull AsyncResult<UpdateData> asyncResult) {
        updating = true;
        XLEAssert.assertTrue(nextScreenData == null);
        nextScreenData = null;
        if (asyncResult.getException() != null) {
            long errorCode = asyncResult.getException().getErrorCode();
            if (!asyncResult.getException().getIsHandled() && errorCode == XLEErrorCode.INVALID_ACCESS_TOKEN) {
                asyncResult.getException().setIsHandled(true);
            }
        }
        if (nextScreenData == null && (adapter != null || updateWithoutAdapter())) {
            updateOverride(asyncResult);
        }
        updating = false;
        if (nextScreenData != null) {
            try {
                switch (nextScreenData.getNavigationType()) {
                    case Push:
                        NavigationManager.getInstance().NavigateTo(nextScreenData.getScreenClass(), true);
                        break;
                    case PopReplace:
                        NavigationManager.getInstance().NavigateTo(nextScreenData.getScreenClass(), false);
                        break;
                    case PopAll:
                        NavigationManager.getInstance().GotoScreenWithPop(nextScreenData.getScreenClass());
                        break;
                }
            } catch (XLEException e) {
                e.printStackTrace();
            }
        } else if (shouldProcessErrors()) {
            if (asyncResult.getException() != null && !asyncResult.getException().getIsHandled() && updateTypesToCheck != null && updateTypesToCheck.contains(asyncResult.getResult().getUpdateType())) {
                updateExceptions.put(asyncResult.getResult().getUpdateType(), asyncResult.getException());
            }
            if (asyncResult.getResult().getIsFinal()) {
                if (updateTypesToCheck != null) {
                    updateTypesToCheck.remove(asyncResult.getResult().getUpdateType());
                }
                if (updateTypesToCheck == null || updateTypesToCheck.isEmpty()) {
                    onUpdateFinished();
                    updateTypesToCheck = null;
                }
            }
        }
        nextScreenData = null;
    }

    public void setUpdateTypesToCheck(EnumSet<UpdateType> enumSet) {
        this.updateTypesToCheck = enumSet;
        this.updateExceptions.clear();
    }

    public boolean checkErrorCode(UpdateType updateType, long j) {
        return this.updateExceptions.containsKey(updateType) && this.updateExceptions.get(updateType).getErrorCode() == j && !this.updateExceptions.get(updateType).getIsHandled();
    }

    public boolean updateTypesToCheckIsEmpty() {
        EnumSet<UpdateType> enumSet = this.updateTypesToCheck;
        return enumSet == null || enumSet.isEmpty();
    }

    public boolean updateTypesToCheckHadAnyErrors() {
        return !this.updateExceptions.isEmpty();
    }

    public void onUpdateFinished() {
        this.updateTypesToCheck = null;
        this.updateExceptions.clear();
    }

    public XLEAnimationPackage getAnimateOut(boolean z) {
        ArrayList<XLEAnimation> animateOut = this.adapter.getAnimateOut(z);
        if (animateOut == null || animateOut.size() <= 0) {
            return null;
        }
        XLEAnimationPackage xLEAnimationPackage = new XLEAnimationPackage();
        Iterator<XLEAnimation> it = animateOut.iterator();
        while (it.hasNext()) {
            xLEAnimationPackage.add(it.next());
        }
        return xLEAnimationPackage;
    }

    public XLEAnimationPackage getAnimateIn(boolean z) {
        ArrayList<XLEAnimation> animateIn = this.adapter.getAnimateIn(z);
        if (animateIn == null || animateIn.size() <= 0) {
            return null;
        }
        XLEAnimationPackage xLEAnimationPackage = new XLEAnimationPackage();
        Iterator<XLEAnimation> it = animateIn.iterator();
        while (it.hasNext()) {
            xLEAnimationPackage.add(it.next());
        }
        return xLEAnimationPackage;
    }

    public void onAnimateInCompleted() {
        AdapterBase adapterBase = this.adapter;
        if (adapterBase != null) {
            adapterBase.onAnimateInCompleted();
        }
    }

    public void NavigateTo(Class<? extends ScreenLayout> cls, ActivityParameters activityParameters) {
        NavigateTo(cls, true, activityParameters);
    }

    public void NavigateTo(Class<? extends ScreenLayout> cls) {
        NavigateTo(cls, null);
    }

    public void NavigateTo(Class<? extends ScreenLayout> cls, boolean z, ActivityParameters activityParameters) {
        cancelLaunchTimeout();
        XLEAssert.assertFalse("We shouldn't navigate to a new screen if the current screen is blocking", isBlockingBusy());
        if (this.updating) {
            this.nextScreenData = new NavigationData(cls, z ? NavigationType.Push : NavigationType.PopReplace);
            return;
        }
        XLEAssert.assertFalse("We shouldn't navigate to a new screen if the current screen is blocking", isBlockingBusy());
        NavigationManager.getInstance().NavigateTo(cls, z, activityParameters);
    }

    public void NavigateTo(Class<? extends ScreenLayout> cls, boolean z) {
        NavigateTo(cls, z, null);
    }

    public void showOkCancelDialog(String str, String str2, Runnable runnable, String str3, Runnable runnable2) {
        showOkCancelDialog(null, str, str2, runnable, str3, runnable2);
    }

    public void showOkCancelDialog(String str, String str2, String str3, Runnable runnable, String str4, Runnable runnable2) {
        if (shouldProcessErrors()) {
            XLEUtil.showOkCancelDialog(str, str2, str3, runnable, str4, runnable2);
        }
    }

    public void showError(int i) {
        DialogManager.getInstance().showToast(i);
    }

    public void onSetActive() {
        this.isActive = true;
        AdapterBase adapterBase = this.adapter;
        if (adapterBase != null) {
            adapterBase.onSetActive();
        }
    }

    public void onSetInactive() {
        DialogManager.getInstance().dismissToast();
        this.isActive = false;
        AdapterBase adapterBase = this.adapter;
        if (adapterBase != null) {
            adapterBase.onSetInactive();
        }
    }

    public boolean getIsActive() {
        return this.isActive;
    }

    public boolean getShowNoNetworkPopup() {
        return this.showNoNetworkPopup;
    }

    private boolean shouldProcessErrors() {
        if (this.onlyProcessExceptionsAndShowToastsWhenActive) {
            return this.isActive;
        }
        return true;
    }

    public void setAsPivotPane() {
        this.showNoNetworkPopup = true;
        this.onlyProcessExceptionsAndShowToastsWhenActive = true;
    }

    public void cancelLaunchTimeout() {
        this.isLaunching = false;
        if (this.launchTimeoutHandler != null) {
            ThreadManager.Handler.removeCallbacks(this.launchTimeoutHandler);
        }
    }

    public void cancelLaunch() {
        this.isLaunching = false;
    }

    public void adapterUpdateView() {
        AdapterBase adapterBase = this.adapter;
        if (adapterBase != null) {
            adapterBase.updateView();
        }
    }

    public void setScreenState(int i) {
        AdapterBase adapterBase = this.adapter;
        if (adapterBase != null) {
            adapterBase.setScreenState(i);
        }
    }

    public void leaveViewModel(@NotNull Runnable runnable) {
        runnable.run();
    }

    private enum NavigationType {
        Push,
        PopReplace,
        PopAll
    }

    private class NavigationData {
        private final NavigationType navigationType;
        private final Class<? extends ScreenLayout> screenClass;

        protected NavigationData(Class<? extends ScreenLayout> cls, NavigationType navigationType2) {
            this.screenClass = cls;
            this.navigationType = navigationType2;
        }

        public Class<? extends ScreenLayout> getScreenClass() {
            return this.screenClass;
        }

        public NavigationType getNavigationType() {
            return this.navigationType;
        }
    }
}
