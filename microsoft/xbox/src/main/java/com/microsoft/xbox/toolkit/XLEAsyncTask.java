package com.microsoft.xbox.toolkit;

import com.microsoft.xbox.toolkit.network.XLEThreadPool;

import org.jetbrains.annotations.NotNull;

public abstract class XLEAsyncTask<Result> {
    public XLEAsyncTask chainedTask = null;
    protected boolean cancelled = false;
    protected boolean isBusy = false;
    private Runnable doBackgroundAndPostExecuteRunnable = null;
    private XLEThreadPool threadPool = null;

    public XLEAsyncTask(XLEThreadPool xLEThreadPool) {
        this.threadPool = xLEThreadPool;
        this.doBackgroundAndPostExecuteRunnable = () -> {
            final Object doInBackground = !cancelled ? doInBackground() : null;
            ThreadManager.UIThreadPost(() -> {
                isBusy = false;
                if (!cancelled) {
                    onPostExecute((Result) doInBackground);
                    if (chainedTask != null) {
                        chainedTask.execute();
                    }
                }
            });
        };
    }

    public static void executeAll(@NotNull XLEAsyncTask ... xLEAsyncTaskArr) {
        if (xLEAsyncTaskArr.length > 0) {
            int i = 0;
            while (i < xLEAsyncTaskArr.length - 1) {
                XLEAsyncTask xLEAsyncTask = xLEAsyncTaskArr[i];
                i++;
                xLEAsyncTask.chainedTask = xLEAsyncTaskArr[i];
            }
            xLEAsyncTaskArr[0].execute();
        }
    }

    public abstract Result doInBackground();

    public abstract void onPostExecute(Result result);

    public abstract void onPreExecute();

    public void cancel() {
        XLEAssert.assertTrue(Thread.currentThread() == ThreadManager.UIThread);
        this.cancelled = true;
    }

    public void execute() {
        XLEAssert.assertTrue(Thread.currentThread() == ThreadManager.UIThread);
        this.cancelled = false;
        this.isBusy = true;
        onPreExecute();
        executeBackground();
    }

    public boolean getIsBusy() {
        return this.isBusy && !this.cancelled;
    }

    public void executeBackground() {
        this.cancelled = false;
        this.threadPool.run(this.doBackgroundAndPostExecuteRunnable);
    }
}
