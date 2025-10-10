package com.microsoft.xbox.toolkit;

import android.app.Dialog;


public interface IProjectSpecificDialogManager {
    void addManagedDialog(IXLEManagedDialog iXLEManagedDialog);

    void dismissBlocking();

    void dismissManagedDialog(IXLEManagedDialog iXLEManagedDialog);

    void dismissToast();

    void dismissTopNonFatalAlert();

    void forceDismissAlerts();

    void forceDismissAll();

    boolean getIsBlocking();

    Dialog getVisibleDialog();

    void onApplicationPause();

    void onApplicationResume();

    void onDialogStopped(IXLEManagedDialog iXLEManagedDialog);

    void setBlocking(boolean z, String str);

    void setCancelableBlocking(boolean z, String str, Runnable runnable);

    void setEnabled(boolean z);

    void showFatalAlertDialog(String str, String str2, String str3, Runnable runnable);

    void showManagedDialog(IXLEManagedDialog iXLEManagedDialog);

    void showNonFatalAlertDialog(String str, String str2, String str3, Runnable runnable);

    void showOkCancelDialog(String str, String str2, String str3, Runnable runnable, String str4, Runnable runnable2);

    void showToast(int i);
}
