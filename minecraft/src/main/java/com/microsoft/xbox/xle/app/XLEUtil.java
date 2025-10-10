package com.microsoft.xbox.xle.app;

import android.annotation.SuppressLint;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import com.microsoft.xbox.toolkit.DialogManager;
import com.microsoft.xbox.toolkit.ThreadManager;
import com.microsoft.xbox.toolkit.XLEAssert;
import com.microsoft.xboxtcui.XboxTcuiSdk;

import java.util.Date;


public class XLEUtil {
    @SuppressLint("WrongConstant")
    public static void updateAndShowTextViewUnlessEmpty(TextView textView, CharSequence charSequence) {
        if (textView == null) {
            return;
        }
        if (charSequence == null || charSequence.length() <= 0) {
            textView.setVisibility(8);
            return;
        }
        textView.setText(charSequence);
        textView.setVisibility(0);
    }

    public static void updateTextAndVisibilityIfNotNull(TextView textView, CharSequence charSequence, int i) {
        if (textView != null) {
            textView.setText(charSequence);
            textView.setVisibility(i);
        }
    }

    public static void updateVisibilityIfNotNull(View view, int i) {
        if (view != null) {
            view.setVisibility(i);
        }
    }

    public static void showKeyboard(final View view, int i) {
        ThreadManager.UIThreadPostDelayed(() -> ((InputMethodManager) XboxTcuiSdk.getSystemService("input_method")).showSoftInput(view, 1), i);
    }

    public static <T> boolean isNullOrEmpty(Iterable<T> iterable) {
        return iterable == null || !iterable.iterator().hasNext();
    }

    public static <T> boolean isNullOrEmpty(T[] tArr) {
        return tArr == null || tArr.length == 0;
    }

    public static boolean shouldRefresh(Date date, long j) {
        return date == null || new Date().getTime() - date.getTime() > j;
    }

    public static void showOkCancelDialog(String str, String str2, String str3, Runnable runnable, String str4, Runnable runnable2) {
        XLEAssert.assertNotNull("You must supply cancel text if this is not a must-act dialog.", str4);
        DialogManager.getInstance().showOkCancelDialog(str, str2, str3, runnable, str4, runnable2);
    }
}
