package com.microsoft.applications.events;

/**
 * @author <a href="https://github.com/RadiantByte">RadiantByte</a>
 */

public enum NetworkCost {
    Any(1),
    Unknown(0),
    Unmetered(1),
    Metered(2),
    Roaming(3);

    private final int m_value;

    NetworkCost(int i) {
        this.m_value = i;
    }

    int getValue() {
        return this.m_value;
    }
}
