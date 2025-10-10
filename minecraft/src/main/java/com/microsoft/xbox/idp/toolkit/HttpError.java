package com.microsoft.xbox.idp.toolkit;

import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.util.Scanner;


public class HttpError {
    private static final String INPUT_START_TOKEN = "\\A";
    private final int errorCode;
    private final String errorMessage;
    private final int httpStatus;

    public HttpError(int i, int i2, String str) {
        errorCode = i;
        httpStatus = i2;
        errorMessage = str;
    }

    public HttpError(int i, int i2, InputStream inputStream) {
        this.errorCode = i;
        this.httpStatus = i2;
        Scanner useDelimiter = new Scanner(inputStream).useDelimiter(INPUT_START_TOKEN);
        this.errorMessage = useDelimiter.hasNext() ? useDelimiter.next() : "";
    }

    public int getErrorCode() {
        return errorCode;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public @NotNull String toString() {
        return "errorCode: " +
                errorCode +
                ", httpStatus: " +
                httpStatus +
                ", errorMessage: " +
                errorMessage;
    }
}
