package com.microsoft.xbox.idp.util;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.fragment.app.Fragment;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * 07.01.2021
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */

public class FragmentLoaderKey implements Parcelable {
    public static final Parcelable.Creator<FragmentLoaderKey> CREATOR = new Parcelable.Creator<FragmentLoaderKey>() {
        @NotNull
        @Contract("_ -> new")
        public FragmentLoaderKey createFromParcel(Parcel parcel) {
            return new FragmentLoaderKey(parcel);
        }

        @NotNull
        @Contract(value = "_ -> new", pure = true)
        public FragmentLoaderKey [] newArray(int i) {
            return new FragmentLoaderKey[i];
        }
    };
    private final String className;
    private final int loaderId;

    public FragmentLoaderKey(@NotNull Class<? extends Fragment> cls, int i) {
        this.className = cls.getName();
        this.loaderId = i;
    }

    protected FragmentLoaderKey(@NotNull Parcel parcel) {
        this.className = parcel.readString();
        this.loaderId = parcel.readInt();
    }

    public int describeContents() {
        return 0;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        FragmentLoaderKey fragmentLoaderKey = (FragmentLoaderKey) obj;
        if (this.loaderId != fragmentLoaderKey.loaderId) {
            return false;
        }
        return this.className.equals(fragmentLoaderKey.className);
    }

    public int hashCode() {
        return (this.className.hashCode() * 31) + this.loaderId;
    }

    public void writeToParcel(@NotNull Parcel parcel, int i) {
        parcel.writeString(this.className);
        parcel.writeInt(this.loaderId);
    }
}
