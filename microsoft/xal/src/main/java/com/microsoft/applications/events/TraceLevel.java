package com.microsoft.applications.events;

/**
 * @author <a href="https://github.com/RadiantByte">RadiantByte</a>
 */

public enum TraceLevel {
    None(0),
    Error(1),
    Warning(2),
    Information(3),
    Verbose(4);

    private final int m_value;

    TraceLevel(int i) {
        this.m_value = i;
    }

    int getValue() {
        return this.m_value;
    }
}