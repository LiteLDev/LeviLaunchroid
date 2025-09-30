package com.microsoft.applications.events;

/**
 * @author <a href="https://github.com/RadiantByte">RadiantByte</a>
 */

public class EventPropertyDoubleArrayValue extends EventPropertyValue {
    private double[] m_value;

    public EventPropertyDoubleArrayValue(double[] dArr) {
        super(EventPropertyType.TYPE_DOUBLE_ARRAY);
        if (dArr == null || dArr.length == 0) {
            throw new IllegalArgumentException("value is null or empty");
        }
        this.m_value = dArr;
    }

    @Override
    public double[] getDoubleArray() {
        return this.m_value;
    }
}
