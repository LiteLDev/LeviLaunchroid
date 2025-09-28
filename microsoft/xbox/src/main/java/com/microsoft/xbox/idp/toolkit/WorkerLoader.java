package com.microsoft.xbox.idp.toolkit;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import androidx.loader.content.Loader;

/**
 * 07.01.2021
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */

public abstract class WorkerLoader<D> extends Loader<D> {
    public final Handler dispatcher = new Handler();
    public final Object lock = new Object();
    private final Worker<D> worker;
    public ResultListener<D> resultListener;
    private D result;

    public WorkerLoader(Context context, Worker<D> worker2) {
        super(context);
        this.worker = worker2;
    }

    public abstract boolean isDataReleased(D d);

    public abstract void releaseData(D d);

    public void onStartLoading() {
        D d = this.result;
        if (d != null) {
            deliverResult(d);
        }
        if (takeContentChanged() || this.result == null) {
            forceLoad();
        }
    }

    public void onStopLoading() {
        cancelLoadCompat();
    }

    public void onCanceled(D d) {
        if (d != null && !isDataReleased(d)) {
            releaseData(d);
        }
    }

    public void onForceLoad() {
        super.onForceLoad();
        cancelLoadCompat();
        synchronized (this.lock) {
            ResultListenerImpl resultListenerImpl = new ResultListenerImpl();
            this.resultListener = resultListenerImpl;
            this.worker.start(resultListenerImpl);
        }
    }

    public boolean onCancelLoad() {
        synchronized (this.lock) {
            if (this.resultListener == null) {
                return false;
            }
            this.worker.cancel();
            this.resultListener = null;
            return true;
        }
    }

    public void deliverResult(D d) {
        if (!isReset()) {
            D d2 = this.result;
            this.result = d;
            if (isStarted()) {
                super.deliverResult(d);
            }
            if (d2 != null && d2 != d && !isDataReleased(d2)) {
                releaseData(d2);
            }
        } else if (d != null) {
            releaseData(d);
        }
    }

    public void onReset() {
        cancelLoadCompat();
        D d = this.result;
        if (d != null && !isDataReleased(d)) {
            releaseData(this.result);
        }
        this.result = null;
    }

    private boolean cancelLoadCompat() {
        return Build.VERSION.SDK_INT < 16 ? onCancelLoad() : cancelLoad();
    }

    public interface ResultListener<D> {
        void onResult(D d);
    }

    public interface Worker<D> {
        void cancel();

        void start(ResultListener<D> resultListener);
    }

    private class ResultListenerImpl implements ResultListener<D> {
        private ResultListenerImpl() {
        }

        public void onResult(final D d) {
            synchronized (WorkerLoader.this.lock) {
                final boolean z = this != WorkerLoader.this.resultListener;
                WorkerLoader.this.dispatcher.post(() -> {
                    if (z) {
                        onCanceled(d);
                    } else {
                        deliverResult(d);
                    }
                });
            }
        }
    }
}
