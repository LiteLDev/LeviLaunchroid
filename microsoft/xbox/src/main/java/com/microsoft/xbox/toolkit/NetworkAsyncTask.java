package com.microsoft.xbox.toolkit;

import com.microsoft.xbox.toolkit.network.XLEThreadPool;

/**
 * 07.01.2021
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */

public abstract class NetworkAsyncTask<T> extends XLEAsyncTask<T> {
    protected boolean forceLoad = true;
    private boolean shouldExecute = true;

    public NetworkAsyncTask() {
        super(XLEThreadPool.networkOperationsThreadPool);
    }

    public NetworkAsyncTask(XLEThreadPool xLEThreadPool) {
        super(XLEThreadPool.networkOperationsThreadPool);
    }

    public abstract boolean checkShouldExecute();

    public abstract T loadDataInBackground();

    public abstract T onError();

    public abstract void onNoAction();

    public void load(boolean z) {
        this.forceLoad = z;
        execute();
    }

    public final T doInBackground() {
        try {
            return loadDataInBackground();
        } catch (Exception unused) {
            return onError();
        }
    }

    public void execute() {
        XLEAssert.assertTrue(Thread.currentThread() == ThreadManager.UIThread);
        boolean z = this.cancelled;
        boolean checkShouldExecute = checkShouldExecute();
        this.shouldExecute = checkShouldExecute;
        if (checkShouldExecute || this.forceLoad) {
            this.isBusy = true;
            onPreExecute();
            super.executeBackground();
            return;
        }
        onNoAction();
        this.isBusy = false;
    }
}
