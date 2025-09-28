package com.microsoft.xbox.toolkit;

/**
 * 07.01.2021
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */

public class SingleEntryLoadingStatus {
    private final Object syncObj = new Object();
    private boolean isLoading = false;
    private XLEException lastError = null;

    public boolean getIsLoading() {
        return this.isLoading;
    }

    public XLEException getLastError() {
        return this.lastError;
    }

    public void setSuccess() {
        setDone(null);
    }

    public void setFailed(XLEException xLEException) {
        setDone(xLEException);
    }

    private void setDone(XLEException xLEException) {
        synchronized (this.syncObj) {
            this.isLoading = false;
            this.lastError = xLEException;
            this.syncObj.notifyAll();
        }
    }

    public WaitResult waitForNotLoading() {
        synchronized (this.syncObj) {
            if (this.isLoading) {
                try {
                    this.syncObj.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Thread.currentThread().interrupt();
                WaitResult waitResult = new WaitResult(true, this.lastError);
                return waitResult;
            }
            this.isLoading = true;
            WaitResult waitResult2 = new WaitResult(false, null);
            return waitResult2;
        }
    }

    public void reset() {
        synchronized (this.syncObj) {
            this.isLoading = false;
            this.lastError = null;
            this.syncObj.notifyAll();
        }
    }

    public class WaitResult {
        public XLEException error;
        public boolean waited;

        public WaitResult(boolean z, XLEException xLEException) {
            this.waited = z;
            this.error = xLEException;
        }
    }
}
