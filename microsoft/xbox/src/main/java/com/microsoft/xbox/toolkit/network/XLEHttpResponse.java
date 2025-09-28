package com.microsoft.xbox.toolkit.network;

/**
 * 07.01.2021
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */

public class XLEHttpResponse {
    public int callbackPtr;
    public String[] headerArray;
    public int requestTypeTag;
    public byte[] responseBytes;
    public int statusCode;
}
