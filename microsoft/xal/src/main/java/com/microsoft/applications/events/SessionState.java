package com.microsoft.applications.events;

/**
 * @author <a href="https://github.com/RadiantByte">RadiantByte</a>
 */

public enum SessionState {
    Started(0),
    Ended(1);

    private final int m_value;

    SessionState(int i) {
        this.m_value = i;
    }

    int getValue() {
        return this.m_value;
    }
}
