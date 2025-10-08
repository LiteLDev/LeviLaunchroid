package com.microsoft.applications.events;

/**
 * @author <a href="https://github.com/RadiantByte">RadiantByte</a>
 */

public class EventPropertyStringArrayValue extends EventPropertyValue {
    private String[] m_value;

    public EventPropertyStringArrayValue(String[] strArr) {
        super(EventPropertyType.TYPE_STRING_ARRAY);
        if (strArr == null || strArr.length == 0) {
            throw new IllegalArgumentException("value is null or empty");
        }
        this.m_value = new String[strArr.length];
        for (int i = 0; i < strArr.length; i++) {
            String str = strArr[i];
            if (str == null) {
                throw new IllegalArgumentException("String value is null for array index:" + i);
            }
            this.m_value[i] = str;
        }
    }

    @Override
    public String[] getStringArray() {
        return this.m_value;
    }
}
