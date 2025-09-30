package com.microsoft.applications.events;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

/**
 * @author <a href="https://github.com/RadiantByte">RadiantByte</a>
 */

class Request implements Runnable {
    private byte[] m_body;
    private HttpURLConnection m_connection;
    private final HttpClient m_parent;
    private String m_request_id;

    Request(HttpClient httpClient, String str, String str2, byte[] bArr, String str3, int[] iArr, byte[] bArr2) throws IOException {
        this.m_parent = httpClient;
        HttpURLConnection httpURLConnection = (HttpURLConnection) httpClient.newUrl(str).openConnection();
        this.m_connection = httpURLConnection;
        httpURLConnection.setRequestMethod(str2);
        this.m_body = bArr;
        if (bArr.length > 0) {
            this.m_connection.setFixedLengthStreamingMode(bArr.length);
            this.m_connection.setDoOutput(true);
        }
        this.m_request_id = str3;
        int i = 0;
        int i2 = 0;
        while (true) {
            int i3 = i + 1;
            if (i3 >= iArr.length) {
                return;
            }
            String str4 = new String(bArr2, i2, iArr[i], StandardCharsets.UTF_8);
            int i4 = i2 + iArr[i];
            String str5 = new String(bArr2, i4, iArr[i3], StandardCharsets.UTF_8);
            i2 = i4 + iArr[i3];
            this.m_connection.setRequestProperty(str4, str5);
            i += 2;
        }
    }

    @Override // java.lang.Runnable
    public void run() {
        int responseCode = 0;
        String[] strArr = new String[0];
        BufferedInputStream bufferedInputStream;
        int i = 0;
        String[] strArr2 = new String[0];
        byte[] bArr = new byte[0];
        try {
            try {
                if (this.m_body.length > 0) {
                    this.m_connection.getOutputStream().write(this.m_body);
                }
                responseCode = this.m_connection.getResponseCode();
                try {
                    Map<String, List<String>> headerFields = this.m_connection.getHeaderFields();
                    Vector vector = new Vector();
                    for (Map.Entry<String, List<String>> entry : headerFields.entrySet()) {
                        if (entry.getKey() != null) {
                            for (String str : entry.getValue()) {
                                vector.add(entry.getKey());
                                vector.add(str);
                            }
                        }
                    }
                    strArr = (String[]) vector.toArray(strArr2);
                    try {
                        if (responseCode >= 300) {
                            bufferedInputStream = new BufferedInputStream(this.m_connection.getErrorStream());
                        } else {
                            bufferedInputStream = new BufferedInputStream(this.m_connection.getInputStream());
                        }
                        byte[] bArr2 = new byte[1024];
                        Vector vector2 = new Vector();
                        int i2 = 0;
                        while (true) {
                            int i3 = bufferedInputStream.read(bArr2, 0, 1024);
                            if (i3 < 0) {
                                break;
                            } else if (i3 > 0) {
                                vector2.add(Arrays.copyOfRange(bArr2, 0, i3));
                                i2 += i3;
                            }
                        }
                        bArr = new byte[i2];
                        Iterator it = vector2.iterator();
                        int i4 = 0;
                        while (it.hasNext()) {
                            for (byte b : (byte[]) it.next()) {
                                bArr[i4] = b;
                                i4++;
                            }
                        }
                    } catch (Exception unused) {
                        i = responseCode;
                        strArr2 = strArr;
                        this.m_connection.disconnect();
                        responseCode = i;
                        strArr = strArr2;
                        this.m_parent.dispatchCallback(this.m_request_id, responseCode, strArr, bArr);
                    }
                } catch (Exception unused2) {
                    i = responseCode;
                }
            } catch (Exception unused3) {
            }
            this.m_parent.dispatchCallback(this.m_request_id, responseCode, strArr, bArr);
        } finally {
            this.m_connection.disconnect();
        }
    }
}
