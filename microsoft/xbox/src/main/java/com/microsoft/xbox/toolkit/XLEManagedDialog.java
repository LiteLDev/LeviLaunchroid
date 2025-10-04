package com.microsoft.xbox.toolkit;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.view.View;

import com.microsoft.xbox.toolkit.anim.MAAS;
import com.microsoft.xbox.toolkit.anim.XLEAnimation;
import com.microsoft.xbox.toolkit.anim.XLEAnimationPackage;
import com.microsoft.xbox.toolkit.system.SystemUtil;
import com.microsoft.xbox.toolkit.ui.NavigationManager;
import com.microsoft.xbox.xle.anim.XLEMAASAnimationPackageNavigationManager;

/**
 * 07.01.2021
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */

public class XLEManagedDialog extends Dialog implements IXLEManagedDialog {
    protected static final String BODY_ANIMATION_NAME = "Dialog";
    final Runnable callAfterAnimationIn = new Runnable() {
        public void run() {
            XLEManagedDialog.this.OnAnimationInEnd();
        }
    };
    protected String bodyAnimationName = BODY_ANIMATION_NAME;
    protected View dialogBody = null;
    protected Runnable onAnimateOutCompletedRunable = null;
    final Runnable callAfterAnimationOut = new Runnable() {
        public void run() {
            XLEManagedDialog.this.OnAnimationOutEnd();
        }
    };
    private IXLEManagedDialog.DialogType dialogType = IXLEManagedDialog.DialogType.NORMAL;

    protected XLEManagedDialog(Context context, boolean z, DialogInterface.OnCancelListener onCancelListener) {
        super(context, z, onCancelListener);
    }

    public XLEManagedDialog(Context context, int i) {
        super(context, i);
    }

    public XLEManagedDialog(Context context) {
        super(context);
    }

    protected static boolean isKindle() {
        return SystemUtil.isKindle();
    }

    public Dialog getDialog() {
        return this;
    }

    public String getBodyAnimationName() {
        return this.bodyAnimationName;
    }

    public void setBodyAnimationName(String str) {
        this.bodyAnimationName = str;
    }

    public IXLEManagedDialog.DialogType getDialogType() {
        return this.dialogType;
    }

    public void setDialogType(IXLEManagedDialog.DialogType dialogType2) {
        this.dialogType = dialogType2;
    }

    public void makeFullScreen() {
        getWindow().setLayout(-1, -2);
    }

    public void safeDismiss() {
        DialogManager.getInstance().dismissManagedDialog(this);
    }

    public void quickDismiss() {
        super.dismiss();
    }

    public void onWindowFocusChanged(boolean z) {
        if (!z) {
            safeDismiss();
        }
    }

    public XLEAnimationPackage getAnimateOut() {
        XLEAnimation bodyAnimation = getBodyAnimation(MAAS.MAASAnimationType.ANIMATE_OUT, true);
        if (bodyAnimation == null) {
            return null;
        }
        XLEAnimationPackage xLEAnimationPackage = new XLEAnimationPackage();
        xLEAnimationPackage.add(bodyAnimation);
        return xLEAnimationPackage;
    }

    public XLEAnimationPackage getAnimateIn() {
        XLEAnimation bodyAnimation = getBodyAnimation(MAAS.MAASAnimationType.ANIMATE_IN, false);
        if (bodyAnimation == null) {
            return null;
        }
        XLEAnimationPackage xLEAnimationPackage = new XLEAnimationPackage();
        xLEAnimationPackage.add(bodyAnimation);
        return xLEAnimationPackage;
    }

    public void OnAnimationInEnd() {
        NavigationManager.getInstance().setAnimationBlocking(false);
    }

    public void OnAnimationOutEnd() {
        NavigationManager.getInstance().setAnimationBlocking(false);
        super.dismiss();
        Runnable runnable = this.onAnimateOutCompletedRunable;
        if (runnable != null) {
            try {
                runnable.run();
            } catch (Exception unused) {
            }
        }
    }

    public void setAnimateOutRunnable(Runnable runnable) {
        this.onAnimateOutCompletedRunable = runnable;
    }

    public View getDialogBody() {
        return this.dialogBody;
    }

    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        XLEAnimationPackage animateIn = getAnimateIn();
        if (getDialogBody() != null && animateIn != null) {
            NavigationManager.getInstance().setAnimationBlocking(true);
            animateIn.setOnAnimationEndRunnable(this.callAfterAnimationIn);
            animateIn.startAnimation();
        }
    }

    public void dismiss() {
        if (!isShowing()) {
            super.dismiss();
            return;
        }
        XLEAnimationPackage animateOut = getAnimateOut();
        if (getDialogBody() == null || animateOut == null) {
            Runnable runnable = this.onAnimateOutCompletedRunable;
            if (runnable != null) {
                runnable.run();
            }
            super.dismiss();
            return;
        }
        NavigationManager.getInstance().setAnimationBlocking(true);
        animateOut.setOnAnimationEndRunnable(this.callAfterAnimationOut);
        animateOut.startAnimation();
    }

    public XLEAnimation getBodyAnimation(MAAS.MAASAnimationType mAASAnimationType, boolean z) {
        if (getDialogBody() != null) {
            return ((XLEMAASAnimationPackageNavigationManager) MAAS.getInstance().getAnimation(this.bodyAnimationName)).compile(mAASAnimationType, z, getDialogBody());
        }
        return null;
    }

    public void forceKindleRespectDimOptions() {
        new Handler().postDelayed(new Runnable() {
            public void run() {
                XLEManagedDialog.this.getWindow().addFlags(2);
            }
        }, 100);
    }
}
