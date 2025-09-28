package com.xbox.httpclient;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import okhttp3.*;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.UnknownHostException;

/**
 * 13.08.2022
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */
public class HttpClientRequest {
    private static final byte[] NO_BODY = new byte[0];
    private static final OkHttpClient OK_CLIENT = new OkHttpClient.Builder().retryOnConnectionFailure(false).build();
    private final Context appContext;
    private Request.Builder requestBuilder = new Request.Builder();

    public HttpClientRequest(Context context) {
        appContext = context;
    }

    private native void OnRequestCompleted(long requestId, HttpClientResponse httpClientResponse);

    private native void OnRequestFailed(long requestId, String exceptionType, String stackTrace, String networkInfo, boolean isUnknownHost);

    public void setHttpUrl(String url) {
        requestBuilder = requestBuilder.url(url);
    }

    public void setHttpMethodAndBody(String method, long contentLength, String contentType, long bodyOffset) {
        RequestBody requestBody;
        if (bodyOffset == 0) {
            requestBody = null;
            if (HttpPost.METHOD_NAME.equals(method) || HttpPut.METHOD_NAME.equals(method)) {
                MediaType mediaType = contentType != null ? MediaType.parse(contentType) : null;
                requestBody = RequestBody.create(NO_BODY, mediaType);
            }
        } else {
            requestBody = new HttpClientRequestBody(contentLength, contentType, bodyOffset);
        }
        requestBuilder.method(method, requestBody);
    }

    public void setHttpHeader(String name, String value) {
        requestBuilder = requestBuilder.addHeader(name, value);
    }

    public void doRequestAsync(final long requestId) {
        OK_CLIENT.newCall(requestBuilder.build()).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException exception) {
                StringWriter stringWriter = new StringWriter();
                exception.printStackTrace(new PrintWriter(stringWriter));
                OnRequestFailed(
                        requestId,
                        exception.getClass().getCanonicalName(),
                        stringWriter.toString(),
                        GetAllNetworksInfo(),
                        exception instanceof UnknownHostException
                );
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) {
                OnRequestCompleted(requestId, new HttpClientResponse(requestId, response));
            }
        });
    }

    @NotNull
    private String GetAllNetworksInfo() {
        ConnectivityManager connectivityManager = (ConnectivityManager) appContext.getSystemService("connectivity");
        StringBuilder sb = new StringBuilder("Has default proxy: ");
        sb.append(connectivityManager.getDefaultProxy() != null)
                .append("\nHas active network: ")
                .append(connectivityManager.getActiveNetwork() != null)
                .append('\n');

        Network[] allNetworks = connectivityManager.getAllNetworks();
        for (int i = 0; i < allNetworks.length; i++) {
            if (i > 0) {
                sb.append("\n");
            }
            sb.append(NetworkObserver.NetworkDetails.getNetworkDetails(allNetworks[i], connectivityManager));
        }
        return sb.toString();
    }
}