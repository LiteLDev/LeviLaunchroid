package com.microsoft.playfab.utilities.multiplayer;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.TimeUnit;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * @author <a href="https://github.com/RadiantByte">RadiantByte</a>
 */

final class HttpRequestMultiplayer {
    private static OkHttpClient client = new OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).readTimeout(10, TimeUnit.SECONDS).writeTimeout(10, TimeUnit.SECONDS).build();
    private byte[] context;
    private Request.Builder requestBuilder = new Request.Builder();

    public native void onRequestComplete(HttpResponseMultiplayer httpResponseMultiplayer, int i, byte[] bArr, byte[] bArr2);

    public native void onRequestFailure(byte[] bArr);

    public native void printErrorMessage(byte[] bArr);

    public static HttpRequestMultiplayer createHttpRequest() {
        return new HttpRequestMultiplayer();
    }

    public void setHttpUrl(String str) {
        this.requestBuilder.url(str);
    }

    public void setHttpMethodAndBody(String str, String str2, byte[] bArr) {
        if (bArr == null) {
            this.requestBuilder.method(str, null);
        } else {
            this.requestBuilder.method(str, RequestBody.create(MediaType.parse(str2), bArr));
        }
    }

    public void setHttpHeader(String str, String str2) {
        this.requestBuilder.addHeader(str, str2);
    }

    public void setContext(byte[] bArr) {
        this.context = bArr;
    }

    public void doAsyncRequest() {
        try {
            client.newCall(this.requestBuilder.build()).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException iOException) {
                    HttpRequestMultiplayer httpRequestMultiplayer = HttpRequestMultiplayer.this;
                    httpRequestMultiplayer.onRequestFailure(httpRequestMultiplayer.context);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    HttpRequestMultiplayer.this.onRequestComplete(new HttpResponseMultiplayer(response), response.code(), response.body().bytes(), HttpRequestMultiplayer.this.context);
                }
            });
        } catch (Exception e) {
            onRequestFailure(this.context);
            StringWriter stringWriter = new StringWriter();
            e.printStackTrace(new PrintWriter(stringWriter));
            printErrorMessage(stringWriter.toString().getBytes());
        }
    }
}
