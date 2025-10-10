package com.microsoft.xbox.toolkit.system;

import android.annotation.SuppressLint;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.Display;
import android.view.WindowManager;

import com.microsoft.xbox.toolkit.XLEAssert;
import com.microsoft.xboxtcui.XboxTcuiSdk;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.net.NetworkInterface;
import java.util.Collections;


public class SystemUtil {
    private static final int MAX_SD_SCREEN_PIXELS = 384000;

    public static int getSdkInt() {
        return Build.VERSION.SDK_INT;
    }

    public static int DIPtoPixels(float f) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, f, XboxTcuiSdk.getResources().getDisplayMetrics());
    }

    public static int SPtoPixels(float f) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, f, XboxTcuiSdk.getResources().getDisplayMetrics());
    }

    public static int getScreenWidth() {
        return XboxTcuiSdk.getResources().getDisplayMetrics().widthPixels;
    }

    public static int getScreenHeight() {
        return XboxTcuiSdk.getResources().getDisplayMetrics().heightPixels;
    }

    @SuppressLint("WrongConstant")
    public static int getColorDepth() {
        PixelFormat.getPixelFormatInfo(1, null);
        //return null.bitsPerPixel;
        return 0;
    }

    public static float getScreenWidthInches() {
        return ((float) getScreenWidth()) / XboxTcuiSdk.getResources().getDisplayMetrics().xdpi;
    }

    public static float getScreenHeightInches() {
        return ((float) getScreenHeight()) / XboxTcuiSdk.getResources().getDisplayMetrics().ydpi;
    }

    public static float getYDPI() {
        return XboxTcuiSdk.getResources().getDisplayMetrics().ydpi;
    }

    public static int getRotation() {
        return getDisplay().getRotation();
    }

    public static int getOrientation() {
        int rotation = getRotation();
        return (rotation == 0 || rotation == 2) ? 1 : 2;
    }

    public static boolean isHDScreen() {
        return getScreenHeight() * getScreenWidth() > MAX_SD_SCREEN_PIXELS;
    }

    public static boolean isSlate() {
        return Math.sqrt(Math.pow(getScreenWidthInches(), 2.0d) + Math.pow(getScreenHeightInches(), 2.0d)) > 6.0d;
    }

    @Contract(pure = true)
    public static @NotNull String getDeviceType() {
        XLEAssert.assertTrue(false);
        return "";
    }

    private static Display getDisplay() {
        return ((WindowManager) XboxTcuiSdk.getSystemService("window")).getDefaultDisplay();
    }

    public static boolean isSDCardAvailable() {
        return Environment.getExternalStorageState().equals("mounted");
    }

    public static float getScreenWidthHeightAspectRatio() {
        int screenWidth = getScreenWidth();
        int screenHeight = getScreenHeight();
        if (screenWidth <= 0 || screenHeight <= 0) {
            return 0.0f;
        }
        return screenWidth > screenHeight ? ((float) screenWidth) / ((float) screenHeight) : ((float) screenHeight) / ((float) screenWidth);
    }

    public static String getDeviceId() {
        return Settings.Secure.getString(XboxTcuiSdk.getContentResolver(), "android_id");
    }

    public static String getDeviceModelName() {
        return Build.MODEL;
    }

    @NotNull
    public static String getMACAddress(String interfaceName) {
        try {
            for (NetworkInterface intf : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (interfaceName == null || intf.getName().equalsIgnoreCase(interfaceName)) {
                    byte[] mac = intf.getHardwareAddress();
                    if (mac == null) {
                        return "";
                    }
                    StringBuilder buf = new StringBuilder();
                    for (int idx = 0; idx < mac.length; idx++) {
                        buf.append(String.format("%02X:", mac[idx]));
                    }
                    if (buf.length() > 0) {
                        buf.deleteCharAt(buf.length() - 1);
                    }
                    return buf.toString();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public static void TEST_randomSleep(int i) {
        XLEAssert.assertTrue(false);
    }

    public static boolean TEST_randomFalseOutOf(int i) {
        XLEAssert.assertTrue(false);
        return true;
    }

    public static boolean isKindle() {
        String str = Build.MANUFACTURER;
        return str != null && "AMAZON".compareToIgnoreCase(str) == 0;
    }
}
