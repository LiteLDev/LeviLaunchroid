package com.microsoft.xbox.idp.model;

import android.os.Parcel;
import android.os.Parcelable;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;


public class Suggestions {

    public static class Request {
        public int Algorithm;
        public int Count;
        public String Locale;
        public String Seed;
    }

    public static class Response implements Parcelable {
        public static final Parcelable.Creator<Response> CREATOR = new Parcelable.Creator<Response>() {
            @NotNull
            @Contract("_ -> new")
            public Response createFromParcel(Parcel parcel) {
                return new Response(parcel);
            }

            @NotNull
            @Contract(value = "_ -> new", pure = true)
            public Response [] newArray(int i) {
                return new Response[i];
            }
        };
        public ArrayList<String> Gamertags;

        protected Response(@NotNull Parcel parcel) {
            this.Gamertags = parcel.createStringArrayList();
        }

        public int describeContents() {
            return 0;
        }

        public void writeToParcel(@NotNull Parcel parcel, int i) {
            parcel.writeStringList(Gamertags);
        }
    }
}
