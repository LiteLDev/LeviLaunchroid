package com.microsoft.xbox.toolkit;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

/**
 * 07.01.2021
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */

public class StreamUtil {
    @Nullable
    public static byte [] CreateByteArray(InputStream inputStream) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            CopyStream(byteArrayOutputStream, inputStream);
            return byteArrayOutputStream.toByteArray();
        } catch (IOException unused) {
            return null;
        }
    }

    public static void CopyStream(OutputStream outputStream, @NotNull InputStream inputStream) throws IOException {
        byte[] bArr = new byte[16384];
        while (true) {
            int read = inputStream.read(bArr);
            if (read > 0) {
                outputStream.write(bArr, 0, read);
            } else {
                outputStream.flush();
                return;
            }
        }
    }

    public static @Nullable String ReadAsString(InputStream inputStream) {
        StringBuilder sb = new StringBuilder();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        while (true) {
            try {
                String readLine = bufferedReader.readLine();
                if (readLine == null) {
                    return sb.toString();
                }
                sb.append(readLine);
                sb.append('\n');
            } catch (IOException unused) {
                return null;
            }
        }
    }

    public static void consumeAndClose(InputStream inputStream) throws IOException {
        BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
        do {
            try {
            } finally {
                bufferedInputStream.close();
            }
        } while (bufferedInputStream.read() != -1);
    }

    @NotNull
    @Contract("null -> fail")
    public static byte [] HexStringToByteArray(String str) {
        if (str != null) {
            if (str.length() % 2 != 0) {
                str = "0" + str;
            }
            int i = 0;
            XLEAssert.assertTrue(str.length() % 2 == 0);
            byte[] bArr = new byte[(str.length() / 2)];
            while (i < str.length()) {
                int i2 = i + 2;
                bArr[i / 2] = Byte.parseByte(str.substring(i, i2), 16);
                i = i2;
            }
            return bArr;
        }
        throw new IllegalArgumentException("hexString invalid");
    }
}