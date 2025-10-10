package com.microsoft.xbox.toolkit;


public interface ModelData<T> {
    void updateWithNewData(AsyncResult<T> asyncResult);
}
