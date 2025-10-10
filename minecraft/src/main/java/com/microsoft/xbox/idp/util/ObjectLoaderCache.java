package com.microsoft.xbox.idp.util;

import com.microsoft.xbox.idp.toolkit.ObjectLoader;

import java.util.HashMap;


public class ObjectLoaderCache implements ObjectLoader.Cache {
    private final HashMap<Object, ObjectLoader.Result<?>> map = new HashMap<>();

    public <T> ObjectLoader.Result<T> get(Object key) {
        return (ObjectLoader.Result<T>) map.get(key);
    }

    public <T> ObjectLoader.Result<T> put(Object key, ObjectLoader.Result<T> value) {
        return (ObjectLoader.Result<T>) map.put(key, value);
    }

    public <T> ObjectLoader.Result<T> remove(Object key) {
        return (ObjectLoader.Result<T>) map.remove(key);
    }

    public void clear() {
        map.clear();
    }
}