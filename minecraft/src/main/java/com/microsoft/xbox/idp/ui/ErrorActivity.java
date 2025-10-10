package com.microsoft.xbox.idp.ui;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import com.microsoft.xbox.idp.compat.BaseActivity;
import com.microsoft.xbox.idp.compat.BaseFragment;
import com.microsoft.xbox.idp.interop.Interop;
import com.microsoft.xbox.idp.model.Const;
import com.microsoft.xbox.telemetry.helpers.UTCError;
import com.microsoft.xbox.telemetry.helpers.UTCPageView;
import com.mojang.minecraftpe.R;
import org.jetbrains.annotations.Nullable;



public class ErrorActivity extends BaseActivity implements HeaderFragment.Callbacks, ErrorButtonsFragment.Callbacks {
    public static final String ARG_ERROR_TYPE = "ARG_ERROR_TYPE";
    public static final String ARG_GAMER_TAG = "ARG_GAMER_TAG";
    public static final int RESULT_TRY_AGAIN = 1;
    private static final String TAG = ErrorActivity.class.getSimpleName();
    private int activityResult = 0;

    public void onCreate(Bundle bundle) {
        Log.d(TAG, "onCreate");
        super.onCreate(bundle);
        setContentView(R.layout.xbid_activity_error);
        Intent intent = getIntent();
        UiUtil.ensureHeaderFragment(this, R.id.xbid_header_fragment, intent.getExtras());
        if (intent.hasExtra(ARG_ERROR_TYPE)) {
            ErrorScreen fromId = ErrorScreen.fromId(intent.getIntExtra(ARG_ERROR_TYPE, -1));
            if (fromId != null) {
                UiUtil.ensureErrorFragment(this, fromId);
                UiUtil.ensureErrorButtonsFragment(this, fromId);
                UTCError.trackPageView(fromId, getTitle());
                return;
            }
            Log.e(TAG, "Incorrect error type was provided");
            return;
        }
        Log.e(TAG, "No error type was provided");
    }

    public void onClickCloseHeader() {
        Log.d(TAG, "onClickCloseHeader");
        UTCError.trackClose(ErrorScreen.fromId(getIntent().getIntExtra(ARG_ERROR_TYPE, -1)), getTitle());
        finish();
    }

    public void onClickedLeftButton() {
        Log.d(TAG, "onClickedLeftButton");
        ErrorScreen fromId = ErrorScreen.fromId(getIntent().getIntExtra(ARG_ERROR_TYPE, -1));
        if (fromId == ErrorScreen.BAN) {
            UTCError.trackGoToEnforcement(fromId, getTitle());
            try {
                startActivity(new Intent("android.intent.action.VIEW", Const.URL_ENFORCEMENT_XBOX_COM));
            } catch (ActivityNotFoundException e) {
                Log.e(TAG, e.getMessage());
            }
        } else {
            UTCError.trackTryAgain(fromId, getTitle());
            this.activityResult = 1;
            setResult(1);
            finish();
        }
    }

    public void onClickedRightButton() {
        Log.d(TAG, "onClickedRightButton");
        UTCError.trackRightButton(ErrorScreen.fromId(getIntent().getIntExtra(ARG_ERROR_TYPE, -1)), getTitle());
        finish();
    }

    public void finish() {
        UTCPageView.removePage();
        super.finish();
    }

    public enum ErrorScreen {
        BAN(Interop.ErrorType.BAN, BanErrorFragment.class, R.string.xbid_more_info),
        CREATION(Interop.ErrorType.CREATION, CreationErrorFragment.class, R.string.xbid_try_again),
        OFFLINE(Interop.ErrorType.OFFLINE, OfflineErrorFragment.class, R.string.xbid_try_again),
        CATCHALL(Interop.ErrorType.CATCHALL, CatchAllErrorFragment.class, R.string.xbid_try_again);

        public final Class<? extends BaseFragment> errorFragmentClass;
        public final int leftButtonTextId;
        public final Interop.ErrorType type;

        ErrorScreen(Interop.ErrorType errorType, Class<? extends BaseFragment> cls, int i) {
            this.type = errorType;
            this.errorFragmentClass = cls;
            this.leftButtonTextId = i;
        }

        public static @Nullable ErrorScreen fromId(int i) {
            for (ErrorScreen errorScreen : values()) {
                if (errorScreen.type.getId() == i) {
                    return errorScreen;
                }
            }
            return null;
        }
    }
}
