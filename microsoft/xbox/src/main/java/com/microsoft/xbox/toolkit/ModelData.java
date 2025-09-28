package com.microsoft.xbox.toolkit;

/**
 * 07.01.2021
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */

public interface ModelData<T> {
    void updateWithNewData(AsyncResult<T> asyncResult);
}
