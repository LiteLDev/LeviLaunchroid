package com.microsoft.xbox.service.model.serialization;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.microsoft.xbox.toolkit.JavaUtil;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * 07.01.2021
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */

public class UTCDateConverterGson {
    private static final int NO_MS_STRING_LENGTH = 19;
    private static final SimpleDateFormat defaultFormatMs = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.ENGLISH);
    public static SimpleDateFormat defaultFormatNoMs = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH);
    public static SimpleDateFormat shortDateAlternateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.ENGLISH);
    public static SimpleDateFormat shortDateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss", Locale.ENGLISH);

    public static synchronized Date convert(String value) {
        Date date = null;
        synchronized (UTCDateConverterGson.class) {
            if (!JavaUtil.isNullOrEmpty(value)) {
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
                    value = value.replaceAll("([.][0-9]{3})[0-9]*$", "$1");
                }
                boolean noMsDate = value.length() == 19;
                if (timeZone == null) {
                    timeZone = TimeZone.getTimeZone("GMT");
                }
                if (noMsDate) {
                    try {
                        defaultFormatNoMs.setTimeZone(timeZone);
                        date = defaultFormatNoMs.parse(value);
                    } catch (ParseException e) {
                        e.printStackTrace();
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

    public static class UTCDateConverterJSONDeserializer implements JsonDeserializer<Date> {
        public Date deserialize(@NotNull JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) {
            return UTCDateConverterGson.convert(jsonElement.getAsJsonPrimitive().getAsString());
        }
    }

    public static class UTCDateConverterShortDateFormatJSONDeserializer implements JsonDeserializer<Date> {
        public Date deserialize(@NotNull JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) {
            String asString = jsonElement.getAsJsonPrimitive().getAsString();
            UTCDateConverterGson.shortDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
            try {
                return UTCDateConverterGson.shortDateFormat.parse(asString);
            } catch (ParseException unused) {
                return null;
            }
        }
    }

    public static class UTCDateConverterShortDateAlternateFormatJSONDeserializer implements JsonDeserializer<Date> {
        public Date deserialize(@NotNull JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) {
            Date date;
            String asString = jsonElement.getAsJsonPrimitive().getAsString();
            TimeZone timeZone = TimeZone.getTimeZone("GMT");
            UTCDateConverterGson.shortDateFormat.setTimeZone(timeZone);
            try {
                date = UTCDateConverterGson.shortDateFormat.parse(asString);
            } catch (ParseException unused) {
                date = null;
            }
            if (date == null || date.getYear() + 1900 >= 2000) {
                return date;
            }
            UTCDateConverterGson.shortDateAlternateFormat.setTimeZone(timeZone);
            try {
                return UTCDateConverterGson.shortDateAlternateFormat.parse(asString);
            } catch (ParseException unused2) {
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
            UTCDateConverterGson.defaultFormatNoMs.setTimeZone(TimeZone.getTimeZone("GMT"));
            try {
                return UTCDateConverterGson.defaultFormatNoMs.parse(asString);
            } catch (ParseException unused) {
                return null;
            }
        }
    }
}
