package com.microsoft.xbox.toolkit;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;

/**
 * 07.01.2021
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */

public class XLEManagedAlertDialog extends AlertDialog implements IXLEManagedDialog {
    private IXLEManagedDialog.DialogType dialogType = IXLEManagedDialog.DialogType.NORMAL;

    protected XLEManagedAlertDialog(Context context) {
        super(context);
    }

    public Dialog getDialog() {
        return this;
    }

    public IXLEManagedDialog.DialogType getDialogType() {
        return this.dialogType;
    }

    public void setDialogType(IXLEManagedDialog.DialogType dialogType2) {
        this.dialogType = dialogType2;
    }

    public void safeDismiss() {
        DialogManager.getInstance().dismissManagedDialog(this);
    }

    public void quickDismiss() {
        super.dismiss();
    }

    public void onStop() {
        super.onStop();
        DialogManager.getInstance().onDialogStopped(this);
    }
}
