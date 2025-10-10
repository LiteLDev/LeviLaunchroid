package com.microsoft.xbox.toolkit.network;

import org.apache.http.Header;

import java.io.InputStream;


public class XLEHttpStatusAndStream {
    public Header[] headers = new Header[0];
    public String redirectUrl = null;
    public int statusCode = -1;
    public String statusLine = null;
    public InputStream stream = null;

    public void close() {
        InputStream inputStream = this.stream;
        if (inputStream != null) {
            try {
                inputStream.close();
                this.stream = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
