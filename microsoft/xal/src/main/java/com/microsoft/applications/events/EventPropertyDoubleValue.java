package com.microsoft.applications.events;

/**
 * @author <a href="https://github.com/RadiantByte">RadiantByte</a>
 */

public class EventPropertyDoubleValue extends EventPropertyValue {
    private double m_value;

    public EventPropertyDoubleValue(double d) {
        super(EventPropertyType.TYPE_DOUBLE);
        this.m_value = d;
    }

    @Override
    public double getDouble() {
        return this.m_value;
    }
}
