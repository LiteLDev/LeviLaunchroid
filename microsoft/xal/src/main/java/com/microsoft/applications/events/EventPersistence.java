package com.microsoft.applications.events;

/**
 * @author <a href="https://github.com/RadiantByte">RadiantByte</a>
 */

public enum EventPersistence {
    Normal(1),
    Critical(2),
    DoNotStoreOnDisk(3);

    private final int m_value;

    EventPersistence(int i) {
        this.m_value = i;
    }

    int getValue() {
        return this.m_value;
    }
}
