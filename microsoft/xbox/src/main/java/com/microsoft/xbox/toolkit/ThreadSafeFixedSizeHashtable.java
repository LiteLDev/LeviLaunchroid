package com.microsoft.xbox.toolkit;

import org.jetbrains.annotations.NotNull;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.PriorityQueue;

/**
 * 07.01.2021
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */

public class ThreadSafeFixedSizeHashtable<K, V> {
    private final PriorityQueue<ThreadSafeFixedSizeHashtable<K, V>.KeyTuple> fifo = new PriorityQueue<>();
    private final Hashtable<K, V> hashtable = new Hashtable<>();
    private final int maxSize;
    private final Object syncObject = new Object();
    private int count = 0;

    public ThreadSafeFixedSizeHashtable(int i) {
        this.maxSize = i;
        if (i <= 0) {
            throw new IllegalArgumentException();
        }
    }

    public void put(K k, V v) {
        if (k != null && v != null) {
            synchronized (syncObject) {
                if (!hashtable.containsKey(k)) {
                    count++;
                    fifo.add(new KeyTuple(k, count));
                    hashtable.put(k, v);
                    cleanupIfNecessary();
                }
            }
        }
    }

    public V get(K k) {
        V v;
        if (k == null) {
            return null;
        }
        synchronized (syncObject) {
            v = hashtable.get(k);
        }
        return v;
    }

    public void remove(K k) {
        if (k != null) {
            synchronized (syncObject) {
                if (hashtable.containsKey(k)) {
                    hashtable.remove(k);
                    ThreadSafeFixedSizeHashtable<K, V>.KeyTuple keyTuple = null;
                    Iterator<ThreadSafeFixedSizeHashtable<K, V>.KeyTuple> it = fifo.iterator();
                    while (true) {
                        if (!it.hasNext()) {
                            break;
                        }
                        ThreadSafeFixedSizeHashtable<K, V>.KeyTuple next = it.next();
                        if (next.key.equals(k)) {
                            keyTuple = next;
                            break;
                        }
                    }
                    if (keyTuple != null) {
                        fifo.remove(keyTuple);
                    }
                }
            }
        }
    }

    public Enumeration<V> elements() {
        return hashtable.elements();
    }

    public Enumeration<K> keys() {
        return hashtable.keys();
    }

    private void cleanupIfNecessary() {
        XLEAssert.assertTrue(hashtable.size() == fifo.size());
        while (hashtable.size() > maxSize) {
            hashtable.remove(fifo.remove().getKey());
            XLEAssert.assertTrue(hashtable.size() == fifo.size());
        }
    }

    public class KeyTuple implements Comparable<ThreadSafeFixedSizeHashtable<K, V>.KeyTuple> {
        private final K key;
        private int index = 0;

        public KeyTuple(K k, int i) {
            key = k;
            index = i;
        }

        public int compareTo(@NotNull ThreadSafeFixedSizeHashtable<K, V>.KeyTuple keyTuple) {
            return index - keyTuple.index;
        }

        public K getKey() {
            return this.key;
        }
    }
}