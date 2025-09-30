package com.microsoft.applications.events;

/**
 * @author <a href="https://github.com/RadiantByte">RadiantByte</a>
 */

public enum UserState {
    Unknown(0),
    Connected(1),
    Reachable(2),
    SignedIn(3),
    SignedOut(4);

    private final int m_value;

    UserState(int i) {
        this.m_value = i;
    }

    int getValue() {
        return this.m_value;
    }
}