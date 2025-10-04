package com.microsoft.xbox.idp.compat;

import android.app.Activity;
import androidx.fragment.app.Fragment;

/**
 * 07.01.2021
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */

public abstract class BaseFragment extends Fragment {
    public static final String ARG_ALT_BUTTON_TEXT = "ARG_ALT_BUTTON_TEXT";
    public static final String ARG_LOG_IN_BUTTON_TEXT = "ARG_LOG_IN_BUTTON_TEXT";
    public static final String ARG_USER_PTR = "ARG_USER_PTR";

    public CharSequence getActivityTitle() {
        Activity activity = getActivity();
        if (activity == null) {
            return null;
        }
        return activity.getTitle();
    }
}
