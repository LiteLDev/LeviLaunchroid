package com.xbox.httpclient;

import okhttp3.*;
import okio.ByteString;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

/**
 * 29.03.2023
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */
public final class HttpClientWebSocket extends WebSocketListener {
    private static final OkHttpClient OK_CLIENT = new OkHttpClient();
    private final long owner;
    private final Headers.Builder headers = new Headers.Builder();
    private WebSocket socket;
    private long pingInterval = 0;

    HttpClientWebSocket(long j) {
        this.owner = j;
    }

    public native void onBinaryMessage(ByteBuffer byteBuffer);

    public native void onClose(int i);

    @Override
    public void onClosing(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
    }

    public native void onFailure(int responseCode);

    public native void onMessage(String text);

    public native void onOpen();

    public void setPingInterval(long j) {
        this.pingInterval = j;
    }

    public void addHeader(String str, String str2) {
        this.headers.add(str, str2);
    }

    public void connect(String url, String header) {
        addHeader("Sec-WebSocket-Protocol", header);
        this.socket = OK_CLIENT.newBuilder().pingInterval(pingInterval, TimeUnit.SECONDS).build().newWebSocket(new Request.Builder().url(url).headers(headers.build()).build(), this);
    }

    public boolean sendMessage(String str) {
        return this.socket.send(str);
    }

    public boolean sendBinaryMessage(ByteBuffer byteBuffer) {
        return this.socket.send(ByteString.of(byteBuffer));
    }

    public void disconnect(int code) {
        socket.close(code, null);
    }

    @Override
    public void onOpen(@NotNull WebSocket webSocket, @NotNull Response response) {
        onOpen();
    }

    @Override
    public void onFailure(@NotNull WebSocket webSocket, @NotNull Throwable th, Response response) {
        onFailure(response != null ? response.code() : -1);
    }

    @Override
    public void onClosed(@NotNull WebSocket webSocket, int i, @NotNull String str) {
        onClose(i);
    }

    @Override
    public void onMessage(@NotNull WebSocket webSocket, @NotNull String text) {
        onMessage(text);
    }

    @Override
    public void onMessage(@NotNull WebSocket webSocket, @NotNull ByteString byteString) {
        ByteBuffer byteBufferAllocateDirect = ByteBuffer.allocateDirect(byteString.size());
        byteBufferAllocateDirect.put(byteString.toByteArray());
        byteBufferAllocateDirect.position(0);
        onBinaryMessage(byteBufferAllocateDirect);
    }

    protected void finalize() {
        socket.cancel();
    }
}
