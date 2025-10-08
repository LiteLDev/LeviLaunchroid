package com.microsoft.applications.events;

/**
 * @author <a href="https://github.com/RadiantByte">RadiantByte</a>
 */

public enum ActionType {
    Unspecified(0),
    Unknown(1),
    Other(2),
    Click(3),
    Pan(5),
    Zoom(6),
    Hover(7);

    private final int m_value;

    ActionType(int i) {
        this.m_value = i;
    }

    int getValue() {
        return this.m_value;
    }
}