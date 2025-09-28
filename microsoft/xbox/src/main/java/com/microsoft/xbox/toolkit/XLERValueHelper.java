package com.microsoft.xbox.toolkit;

import android.view.View;

import com.microsoft.xboxtcui.R;
import com.microsoft.xboxtcui.XboxTcuiSdk;

import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;

/**
 * 07.01.2021
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */

public class XLERValueHelper {
    public static int getStringRValue(String str) {
        try {
            return getStringRClass().getDeclaredField(str).getInt(null);
        } catch (Exception unused) {
            XLEAssert.assertTrue("Can't find " + str, false);
            return -1;
        }
    }

    public static int getDrawableRValue(String str) {
        try {
            return getDrawableRClass().getDeclaredField(str).getInt(null);
        } catch (Exception unused) {
            XLEAssert.assertTrue("Can't find " + str, false);
            return -1;
        }
    }

    public static int getIdRValue(String str) {
        try {
            return getIdRClass().getDeclaredField(str).getInt(null);
        } catch (Exception unused) {
            XLEAssert.assertTrue("Can't find " + str, false);
            return -1;
        }
    }

    public static int getStyleRValue(String str) {
        try {
            return getStyleRClass().getDeclaredField(str).getInt(null);
        } catch (Exception unused) {
            XLEAssert.assertTrue("Can't find " + str, false);
            return -1;
        }
    }

    @Nullable
    public static int [] getStyleableRValueArray(String str) {
        try {
            return (int[]) getStyleableRClass().getDeclaredField(str).get(null);
        } catch (Exception unused) {
            XLEAssert.assertTrue("Can't find " + str, false);
            return null;
        }
    }

    public static int getStyleableRValue(String str) {
        try {
            return getStyleableRClass().getDeclaredField(str).getInt(null);
        } catch (Exception unused) {
            XLEAssert.assertTrue("Can't find " + str, false);
            return -1;
        }
    }

    public static int getLayoutRValue(String str) {
        try {
            return getLayoutRClass().getDeclaredField(str).getInt(null);
        } catch (Exception unused) {
            XLEAssert.assertTrue("Can't find " + str, false);
            return -1;
        }
    }

    public static int getDimenRValue(String str) {
        try {
            return getDimenRClass().getDeclaredField(str).getInt(null);
        } catch (Exception unused) {
            XLEAssert.assertTrue("Can't find " + str, false);
            return -1;
        }
    }

    public static int getColorRValue(String str) {
        try {
            return getColorRClass().getDeclaredField(str).getInt(null);
        } catch (Exception unused) {
            XLEAssert.assertTrue("Can't find " + str, false);
            return -1;
        }
    }

    public static int findDimensionIdByName(String str) {
        Field field;
        try {
            field = R.dimen.class.getField(str);
        } catch (NoSuchFieldException unused) {
            field = null;
        }
        if (field == null) {
            return -1;
        }
        try {
            return field.getInt(null);
        } catch (IllegalAccessException unused2) {
            return -1;
        }
    }

    public static View findViewByString(String str) {
        Field field;
        try {
            field = R.id.class.getField(str);
        } catch (NoSuchFieldException unused) {
            field = null;
        }
        int i = -1;
        if (field != null) {
            try {
                i = field.getInt(null);
            } catch (IllegalAccessException unused2) {
            }
        }
        return XboxTcuiSdk.getActivity().findViewById(i);
    }

    protected static Class getStringRClass() {
        return R.string.class;
    }

    protected static Class getDrawableRClass() {
        return R.drawable.class;
    }

    protected static Class getIdRClass() {
        return R.id.class;
    }

    protected static Class getStyleRClass() {
        return R.style.class;
    }

    protected static Class getStyleableRClass() {
        return R.styleable.class;
    }

    protected static Class getLayoutRClass() {
        return R.layout.class;
    }

    protected static Class getDimenRClass() {
        return R.dimen.class;
    }

    protected static Class getColorRClass() {
        return R.color.class;
    }
}
