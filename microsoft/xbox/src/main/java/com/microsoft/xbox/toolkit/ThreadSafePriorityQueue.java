package com.microsoft.xbox.toolkit;

import java.util.HashSet;
import java.util.PriorityQueue;

/**
 * 07.01.2021
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */

public class ThreadSafePriorityQueue<T> {
    private final HashSet<T> hashSet = new HashSet<>();
    private final PriorityQueue<T> queue = new PriorityQueue<>();
    private final Object syncObject = new Object();

    public void push(T t) {
        synchronized (this.syncObject) {
            if (!this.hashSet.contains(t)) {
                this.queue.add(t);
                this.hashSet.add(t);
                this.syncObject.notifyAll();
            }
        }
    }

    public T pop() {
        T t = null;
        try {
            synchronized (this.syncObject) {
                while (this.queue.isEmpty()) {
                    this.syncObject.wait();
                }
                t = this.queue.remove();
                this.hashSet.remove(t);
            }
        } catch (InterruptedException unused) {
        }
        return t;
    }
}
