package com.microsoft.applications.events;

/**
 * @author <a href="https://github.com/RadiantByte">RadiantByte</a>
 */

public class EventPropertyLongValue extends EventPropertyValue {
    private long m_value;

    public EventPropertyLongValue(long j) {
        super(EventPropertyType.TYPE_LONG);
        this.m_value = j;
    }

    @Override
    public long getLong() {
        return this.m_value;
    }
}
