package com.microsoft.xbox.service.model;

import com.microsoft.xbox.toolkit.AsyncActionStatus;
import com.microsoft.xbox.toolkit.AsyncResult;
import com.microsoft.xbox.toolkit.DataLoadUtil;
import com.microsoft.xbox.toolkit.ModelData;
import com.microsoft.xbox.toolkit.SingleEntryLoadingStatus;
import com.microsoft.xbox.toolkit.XLEAssert;
import com.microsoft.xbox.toolkit.XLEObservable;
import com.microsoft.xbox.toolkit.network.IDataLoaderRunnable;
import com.microsoft.xbox.xle.app.XLEUtil;

import org.jetbrains.annotations.NotNull;

import java.util.Date;

/**
 * 07.01.2021
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */

public abstract class ModelBase<T> extends XLEObservable<UpdateData> implements ModelData<T> {
    protected static final long MilliSecondsInADay = 86400000;
    protected static final long MilliSecondsInAnHour = 3600000;
    protected static final long MilliSecondsInHalfHour = 1800000;
    private final SingleEntryLoadingStatus loadingStatus = new SingleEntryLoadingStatus();
    protected boolean isLoading = false;
    protected long lastInvalidatedTick = 0;
    protected Date lastRefreshTime;
    protected long lifetime = MilliSecondsInADay;
    protected IDataLoaderRunnable<T> loaderRunnable;

    public boolean shouldRefresh() {
        return shouldRefresh(this.lastRefreshTime);
    }

    public boolean hasValidData() {
        return this.lastRefreshTime != null;
    }

    public boolean shouldRefresh(Date date) {
        return XLEUtil.shouldRefresh(date, this.lifetime);
    }

    public boolean isLoaded() {
        return this.lastRefreshTime != null;
    }

    public void updateWithNewData(@NotNull AsyncResult<T> asyncResult) {
        this.isLoading = false;
        if (asyncResult.getException() == null && asyncResult.getStatus() == AsyncActionStatus.SUCCESS) {
            this.lastRefreshTime = new Date();
        }
    }

    public boolean getIsLoading() {
        return this.loadingStatus.getIsLoading();
    }

    public void invalidateData() {
        this.lastRefreshTime = null;
    }

    public AsyncResult<T> loadData(boolean z, IDataLoaderRunnable<T> iDataLoaderRunnable) {
        XLEAssert.assertIsNotUIThread();
        return DataLoadUtil.Load(z, this.lifetime, this.lastRefreshTime, this.loadingStatus, iDataLoaderRunnable);
    }

    public void loadInternal(boolean z, UpdateType updateType, IDataLoaderRunnable<T> iDataLoaderRunnable) {
        loadInternal(z, updateType, iDataLoaderRunnable, this.lastRefreshTime);
    }

    public void loadInternal(boolean z, UpdateType updateType, IDataLoaderRunnable<T> iDataLoaderRunnable, Date date) {
        XLEAssert.assertIsUIThread();
        if (getIsLoading() || (!z && !shouldRefresh(date))) {
            notifyObservers(new AsyncResult(new UpdateData(updateType, !getIsLoading()), this, null));
            return;
        }
        DataLoadUtil.StartLoadFromUI(z, this.lifetime, this.lastRefreshTime, this.loadingStatus, iDataLoaderRunnable);
        notifyObservers(new AsyncResult(new UpdateData(updateType, false), this, null));
    }
}
