package com.microsoft.xbox.idp.toolkit;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.net.URL;

/**
 * 07.01.2021
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */

public class BitmapLoader extends WorkerLoader<BitmapLoader.Result> {
    public static final String TAG = BitmapLoader.class.getSimpleName();

    public BitmapLoader(Context context, String urlString) {
        this(context, null, null, urlString);
    }

    public BitmapLoader(Context context, Cache cache, Object resultKey, String urlString) {
        super(context, new MyWorker(cache, resultKey, urlString));
    }

    public boolean isDataReleased(@NotNull Result result) {
        return result.isReleased();
    }

    public void releaseData(@NotNull Result result) {
        result.release();
    }

    public interface Cache {
        void clear();

        Bitmap get(Object obj);

        Bitmap put(Object obj, Bitmap bitmap);

        Bitmap remove(Object obj);
    }

    public static class Result extends LoaderResult<Bitmap> {
        protected Result(Bitmap data) {
            super(data, null);
        }

        protected Result(Exception exception) {
            super(exception);
        }

        public boolean isReleased() {
            return hasData() && (getData()).isRecycled();
        }

        public void release() {
            if (hasData()) {
                (getData()).recycle();
            }
        }
    }

    private static class MyWorker implements WorkerLoader.Worker<Result> {
        static final boolean assertionsDisabled = (!BitmapLoader.class.desiredAssertionStatus());
        public final Cache cache;
        public final Object resultKey;
        public final String urlString;

        private MyWorker(Cache cache2, Object resultKey2, String urlString2) {
            if (assertionsDisabled || urlString2 != null) {
                cache = cache2;
                resultKey = resultKey2;
                urlString = urlString2;
                return;
            }
            throw new AssertionError();
        }

        public boolean hasCache() {
            return cache != null && resultKey != null;
        }

        public void start(final WorkerLoader.ResultListener<Result> listener) {
            final Bitmap data;
            if (hasCache()) {
                synchronized (cache) {
                    data = cache.get(this.resultKey);
                }
                if (data != null) {
                    Log.d(BitmapLoader.TAG, "Successfully retrieved Bitmap from BitmapLoader.Cache");
                    new Thread(() -> listener.onResult(new Result(data))).start();
                    return;
                }
            }
            new Thread(() -> {
                InputStream stream;
                try {
                    URL url = new URL(urlString);
                    Log.d(BitmapLoader.TAG, "url created: " + url);
                    stream = url.openStream();
                    Bitmap image = BitmapFactory.decodeStream(stream);
                    if (hasCache()) {
                        synchronized (cache) {
                            Log.d(BitmapLoader.TAG, "Caching retrieved bitmap");
                            cache.put(resultKey, image);
                        }
                    }
                    listener.onResult(new Result(image));
                    stream.close();
                } catch (Exception e) {
                    listener.onResult(new Result(e));
                }
            }).start();
        }

        public void cancel() {
        }
    }
}