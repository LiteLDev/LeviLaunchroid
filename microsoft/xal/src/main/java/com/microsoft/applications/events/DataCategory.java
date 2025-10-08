package com.microsoft.applications.events;

/**
 * @author <a href="https://github.com/RadiantByte">RadiantByte</a>
 */

public enum DataCategory {
    PartC(0),
    PartB(1),
    MAX(2);

    private final int m_value;

    DataCategory(int i) {
        this.m_value = i;
    }

    public int getValue() {
        return this.m_value;
    }
}
