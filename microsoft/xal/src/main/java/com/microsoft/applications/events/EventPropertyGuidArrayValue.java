package com.microsoft.applications.events;

import java.util.UUID;

/**
 * @author <a href="https://github.com/RadiantByte">RadiantByte</a>
 */

public class EventPropertyGuidArrayValue extends EventPropertyValue {
    private String[] m_value;

    public EventPropertyGuidArrayValue(UUID[] uuidArr) {
        super(EventPropertyType.TYPE_GUID_ARRAY);
        if (uuidArr == null || uuidArr.length == 0) {
            throw new IllegalArgumentException("value is null or empty");
        }
        this.m_value = new String[uuidArr.length];
        for (int i = 0; i < uuidArr.length; i++) {
            UUID uuid = uuidArr[i];
            if (uuid == null) {
                throw new IllegalArgumentException("UUID value is null for array index:" + i);
            }
            this.m_value[i] = uuid.toString();
        }
    }

    @Override
    public String[] getGuidArray() {
        return this.m_value;
    }
}
