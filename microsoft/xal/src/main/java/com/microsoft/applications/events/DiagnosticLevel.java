package com.microsoft.applications.events;

import com.android.billingclient.api.ProxyBillingActivity;

/**
 * @author <a href="https://github.com/RadiantByte">RadiantByte</a>
 */

public enum DiagnosticLevel {
    DIAG_LEVEL_REQUIRED(1),
    DIAG_LEVEL_OPTIONAL(2),
    DIAG_LEVEL_RSDES(120);

    private final int m_value;

    DiagnosticLevel(int i) {
        this.m_value = i;
    }

    int getValue() {
        return this.m_value;
    }
}
