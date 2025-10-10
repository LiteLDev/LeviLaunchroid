package com.microsoft.xbox.toolkit.network;

import com.microsoft.xbox.toolkit.XLEException;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;


public abstract class AbstractXLEHttpClient {
    public abstract HttpResponse execute(HttpUriRequest httpUriRequest) throws IOException;

    public XLEHttpStatusAndStream getHttpStatusAndStreamInternal(HttpUriRequest httpUriRequest, boolean z) throws XLEException {
        HttpEntity httpEntity;
        XLEHttpStatusAndStream xLEHttpStatusAndStream = new XLEHttpStatusAndStream();
        try {
            HttpResponse execute = execute(httpUriRequest);
            if (!(execute == null || execute.getStatusLine() == null)) {
                xLEHttpStatusAndStream.statusLine = execute.getStatusLine().toString();
                xLEHttpStatusAndStream.statusCode = execute.getStatusLine().getStatusCode();
            }
            if (!(execute == null || execute.getLastHeader("Location") == null)) {
                xLEHttpStatusAndStream.redirectUrl = execute.getLastHeader("Location").getValue();
            }
            if (execute != null) {
                xLEHttpStatusAndStream.headers = execute.getAllHeaders();
            }
            if (execute == null) {
                httpEntity = null;
            } else {
                httpEntity = execute.getEntity();
            }
            if (httpEntity != null) {
                xLEHttpStatusAndStream.stream = new ByteArrayInputStream(EntityUtils.toByteArray(httpEntity));
                httpEntity.consumeContent();
                Header firstHeader = execute.getFirstHeader(HTTP.CONTENT_ENCODING);
                if (firstHeader != null && firstHeader.getValue().equalsIgnoreCase("gzip")) {
                    xLEHttpStatusAndStream.stream = new GZIPInputStream(xLEHttpStatusAndStream.stream);
                }
            }
            return xLEHttpStatusAndStream;
        } catch (Exception e) {
            httpUriRequest.abort();
            if (xLEHttpStatusAndStream.stream != null) {
                xLEHttpStatusAndStream.close();
            }
            throw new XLEException(4, e);
        }
    }
}
