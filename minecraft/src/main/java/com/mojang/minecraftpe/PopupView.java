package com.mojang.minecraftpe;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.IBinder;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import androidx.annotation.NonNull;

/**
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */
public class PopupView {
    private final Context mContext;
    private final WindowManager mWindowManager;
    private View mContentView;
    private int mHeight;
    private int mOriginX;
    private int mOriginY;
    private View mParentView;
    private View mPopupView;
    private int mWidth;

    public PopupView(@NonNull Context context) {
        this.mContext = context;
        this.mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    }

    private int computeFlags(int curFlags) {
        return curFlags | 32;
    }

    private void preparePopup(WindowManager.LayoutParams p) {
    }

    public void setContentView(View contentView) {
        mContentView = contentView;
    }

    public void setParentView(View parentView) {
        mParentView = parentView;
    }

    public void setWidth(int width) {
        mWidth = width;
    }

    public void setHeight(int height) {
        mHeight = height;
    }

    public void setRect(int minX, int maxX, int minY, int maxY) {
        mWidth = maxX - minX;
        mHeight = maxY - minY;
        mOriginX = minX;
        mOriginY = minY;
    }

    public boolean getVisible() {
        View view = mPopupView;
        return view != null && view.getParent() != null;
    }

    public void setVisible(boolean visible) {
        if (visible != getVisible()) {
            if (visible) {
                addPopupView();
            } else {
                removePopupView();
            }
        }
    }

    public void dismiss() {
        if (mPopupView != null) {
            removePopupView();
            View view = mPopupView;
            View view2 = mContentView;
            if (view != view2 && (view instanceof ViewGroup)) {
                ((ViewGroup) view).removeView(view2);
            }
            mPopupView = null;
        }
    }

    public void update() {
        if (getVisible()) {
            WindowManager.LayoutParams layoutParams = (WindowManager.LayoutParams) mPopupView.getLayoutParams();
            int computeFlags = computeFlags(layoutParams.flags);
            if (computeFlags != layoutParams.flags) {
                layoutParams.flags = computeFlags;
            }
            setLayoutRect(layoutParams);
            this.mWindowManager.updateViewLayout(mPopupView, layoutParams);
        }
    }

    private void addPopupView() {
        mPopupView = mContentView;
        WindowManager.LayoutParams createPopupLayout = createPopupLayout(mParentView.getWindowToken());
        setLayoutRect(createPopupLayout);
        invokePopup(createPopupLayout);
    }

    private void removePopupView() {
        try {
            mParentView.requestFocus();
            mWindowManager.removeView(mPopupView);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @NonNull
    private WindowManager.LayoutParams createPopupLayout(IBinder token) {
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
        layoutParams.format = -3;
        layoutParams.flags = computeFlags(layoutParams.flags);
        layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_PANEL;
        layoutParams.token = token;
        layoutParams.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_STATE_UNCHANGED;
        layoutParams.setTitle("PopupWindow:" + Integer.toHexString(hashCode()));
        layoutParams.windowAnimations = -1;
        return layoutParams;
    }

    private void invokePopup(@NonNull WindowManager.LayoutParams p) {
        p.packageName = mContext.getPackageName();
        mWindowManager.addView(mPopupView, p);
        mParentView.requestFocus();
    }

    @SuppressLint("WrongConstant")
    private void setLayoutRect(@NonNull WindowManager.LayoutParams p) {
        p.width = mWidth;
        p.height = mHeight;
        p.x = mOriginX;
        p.y = mOriginY;
        p.gravity = 51;
    }
}
