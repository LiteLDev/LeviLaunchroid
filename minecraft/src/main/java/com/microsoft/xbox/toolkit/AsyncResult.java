package com.microsoft.xbox.toolkit;


public class AsyncResult<T> {
    private final XLEException exception;
    private final T result;
    private final Object sender;
    private final AsyncActionStatus status;

    public AsyncResult(T t, Object obj, XLEException xLEException) {
        this(t, obj, xLEException, xLEException == null ? AsyncActionStatus.SUCCESS : AsyncActionStatus.FAIL);
    }

    public AsyncResult(T t, Object obj, XLEException xLEException, AsyncActionStatus asyncActionStatus) {
        this.sender = obj;
        this.exception = xLEException;
        this.result = t;
        this.status = asyncActionStatus;
    }

    public Object getSender() {
        return this.sender;
    }

    public XLEException getException() {
        return this.exception;
    }

    public T getResult() {
        return this.result;
    }

    public AsyncActionStatus getStatus() {
        return this.status;
    }
}
