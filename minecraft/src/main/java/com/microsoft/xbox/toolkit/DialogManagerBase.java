package com.microsoft.xbox.toolkit;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.widget.Toast;

import com.microsoft.xbox.toolkit.ui.BlockingScreen;
import com.microsoft.xbox.toolkit.ui.CancellableBlockingScreen;
import com.microsoft.xboxtcui.XboxTcuiSdk;

import org.jetbrains.annotations.NotNull;

import java.util.Stack;


public abstract class DialogManagerBase implements IProjectSpecificDialogManager {
    private final Stack<IXLEManagedDialog> dialogStack = new Stack<>();
    public CancellableBlockingScreen cancelableBlockingDialog;
    private BlockingScreen blockingSpinner;
    private boolean isEnabled;
    private Toast visibleToast;

    protected DialogManagerBase() {
        XLEAssert.assertTrue(Thread.currentThread() == ThreadManager.UIThread);
    }

    public Dialog getVisibleDialog() {
        if (!dialogStack.isEmpty()) {
            return dialogStack.peek().getDialog();
        }
        return null;
    }

    public boolean getIsBlocking() {
        return (blockingSpinner != null && blockingSpinner.isShowing()) || (cancelableBlockingDialog != null && cancelableBlockingDialog.isShowing());
    }

    public void setEnabled(boolean value) {
        if (isEnabled != value) {
            isEnabled = value;
        }
    }

    public void showManagedDialog(IXLEManagedDialog dialog) {
        if (shouldDismissAllBeforeOpeningADialog()) {
            forceDismissAll();
        }
        if (isEnabled && XboxTcuiSdk.getActivity() != null && !XboxTcuiSdk.getActivity().isFinishing()) {
            dialogStack.push(dialog);
            try {
                dialog.getDialog().show();
            } catch (RuntimeException e) {
                String msg = e.getMessage();
                if (msg == null || !msg.contains("Adding window failed")) {
                    throw e;
                }
            }
        }
    }

    public void addManagedDialog(IXLEManagedDialog dialog) {
        if (isEnabled) {
            dialogStack.push(dialog);
            dialog.getDialog().show();
        }
    }

    public void dismissManagedDialog(IXLEManagedDialog dialog) {
        if (isEnabled) {
            dialogStack.remove(dialog);
            dialog.getDialog().dismiss();
        }
    }

    public void onDialogStopped(IXLEManagedDialog dialog) {
        dialogStack.remove(dialog);
    }

    public void showFatalAlertDialog(String title, String promptText, String okText, Runnable okHandler) {
        forceDismissAll();
        if (isEnabled) {
            XLEManagedAlertDialog dialog = buildDialog(title, promptText, okText, okHandler, null, null);
            dialog.setDialogType(IXLEManagedDialog.DialogType.FATAL);
            dialogStack.push(dialog);
            dialog.show();
        }
    }

    public void showNonFatalAlertDialog(String title, String promptText, String okText, Runnable okHandler) {
        if (isEnabled) {
            XLEManagedAlertDialog dialog = buildDialog(title, promptText, okText, okHandler, null, null);
            dialog.setDialogType(IXLEManagedDialog.DialogType.NON_FATAL);
            dialogStack.push(dialog);
            dialog.show();
        }
    }

    public void showOkCancelDialog(String title, String promptText, String okText, Runnable okHandler, String cancelText, Runnable cancelHandler) {
        XLEAssert.assertNotNull("You must supply cancel text if this is not a must-act dialog.", cancelText);
        if (dialogStack.size() <= 0 && isEnabled && XboxTcuiSdk.getActivity() != null && !XboxTcuiSdk.getActivity().isFinishing()) {
            XLEManagedAlertDialog dialog = buildDialog(title, promptText, okText, okHandler, cancelText, cancelHandler);
            dialog.setDialogType(IXLEManagedDialog.DialogType.NORMAL);
            dialogStack.push(dialog);
            dialog.show();
        }
    }

    @SuppressLint("WrongConstant")
    public void showToast(int contentResId) {
        dismissToast();
        if (isEnabled) {
            visibleToast = Toast.makeText(XboxTcuiSdk.getActivity(), contentResId, 1);
            visibleToast.show();
        }
    }

    public void setBlocking(boolean visible, String statusText) {
        XLEAssert.assertTrue(Thread.currentThread() == ThreadManager.UIThread);
        if (!isEnabled) {
            return;
        }
        if (visible) {
            if (blockingSpinner == null) {
                blockingSpinner = new BlockingScreen(XboxTcuiSdk.getActivity());
            }
            blockingSpinner.show(XboxTcuiSdk.getActivity(), statusText);
        } else if (blockingSpinner != null) {
            blockingSpinner.dismiss();
            blockingSpinner = null;
        }
    }

    public void setCancelableBlocking(boolean visible, String statusText, final Runnable cancelRunnable) {
        XLEAssert.assertTrue(Thread.currentThread() == ThreadManager.UIThread);
        if (!isEnabled) {
            return;
        }
        if (visible) {
            if (cancelableBlockingDialog == null) {
                cancelableBlockingDialog = new CancellableBlockingScreen(XboxTcuiSdk.getActivity());
                cancelableBlockingDialog.setCancelButtonAction(v -> {
                    cancelableBlockingDialog.dismiss();
                    CancellableBlockingScreen unused = cancelableBlockingDialog = null;
                    cancelRunnable.run();
                });
            }
            cancelableBlockingDialog.show(XboxTcuiSdk.getActivity(), statusText);
        } else if (cancelableBlockingDialog != null) {
            cancelableBlockingDialog.dismiss();
            cancelableBlockingDialog = null;
        }
    }

    public void forceDismissAll() {
        dismissToast();
        forceDismissAlerts();
        dismissBlocking();
    }

    public void dismissToast() {
        if (visibleToast != null) {
            visibleToast.cancel();
            visibleToast = null;
        }
    }

    public void forceDismissAlerts() {
        while (dialogStack.size() > 0) {
            dialogStack.pop().quickDismiss();
        }
    }

    public void dismissTopNonFatalAlert() {
        if (dialogStack.size() > 0 && dialogStack.peek().getDialogType() != IXLEManagedDialog.DialogType.FATAL) {
            dialogStack.pop().getDialog().dismiss();
        }
    }

    public void dismissBlocking() {
        if (blockingSpinner != null) {
            blockingSpinner.dismiss();
            blockingSpinner = null;
        }
        if (cancelableBlockingDialog != null) {
            cancelableBlockingDialog.dismiss();
            cancelableBlockingDialog = null;
        }
    }

    public boolean shouldDismissAllBeforeOpeningADialog() {
        return true;
    }

    @NotNull
    private XLEManagedAlertDialog buildDialog(String title, String promptText, String okText, final Runnable okHandler, String cancelText, final Runnable cancelHandler) {
        final XLEManagedAlertDialog dialog = new XLEManagedAlertDialog(XboxTcuiSdk.getActivity());
        dialog.setTitle(title);
        dialog.setMessage(promptText);
        dialog.setButton(-1, okText, (arg0, arg1) -> ThreadManager.UIThreadPost(okHandler));
        final Runnable wrappedCancelHandler = () -> {
            dismissManagedDialog(dialog);
            if (cancelHandler != null) {
                cancelHandler.run();
            }
        };
        dialog.setButton(-2, cancelText, (arg0, arg1) -> ThreadManager.UIThreadPost(wrappedCancelHandler));
        if (cancelText == null || cancelText.length() == 0) {
            dialog.setCancelable(false);
        } else {
            dialog.setCancelable(true);
            dialog.setOnCancelListener(dialog1 -> ThreadManager.UIThreadPost(wrappedCancelHandler));
        }
        return dialog;
    }
}