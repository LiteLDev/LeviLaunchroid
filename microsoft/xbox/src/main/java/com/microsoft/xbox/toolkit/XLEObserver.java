package com.microsoft.xbox.toolkit;

/**
 * 07.01.2021
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */

public interface XLEObserver<T> {
    void update(AsyncResult<T> asyncResult);
}
