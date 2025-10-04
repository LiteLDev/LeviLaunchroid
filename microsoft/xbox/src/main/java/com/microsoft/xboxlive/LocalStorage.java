package com.microsoft.xboxlive;

import android.content.Context;

import org.jetbrains.annotations.NotNull;

/**
 * 02.10.2020
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */

public class LocalStorage {
    @NotNull
    public static String getPath(@NotNull Context context) {
        return context.getFilesDir().getPath();
    }
}