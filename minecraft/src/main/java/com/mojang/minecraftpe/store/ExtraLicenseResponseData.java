package com.mojang.minecraftpe.store;

/**
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */
public class ExtraLicenseResponseData {
    private long mRetryAttempts = 0;
    private long mRetryUntilTime = 0;
    private long mValidationTime = 0;

    public ExtraLicenseResponseData(long validationTime, long retryUntilTime, long retryAttempts) {
        mValidationTime = validationTime;
        mRetryUntilTime = retryUntilTime;
        mRetryAttempts = retryAttempts;
    }

    public long getValidationTime() {
        return mValidationTime;
    }

    public long getRetryUntilTime() {
        return mRetryUntilTime;
    }

    public long getRetryAttempts() {
        return mRetryAttempts;
    }
}