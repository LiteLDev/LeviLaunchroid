package com.microsoft.xbox.toolkit;

import android.app.Dialog;

/**
 * 07.01.2021
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */

public class DialogManager implements IProjectSpecificDialogManager {
    private static final DialogManager instance = new DialogManager();
    private IProjectSpecificDialogManager manager;

    private DialogManager() {
    }

    public static DialogManager getInstance() {
        return instance;
    }

    private void checkProvider() {
    }

    public IProjectSpecificDialogManager getManager() {
        return this.manager;
    }

    public void setManager(IProjectSpecificDialogManager iProjectSpecificDialogManager) {
        this.manager = iProjectSpecificDialogManager;
    }

    public Dialog getVisibleDialog() {
        checkProvider();
        IProjectSpecificDialogManager iProjectSpecificDialogManager = this.manager;
        if (iProjectSpecificDialogManager != null) {
            return iProjectSpecificDialogManager.getVisibleDialog();
        }
        return null;
    }

    public boolean getIsBlocking() {
        checkProvider();
        IProjectSpecificDialogManager iProjectSpecificDialogManager = this.manager;
        if (iProjectSpecificDialogManager != null) {
            return iProjectSpecificDialogManager.getIsBlocking();
        }
        return false;
    }

    public void setEnabled(boolean z) {
        checkProvider();
        IProjectSpecificDialogManager iProjectSpecificDialogManager = this.manager;
        if (iProjectSpecificDialogManager != null) {
            iProjectSpecificDialogManager.setEnabled(z);
        }
    }

    public void showManagedDialog(IXLEManagedDialog iXLEManagedDialog) {
        checkProvider();
        IProjectSpecificDialogManager iProjectSpecificDialogManager = this.manager;
        if (iProjectSpecificDialogManager != null) {
            iProjectSpecificDialogManager.showManagedDialog(iXLEManagedDialog);
        }
    }

    public void dismissManagedDialog(IXLEManagedDialog iXLEManagedDialog) {
        checkProvider();
        IProjectSpecificDialogManager iProjectSpecificDialogManager = this.manager;
        if (iProjectSpecificDialogManager != null) {
            iProjectSpecificDialogManager.dismissManagedDialog(iXLEManagedDialog);
        }
    }

    public void onDialogStopped(IXLEManagedDialog iXLEManagedDialog) {
        checkProvider();
        IProjectSpecificDialogManager iProjectSpecificDialogManager = this.manager;
        if (iProjectSpecificDialogManager != null) {
            iProjectSpecificDialogManager.onDialogStopped(iXLEManagedDialog);
        }
    }

    public void showFatalAlertDialog(String str, String str2, String str3, Runnable runnable) {
        checkProvider();
        IProjectSpecificDialogManager iProjectSpecificDialogManager = this.manager;
        if (iProjectSpecificDialogManager != null) {
            iProjectSpecificDialogManager.showFatalAlertDialog(str, str2, str3, runnable);
        }
    }

    public void showNonFatalAlertDialog(String str, String str2, String str3, Runnable runnable) {
        checkProvider();
        IProjectSpecificDialogManager iProjectSpecificDialogManager = this.manager;
        if (iProjectSpecificDialogManager != null) {
            iProjectSpecificDialogManager.showNonFatalAlertDialog(str, str2, str3, runnable);
        }
    }

    public void showOkCancelDialog(String str, String str2, String str3, Runnable runnable, String str4, Runnable runnable2) {
        checkProvider();
        IProjectSpecificDialogManager iProjectSpecificDialogManager = this.manager;
        if (iProjectSpecificDialogManager != null) {
            iProjectSpecificDialogManager.showOkCancelDialog(str, str2, str3, runnable, str4, runnable2);
        }
    }

    public void showToast(int i) {
        checkProvider();
        IProjectSpecificDialogManager iProjectSpecificDialogManager = this.manager;
        if (iProjectSpecificDialogManager != null) {
            iProjectSpecificDialogManager.showToast(i);
        }
    }

    public void setBlocking(boolean z, String str) {
        checkProvider();
        IProjectSpecificDialogManager iProjectSpecificDialogManager = this.manager;
        if (iProjectSpecificDialogManager != null) {
            iProjectSpecificDialogManager.setBlocking(z, str);
        }
    }

    public void setCancelableBlocking(boolean z, String str, Runnable runnable) {
        checkProvider();
        IProjectSpecificDialogManager iProjectSpecificDialogManager = this.manager;
        if (iProjectSpecificDialogManager != null) {
            iProjectSpecificDialogManager.setCancelableBlocking(z, str, runnable);
        }
    }

    public void forceDismissAll() {
        checkProvider();
        IProjectSpecificDialogManager iProjectSpecificDialogManager = this.manager;
        if (iProjectSpecificDialogManager != null) {
            iProjectSpecificDialogManager.forceDismissAll();
        }
    }

    public void dismissToast() {
        checkProvider();
        IProjectSpecificDialogManager iProjectSpecificDialogManager = this.manager;
        if (iProjectSpecificDialogManager != null) {
            iProjectSpecificDialogManager.dismissToast();
        }
    }

    public void forceDismissAlerts() {
        checkProvider();
        IProjectSpecificDialogManager iProjectSpecificDialogManager = this.manager;
        if (iProjectSpecificDialogManager != null) {
            iProjectSpecificDialogManager.forceDismissAlerts();
        }
    }

    public void dismissTopNonFatalAlert() {
        checkProvider();
        IProjectSpecificDialogManager iProjectSpecificDialogManager = this.manager;
        if (iProjectSpecificDialogManager != null) {
            iProjectSpecificDialogManager.dismissTopNonFatalAlert();
        }
    }

    public void dismissBlocking() {
        checkProvider();
        IProjectSpecificDialogManager iProjectSpecificDialogManager = this.manager;
        if (iProjectSpecificDialogManager != null) {
            iProjectSpecificDialogManager.dismissBlocking();
        }
    }

    public void addManagedDialog(IXLEManagedDialog iXLEManagedDialog) {
        checkProvider();
        IProjectSpecificDialogManager iProjectSpecificDialogManager = this.manager;
        if (iProjectSpecificDialogManager != null) {
            iProjectSpecificDialogManager.addManagedDialog(iXLEManagedDialog);
        }
    }

    public void onApplicationPause() {
        IProjectSpecificDialogManager iProjectSpecificDialogManager = this.manager;
        if (iProjectSpecificDialogManager != null) {
            iProjectSpecificDialogManager.onApplicationPause();
        }
    }

    public void onApplicationResume() {
        IProjectSpecificDialogManager iProjectSpecificDialogManager = this.manager;
        if (iProjectSpecificDialogManager != null) {
            iProjectSpecificDialogManager.onApplicationResume();
        }
    }
}
