package com.microsoft.xbox.service.network.managers;

import android.text.TextUtils;

import com.google.gson.JsonObject;
import com.microsoft.xbox.toolkit.JavaUtil;
import com.microsoft.xbox.toolkit.StreamUtil;
import com.microsoft.xbox.toolkit.TimeMonitor;
import com.microsoft.xbox.toolkit.UrlUtil;
import com.microsoft.xbox.toolkit.XLEErrorCode;
import com.microsoft.xbox.toolkit.XLEException;
import com.microsoft.xbox.toolkit.network.HttpClientFactory;
import com.microsoft.xbox.toolkit.network.XLEHttpStatusAndStream;

import org.apache.http.Header;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ByteArrayEntity;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 07.01.2021
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */

public class ServiceCommon {
    public static final int MaxBIErrorParamLength = 2048;

    public static void AddWebHeaders(HttpUriRequest httpUriRequest, List<Header> list) {
        if (list != null) {
            for (Header addHeader : list) {
                httpUriRequest.addHeader(addHeader);
            }
        }
    }

    public static int deleteWithStatus(String str, List<Header> list) throws XLEException {
        URI encodedUri = UrlUtil.getEncodedUri(str);
        String uri = encodedUri.toString();
        new TimeMonitor();
        XLEHttpStatusAndStream excuteHttpRequest = excuteHttpRequest(new HttpDelete(encodedUri), uri, list, false, 0);
        excuteHttpRequest.close();
        return excuteHttpRequest.statusCode;
    }

    public static boolean delete(String str, List<Header> list) throws XLEException {
        int deleteWithStatus = deleteWithStatus(str, list);
        return deleteWithStatus == 200 || deleteWithStatus == 204;
    }

    public static boolean delete(String str, List<Header> list, String str2) throws XLEException {
        return JavaUtil.isNullOrEmpty(str2) ? delete(str, list) : delete(str, list, str2.getBytes(StandardCharsets.UTF_8));
    }

    public static boolean delete(String str, List<Header> list, byte[] bArr) throws XLEException {
        URI encodedUri = UrlUtil.getEncodedUri(str);
        String uri = encodedUri.toString();
        new TimeMonitor();
        HttpDeleteWithRequestBody httpDeleteWithRequestBody = new HttpDeleteWithRequestBody(encodedUri);
        if (bArr != null && bArr.length > 0) {
            try {
                httpDeleteWithRequestBody.setEntity(new ByteArrayEntity(bArr));
            } catch (Exception e) {
                throw new XLEException(5, e);
            }
        }
        boolean z = false;
        XLEHttpStatusAndStream excuteHttpRequest = excuteHttpRequest(httpDeleteWithRequestBody, uri, list, false, 0);
        if (excuteHttpRequest.statusCode == 200 || excuteHttpRequest.statusCode == 204) {
            z = true;
        }
        excuteHttpRequest.close();
        return z;
    }

    private static void ParseHttpResponseForStatus(String str, int i, String str2) throws XLEException {
        ParseHttpResponseForStatus(str, i, str2, null);
    }

    private static void ParseHttpResponseForStatus(String str, int i, String str2, InputStream inputStream) throws XLEException {
        if (i >= 200 && i < 400) {
            return;
        }
        if (i == -1) {
            throw new XLEException(3);
        } else if (i == 401 || i == 403) {
            throw new XLEException(XLEErrorCode.NOT_AUTHORIZED);
        } else if (i == 400) {
            if (inputStream == null) {
                throw new XLEException(15);
            }
            throw new XLEException(15, null, null, StreamUtil.ReadAsString(inputStream));
        } else if (i == 500) {
            throw new XLEException(13);
        } else if (i == 503) {
            throw new XLEException(18);
        } else if (i == 404) {
            throw new XLEException(21);
        } else {
            throw new XLEException(4);
        }
    }

    public static @NotNull XLEHttpStatusAndStream getStreamAndStatus(String str, List<Header> list) throws XLEException {
        XLEHttpStatusAndStream streamAndStatus = getStreamAndStatus(str, list, true, 0);
        return (streamAndStatus == null || JavaUtil.isNullOrEmpty(streamAndStatus.redirectUrl)) ? streamAndStatus : getStreamAndStatus(streamAndStatus.redirectUrl, list);
    }

    private static @NotNull XLEHttpStatusAndStream getStreamAndStatus(String str, List<Header> list, boolean z, int i) throws XLEException {
        return getStreamAndStatus(str, list, z, i, false);
    }

    private static @NotNull XLEHttpStatusAndStream getStreamAndStatus(String str, List<Header> list, boolean z, int i, boolean z2) throws XLEException {
        URI uri;
        if (z) {
            uri = UrlUtil.getEncodedUri(str);
        } else {
            try {
                uri = new URI(str);
            } catch (URISyntaxException unused) {
                uri = null;
            }
        }
        return excuteHttpRequest(new HttpGet(uri), uri.toString(), list, true, i, z2);
    }

    public static @NotNull XLEHttpStatusAndStream postStringWithStatus(String str, List<Header> list, @NotNull String str2) throws XLEException {
        return postStreamWithStatus(str, list, str2.getBytes(StandardCharsets.UTF_8));
    }

    public static @NotNull XLEHttpStatusAndStream postStreamWithStatus(String str, List<Header> list, byte[] bArr) throws XLEException {
        URI encodedUri = UrlUtil.getEncodedUri(str);
        String uri = encodedUri.toString();
        HttpPost httpPost = new HttpPost(encodedUri);
        if (bArr != null && bArr.length > 0) {
            try {
                httpPost.setEntity(new ByteArrayEntity(bArr));
            } catch (Exception e) {
                throw new XLEException(5, e);
            }
        }
        return excuteHttpRequest(httpPost, uri, list, false, 0);
    }

    public static @NotNull XLEHttpStatusAndStream putStringWithStatus(String str, List<Header> list, @NotNull String str2) throws XLEException {
        return putStreamWithStatus(str, list, str2.getBytes(StandardCharsets.UTF_8));
    }

    public static @NotNull XLEHttpStatusAndStream putStreamWithStatus(String str, List<Header> list, byte[] bArr) throws XLEException {
        URI encodedUri = UrlUtil.getEncodedUri(str);
        String uri = encodedUri.toString();
        HttpPut httpPut = new HttpPut(encodedUri);
        if (bArr != null && bArr.length > 0) {
            try {
                httpPut.setEntity(new ByteArrayEntity(bArr));
            } catch (Exception e) {
                throw new XLEException(5, e);
            }
        }
        return excuteHttpRequest(httpPut, uri, list, false, 0);
    }

    private static @NotNull XLEHttpStatusAndStream excuteHttpRequest(HttpUriRequest httpUriRequest, String str, List<Header> list, boolean z, int i) throws XLEException {
        return excuteHttpRequest(httpUriRequest, str, list, z, i, false);
    }

    private static @NotNull XLEHttpStatusAndStream excuteHttpRequest(HttpUriRequest httpUriRequest, String str, List<Header> list, boolean z, int i, boolean z2) throws XLEException {
        int i2;
        String str2;
        AddWebHeaders(httpUriRequest, list);
        new XLEHttpStatusAndStream();
        XLEHttpStatusAndStream httpStatusAndStreamInternal = HttpClientFactory.networkOperationsFactory.getHttpClient(i).getHttpStatusAndStreamInternal(httpUriRequest, true);
        if (!z2) {
            try {
                ParseHttpResponseForStatus(str, httpStatusAndStreamInternal.statusCode, httpStatusAndStreamInternal.statusLine);
            } catch (XLEException e) {
                JsonObject jsonObject = new JsonObject();
                JsonObject jsonObject2 = new JsonObject();
                if (httpStatusAndStreamInternal == null) {
                    i2 = 0;
                } else {
                    i2 = httpStatusAndStreamInternal.statusCode;
                }
                if (httpUriRequest != null) {
                    httpUriRequest.getMethod();
                }
                String str3 = "";
                if (httpStatusAndStreamInternal == null || TextUtils.isEmpty(httpStatusAndStreamInternal.statusLine)) {
                    str2 = str3;
                } else {
                    int length = httpStatusAndStreamInternal.statusLine.length();
                    str2 = httpStatusAndStreamInternal.statusLine;
                    if (length > 2048) {
                        str2 = str2.substring(0, 2048);
                    }
                }
                if (!(httpUriRequest == null || httpUriRequest.getURI() == null)) {
                    str3 = httpUriRequest.getURI().toString();
                }
                if (str3.length() > 2048) {
                    str3 = str3.substring(0, 2048);
                }
                jsonObject.addProperty("Request", str3);
                jsonObject2.addProperty("code", i2);
                jsonObject2.addProperty("description", str2);
                jsonObject.add("Response", jsonObject2);
                throw e;
            }
        } else {
            ParseHttpResponseForStatus(str, httpStatusAndStreamInternal.statusCode, httpStatusAndStreamInternal.statusLine, httpStatusAndStreamInternal.stream);
        }
        if (httpStatusAndStreamInternal.stream != null || !z) {
            return httpStatusAndStreamInternal;
        }
        throw new XLEException(7);
    }
}
