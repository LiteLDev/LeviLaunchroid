package com.microsoft.applications.events;

/**
 * @author <a href="https://github.com/RadiantByte">RadiantByte</a>
 */

public enum TransmitProfile {
    RealTime(0),
    NearRealTime(1),
    BestEffort(2);

    private final int m_value;

    TransmitProfile(int i) {
        this.m_value = i;
    }

    int getValue() {
        return this.m_value;
    }
}