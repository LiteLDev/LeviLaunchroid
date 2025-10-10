package com.mojang.minecraftpe;

import android.text.format.DateFormat;
import android.util.Log;


public class DateTimeHelper {
    public static boolean Is24HourTimeFormat() {
        try {
            return DateFormat.is24HourFormat(MainActivity.mInstance.getApplicationContext());
        } catch (Exception e) {
            return true;
        }
    }
}
