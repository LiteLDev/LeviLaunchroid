package com.microsoft.xbox.idp.model.serialization;

import android.text.TextUtils;
import android.util.Log;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;


public class UTCDateConverter {
    public static final String TAG = UTCDateConverter.class.getSimpleName();
    private static final int NO_MS_STRING_LENGTH = 19;
    private static final SimpleDateFormat defaultFormatMs = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.ENGLISH);
    public static SimpleDateFormat defaultFormatNoMs = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH);
    public static SimpleDateFormat shortDateAlternateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.ENGLISH);
    public static SimpleDateFormat shortDateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss", Locale.ENGLISH);

    public static synchronized Date convert(String value) {
        Date date = null;
        synchronized (UTCDateConverter.class) {
            if (!TextUtils.isEmpty(value)) {
                if (value.endsWith("Z")) {
                    value = value.replace("Z", "");
                }
                TimeZone timeZone = null;
                if (value.endsWith("+00:00")) {
                    value = value.replace("+00:00", "");
                } else if (value.endsWith("+01:00")) {
                    value = value.replace("+01:00", "");
                    timeZone = TimeZone.getTimeZone("GMT+01:00");
                } else if (value.contains(".")) {
                    value = value.replaceAll("([.]\\d{3})\\d*$", "$1");
                }
                boolean noMsDate = value.length() == NO_MS_STRING_LENGTH;
                if (timeZone == null) {
                    timeZone = TimeZone.getTimeZone("GMT");
                }
                if (noMsDate) {
                    try {
                        defaultFormatNoMs.setTimeZone(timeZone);
                        date = defaultFormatNoMs.parse(value);
                    } catch (ParseException e) {
                        Log.e(TAG, e.toString());
                    }
                } else {
                    defaultFormatMs.setTimeZone(timeZone);
                    try {
                        date = defaultFormatMs.parse(value);
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return date;
    }

    public static class UTCDateConverterJSONDeserializer implements JsonDeserializer<Date>, JsonSerializer<Date> {
        public Date deserialize(@NotNull JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) {
            return UTCDateConverter.convert(jsonElement.getAsJsonPrimitive().getAsString());
        }

        public JsonElement serialize(Date date, Type type, JsonSerializationContext jsonSerializationContext) {
            return new JsonPrimitive(UTCDateConverter.defaultFormatNoMs.format(date));
        }
    }

    public static class UTCDateConverterShortDateFormatJSONDeserializer implements JsonDeserializer<Date> {
        public Date deserialize(@NotNull JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) {
            String asString = jsonElement.getAsJsonPrimitive().getAsString();
            UTCDateConverter.shortDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
            try {
                return UTCDateConverter.shortDateFormat.parse(asString);
            } catch (ParseException unused) {
                Log.d(TAG, "failed to parse date " + asString);
                return null;
            }
        }
    }

    public static class UTCDateConverterShortDateAlternateFormatJSONDeserializer implements JsonDeserializer<Date> {
        public Date deserialize(@NotNull JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) {
            Date date;
            String asString = jsonElement.getAsJsonPrimitive().getAsString();
            TimeZone timeZone = TimeZone.getTimeZone("GMT");
            UTCDateConverter.shortDateFormat.setTimeZone(timeZone);
            try {
                date = UTCDateConverter.shortDateFormat.parse(asString);
            } catch (ParseException unused) {
                Log.d(TAG, "failed to parse short date " + asString);
                date = null;
            }
            if (date == null || date.getYear() + 1900 >= 2000) {
                return date;
            }
            UTCDateConverter.shortDateAlternateFormat.setTimeZone(timeZone);
            try {
                return UTCDateConverter.shortDateAlternateFormat.parse(asString);
            } catch (ParseException unused2) {
                Log.d(TAG, "failed to parse alternate short date " + asString);
                return date;
            }
        }
    }

    public static class UTCRoundtripDateConverterJSONDeserializer implements JsonDeserializer<Date> {
        public Date deserialize(@NotNull JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) {
            String asString = jsonElement.getAsJsonPrimitive().getAsString();
            if (asString.endsWith("Z")) {
                asString = asString.replace("Z", "");
            }
            UTCDateConverter.defaultFormatNoMs.setTimeZone(TimeZone.getTimeZone("GMT"));
            try {
                return UTCDateConverter.defaultFormatNoMs.parse(asString);
            } catch (ParseException unused) {
                Log.d(TAG, "failed to parse date " + asString);
                return null;
            }
        }
    }
}
