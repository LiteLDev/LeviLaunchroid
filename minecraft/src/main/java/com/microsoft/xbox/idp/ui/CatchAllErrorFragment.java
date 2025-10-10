package com.microsoft.xbox.idp.ui;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.microsoft.xbox.idp.compat.BaseFragment;
import com.microsoft.xbox.idp.model.Const;
import com.mojang.minecraftpe.R;
import org.jetbrains.annotations.NotNull;


public class CatchAllErrorFragment extends BaseFragment {
    public static final String TAG = CatchAllErrorFragment.class.getSimpleName();

    public View onCreateView(@NotNull LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        return layoutInflater.inflate(R.layout.xbid_fragment_error_catch_all, viewGroup, false);
    }

    public void onViewCreated(@NotNull View view, Bundle bundle) {
        super.onViewCreated(view, bundle);
        UiUtil.ensureClickableSpanOnUnderlineSpan(view.findViewById(R.id.xbid_error_message), R.string.xbid_catchall_error_android, new ClickableSpan() {
            public void onClick(View view) {
                Log.d(CatchAllErrorFragment.TAG, "onClick");
                try {
                    startActivity(new Intent("android.intent.action.VIEW", Const.URL_XBOX_COM));
                } catch (ActivityNotFoundException e) {
                    Log.e(CatchAllErrorFragment.TAG, e.getMessage());
                }
            }
        });
    }
}
