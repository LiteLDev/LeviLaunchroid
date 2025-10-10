package com.microsoft.xbox.toolkit;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonWriter;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.lang.reflect.Type;
import java.util.Map;


public class GsonUtil {

    public static <T> T deserializeJson(InputStream inputStream, Class<T> cls) {
        return deserializeJson(createMinimumGsonBuilder().create(), inputStream, cls);
    }

    public static <T> T deserializeJson(String str, Class<T> cls) {
        return deserializeJson(createMinimumGsonBuilder().create(), str, cls);
    }

    public static <T> T deserializeJson(InputStream inputStream, Class<T> cls, Type type, Object obj) {
        return deserializeJson(createMinimumGsonBuilder().registerTypeAdapter(type, obj).create(), inputStream, cls);
    }

    public static <T> T deserializeJson(InputStream inputStream, Class<T> cls, @NotNull Map<Type, Object> map) {
        GsonBuilder createMinimumGsonBuilder = createMinimumGsonBuilder();
        for (Map.Entry next : map.entrySet()) {
            createMinimumGsonBuilder.registerTypeAdapter((Type) next.getKey(), next.getValue());
        }
        return deserializeJson(createMinimumGsonBuilder.create(), inputStream, cls);
    }

    public static <T> T deserializeJson(String str, Class<T> cls, Type type, Object obj) {
        return deserializeJson(createMinimumGsonBuilder().registerTypeAdapter(type, obj).create(), str, cls);
    }

    public static <T> T deserializeJson(Gson gson, InputStream stream, Class<T> resultClass) {
        InputStreamReader iReader = null;
        BufferedReader bReader = null;
        T result = null;
        try {
            InputStreamReader iReader2 = new InputStreamReader(stream);
            try {
                BufferedReader bReader2 = new BufferedReader(iReader2);
                try {
                    result = gson.fromJson(bReader2, resultClass);
                    if (bReader2 != null) {
                        try {
                            bReader2.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    if (iReader2 != null) {
                        try {
                            iReader2.close();
                            BufferedReader bufferedReader = bReader2;
                            InputStreamReader inputStreamReader = iReader2;
                        } catch (Exception e2) {
                            BufferedReader bufferedReader2 = bReader2;
                            InputStreamReader inputStreamReader2 = iReader2;
                        }
                    } else {
                        InputStreamReader inputStreamReader3 = iReader2;
                    }
                } catch (Exception e3) {
                    e3.printStackTrace();
                } catch (Throwable th) {
                    throw th;
                }
            } catch (Exception e4) {
                iReader = iReader2;
                if (bReader != null) {
                    try {
                        bReader.close();
                    } catch (Exception e5) {
                        e5.printStackTrace();
                    }
                }
                if (iReader != null) {
                    try {
                        iReader.close();
                    } catch (Exception e6) {
                        e6.printStackTrace();
                    }
                }
                return result;
            } catch (Throwable th2) {
                iReader = iReader2;
                if (bReader != null) {
                    try {
                        bReader.close();
                    } catch (Exception e7) {
                        e7.printStackTrace();
                    }
                }
                if (iReader != null) {
                    try {
                        iReader.close();
                    } catch (Exception e8) {
                        e8.printStackTrace();
                    }
                }
            }
        } catch (Exception e9) {
            e9.printStackTrace();
            return result;
        } catch (Throwable th3) {
            th3.printStackTrace();
        }
        return result;
    }

    public static <T> @Nullable T deserializeJson(Gson gson, String str, Class<T> cls) {
        try {
            return gson.fromJson(str, cls);
        } catch (Exception unused) {
            return null;
        }
    }

    public static @NotNull GsonBuilder createMinimumGsonBuilder() {
        return new GsonBuilder().excludeFieldsWithModifiers(128);
    }

    public static String toJsonString(Object obj) {
        return new Gson().toJson(obj);
    }

    public static @NotNull String buildJsonBody(JsonBodyBuilder jsonBodyBuilder) throws IOException {
        JsonWriter jsonWriter;
        StringWriter stringWriter = new StringWriter();
        try {
            jsonWriter = new JsonWriter(stringWriter);
            jsonBodyBuilder.buildBody(jsonWriter);
            String stringWriter2 = stringWriter.toString();
            jsonWriter.close();
            stringWriter.close();
            return stringWriter2;
        } catch (Throwable th) {
            stringWriter.close();
            throw th;
        }
    }

    public interface JsonBodyBuilder {
        void buildBody(JsonWriter jsonWriter) throws IOException;
    }
}
