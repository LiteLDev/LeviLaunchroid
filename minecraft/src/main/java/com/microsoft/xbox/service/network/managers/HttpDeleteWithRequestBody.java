package com.microsoft.xbox.service.network.managers;

import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPost;

import java.net.URI;


public class HttpDeleteWithRequestBody extends HttpPost {
    public HttpDeleteWithRequestBody(URI uri) {
        super(uri);
    }

    public String getMethod() {
        return HttpDelete.METHOD_NAME;
    }
}
