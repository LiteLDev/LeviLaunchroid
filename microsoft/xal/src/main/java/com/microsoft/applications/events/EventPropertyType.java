package com.microsoft.applications.events;

/**
 * @author <a href="https://github.com/RadiantByte">RadiantByte</a>
 */

enum EventPropertyType {
    TYPE_STRING(0),
    TYPE_LONG(1),
    TYPE_DOUBLE(2),
    TYPE_TIME(3),
    TYPE_BOOLEAN(4),
    TYPE_GUID(5),
    TYPE_STRING_ARRAY(6),
    TYPE_LONG_ARRAY(7),
    TYPE_DOUBLE_ARRAY(8),
    TYPE_GUID_ARRAY(9);

    private final int m_value;

    EventPropertyType(int i) {
        this.m_value = i;
    }

    int getValue() {
        return this.m_value;
    }
}
