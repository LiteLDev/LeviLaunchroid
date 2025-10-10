package com.microsoft.xbox.toolkit;

import android.app.Dialog;


public interface IXLEManagedDialog {

    Dialog getDialog();

    DialogType getDialogType();

    void setDialogType(DialogType dialogType);

    void quickDismiss();

    void safeDismiss();

    enum DialogType {
        FATAL,
        NON_FATAL,
        NORMAL
    }
}
