package com.microsoft.applications.events;

/**
 * @author <a href="https://github.com/RadiantByte">RadiantByte</a>
 */

public enum AggregateType {
    Sum(0),
    Maximum(1),
    Minimum(2),
    SumOfSquares(3);

    private final int m_value;

    AggregateType(int i) {
        this.m_value = i;
    }

    int getValue() {
        return this.m_value;
    }
}
