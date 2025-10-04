package com.microsoft.xbox.toolkit;

import android.app.Dialog;

/**
 * 07.01.2021
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */

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
