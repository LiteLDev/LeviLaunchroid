package com.microsoft.applications.events;

/**
 * @author <a href="https://github.com/RadiantByte">RadiantByte</a>
 */

public class EventPropertyBooleanValue extends EventPropertyValue {
    private boolean m_value;

    public EventPropertyBooleanValue(boolean z) {
        super(EventPropertyType.TYPE_BOOLEAN);
        this.m_value = z;
    }

    @Override
    public boolean getBoolean() {
        return this.m_value;
    }
}
