package com.microsoft.playfab.utilities.multiplayer;

import java.io.IOException;
import okhttp3.Response;

/**
 * @author <a href="https://github.com/RadiantByte">RadiantByte</a>
 */

final class HttpResponseMultiplayer {
    private Response response;

    public HttpResponseMultiplayer(Response response) {
        this.response = response;
    }

    public int getNumHeaders() {
        return this.response.headers().size();
    }

    public String getHeaderNameAtIndex(int i) {
        if (i < 0 || i >= this.response.headers().size()) {
            return null;
        }
        return this.response.headers().name(i);
    }

    public String getHeaderValueAtIndex(int i) {
        if (i < 0 || i >= this.response.headers().size()) {
            return null;
        }
        return this.response.headers().value(i);
    }

    public String getResponseBody() {
        try {
            return this.response.body().string();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public int getStatusCode() {
        return this.response.code();
    }

    public String getStatusMessage() {
        return this.response.message();
    }

    public boolean isSuccessful() {
        return this.response.isSuccessful();
    }
}
