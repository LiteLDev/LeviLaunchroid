package com.microsoft.applications.events;

/**
 * @author <a href="https://github.com/RadiantByte">RadiantByte</a>
 */

public class EventPropertyTimeTicksValue extends EventPropertyValue {
    private long m_value;

    public EventPropertyTimeTicksValue(long j) {
        super(EventPropertyType.TYPE_TIME);
        this.m_value = j;
    }

    @Override
    public long getTimeTicks() {
        return this.m_value;
    }
}
