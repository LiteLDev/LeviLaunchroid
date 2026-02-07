package org.levimc.launcher.util;

import android.content.Context;
import android.widget.Toast;

public class UIHelper {

    public static void showToast(Context context, String message) {
        runOnUiThread(context, () ->
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        );
    }

    public static void showToast(Context context, int resId, Object... formatArgs) {
        runOnUiThread(context, () -> {
            String message = context.getString(resId, formatArgs);
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        });
    }

    public static void showLongToast(Context context, String message) {
        runOnUiThread(context, () ->
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        );
    }

    private static void runOnUiThread(Context context, Runnable action) {
        if (context instanceof android.app.Activity) {
            ((android.app.Activity) context).runOnUiThread(action);
        } else {
            new android.os.Handler(
                    android.os.Looper.getMainLooper()
            ).post(action);
        }
    }

    public static void hideKeyboard(android.app.Activity activity) {
        android.view.View view = activity.getCurrentFocus();
        if (view != null) {
            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }
}