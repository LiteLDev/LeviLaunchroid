package com.microsoft.applications.events;

import java.util.UUID;

/**
 * @author <a href="https://github.com/RadiantByte">RadiantByte</a>
 */

public class EventPropertyGuidValue extends EventPropertyValue {
    private String m_value;

    public EventPropertyGuidValue(UUID uuid) {
        super(EventPropertyType.TYPE_GUID);
        if (uuid == null) {
            throw new IllegalArgumentException("value is null");
        }
        this.m_value = uuid.toString();
    }

    @Override
    public String getGuid() {
        return this.m_value;
    }
}
