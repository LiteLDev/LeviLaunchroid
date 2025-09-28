package com.microsoft.xbox.toolkit;

import android.annotation.SuppressLint;

import org.jetbrains.annotations.NotNull;

/**
 * 07.01.2021
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */

public class XLEException extends Exception {
    private final long errorCode;
    private final Object userObject;
    private boolean isHandled;

    public XLEException(long j) {
        this(j, null, null, null);
    }

    public XLEException(long j, String str) {
        this(j, str, null, null);
    }

    public XLEException(long j, Throwable th) {
        this(j, null, th, null);
    }

    public XLEException(long j, String str, Throwable th) {
        this(j, null, th, null);
    }

    public XLEException(long j, String str, Throwable th, Object obj) {
        super(str, th);
        this.errorCode = j;
        this.userObject = obj;
        this.isHandled = false;
    }

    public long getErrorCode() {
        return this.errorCode;
    }

    public Object getUserObject() {
        return this.userObject;
    }

    public boolean getIsHandled() {
        return this.isHandled;
    }

    public void setIsHandled(boolean z) {
        this.isHandled = z;
    }

    @SuppressLint("DefaultLocale")
    public @NotNull String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("XLEException ErrorCode: %d; ErrorMessage: %s \n\n", Long.valueOf(this.errorCode), getMessage()));
        if (getCause() != null) {
            sb.append(String.format("\t Cause ErrorMessage: %s, StackTrace: ", getCause().toString()));
            for (StackTraceElement stackTraceElement : getCause().getStackTrace()) {
                sb.append("\n\n \t " + stackTraceElement.toString());
            }
        }
        return sb.toString();
    }
}
