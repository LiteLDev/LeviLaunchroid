package com.xbox.httpclient;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.BufferedSink;
import okio.Okio;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;


public final class HttpClientRequestBody extends RequestBody {
    private final long callHandle;
    private final long contentLength;
    private final MediaType contentType;

    public HttpClientRequestBody(long callHandle, String contentType, long contentLength) {
        this.callHandle = callHandle;
        this.contentType = contentType != null ? MediaType.parse(contentType) : null;
        this.contentLength = contentLength;
    }

    @Override
    public MediaType contentType() {
        return contentType;
    }

    @Override
    public long contentLength() {
        return contentLength;
    }

    @Override
    public void writeTo(@NotNull BufferedSink sink) throws IOException {
        sink.writeAll(Okio.source(new NativeInputStream(callHandle)));
    }

    private final class NativeInputStream extends InputStream {
        private final long callHandle;
        private long offset = 0;

        public NativeInputStream(long callHandle) {
            this.callHandle = callHandle;
        }

        private native int nativeRead(long callHandle, long offset, byte[] buffer, int bufferOffset, int byteCount) throws IOException;

        @Override
        public int read() throws IOException {
            byte[] buffer = new byte[1];
            int result = read(buffer);
            return result == -1 ? -1 : buffer[0] & 0xFF;
        }

        @Override
        public int read(byte[] buffer) throws IOException {
            return read(buffer, 0, buffer.length);
        }

        @Override
        public int read(byte[] buffer, int bufferOffset, int byteCount) throws IOException {
            if (buffer == null) {
                throw new NullPointerException("Buffer cannot be null");
            }
            if (bufferOffset < 0 || byteCount < 0 || bufferOffset + byteCount > buffer.length) {
                throw new IndexOutOfBoundsException();
            }
            if (byteCount == 0) {
                return 0;
            }

            int bytesRead = nativeRead(callHandle, offset, buffer, bufferOffset, byteCount);
            if (bytesRead == -1) {
                return -1;
            }

            offset += bytesRead;
            return bytesRead;
        }

        @Override
        public long skip(long byteCount) throws IOException {
            offset += byteCount;
            return byteCount;
        }
    }
}