package com.microsoft.xbox.toolkit;

import com.microsoft.xbox.toolkit.network.IDataLoaderRunnable;
import com.microsoft.xbox.xle.app.XLEUtil;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Date;


public class DataLoadUtil {
    @NotNull
    public static <T> NetworkAsyncTask StartLoadFromUI(boolean forceLoad, long lifetime, Date lastRefreshedTime, SingleEntryLoadingStatus loadingStatus, IDataLoaderRunnable<T> runner) {
        final long j = lifetime;
        final Date date = lastRefreshedTime;
        final SingleEntryLoadingStatus singleEntryLoadingStatus = loadingStatus;
        final IDataLoaderRunnable<T> iDataLoaderRunnable = runner;
        NetworkAsyncTask<T> task = new NetworkAsyncTask<T>() {
            public boolean checkShouldExecute() {
                return forceLoad;
            }

            public void onNoAction() {
            }

            public void onPreExecute() {
            }

            public void onPostExecute(T t) {
            }

            @Nullable
            @Contract(pure = true)
            public T onError() {
                return null;
            }

            public T loadDataInBackground() {
                return DataLoadUtil.Load(forceLoad, j, date, singleEntryLoadingStatus, iDataLoaderRunnable).getResult();
            }
        };
        task.execute();
        return task;
    }

    @NotNull
    public static <T> AsyncResult<T> Load(boolean forceLoad, long lifetime, Date lastRefreshedTime, SingleEntryLoadingStatus loadingStatus, IDataLoaderRunnable<T> runner) {
        XLEAssert.assertNotNull(loadingStatus);
        XLEAssert.assertNotNull(runner);
        XLEAssert.assertIsNotUIThread();
        SingleEntryLoadingStatus.WaitResult waitResult = loadingStatus.waitForNotLoading();
        if (waitResult.waited) {
            XLEException exception = waitResult.error;
            if (exception == null) {
                return safeReturnResult(null, runner, null, AsyncActionStatus.NO_OP_SUCCESS);
            }
            return safeReturnResult(null, runner, exception, AsyncActionStatus.NO_OP_FAIL);
        } else if (XLEUtil.shouldRefresh(lastRefreshedTime, lifetime) || forceLoad) {
            final IDataLoaderRunnable<T> iDataLoaderRunnable = runner;
            ThreadManager.UIThreadSend(iDataLoaderRunnable::onPreExecute);
            XLEException error = null;
            int retryCount = runner.getShouldRetryCountOnTokenError();
            int i = 0;
            while (true) {
                if (i > retryCount) {
                    break;
                }
                try {
                    T result = runner.buildData();
                    postExecute(result, runner, null, AsyncActionStatus.SUCCESS);
                    loadingStatus.setSuccess();
                    return new AsyncResult<>(result, runner, null, AsyncActionStatus.SUCCESS);
                } catch (XLEException xex) {
                    error = xex;
                    if (xex.getErrorCode() == XLEErrorCode.NOT_AUTHORIZED) {
                        i++;
                    } else if (xex.getErrorCode() == XLEErrorCode.INVALID_ACCESS_TOKEN) {
                    }
                } catch (Exception ex) {
                    error = new XLEException(runner.getDefaultErrorCode(), ex);
                }
            }
            loadingStatus.setFailed(error);
            return safeReturnResult(null, runner, error, AsyncActionStatus.FAIL);
        } else {
            loadingStatus.setSuccess();
            return safeReturnResult(null, runner, null, AsyncActionStatus.NO_CHANGE);
        }
    }

    @NotNull
    @Contract("_, _, _, _ -> new")
    private static <T> AsyncResult<T> safeReturnResult(T result, IDataLoaderRunnable<T> runner, XLEException error, AsyncActionStatus status) {
        postExecute(result, runner, error, status);
        return new AsyncResult<>(result, runner, error, status);
    }

    private static <T> void postExecute(final T result, final IDataLoaderRunnable<T> runner, final XLEException error, final AsyncActionStatus status) {
        ThreadManager.UIThreadSend(() -> runner.onPostExcute(new AsyncResult(result, runner, error, status)));
    }
}