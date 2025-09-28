package com.microsoft.xboxtcui;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.FocusFinder;
import android.view.KeyEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.microsoft.xbox.service.model.ProfileModel;
import com.microsoft.xbox.toolkit.BackgroundThreadWaitor;
import com.microsoft.xbox.toolkit.DialogManager;
import com.microsoft.xbox.toolkit.JavaUtil;
import com.microsoft.xbox.toolkit.ProjectSpecificDataProvider;
import com.microsoft.xbox.toolkit.ThreadManager;
import com.microsoft.xbox.toolkit.XLEAssert;
import com.microsoft.xbox.toolkit.XLEException;
import com.microsoft.xbox.toolkit.XLEUnhandledExceptionHandler;
import com.microsoft.xbox.toolkit.ui.ActivityParameters;
import com.microsoft.xbox.toolkit.ui.NavigationManager;
import com.microsoft.xbox.toolkit.ui.ScreenLayout;
import com.microsoft.xbox.xle.app.SGProjectSpecificDialogManager;
import com.microsoft.xbox.xle.app.XleProjectSpecificDataProvider;

import org.jetbrains.annotations.NotNull;

import java.util.Stack;

/**
 * 02.10.2020
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */

public class XboxTcuiWindow extends FrameLayout implements NavigationManager.NavigationCallbacks, NavigationManager.OnNavigatedListener {
    private static final int NAVIGATION_BLOCK_TIMEOUT_MS = 5000;
    private static final String TAG = XboxTcuiWindow.class.getSimpleName();
    private final ActivityParameters launchParams;
    private final Class<? extends ScreenLayout> launchScreenClass;
    private final Stack<ScreenLayout> screens = new Stack<>();
    private final Activity mActivity;
    private boolean mAnimationBlocking;
    private boolean wasRestarted;

    public XboxTcuiWindow(Activity activity, Class<? extends ScreenLayout> screenClass, @NotNull ActivityParameters params) {
        super(activity);
        XLEAssert.assertNotNull(params.getMeXuid());
        mActivity = activity;
        launchScreenClass = screenClass;
        launchParams = params;
        setBackgroundResource(R.color.backgroundColor);
    }

    public void onCreate(Bundle savedInstanceState) {
        wasRestarted = savedInstanceState != null;
        setupThreadManager();
        ProjectSpecificDataProvider.getInstance().setProvider(XleProjectSpecificDataProvider.getInstance());
        String previousXuid = ProjectSpecificDataProvider.getInstance().getXuidString();
        if (!JavaUtil.isNullOrEmpty(previousXuid) && !previousXuid.equalsIgnoreCase(launchParams.getMeXuid())) {
            ProfileModel.getMeProfileModel();
            ProfileModel.reset();
        }
        ProjectSpecificDataProvider.getInstance().setXuidString(launchParams.getMeXuid());
        ProjectSpecificDataProvider.getInstance().setPrivileges(launchParams.getPrivileges());
        DialogManager.getInstance().setManager(SGProjectSpecificDialogManager.getInstance());
        setFocusableInTouchMode(true);
        requestFocus();
        setupNavigationManager();
    }

    public boolean dispatchUnhandledMove(View focused, int direction) {
        if (focused != this) {
            return false;
        }
        switch (direction) {
            case 1:
            case 33:
                View viewToFocus = FocusFinder.getInstance().findNextFocus(this, getFocusedChild(), 33);
                if (viewToFocus != null) {
                    viewToFocus.requestFocus();
                    break;
                }
                break;
            case 2:
            case 130:
                View viewToFocus2 = FocusFinder.getInstance().findNextFocus(this, getFocusedChild(), 130);
                if (viewToFocus2 != null) {
                    viewToFocus2.requestFocus();
                    break;
                }
                break;
        }
        return true;
    }

    public boolean dispatchKeyEvent(KeyEvent event) {
        if (!NavigationManager.getInstance().onKey(this, event.getKeyCode(), event)) {
            return super.dispatchKeyEvent(event);
        }
        return true;
    }

    public void onStart() {
        XboxTcuiSdk.sdkInitialize(mActivity);
        DialogManager.getInstance().setEnabled(true);
        try {
            if (wasRestarted) {
                ScreenLayout currentScreen = NavigationManager.getInstance().getCurrentActivity();
                if (currentScreen != null) {
                    Bundle outState = new Bundle();
                    NavigationManager.getInstance().getCurrentActivity().onSaveInstanceState(outState);
                    NavigationManager.getInstance().RestartCurrentScreen(false);
                    currentScreen.onRestoreInstanceState(outState);
                }
            } else {
                NavigationManager.getInstance().PushScreen(launchScreenClass, launchParams);
            }
        } catch (XLEException ex) {
            Log.e(TAG, "onStart: " + Log.getStackTraceString(ex));
        } finally {
            wasRestarted = false;
        }
    }

    public void onStop() {
        DialogManager.getInstance().setEnabled(false);
        try {
            NavigationManager.getInstance().PopAllScreens();
        } catch (XLEException ex) {
            Log.e(TAG, "onStop: " + Log.getStackTraceString(ex));
        }
    }

    private void setupThreadManager() {
        ThreadManager.UIThread = Thread.currentThread();
        ThreadManager.Handler = new Handler();
        Thread thread = ThreadManager.UIThread;
        Thread.setDefaultUncaughtExceptionHandler(XLEUnhandledExceptionHandler.Instance);
    }

    private void setupNavigationManager() {
        NavigationManager.getInstance().setNavigationCallbacks(this);
        NavigationManager.getInstance().setOnNavigatedListener(this);
        try {
            NavigationManager.getInstance().PopAllScreens();
        } catch (XLEException ex) {
            Log.e(TAG, "setupNavigationManager: " + Log.getStackTraceString(ex));
        }
    }

    public void addContentViewXLE(ScreenLayout screen) {
        if (!screens.isEmpty()) {
            if (screen == screens.peek()) {
                screen.setAllEventsEnabled(true);
                return;
            } else if (screen.isKeepPreviousScreen()) {
                screens.peek().setAllEventsEnabled(false);
            } else {
                removeView(screens.pop());
            }
        }
        RelativeLayout.LayoutParams activityParams = new RelativeLayout.LayoutParams(-1, -1);
        activityParams.addRule(10);
        activityParams.addRule(12);
        addView(screen, activityParams);
        screens.push(screen);
    }

    public void removeContentViewXLE(ScreenLayout screen) {
        int screenIndex = screens.indexOf(screen);
        if (screenIndex >= 0) {
            while (screens.size() > screenIndex) {
                removeView(screens.pop());
            }
        }
    }

    public void setAnimationBlocking(boolean animationBlocking) {
        if (mAnimationBlocking != animationBlocking) {
            mAnimationBlocking = animationBlocking;
            if (mAnimationBlocking) {
                BackgroundThreadWaitor.getInstance().setBlocking(BackgroundThreadWaitor.WaitType.Navigation, NAVIGATION_BLOCK_TIMEOUT_MS);
            } else {
                BackgroundThreadWaitor.getInstance().clearBlocking(BackgroundThreadWaitor.WaitType.Navigation);
            }
        }
    }

    public void onBeforeNavigatingIn() {
    }

    public void onPageNavigated(ScreenLayout from, ScreenLayout to) {
    }

    public void onPageRestarted(ScreenLayout screen) {
    }
}