package com.microsoft.xbox.toolkit;


public interface XLEObserver<T> {
    void update(AsyncResult<T> asyncResult);
}
