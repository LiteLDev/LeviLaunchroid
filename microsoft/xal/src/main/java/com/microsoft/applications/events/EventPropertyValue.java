package com.microsoft.applications.events;

/**
 * @author <a href="https://github.com/RadiantByte">RadiantByte</a>
 */

public abstract class EventPropertyValue {
    private EventPropertyType m_type;

    EventPropertyValue(EventPropertyType eventPropertyType) {
        this.m_type = eventPropertyType;
    }

    public int getType() {
        return this.m_type.getValue();
    }

    public String getString() {
        throw new UnsupportedOperationException();
    }

    public long getLong() {
        throw new UnsupportedOperationException();
    }

    public double getDouble() {
        throw new UnsupportedOperationException();
    }

    public boolean getBoolean() {
        throw new UnsupportedOperationException();
    }

    public String getGuid() {
        throw new UnsupportedOperationException();
    }

    public long getTimeTicks() {
        throw new UnsupportedOperationException();
    }

    public String[] getStringArray() {
        throw new UnsupportedOperationException();
    }

    public long[] getLongArray() {
        throw new UnsupportedOperationException();
    }

    public double[] getDoubleArray() {
        throw new UnsupportedOperationException();
    }

    public String[] getGuidArray() {
        throw new UnsupportedOperationException();
    }
}
