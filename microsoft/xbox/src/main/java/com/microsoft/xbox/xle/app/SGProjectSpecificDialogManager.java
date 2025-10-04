package com.microsoft.xbox.xle.app;

import com.microsoft.xbox.toolkit.DialogManager;
import com.microsoft.xbox.toolkit.DialogManagerBase;
import com.microsoft.xbox.toolkit.IProjectSpecificDialogManager;
import com.microsoft.xbox.xle.app.dialog.ChangeFriendshipDialog;
import com.microsoft.xbox.xle.viewmodel.ChangeFriendshipDialogViewModel;
import com.microsoft.xbox.xle.viewmodel.ViewModelBase;
import com.microsoft.xboxtcui.XboxTcuiSdk;

/**
 * 07.01.2021
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */

public class SGProjectSpecificDialogManager extends DialogManagerBase {
    private static final IProjectSpecificDialogManager instance = new SGProjectSpecificDialogManager();
    private ChangeFriendshipDialog changeFriendshipDialog;

    private SGProjectSpecificDialogManager() {
    }

    public static IProjectSpecificDialogManager getInstance() {
        return instance;
    }

    public static SGProjectSpecificDialogManager getProjectSpecificInstance() {
        return (SGProjectSpecificDialogManager) DialogManager.getInstance().getManager();
    }

    public void onApplicationResume() {
    }

    public boolean shouldDismissAllBeforeOpeningADialog() {
        return false;
    }

    public void forceDismissAll() {
        super.forceDismissAll();
        dismissChangeFriendshipDialog();
    }

    public void onApplicationPause() {
        forceDismissAll();
    }

    public void showChangeFriendshipDialog(ChangeFriendshipDialogViewModel changeFriendshipDialogViewModel, ViewModelBase viewModelBase) {
        ChangeFriendshipDialog changeFriendshipDialog2 = this.changeFriendshipDialog;
        if (changeFriendshipDialog2 != null) {
            changeFriendshipDialog2.setVm(changeFriendshipDialogViewModel);
            this.changeFriendshipDialog.getDialog().show();
            return;
        }
        ChangeFriendshipDialog changeFriendshipDialog3 = new ChangeFriendshipDialog(XboxTcuiSdk.getActivity(), changeFriendshipDialogViewModel, viewModelBase);
        this.changeFriendshipDialog = changeFriendshipDialog3;
        addManagedDialog(changeFriendshipDialog3);
    }

    public void notifyChangeFriendshipDialogUpdateView() {
        ChangeFriendshipDialog changeFriendshipDialog2 = this.changeFriendshipDialog;
        if (changeFriendshipDialog2 != null) {
            changeFriendshipDialog2.updateView();
        }
    }

    public void notifyChangeFriendshipDialogAsyncTaskCompleted() {
        ChangeFriendshipDialog changeFriendshipDialog2 = this.changeFriendshipDialog;
        if (changeFriendshipDialog2 != null) {
            changeFriendshipDialog2.reportAsyncTaskCompleted();
        }
    }

    public void notifyChangeFriendshipDialogAsyncTaskFailed(String str) {
        ChangeFriendshipDialog changeFriendshipDialog2 = this.changeFriendshipDialog;
        if (changeFriendshipDialog2 != null) {
            changeFriendshipDialog2.reportAsyncTaskFailed(str);
        }
    }

    public void dismissChangeFriendshipDialog() {
        ChangeFriendshipDialog changeFriendshipDialog2 = this.changeFriendshipDialog;
        if (changeFriendshipDialog2 != null) {
            dismissManagedDialog(changeFriendshipDialog2);
            this.changeFriendshipDialog = null;
        }
    }
}
