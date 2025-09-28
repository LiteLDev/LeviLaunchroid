package com.microsoft.xbox.idp.util;

import java.io.InputStream;

/**
 * 07.01.2021
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */

public class HttpCall {
    private final long id;

    public HttpCall(String str, String str2, String str3) {
        this.id = create(str, str2, str3, true);
    }

    public HttpCall(String str, String str2, String str3, boolean z) {
        this.id = create(str, str2, str3, z);
    }

    private static native long create(String str, String str2, String str3, boolean z);

    private static native void delete(long j);

    public native void getResponseAsync(Callback callback);

    public native void setContentTypeHeaderValue(String str);

    public native void setCustomHeader(String str, String str2);

    public native void setLongHttpCall(boolean z);

    public native void setRequestBody(String str);

    public native void setRequestBody(byte[] bArr);

    public native void setRetryAllowed(boolean z);

    public native void setXboxContractVersionHeaderValue(String str);

    public void finalize() throws Throwable {
        delete(this.id);
        super.finalize();
    }

    public interface Callback {
        void processResponse(int i, InputStream inputStream, HttpHeaders httpHeaders) throws Exception;
    }
}
