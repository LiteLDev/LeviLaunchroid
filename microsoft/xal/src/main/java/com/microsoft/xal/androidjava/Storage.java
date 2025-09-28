package com.microsoft.xal.androidjava;

import android.content.Context;
import org.jetbrains.annotations.NotNull;

/**
 * 02.10.2020
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */

public class Storage {
    @NotNull
    public static String getStoragePath(@NotNull Context context) {
        return context.getFilesDir().getPath();
    }
}
