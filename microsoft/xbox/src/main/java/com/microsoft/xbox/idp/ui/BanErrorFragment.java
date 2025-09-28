package com.microsoft.xbox.idp.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.microsoft.xbox.idp.compat.BaseFragment;

import com.microsoft.xboxtcui.R;
import org.jetbrains.annotations.NotNull;

/**
 * 07.01.2021
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */

public class BanErrorFragment extends BaseFragment {
    public static final String ARG_GAMER_TAG = "ARG_GAMER_TAG";
    private static final String TAG = BanErrorFragment.class.getSimpleName();

    public View onCreateView(@NotNull LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        return layoutInflater.inflate(R.layout.xbid_fragment_error_ban, viewGroup, false);
    }

    public void onViewCreated(@NotNull View view, Bundle bundle) {
        super.onViewCreated(view, bundle);
        Bundle arguments = getArguments();
        if (arguments == null) {
            Log.e(TAG, "No arguments provided");
        } else if (arguments.containsKey("ARG_GAMER_TAG")) {
            arguments.getString("ARG_GAMER_TAG");
        } else {
            Log.e(TAG, "No ARG_GAMER_TAG provided");
        }
    }
}
