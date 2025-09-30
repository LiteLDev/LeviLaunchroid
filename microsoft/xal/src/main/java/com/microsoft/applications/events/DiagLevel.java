package com.microsoft.applications.events;

import com.android.billingclient.api.ProxyBillingActivity;

/**
 * @author <a href="https://github.com/RadiantByte">RadiantByte</a>
 */

public enum DiagLevel {
    DIAG_LEVEL_REQUIRED(1),
    DIAG_LEVEL_OPTIONAL(2),
    DIAG_LEVEL_RSDES(120);

    private int value;

    DiagLevel(int i) {
        this.value = i;
    }

    public int value() {
        return this.value;
    }
}
