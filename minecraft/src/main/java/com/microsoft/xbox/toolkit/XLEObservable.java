package com.microsoft.xbox.toolkit;

import java.util.ArrayList;
import java.util.HashSet;


public abstract class XLEObservable<T> {
    private final HashSet<XLEObserver<T>> data = new HashSet<>();

    public synchronized void addUniqueObserver(XLEObserver<T> xLEObserver) {
        if (!data.contains(xLEObserver)) {
            addObserver(xLEObserver);
        }
    }

    public synchronized void addObserver(XLEObserver<T> xLEObserver) {
        XLEAssert.assertTrue(Thread.currentThread() == ThreadManager.UIThread);
        data.add(xLEObserver);
    }

    public synchronized void removeObserver(XLEObserver<T> xLEObserver) {
        XLEAssert.assertTrue(Thread.currentThread() == ThreadManager.UIThread);
        data.remove(xLEObserver);
    }

    public synchronized void notifyObservers(AsyncResult<T> asyncResult) {
        XLEAssert.assertTrue(Thread.currentThread() == ThreadManager.UIThread);
        for (XLEObserver<T> xLEObserver : new ArrayList<>(data)) {
            xLEObserver.update(asyncResult);
        }
    }

    public synchronized void clearObserver() {
        XLEAssert.assertTrue(Thread.currentThread() == ThreadManager.UIThread);
        data.clear();
    }

    public synchronized ArrayList<XLEObserver<T>> getObservers() {
        return new ArrayList<>(data);
    }
}