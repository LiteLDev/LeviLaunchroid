package com.microsoft.xbox.toolkit;

import java.util.ArrayList;
import java.util.HashSet;


public class MultiSelection<T> {
    private final HashSet<T> selection = new HashSet<>();

    public void add(T t) {
        this.selection.add(t);
    }

    public void remove(T t) {
        this.selection.remove(t);
    }

    public boolean contains(T t) {
        return this.selection.contains(t);
    }

    public boolean isEmpty() {
        return this.selection.isEmpty();
    }

    public ArrayList<T> toArrayList() {
        return new ArrayList<>(this.selection);
    }

    public void reset() {
        this.selection.clear();
    }

    public int size() {
        return this.selection.size();
    }
}
