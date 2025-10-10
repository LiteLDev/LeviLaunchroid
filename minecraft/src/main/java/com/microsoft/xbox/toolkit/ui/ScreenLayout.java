package com.microsoft.xbox.toolkit.ui;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;

import com.microsoft.xbox.toolkit.ThreadManager;
import com.microsoft.xbox.toolkit.XLEAssert;
import com.microsoft.xbox.toolkit.XLERValueHelper;
import com.microsoft.xbox.toolkit.anim.XLEAnimationPackage;
import com.microsoft.xbox.toolkit.system.SystemUtil;
import com.microsoft.xboxtcui.XboxTcuiSdk;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;


public abstract class ScreenLayout extends FrameLayout {
    private static final ArrayList<View> badList = new ArrayList<>();
    protected boolean isTombstoned;
    private boolean allEventsEnabled;
    private boolean drawerEnabled;
    private boolean isActive;
    private boolean isEditable;
    private boolean isReady;
    private boolean isStarted;
    private Runnable onLayoutChangedListener;
    private int orientation;
    private int screenPercent;

    public ScreenLayout() {
        this(XboxTcuiSdk.getApplicationContext());
    }

    public ScreenLayout(Context context) {
        this(context, 0);
    }

    public ScreenLayout(Context context, int i) {
        super(context);
        this.onLayoutChangedListener = null;
        this.isEditable = false;
        this.screenPercent = 100;
        this.drawerEnabled = true;
        this.allEventsEnabled = true;
        Initialize(i);
    }

    public ScreenLayout(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.onLayoutChangedListener = null;
        this.isEditable = false;
        this.screenPercent = 100;
        this.drawerEnabled = true;
        this.allEventsEnabled = true;
        TypedArray obtainStyledAttributes = context.obtainStyledAttributes(attributeSet, XLERValueHelper.getStyleableRValueArray("ScreenLayout"));
        if (obtainStyledAttributes.hasValue(XLERValueHelper.getStyleableRValue("ScreenLayout_screenDIPs"))) {
            this.screenPercent = (int) ((((float) obtainStyledAttributes.getDimensionPixelSize(XLERValueHelper.getStyleableRValue("ScreenLayout_screenDIPs"), SystemUtil.getScreenWidth())) / ((float) SystemUtil.getScreenWidth())) * 100.0f);
        } else {
            this.screenPercent = obtainStyledAttributes.getInt(XLERValueHelper.getStyleableRValue("ScreenLayout_screenPercent"), -2);
        }
        obtainStyledAttributes.recycle();
        Initialize(7);
    }

    public static void addViewThatCausesAndroidLeaks(View view) {
        badList.add(view);
    }

    public static void removeViewAndWorkaroundAndroidLeaks(View view) {
        if (view != null) {
            ViewParent parent = view.getParent();
            boolean z = true;
            if (parent instanceof ViewGroup) {
                ((ViewGroup) parent).removeAllViews();
                XLEAssert.assertTrue(view.getParent() == null);
            }
            if (view instanceof ViewGroup) {
                ViewGroup viewGroup = (ViewGroup) view;
                viewGroup.removeAllViews();
                viewGroup.destroyDrawingCache();
                if (viewGroup.getChildCount() != 0) {
                    z = false;
                }
                XLEAssert.assertTrue(z);
            }
        }
    }

    public void adjustBottomMargin(int i) {
    }

    public abstract void forceRefresh();

    public abstract void forceUpdateViewImmediately();

    public XLEAnimationPackage getAnimateIn(boolean z) {
        return null;
    }

    public XLEAnimationPackage getAnimateOut(boolean z) {
        return null;
    }

    public String getContent() {
        return null;
    }

    public abstract String getName();

    public String getRelativeId() {
        return null;
    }

    public boolean isAnimateOnPop() {
        return true;
    }

    public boolean isAnimateOnPush() {
        return true;
    }

    public boolean isKeepPreviousScreen() {
        return false;
    }

    public void onActivityResult(int i, int i2, Intent intent) {
    }

    public abstract void onAnimateInCompleted();

    public abstract void onAnimateInStarted();

    public void onApplicationPause() {
    }

    public void onApplicationResume() {
    }

    public abstract boolean onBackButtonPressed();

    public boolean onContextItemSelected(MenuItem menuItem) {
        return false;
    }

    public void onCreate() {
    }

    public void onCreateContextMenu(ContextMenu contextMenu, View view, ContextMenu.ContextMenuInfo contextMenuInfo) {
    }

    public abstract void onRehydrateOverride();

    public void onRestart() {
    }

    public void onRestoreInstanceState(Bundle bundle) {
    }

    public void onSaveInstanceState(Bundle bundle) {
    }

    public void removeBottomMargin() {
    }

    public void resetBottomMargin() {
    }

    public void setScreenState(int i) {
    }

    public void Initialize(int i) {
        this.isReady = false;
        this.isActive = false;
        this.isStarted = false;
        this.orientation = i;
    }

    public void setContentView(int i) {
        LayoutInflater.from(getContext()).inflate(i, this, true);
    }

    public Boolean getTrackPage() {
        return true;
    }

    public void onStart() {
        this.isStarted = true;
    }

    public void onResume() {
        this.isReady = true;
    }

    public void onPause() {
        this.isReady = false;
    }

    public void onStop() {
        this.isStarted = false;
    }

    public void onDestroy() {
        removeAllViewsAndWorkaroundAndroidLeaks();
    }

    public void onTombstone() {
        this.isTombstoned = true;
        removeAllViewsAndWorkaroundAndroidLeaks();
    }

    public void onRehydrate() {
        this.isTombstoned = false;
        onRehydrateOverride();
    }

    public String getLocalClassName() {
        return getClass().getName();
    }

    public boolean getIsTombstoned() {
        return this.isTombstoned;
    }

    public boolean getIsReady() {
        return this.isReady;
    }

    public boolean getIsActive() {
        return this.isActive;
    }

    public boolean getIsStarted() {
        return this.isStarted;
    }

    public void onSetActive() {
        this.isActive = true;
    }

    public void onSetInactive() {
        this.isActive = false;
    }

    public void onLayout(boolean z, int i, int i2, int i3, int i4) {
        super.onLayout(z, i, i2, i3, i4);
        Runnable runnable = this.onLayoutChangedListener;
        if (runnable != null) {
            runnable.run();
        }
    }

    public boolean onInterceptHoverEvent(MotionEvent motionEvent) {
        if (this.allEventsEnabled) {
            return super.onInterceptHoverEvent(motionEvent);
        }
        return true;
    }

    public boolean onInterceptTouchEvent(MotionEvent motionEvent) {
        if (this.allEventsEnabled) {
            return super.onInterceptTouchEvent(motionEvent);
        }
        return true;
    }

    public boolean onTouchEvent(MotionEvent motionEvent) {
        if (this.allEventsEnabled) {
            return super.onTouchEvent(motionEvent);
        }
        return true;
    }

    public boolean onHoverEvent(MotionEvent motionEvent) {
        if (this.allEventsEnabled) {
            return super.onHoverEvent(motionEvent);
        }
        return true;
    }

    public void setOnLayoutChangedListener(Runnable runnable) {
        this.onLayoutChangedListener = runnable;
    }

    public boolean getIsEditable() {
        return this.isEditable;
    }

    public void setIsEditable(boolean z) {
        this.isEditable = z;
    }

    public boolean getCanAutoLaunch() {
        return !this.isEditable;
    }

    public boolean getShouldShowAppbar() {
        return !this.isEditable;
    }

    private void removeAllViewsAndWorkaroundAndroidLeaks() {
        XLEAssert.assertTrue(Thread.currentThread() == ThreadManager.UIThread);
        removeAllViews();
        Iterator<View> it = badList.iterator();
        while (it.hasNext()) {
            removeViewAndWorkaroundAndroidLeaks(it.next());
        }
        badList.clear();
    }

    public int getScreenPercent() {
        return this.screenPercent;
    }

    public ScreenLayout setScreenPercent(int i) {
        this.screenPercent = i;
        return this;
    }

    public boolean isDrawerEnabled() {
        return this.drawerEnabled;
    }

    public void setDrawerEnabled(boolean z) {
        this.drawerEnabled = z;
    }

    public void leaveScreen(@NotNull Runnable runnable) {
        runnable.run();
    }

    public boolean isAllEventsEnabled() {
        return this.allEventsEnabled;
    }

    public void setAllEventsEnabled(boolean z) {
        this.allEventsEnabled = z;
    }

    public View xleFindViewId(int i) {
        return findViewById(i);
    }
}
