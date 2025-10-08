package com.microsoft.applications.events;

/**
 * @author <a href="https://github.com/RadiantByte">RadiantByte</a>
 */

public enum AppLifecycleState {
    Unknown(0),
    Launch(1),
    Exit(2),
    Suspend(3),
    Resume(4),
    Foreground(5),
    Background(6);

    private final int m_value;

    AppLifecycleState(int i) {
        this.m_value = i;
    }

    int getValue() {
        return this.m_value;
    }
}
