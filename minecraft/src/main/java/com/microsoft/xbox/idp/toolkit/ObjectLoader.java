package com.microsoft.xbox.idp.toolkit;

import android.content.Context;

import com.google.gson.Gson;
import com.microsoft.xbox.idp.util.HttpCall;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedInputStream;
import java.io.InputStreamReader;


public class ObjectLoader<T> extends WorkerLoader<ObjectLoader.Result<T>> {
    private static final String TAG = "ObjectLoader";

    public ObjectLoader(Context context, Class<T> cls, Gson gson, HttpCall httpCall) {
        this(context, null, null, cls, gson, httpCall);
    }

    public ObjectLoader(Context context, Cache cache, Object resultKey, Class<T> cls, Gson gson, HttpCall httpCall) {
        super(context, new MyWorker(cache, resultKey, cls, gson, httpCall));
    }

    public boolean isDataReleased(@NotNull Result<T> result) {
        return result.isReleased();
    }

    public void releaseData(@NotNull Result<T> result) {
        result.release();
    }

    public interface Cache {
        void clear();

        <T> Result<T> get(Object obj);

        <T> Result<T> put(Object obj, Result<T> result);

        <T> Result<T> remove(Object obj);
    }

    public static class Result<T> extends LoaderResult<T> {
        protected Result(T data) {
            super(data, null);
        }

        protected Result(HttpError error) {
            super(null, error);
        }

        public boolean isReleased() {
            return true;
        }

        public void release() {
        }
    }

    private static class MyWorker<T> implements WorkerLoader.Worker<Result<T>> {
        public final Cache cache;
        public final Class<T> cls;
        public final Gson gson;
        public final Object resultKey;
        private final HttpCall httpCall;

        private MyWorker(Cache cache2, Object resultKey2, Class<T> cls2, Gson gson2, HttpCall httpCall2) {
            cache = cache2;
            resultKey = resultKey2;
            cls = cls2;
            gson = gson2;
            httpCall = httpCall2;
        }

        public boolean hasCache() {
            return cache != null && resultKey != null;
        }

        public void start(final WorkerLoader.ResultListener<Result<T>> listener) {
            Result<T> r;
            if (hasCache()) {
                synchronized (cache) {
                    r = cache.get(resultKey);
                }
                if (r != null) {
                    listener.onResult(r);
                    return;
                }
            }
            httpCall.getResponseAsync((httpStatus, stream, httpHeaders) -> {
                if (httpStatus < 200 || httpStatus > 299) {
                    Result<T> result = new Result<>(new HttpError(httpStatus, httpStatus, stream));
                    if (hasCache()) {
                        synchronized (cache) {
                            cache.put(resultKey, result);
                        }
                    }
                    listener.onResult(result);
                } else if (cls == Void.class) {
                    listener.onResult(new Result(null));
                } else {
                    try (InputStreamReader r1 = new InputStreamReader(new BufferedInputStream(stream))) {
                        Result<T> result2 = new Result<>(gson.fromJson(r1, cls));
                        if (hasCache()) {
                            synchronized (cache) {
                                cache.put(resultKey, result2);
                            }
                        }
                        listener.onResult(result2);
                    }
                }
            });
        }

        public void cancel() {
        }
    }
}