package com.microsoft.xbox.service.network.managers;

import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPost;

import java.net.URI;

/**
 * 07.01.2021
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */

public class HttpDeleteWithRequestBody extends HttpPost {
    public HttpDeleteWithRequestBody(URI uri) {
        super(uri);
    }

    public String getMethod() {
        return HttpDelete.METHOD_NAME;
    }
}
