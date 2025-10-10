package com.microsoft.xbox.toolkit.ui;

import android.view.KeyEvent;
import android.view.View;

import com.microsoft.xbox.toolkit.ThreadManager;
import com.microsoft.xbox.toolkit.XLEAssert;
import com.microsoft.xbox.toolkit.XLEException;
import com.microsoft.xbox.toolkit.anim.XLEAnimationPackage;
import com.microsoft.xboxtcui.XboxTcuiSdk;

import java.util.Iterator;
import java.util.Stack;


public class NavigationManager implements View.OnKeyListener {
    private static final String TAG = "NavigationManager";
    public final Stack<ActivityParameters> navigationParameters;
    public final Stack<ScreenLayout> navigationStack;
    final Runnable callAfterAnimation;
    public boolean cannotNavigateTripwire;
    public OnNavigatedListener navigationListener;
    private NavigationManagerAnimationState animationState;
    private XLEAnimationPackage currentAnimation;
    private boolean goingBack;
    private NavigationCallbacks navigationCallbacks;
    private boolean transitionAnimate;
    private Runnable transitionLambda;

    private NavigationManager() {
        this.navigationParameters = new Stack<>();
        this.navigationStack = new Stack<>();
        this.currentAnimation = null;
        this.animationState = NavigationManagerAnimationState.NONE;
        this.transitionLambda = null;
        boolean z = false;
        this.goingBack = false;
        this.transitionAnimate = true;
        this.cannotNavigateTripwire = false;
        this.callAfterAnimation = new Runnable() {
            public void run() {
                NavigationManager.this.OnAnimationEnd();
            }
        };
        XLEAssert.assertTrue("You must access navigation manager on UI thread.", Thread.currentThread() == ThreadManager.UIThread || z);
    }

    public static NavigationManager getInstance() {
        return NavigationManagerHolder.instance;
    }

    public boolean TEST_isAnimatingIn() {
        return false;
    }

    public boolean TEST_isAnimatingOut() {
        return false;
    }

    public ScreenLayout getCurrentActivity() {
        if (this.navigationStack.empty()) {
            return null;
        }
        return this.navigationStack.peek();
    }

    public String getCurrentActivityName() {
        ScreenLayout currentActivity = getCurrentActivity();
        if (currentActivity != null) {
            return currentActivity.getName();
        }
        return null;
    }

    public ScreenLayout getPreviousActivity() {
        if (this.navigationStack.empty() || this.navigationStack.size() <= 1) {
            return null;
        }
        Stack<ScreenLayout> stack = this.navigationStack;
        return stack.get(stack.size() - 2);
    }

    public void setOnNavigatedListener(OnNavigatedListener onNavigatedListener) {
        this.navigationListener = onNavigatedListener;
    }

    public void removeNaviationListener() {
        this.navigationListener = null;
    }

    public void setNavigationCallbacks(NavigationCallbacks navigationCallbacks2) {
        this.navigationCallbacks = navigationCallbacks2;
    }

    public void removeNavigationCallbacks() {
        this.navigationCallbacks = null;
    }

    public ActivityParameters getActivityParameters() {
        return getActivityParameters(0);
    }

    public ActivityParameters getActivityParameters(int i) {
        XLEAssert.assertTrue(i >= 0 && i < this.navigationParameters.size());
        Stack<ActivityParameters> stack = this.navigationParameters;
        return stack.get((stack.size() - i) - 1);
    }

    public void NavigateTo(Class<? extends ScreenLayout> cls, boolean z, ActivityParameters activityParameters) {
        if (z) {
            try {
                PushScreen(cls, activityParameters);
            } catch (XLEException unused) {
            }
        } else {
            try {
                PopScreensAndReplace(1, cls, activityParameters);
            } catch (XLEException e) {
                e.printStackTrace();
            }
        }
    }

    public void NavigateTo(Class<? extends ScreenLayout> cls, boolean z) {
        NavigateTo(cls, z, null);
    }

    public boolean OnBackButtonPressed() {
        boolean ShouldBackCloseApp = ShouldBackCloseApp();
        if (getCurrentActivity() != null && !getCurrentActivity().onBackButtonPressed()) {
            if (ShouldBackCloseApp) {
                try {
                    PopScreensAndReplace(1, null, false, false, false);
                } catch (XLEException unused) {
                }
            } else {
                try {
                    PopScreen();
                } catch (XLEException e) {
                    e.printStackTrace();
                }
            }
        }
        return ShouldBackCloseApp;
    }

    public boolean isAnimating() {
        return this.animationState != NavigationManagerAnimationState.NONE;
    }

    public boolean ShouldBackCloseApp() {
        return Size() <= 1 && this.animationState == NavigationManagerAnimationState.NONE;
    }

    public boolean IsScreenOnStack(Class<? extends ScreenLayout> cls) {
        Iterator it = this.navigationStack.iterator();
        while (it.hasNext()) {
            if (((ScreenLayout) it.next()).getClass().equals(cls)) {
                return true;
            }
        }
        return false;
    }

    public int CountPopsToScreen(Class<? extends ScreenLayout> cls) {
        int size = this.navigationStack.size() - 1;
        for (int i = size; i >= 0; i--) {
            if (((ScreenLayout) this.navigationStack.get(i)).getClass().equals(cls)) {
                return size - i;
            }
        }
        return -1;
    }

    private int Size() {
        return this.navigationStack.size();
    }

    public void RestartCurrentScreen(boolean z) throws XLEException {
        RestartCurrentScreen(null, z);
    }

    public void RestartCurrentScreen(ActivityParameters activityParameters, boolean z) throws XLEException {
        if (this.animationState == NavigationManagerAnimationState.ANIMATING_OUT) {
            OnAnimationEnd();
        } else if (this.animationState == NavigationManagerAnimationState.ANIMATING_IN) {
            OnAnimationEnd();
            PopScreensAndReplace(1, getCurrentActivity().getClass(), z, true, true, activityParameters);
        } else {
            PopScreensAndReplace(1, getCurrentActivity().getClass(), z, true, true, activityParameters);
        }
    }

    public void PopScreen() throws XLEException {
        PopScreens(1);
    }

    public void PopScreens(int i) throws XLEException {
        PopScreensAndReplace(i, null);
    }

    public void PopAllScreens() throws XLEException {
        if (Size() > 0) {
            PopScreensAndReplace(Size(), null, false, false, false);
        }
    }

    public void PopTillScreenThenPush(Class<? extends ScreenLayout> cls, Class<? extends ScreenLayout> cls2, ActivityParameters activityParameters) throws XLEException {
        int CountPopsToScreen = CountPopsToScreen(cls);
        if (CountPopsToScreen > 0) {
            PopScreensAndReplace(CountPopsToScreen, cls2, true, true, false, activityParameters);
        } else if (CountPopsToScreen < 0) {
            PopScreensAndReplace(0, cls2, true, false, false, activityParameters);
        } else {
            PopScreensAndReplace(0, cls2, true, false, false, activityParameters);
        }
    }

    public void PopTillScreenThenPush(Class<? extends ScreenLayout> cls, Class<? extends ScreenLayout> cls2) throws XLEException {
        PopTillScreenThenPush(cls, cls2, null);
    }

    public void GotoScreenWithPop(Class<? extends ScreenLayout> cls) throws XLEException {
        int CountPopsToScreen = CountPopsToScreen(cls);
        if (CountPopsToScreen > 0) {
            PopScreensAndReplace(CountPopsToScreen, null, true, false, false);
        } else if (CountPopsToScreen < 0) {
            PopScreensAndReplace(Size(), cls, true, false, false);
        } else {
            RestartCurrentScreen(true);
        }
    }

    public void GotoScreenWithPop(ActivityParameters activityParameters, Class<? extends ScreenLayout> cls, Class<? extends ScreenLayout>... clsArr) throws XLEException {
        Class<?> cls2;
        int size = this.navigationStack.size() - 1;
        int i = size;
        loop0:
        while (true) {
            if (i < 0) {
                cls2 = null;
                break;
            }
            Class<?> cls3 = ((ScreenLayout) this.navigationStack.get(i)).getClass();
            int length = clsArr.length;
            for (int i2 = 0; i2 < length; i2++) {
                cls2 = clsArr[i2];
                if (cls2 == cls3) {
                    break loop0;
                }
            }
            i--;
        }
        if (cls2 == null) {
            PopScreensAndReplace(Size(), cls, true, true, false, activityParameters);
        } else if (cls2 != cls) {
            PopScreensAndReplace(size - i, cls, true, true, false, activityParameters);
        } else if (i == size) {
            RestartCurrentScreen(activityParameters, false);
        } else {
            PopScreensAndReplace(size - i, null, true, true, false, activityParameters);
        }
    }

    public void GotoScreenWithPush(Class<? extends ScreenLayout> cls) throws XLEException {
        int CountPopsToScreen = CountPopsToScreen(cls);
        if (CountPopsToScreen > 0) {
            PopScreensAndReplace(CountPopsToScreen, null, true, false, false);
        } else if (CountPopsToScreen < 0) {
            PopScreensAndReplace(0, cls, true, false, false);
        } else {
            RestartCurrentScreen(true);
        }
    }

    public void GotoScreenWithPush(Class<? extends ScreenLayout> cls, ActivityParameters activityParameters) throws XLEException {
        int CountPopsToScreen = CountPopsToScreen(cls);
        if (CountPopsToScreen > 0) {
            PopScreensAndReplace(CountPopsToScreen, null, true, false, false, activityParameters);
        } else if (CountPopsToScreen < 0) {
            PopScreensAndReplace(0, cls, true, false, false, activityParameters);
        } else {
            RestartCurrentScreen(true);
        }
    }

    public void PushScreen(Class<? extends ScreenLayout> cls, ActivityParameters activityParameters) throws XLEException {
        PopScreensAndReplace(0, cls, true, false, false, activityParameters);
    }

    public void PushScreen(Class<? extends ScreenLayout> cls) throws XLEException {
        PushScreen(cls, null);
    }

    public void PopScreensAndReplace(int i, Class<? extends ScreenLayout> cls, ActivityParameters activityParameters) throws XLEException {
        PopScreensAndReplace(i, cls, true, true, false, activityParameters);
    }

    public void PopScreensAndReplace(int i, Class<? extends ScreenLayout> cls) throws XLEException {
        PopScreensAndReplace(i, cls, null);
    }

    public void PopScreensAndReplace(int i, Class<? extends ScreenLayout> cls, boolean z, boolean z2) throws XLEException {
        PopScreensAndReplace(i, cls, z, true, z2);
    }

    public void PopScreensAndReplace(int popCount, Class<? extends ScreenLayout> newScreenClass, boolean animate, boolean goingBack2, boolean isRestart, ActivityParameters activityParameters) throws XLEException {
        final ScreenLayout newScreen;
        final ActivityParameters screenParameters;
        Runnable popAndReplaceRunnable;
        XLEAssert.assertTrue("You must access navigation manager on UI thread.", Thread.currentThread() == ThreadManager.UIThread);
        if (cannotNavigateTripwire) {
            throw new UnsupportedOperationException("NavigationManager: attempted to execute a recursive navigation in the OnStop/OnStart method.  This is forbidden.");
        }
        if (newScreenClass == null || isRestart) {
            newScreen = null;
        } else {
            try {
                newScreen = newScreenClass.getConstructor(new Class[0]).newInstance(new Object[0]);
                animate = animate && newScreen.isAnimateOnPush();
            } catch (Exception e) {
                throw new XLEException(19, "FIXME: Failed to create a screen of type " + newScreenClass.getName(), e);
            }
        }
        if (getCurrentActivity() != null) {
            animate = animate && getCurrentActivity().isAnimateOnPop();
        }
        if (activityParameters == null) {
            screenParameters = new ActivityParameters();
        } else {
            screenParameters = activityParameters;
        }
        final NavigationCallbacks callbacks = navigationCallbacks;
        XLEAssert.assertNotNull(callbacks);
        if (isRestart) {
            popAndReplaceRunnable = new RestartRunner(screenParameters);
        } else {
            final int i = popCount;
            popAndReplaceRunnable = () -> {
                boolean unused = cannotNavigateTripwire = true;
                ScreenLayout from = getCurrentActivity();
                screenParameters.putFromScreen(from);
                screenParameters.putSourcePage(getCurrentActivityName());
                if (getCurrentActivity() != null) {
                    getCurrentActivity().onSetInactive();
                    getCurrentActivity().onPause();
                    getCurrentActivity().onStop();
                }
                for (int i1 = 0; i1 < i1; i1++) {
                    getCurrentActivity().onDestroy();
                    callbacks.removeContentViewXLE(navigationStack.pop());
                    navigationParameters.pop();
                }
                TextureManager.Instance().purgeResourceBitmapCache();
                ScreenLayout to = null;
                if (newScreen != null) {
                    if (getCurrentActivity() != null && !newScreen.isKeepPreviousScreen()) {
                        getCurrentActivity().onTombstone();
                    }
                    callbacks.addContentViewXLE(navigationStack.push(newScreen));
                    navigationParameters.push(screenParameters);
                    getCurrentActivity().onCreate();
                } else if (getCurrentActivity() != null) {
                    callbacks.addContentViewXLE(getCurrentActivity());
                    if (getCurrentActivity().getIsTombstoned()) {
                        getCurrentActivity().onRehydrate();
                    }
                }
                if (getCurrentActivity() != null) {
                    getCurrentActivity().onStart();
                    getCurrentActivity().onResume();
                    getCurrentActivity().onSetActive();
                    getCurrentActivity().onAnimateInStarted();
                    XboxTcuiSdk.getActivity().invalidateOptionsMenu();
                    to = getCurrentActivity();
                }
                if (navigationListener != null) {
                    navigationListener.onPageNavigated(from, to);
                }
                boolean unused2 = cannotNavigateTripwire = false;
            };
        }
        switch (this.animationState) {
            case NONE:
                Transition(goingBack2, popAndReplaceRunnable, animate);
                return;
            default:
                ReplaceOnAnimationEnd(goingBack2, popAndReplaceRunnable, animate);
                return;
        }
    }

    public void PopScreensAndReplace(int i, Class<? extends ScreenLayout> cls, boolean z, boolean z2, boolean z3) throws XLEException {
        PopScreensAndReplace(i, cls, z, z2, z3, null);
    }

    public void onApplicationPause() {
        for (int i = 0; i < this.navigationStack.size(); i++) {
            this.navigationStack.get(i).onApplicationPause();
        }
    }

    public void onApplicationResume() {
        for (int i = 0; i < this.navigationStack.size(); i++) {
            this.navigationStack.get(i).onApplicationResume();
        }
    }

    public void setAnimationBlocking(boolean z) {
        NavigationCallbacks navigationCallbacks2 = this.navigationCallbacks;
        if (navigationCallbacks2 != null) {
            navigationCallbacks2.setAnimationBlocking(z);
        }
    }

    private void Transition(boolean z, Runnable runnable, boolean z2) {
        this.transitionLambda = runnable;
        this.transitionAnimate = z2;
        this.goingBack = z;
        XLEAnimationPackage animateOut = getCurrentActivity() == null ? null : getCurrentActivity().getAnimateOut(z);
        this.currentAnimation = animateOut;
        startAnimation(animateOut, NavigationManagerAnimationState.ANIMATING_OUT);
    }

    private void ReplaceOnAnimationEnd(boolean z, Runnable runnable, boolean z2) {
        XLEAssert.assertTrue(this.animationState == NavigationManagerAnimationState.ANIMATING_OUT || this.animationState == NavigationManagerAnimationState.ANIMATING_IN);
        this.animationState = NavigationManagerAnimationState.ANIMATING_OUT;
        this.transitionLambda = runnable;
        this.transitionAnimate = z2;
        this.goingBack = z;
    }

    public void OnAnimationEnd() {
        switch (animationState) {
            case ANIMATING_IN:
                if (navigationCallbacks != null) {
                    navigationCallbacks.setAnimationBlocking(false);
                }
                animationState = NavigationManagerAnimationState.NONE;
                if (getCurrentActivity() != null) {
                    getCurrentActivity().onAnimateInCompleted();
                    return;
                }
                return;
            case ANIMATING_OUT:
                transitionLambda.run();
                XLEAnimationPackage anim = null;
                if (getCurrentActivity() != null) {
                    anim = getCurrentActivity().getAnimateIn(goingBack);
                }
                if (navigationCallbacks != null) {
                    navigationCallbacks.onBeforeNavigatingIn();
                }
                startAnimation(anim, NavigationManagerAnimationState.ANIMATING_IN);
                return;
            default:
                return;
        }
    }

    private void startAnimation(XLEAnimationPackage xLEAnimationPackage, NavigationManagerAnimationState navigationManagerAnimationState) {
        this.animationState = navigationManagerAnimationState;
        this.currentAnimation = xLEAnimationPackage;
        NavigationCallbacks navigationCallbacks2 = this.navigationCallbacks;
        if (navigationCallbacks2 != null) {
            navigationCallbacks2.setAnimationBlocking(true);
        }
        if (!this.transitionAnimate || xLEAnimationPackage == null) {
            this.callAfterAnimation.run();
            return;
        }
        xLEAnimationPackage.setOnAnimationEndRunnable(this.callAfterAnimation);
        xLEAnimationPackage.startAnimation();
    }

    public boolean onKey(View view, int i, KeyEvent keyEvent) {
        if (i != 4 || keyEvent.getAction() != 1) {
            return false;
        }
        if (!OnBackButtonPressed()) {
            return true;
        }
        removeNavigationCallbacks();
        removeNaviationListener();
        return false;
    }

    private enum NavigationManagerAnimationState {
        NONE,
        ANIMATING_IN,
        ANIMATING_OUT,
        COUNT
    }

    public interface NavigationCallbacks {
        void addContentViewXLE(ScreenLayout screenLayout);

        void onBeforeNavigatingIn();

        void removeContentViewXLE(ScreenLayout screenLayout);

        void setAnimationBlocking(boolean z);
    }

    public interface OnNavigatedListener {
        void onPageNavigated(ScreenLayout screenLayout, ScreenLayout screenLayout2);

        void onPageRestarted(ScreenLayout screenLayout);
    }

    private static class NavigationManagerHolder {
        public static final NavigationManager instance = new NavigationManager();

        private NavigationManagerHolder() {
        }
    }

    private class RestartRunner implements Runnable {
        private final ActivityParameters params;

        public RestartRunner(ActivityParameters activityParameters) {
            this.params = activityParameters;
        }

        public void run() {
            ScreenLayout currentActivity = getCurrentActivity();
            XLEAssert.assertNotNull(currentActivity);
            getCurrentActivity().onSetInactive();
            getCurrentActivity().onPause();
            getCurrentActivity().onStop();
            XLEAssert.assertTrue("navigationParameters cannot be empty!", true ^ navigationParameters.isEmpty());
            navigationParameters.pop();
            navigationParameters.push(this.params);
            getCurrentActivity().onStart();
            getCurrentActivity().onResume();
            getCurrentActivity().onSetActive();
            getCurrentActivity().onAnimateInStarted();
            XboxTcuiSdk.getActivity().invalidateOptionsMenu();
            if (navigationListener != null) {
                navigationListener.onPageRestarted(currentActivity);
            }
        }
    }
}
