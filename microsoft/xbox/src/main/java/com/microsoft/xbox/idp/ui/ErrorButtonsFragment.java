package com.microsoft.xbox.idp.ui;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.widget.AppCompatButton;

import com.microsoft.xboxtcui.R;
import com.microsoft.xbox.idp.compat.BaseFragment;

import org.jetbrains.annotations.NotNull;

/**
 * 07.01.2021
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */

public class ErrorButtonsFragment extends BaseFragment implements View.OnClickListener {
    public static final String ARG_LEFT_ERROR_BUTTON_STRING_ID = "ARG_LEFT_ERROR_BUTTON_STRING_ID";
    private static final Callbacks NO_OP_CALLBACKS = new Callbacks() {
        public void onClickedLeftButton() {
        }

        public void onClickedRightButton() {
        }
    };
    private Callbacks callbacks = NO_OP_CALLBACKS;

    @Override
    public void onAttach(@NotNull Activity activity) {
        super.onAttach(activity);
        this.callbacks = (Callbacks) activity;
    }

    public void onDetach() {
        super.onDetach();
        this.callbacks = NO_OP_CALLBACKS;
    }

    public View onCreateView(@NotNull LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        return layoutInflater.inflate(R.layout.xbid_fragment_error_buttons, viewGroup, false);
    }

    public void onViewCreated(@NotNull View view, Bundle bundle) {
        super.onViewCreated(view, bundle);
        AppCompatButton button = view.findViewById(R.id.xbid_error_left_button);
        button.setOnClickListener(this);
        view.findViewById(R.id.xbid_error_right_button).setOnClickListener(this);
        Bundle arguments = getArguments();
        if (arguments != null && arguments.containsKey(ARG_LEFT_ERROR_BUTTON_STRING_ID)) {
            button.setText(arguments.getInt(ARG_LEFT_ERROR_BUTTON_STRING_ID));
        }
    }

    public void onClick(@NotNull View view) {
        int id = view.getId();
        if (id == R.id.xbid_error_left_button) {
            callbacks.onClickedLeftButton();
        } else if (id == R.id.xbid_error_right_button) {
            callbacks.onClickedRightButton();
        }
    }

    public interface Callbacks {
        void onClickedLeftButton();

        void onClickedRightButton();
    }
}
