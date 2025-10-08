package com.microsoft.applications.events;

/**
 * @author <a href="https://github.com/RadiantByte">RadiantByte</a>
 */

public enum PrivacyDiagnosticTag {
    BrowsingHistory(2),
    DeviceConnectivityAndConfiguration(2048),
    InkingTypingAndSpeechUtterance(131072),
    ProductAndServicePerformance(16777216),
    ProductAndServiceUsage(33554432),
    SoftwareSetupAndInventory(2147483648L);

    private final long m_value;

    PrivacyDiagnosticTag(long j) {
        this.m_value = j;
    }

    public long getValue() {
        return this.m_value;
    }
}
