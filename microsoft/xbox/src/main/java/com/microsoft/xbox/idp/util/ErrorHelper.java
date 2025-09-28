package com.microsoft.xbox.idp.util;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import androidx.loader.app.LoaderManager;
import com.microsoft.xbox.idp.ui.ErrorActivity;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 07.01.2021
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */

public final class ErrorHelper implements Parcelable {
    public static final Parcelable.Creator<ErrorHelper> CREATOR = new Parcelable.Creator<ErrorHelper>() {
        @NotNull
        @Contract("_ -> new")
        public ErrorHelper createFromParcel(Parcel parcel) {
            return new ErrorHelper(parcel);
        }

        @NotNull
        @Contract(value = "_ -> new", pure = true)
        public ErrorHelper [] newArray(int i) {
            return new ErrorHelper[i];
        }
    };
    public static final String KEY_RESULT_KEY = "KEY_RESULT_KEY";
    public static final int LOADER_NONE = -1;
    public static final int RC_ERROR_SCREEN = 63;
    private static final String TAG = ErrorHelper.class.getSimpleName();
    public Bundle loaderArgs;
    public int loaderId;
    private ActivityContext activityContext;

    public ErrorHelper() {
        this.loaderId = -1;
        this.loaderArgs = null;
    }

    protected ErrorHelper(@NotNull Parcel parcel) {
        this.loaderId = parcel.readInt();
        this.loaderArgs = parcel.readBundle(getClass().getClassLoader());
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(@NotNull Parcel parcel, int i) {
        parcel.writeInt(this.loaderId);
        parcel.writeBundle(this.loaderArgs);
    }

    private boolean isConnected() {
        @SuppressLint("WrongConstant") NetworkInfo activeNetworkInfo = ((ConnectivityManager) this.activityContext.getActivity().getSystemService("connectivity")).getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public void setActivityContext(ActivityContext activityContext2) {
        this.activityContext = activityContext2;
    }

    public void startErrorActivity(@NotNull ErrorActivity.ErrorScreen errorScreen) {
        Intent intent = new Intent(this.activityContext.getActivity(), ErrorActivity.class);
        intent.putExtra(ErrorActivity.ARG_ERROR_TYPE, errorScreen.type.getId());
        this.activityContext.startActivityForResult(intent, 63);
    }

    public <D> boolean initLoader(int i, Bundle bundle) {
        return initLoader(i, bundle, true);
    }

    public <D> boolean initLoader(int i, Bundle bundle, boolean z) {
        boolean z2;
        Log.d(TAG, "initLoader");
        if (i != -1) {
            this.loaderId = i;
            this.loaderArgs = bundle;
            LoaderManager loaderManager = this.activityContext.getLoaderManager();
            LoaderInfo loaderInfo = this.activityContext.getLoaderInfo(this.loaderId);
            Bundle bundle2 = this.loaderArgs;
            Object obj = bundle2 == null ? null : bundle2.get(KEY_RESULT_KEY);
            if (obj == null) {
                z2 = false;
            } else {
                z2 = loaderInfo.hasCachedData(obj);
            }
            if (z2 || loaderManager.getLoader(i) != null || !z || isConnected()) {
                Log.d(TAG, "initializing loader #" + this.loaderId);
                loaderManager.initLoader(i, bundle, loaderInfo.getLoaderCallbacks());
                return true;
            }
            Log.e(TAG, "Starting error activity: OFFLINE");
            startErrorActivity(ErrorActivity.ErrorScreen.OFFLINE);
            return false;
        }
        Log.e(TAG, "LOADER_NONE");
        return false;
    }

    public <D> boolean restartLoader(int i, Bundle bundle) {
        if (i != -1) {
            this.loaderId = i;
            this.loaderArgs = bundle;
            if (isConnected()) {
                LoaderManager loaderManager = this.activityContext.getLoaderManager();
                int i2 = this.loaderId;
                loaderManager.restartLoader(i2, this.loaderArgs, this.activityContext.getLoaderInfo(i2).getLoaderCallbacks());
                return true;
            }
            startErrorActivity(ErrorActivity.ErrorScreen.OFFLINE);
        }
        return false;
    }

    public <D> boolean restartLoader() {
        if (this.loaderId != -1) {
            if (isConnected()) {
                LoaderManager loaderManager = this.activityContext.getLoaderManager();
                int i = this.loaderId;
                loaderManager.restartLoader(i, this.loaderArgs, this.activityContext.getLoaderInfo(i).getLoaderCallbacks());
                return true;
            }
            startErrorActivity(ErrorActivity.ErrorScreen.OFFLINE);
        }
        return false;
    }

    public void deleteLoader() {
        if (this.loaderId != -1) {
            this.activityContext.getLoaderManager().destroyLoader(this.loaderId);
            Bundle bundle = this.loaderArgs;
            Object obj = bundle == null ? null : bundle.get(KEY_RESULT_KEY);
            if (obj != null) {
                this.activityContext.getLoaderInfo(this.loaderId).clearCache(obj);
            }
            this.loaderId = -1;
            this.loaderArgs = null;
        }
    }

    @Contract(pure = true)
    public @Nullable ActivityResult getActivityResult(int i, int i2, Intent intent) {
        if (i != 63) {
            return null;
        }
        boolean z = true;
        if (i2 != 1) {
            z = false;
        }
        return new ActivityResult(z);
    }

    public interface ActivityContext {
        Activity getActivity();

        LoaderInfo getLoaderInfo(int i);

        LoaderManager getLoaderManager();

        void startActivityForResult(Intent intent, int i);
    }

    public interface LoaderInfo {
        void clearCache(Object obj);

        LoaderManager.LoaderCallbacks<?> getLoaderCallbacks();

        boolean hasCachedData(Object obj);
    }

    public static class ActivityResult {
        private final boolean tryAgain;

        public ActivityResult(boolean z) {
            this.tryAgain = z;
        }

        public boolean isTryAgain() {
            return this.tryAgain;
        }
    }
}
