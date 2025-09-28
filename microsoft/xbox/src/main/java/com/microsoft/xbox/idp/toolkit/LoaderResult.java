package com.microsoft.xbox.idp.toolkit;

/**
 * 07.01.2021
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */

public abstract class LoaderResult<T> {
    private final T data;
    private final HttpError error;
    private final Exception exception;

    protected LoaderResult(T t, HttpError httpError) {
        this.data = t;
        this.error = httpError;
        this.exception = null;
    }

    protected LoaderResult(Exception exc) {
        this.data = null;
        this.error = null;
        this.exception = exc;
    }

    public abstract boolean isReleased();

    public abstract void release();

    public T getData() {
        return this.data;
    }

    public HttpError getError() {
        return this.error;
    }

    public Exception getException() {
        return this.exception;
    }

    public boolean hasData() {
        return this.data != null;
    }

    public boolean hasError() {
        return this.error != null;
    }

    public boolean hasException() {
        return this.exception != null;
    }
}
