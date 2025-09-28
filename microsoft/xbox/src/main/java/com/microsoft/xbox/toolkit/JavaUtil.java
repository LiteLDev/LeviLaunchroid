package com.microsoft.xbox.toolkit;

import android.graphics.Rect;
import android.text.format.DateUtils;
import android.view.View;

import com.microsoft.xboxtcui.XboxTcuiSdk;

import org.apache.http.protocol.HTTP;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Random;
import java.util.TimeZone;

/**
 * 07.01.2021
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */

public class JavaUtil {
    private static final String HEX_PREFIX = "0x";
    private static final NumberFormat INTEGER_FORMATTER = NumberFormat.getIntegerInstance(Locale.getDefault());
    private static final Date MIN_DATE = new Date(100, 1, 1);
    private static final NumberFormat PERCENT_FORMATTER = NumberFormat.getPercentInstance(Locale.getDefault());

    public static boolean containsFlag(int i, int i2) {
        return (i & i2) == i2;
    }

    public static void sleepDebug(long j) {
    }

    public static String getShortClassName(@NotNull Class cls) {
        String[] split = cls.getName().split("\\.");
        return split[split.length - 1];
    }

    public static boolean stringsEqual(String str, String str2) {
        if ((str == null && str2 == null) || str == str2) {
            return true;
        }
        return stringsEqualNonNull(str, str2);
    }

    public static boolean stringsEqualNonNull(String str, String str2) {
        boolean z = false;
        if (str == null || str2 == null) {
            return false;
        }
        if (!(str == null || str2 == null)) {
            z = true;
        }
        XLEAssert.assertTrue(z);
        return str.equals(str2);
    }

    public static boolean stringsEqualCaseInsensitive(String str, String str2) {
        boolean z = true;
        if (str == str2) {
            return true;
        }
        if (str == null || str2 == null) {
            return false;
        }
        if (str == null || str2 == null) {
            z = false;
        }
        XLEAssert.assertTrue(z);
        return str.equalsIgnoreCase(str2);
    }

    public static boolean stringsEqualNonNullCaseInsensitive(String str, String str2) {
        boolean z = false;
        if (str == null || str2 == null) {
            return false;
        }
        if (str == str2) {
            return true;
        }
        if (!(str == null || str2 == null)) {
            z = true;
        }
        XLEAssert.assertTrue(z);
        return str.equalsIgnoreCase(str2);
    }

    public static boolean tryParseBoolean(String str, boolean z) {
        try {
            return Boolean.parseBoolean(str);
        } catch (Exception unused) {
            return z;
        }
    }

    public static int tryParseInteger(String str, int i) {
        try {
            return Integer.parseInt(str);
        } catch (Exception unused) {
            return i;
        }
    }

    public static long tryParseLong(String str, long j) {
        try {
            return Long.parseLong(str);
        } catch (Exception unused) {
            return j;
        }
    }

    public static double tryParseDouble(String str, double d) {
        try {
            return Double.parseDouble(str);
        } catch (Exception unused) {
            return d;
        }
    }

    public static @Nullable String getLocalizedDateString(Date date) {
        try {
            return DateUtils.formatDateTime(XboxTcuiSdk.getApplicationContext(), date.getTime(), 131088);
        } catch (Exception unused) {
            return null;
        }
    }

    public static String getTimeStringMMSS(long j) {
        return DateUtils.formatElapsedTime(j);
    }

    public static int parseInteger(String str) {
        try {
            return Integer.parseInt(str, 10);
        } catch (Exception unused) {
            return 0;
        }
    }

    private static boolean parseBoolean(String str) {
        try {
            return Boolean.parseBoolean(str);
        } catch (Exception unused) {
            return false;
        }
    }

    public static long parseHexLong(String str) {
        if (str == null) {
            return 0;
        }
        if (str.startsWith(HEX_PREFIX)) {
            return parseHexLongExpectHex(str);
        }
        try {
            return Long.parseLong(str, 16);
        } catch (Exception unused) {
            return 0;
        }
    }

    private static long parseHexLongExpectHex(@NotNull String str) {
        XLEAssert.assertTrue(str.startsWith(HEX_PREFIX));
        try {
            return Long.parseLong(str.substring(2), 16);
        } catch (Exception unused) {
            return 0;
        }
    }

    public static boolean isNullOrEmpty(String str) {
        return str == null || str.length() == 0;
    }

    public static boolean isTouchPointInsideView(float f, float f2, @NotNull View view) {
        int[] iArr = new int[2];
        view.getLocationOnScreen(iArr);
        return new Rect(iArr[0], iArr[1], iArr[0] + view.getWidth(), iArr[1] + view.getHeight()).contains((int) f, (int) f2);
    }

    public static @Nullable String urlEncode(String str) {
        try {
            return URLEncoder.encode(str, HTTP.UTF_8);
        } catch (UnsupportedEncodingException unused) {
            return null;
        }
    }

    public static @Nullable String urlDecode(String str) {
        try {
            return URLDecoder.decode(str, HTTP.UTF_8);
        } catch (UnsupportedEncodingException unused) {
            return null;
        }
    }

    public static String stringToUpper(String str) {
        if (str == null) {
            return null;
        }
        return str.toUpperCase();
    }

    public static String stringToLower(String str) {
        if (str == null) {
            return null;
        }
        return str.toLowerCase();
    }

    public static @NotNull String concatenateStringsWithDelimiter(String str, String str2, String str3, String str4) {
        return concatenateStringsWithDelimiter(str, str2, str3, str4, true);
    }

    public static @NotNull String concatenateStringsWithDelimiter(String str, String str2, String str3, String str4, boolean z) {
        StringBuilder sb = new StringBuilder();
        sb.append(z ? " " : "");
        sb.append(str4);
        sb.append(" ");
        String sb2 = sb.toString();
        StringBuilder sb3 = new StringBuilder();
        if (!isNullOrEmpty(str)) {
            sb3.append(str);
        }
        if (!isNullOrEmpty(str2)) {
            if (sb3.length() > 0) {
                sb3.append(sb2);
            }
            sb3.append(str2);
        }
        if (!isNullOrEmpty(str3)) {
            if (sb3.length() > 0) {
                sb3.append(sb2);
            }
            sb3.append(str3);
        }
        return sb3.toString();
    }

    public static @NotNull String concatenateStringsWithDelimiter(String str, boolean z, @NotNull String ... strArr) {
        StringBuilder sb = new StringBuilder();
        sb.append(z ? " " : "");
        sb.append(str);
        sb.append(" ");
        String sb2 = sb.toString();
        StringBuilder sb3 = new StringBuilder();
        if (strArr.length == 0) {
            return "";
        }
        for (int i = 0; i < strArr.length; i++) {
            if (!isNullOrEmpty(strArr[i])) {
                if (sb3.length() > 0) {
                    sb3.append(sb2);
                }
                sb3.append(strArr[i]);
            }
        }
        return sb3.toString();
    }

    public static @NotNull String concatenateUrlWithLinkAndParam(String str, String str2, String str3) {
        StringBuffer stringBuffer = new StringBuffer();
        if (!isNullOrEmpty(str)) {
            stringBuffer.append(str);
        }
        if (!isNullOrEmpty(str2)) {
            if (stringBuffer.length() > 0) {
                stringBuffer.append(str3);
            }
            stringBuffer.append(str2);
        }
        return stringBuffer.toString();
    }

    public static @Nullable Date JSONDateToJavaDate(String str) {
        if (isNullOrEmpty(str)) {
            return null;
        }
        XLEAssert.assertTrue(str.startsWith("/Date("));
        int length = str.length();
        if (str.startsWith("+0000)/", str.length() - 7)) {
            length = str.length() - 7;
        } else if (str.startsWith(")/", str.length() - 2)) {
            length = str.length() - 2;
        } else {
            XLEAssert.assertTrue(false);
        }
        return new Date(Long.parseLong(str.substring(6, length)));
    }

    public static @NotNull String JavaDateToJSONDate(Date date) {
        GregorianCalendar gregorianCalendar = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        gregorianCalendar.setTime(date);
        return String.format("/Date(%d)/", gregorianCalendar.getTimeInMillis());
    }

    public static <T> @NotNull List<T> listIteratorToList(ListIterator<T> listIterator) {
        ArrayList<T> arrayList = new ArrayList<>();
        while (listIterator != null && listIterator.hasNext()) {
            arrayList.add(listIterator.next());
        }
        return arrayList;
    }

    public static String pluralize(int i, String str, String str2, String str3) {
        if (i == 0) {
            return str;
        }
        if (i == 1) {
            return str2;
        }
        return String.format(str3, i);
    }

    public static int randInRange(@NotNull Random random, int i, int i2) {
        XLEAssert.assertTrue(i2 >= i);
        return i + random.nextInt(i2 - i);
    }

    public static <T> @NotNull ArrayList<T> sublistShuffle(ArrayList<T> arrayList, int i) {
        Random random = new Random();
        ArrayList<T> arrayList2 = new ArrayList<>(i);
        if (!(arrayList == null || arrayList.isEmpty())) {
            boolean z = true;
            if (arrayList.size() >= i) {
                for (int i2 = 0; i2 < i; i2++) {
                    int randInRange = randInRange(random, i2, arrayList.size());
                    T t = arrayList.get(i2);
                    arrayList.set(i2, arrayList.get(randInRange));
                    arrayList.set(randInRange, t);
                    arrayList2.add(arrayList.get(i2));
                }
            } else {
                XLEAssert.assertTrue(arrayList.size() > 0 && arrayList.size() < i);
                for (int i3 = 0; i3 < i; i3++) {
                    arrayList2.add(arrayList.get(random.nextInt(arrayList.size())));
                }
            }
            if (arrayList2.size() != i) {
                z = false;
            }
            XLEAssert.assertTrue(z);
        }
        return arrayList2;
    }

    public static @NotNull String formatInteger(int i) {
        return INTEGER_FORMATTER.format(i);
    }

    public static @NotNull String formatPercent(float f) {
        XLEAssert.assertTrue(f + " is not between 0 and 1", f >= 0.0f && f <= 1.0f);
        return PERCENT_FORMATTER.format(f);
    }

    public static int[] concatIntArrays(int[]... iArr) {
        if (iArr == null) {
            return null;
        }
        int i = 0;
        for (int[] length : iArr) {
            i += length.length;
        }
        int[] iArr2 = new int[i];
        int i2 = 0;
        for (int[] iArr3 : iArr) {
            System.arraycopy(iArr3, 0, iArr2, i2, iArr3.length);
            i2 += iArr3.length;
        }
        return iArr2;
    }

    public static <T> boolean DeepCompareArrayList(ArrayList<T> arrayList, ArrayList<T> arrayList2) {
        if (arrayList == arrayList2) {
            return true;
        }
        if (arrayList == null) {
            return arrayList2 == null;
        }
        if (arrayList2 == null || arrayList.size() != arrayList2.size()) {
            return false;
        }
        for (int i = 0; i < arrayList.size(); i++) {
            if (!arrayList.get(i).equals(arrayList2.get(i))) {
                return false;
            }
        }
        return true;
    }

    @Contract(pure = true)
    public static @NotNull String surroundInQuotes(String str) {
        return "\"" + str + "\"";
    }

    public static String EnsureEncode(String str) {
        if (str == null || str.length() == 0) {
            return str;
        }
        try {
            return URLEncoder.encode(URLDecoder.decode(str, HTTP.UTF_8), HTTP.UTF_8);
        } catch (UnsupportedEncodingException unused) {
            return str;
        }
    }

    public static <T> boolean move(ArrayList<T> arrayList, int i, int i2) {
        if (arrayList == null || !isPositionInRange(arrayList, i) || !isPositionInRange(arrayList, i2)) {
            return false;
        }
        T t = arrayList.get(i);
        if (i < i2) {
            while (i < i2) {
                int i3 = i + 1;
                arrayList.set(i, arrayList.get(i3));
                i = i3;
            }
        } else {
            while (i > i2) {
                arrayList.set(i, arrayList.get(i - 1));
                i--;
            }
        }
        arrayList.set(i2, t);
        return true;
    }

    private static <T> boolean isPositionInRange(ArrayList<T> arrayList, int i) {
        return i >= 0 && i < arrayList.size();
    }

    public static @NotNull String getCurrentStackTraceAsString() {
        StringBuilder sb = new StringBuilder();
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        if (stackTrace != null) {
            for (StackTraceElement stackTraceElement : stackTrace) {
                sb.append("\n\n \t " + stackTraceElement.toString());
            }
        }
        return sb.toString();
    }

    public static Date convertToUTC(Date date) {
        if (date == null) {
            return null;
        }
        TimeZone timeZone = TimeZone.getDefault();
        Calendar instance = Calendar.getInstance();
        instance.setTime(date);
        instance.add(14, -timeZone.getOffset(date.getTime()));
        return instance.getTime();
    }

    public static boolean setFieldValue(@NotNull Object obj, String str, Object obj2) {
        try {
            Field declaredField = obj.getClass().getDeclaredField(str);
            declaredField.setAccessible(true);
            declaredField.set(obj, obj2);
            return true;
        } catch (IllegalAccessException | NoSuchFieldException unused) {
            return false;
        }
    }
}
