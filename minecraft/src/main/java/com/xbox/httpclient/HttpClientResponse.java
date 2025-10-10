package com.xbox.httpclient;

import okhttp3.Response;
import okio.Okio;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.OutputStream;


public class HttpClientResponse {
    private final long callHandle;
    private final Response response;

    public HttpClientResponse(long j, Response response) {
        callHandle = j;
        this.response = response;
    }

    public int getNumHeaders() {
        return this.response.headers().size();
    }

    public String getHeaderNameAtIndex(int i) {
        if (i < 0 || i >= response.headers().size()) {
            return null;
        }
        return response.headers().name(i);
    }

    public String getHeaderValueAtIndex(int i) {
        if (i < 0 || i >= response.headers().size()) {
            return null;
        }
        return response.headers().value(i);
    }

    public void getResponseBodyBytes() throws IOException {
        try {
            response.body().source().readAll(Okio.sink(new NativeOutputStream(callHandle)));
        } catch (IOException unused) {
        } catch (Throwable th) {
            response.close();
            throw th;
        }
        response.close();
    }

    public int getResponseCode() {
        return response.code();
    }

    private final class NativeOutputStream extends OutputStream {
        private final long callHandle;

        public NativeOutputStream(long callHandle) {
            this.callHandle = callHandle;
        }

        private native void nativeWrite(long callHandle, byte[] b, int off, int len) throws IOException;

        @Override
        public void write(byte[] bArr) throws IOException {
            write(bArr, 0, bArr.length);
        }

        @Override
        public void write(@NotNull byte[] b, int off, int len) throws IOException {
            b.getClass();
            if (off < 0 || len < 0 || off + len > b.length) {
                throw new IndexOutOfBoundsException();
            }
            nativeWrite(callHandle, b, off, len);
        }

        @Override
        public void write(int b) throws IOException {
            write(new byte[]{(byte) b});
        }
    }
}
