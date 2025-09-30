package com.microsoft.applications.events;

/**
 * @author <a href="https://github.com/RadiantByte">RadiantByte</a>
 */

public class EventPropertyStringValue extends EventPropertyValue {
    private String m_value;

    public EventPropertyStringValue(String str) {
        super(EventPropertyType.TYPE_STRING);
        if (str == null) {
            throw new IllegalArgumentException("value is null");
        }
        this.m_value = str;
    }

    @Override
    public String getString() {
        return this.m_value;
    }
}
