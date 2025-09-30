package com.microsoft.applications.events;

/**
 * @author <a href="https://github.com/RadiantByte">RadiantByte</a>
 */

public class EventPropertyLongArrayValue extends EventPropertyValue {
    private long[] m_value;

    public EventPropertyLongArrayValue(long[] jArr) {
        super(EventPropertyType.TYPE_LONG_ARRAY);
        if (jArr == null || jArr.length == 0) {
            throw new IllegalArgumentException("value is null or empty");
        }
        this.m_value = jArr;
    }

    @Override
    public long[] getLongArray() {
        return this.m_value;
    }
}
