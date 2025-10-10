package com.microsoft.xbox.toolkit;

import android.util.Pair;

import com.microsoft.xbox.idp.util.HttpCall;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;


public class TcuiHttpUtil {
    public static <T> T getResponseSync(@NotNull HttpCall httpCall, final Class<T> cls) throws XLEException {
        final AtomicReference<Pair<Boolean, Object>> atomicReference = new AtomicReference<>();
        atomicReference.set(new Pair<>(false, null));
        httpCall.getResponseAsync((i, inputStream, httpHeaders) -> {
            Object deserializeJson = (i >= 200 || i <= 299) ? GsonUtil.deserializeJson(inputStream, cls) : null;
            synchronized (atomicReference) {
                atomicReference.set(new Pair<>(true, deserializeJson));
                atomicReference.notify();
            }
        });
        synchronized (atomicReference) {
            try {
                while (true) {
                    if (!(atomicReference.get()).first) {
                        atomicReference.wait();
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return (T) (atomicReference.get()).second;
    }

    public static boolean getResponseSyncSucceeded(@NotNull HttpCall httpCall, final List<Integer> list) {
        final AtomicReference<Boolean> atomicReference = new AtomicReference<>();
        httpCall.getResponseAsync((i, inputStream, httpHeaders) -> {
            synchronized (atomicReference) {
                atomicReference.set(list.contains(i));
                atomicReference.notify();
            }
        });
        synchronized (atomicReference) {
            try {
                while (true) {
                    if (atomicReference.get() == null) {
                        atomicReference.wait();
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return (atomicReference.get()).booleanValue();
    }

    public static String getResponseBodySync(@NotNull HttpCall httpCall) throws XLEException {
        final AtomicReference<Pair<Boolean, String>> atomicReference = new AtomicReference<>();
        atomicReference.set(new Pair<>(false, null));
        httpCall.getResponseAsync((i, inputStream, httpHeaders) -> {
            String str = null;
            if (i < 200 || i > 299) {
                synchronized (atomicReference) {
                    atomicReference.set(new Pair<>(true, null));
                    atomicReference.notify();
                }
                return;
            }
            try {
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8), 4096);
                StringBuilder sb = new StringBuilder();
                while (true) {
                    String readLine = bufferedReader.readLine();
                    if (readLine == null) {
                        break;
                    }
                    sb.append(readLine).append("\n");
                }
                str = sb.toString();
            } catch (IOException e) {
                XLEAssert.assertTrue("Failed to read ShortCircuitProfileMessage string - " + e.getMessage(), false);
            }
            synchronized (atomicReference) {
                atomicReference.set(new Pair<>(true, str));
                atomicReference.notify();
            }
        });
        synchronized (atomicReference) {
            try {
                while (true) {
                    if (!(atomicReference.get()).first) {
                        atomicReference.wait();
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return (atomicReference.get()).second;
    }

    public static <T> void throwIfNullOrFalse(T t) throws XLEException {
        if (t == null && !Boolean.getBoolean(t.toString())) {
            throw new XLEException(2);
        }
    }
}