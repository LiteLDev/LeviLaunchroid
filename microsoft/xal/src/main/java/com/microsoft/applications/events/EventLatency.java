package com.microsoft.applications.events;

/**
 * @author <a href="https://github.com/RadiantByte">RadiantByte</a>
 */

public enum EventLatency {
    Unspecified(-1),
    Off(0),
    Normal(1),
    CostDeferred(2),
    RealTime(3),
    Max(4);

    private final int m_value;

    EventLatency(int i) {
        this.m_value = i;
    }

    int getValue() {
        return this.m_value;
    }

    static EventLatency getEnum(int i) {
        if (i == -1) {
            return Unspecified;
        }
        if (i == 0) {
            return Off;
        }
        if (i == 1) {
            return Normal;
        }
        if (i == 2) {
            return CostDeferred;
        }
        if (i == 3) {
            return RealTime;
        }
        if (i == 4) {
            return Max;
        }
        throw new IllegalArgumentException("Unsupported value: " + i);
    }
}
