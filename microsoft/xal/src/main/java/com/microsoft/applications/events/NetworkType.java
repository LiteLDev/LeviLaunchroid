package com.microsoft.applications.events;

/**
 * @author <a href="https://github.com/RadiantByte">RadiantByte</a>
 */

public enum NetworkType {
    Any(-1),
    Unknown(0),
    Wired(1),
    Wifi(2),
    WWAN(3);

    private final int m_value;

    NetworkType(int i) {
        this.m_value = i;
    }

    int getValue() {
        return this.m_value;
    }
}
